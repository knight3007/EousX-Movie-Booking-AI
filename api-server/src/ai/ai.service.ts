import { Injectable, InternalServerErrorException } from '@nestjs/common';
import { GoogleGenAI } from '@google/genai';
import { Prisma, ShowtimeStatus } from '@prisma/client';
import { PrismaService } from '../database/prisma.service';
import {
  AiChatResponse,
  AiNextActionType,
} from './types/ai-chat-response.type';

const GEMINI_MODEL = 'gemini-2.5-flash';
const MAX_CONTEXT_MOVIES = 10;
const MAX_SHOWTIMES_PER_MOVIE = 5;
const FALLBACK_MESSAGE =
  'Hiện tại mình chưa thể phân tích yêu cầu này, nhưng bạn có thể tham khảo các phim có suất chiếu đang mở bên dưới.';
const NO_OPEN_SHOWTIMES_MESSAGE =
  'Hiện tại rạp chưa có suất chiếu nào đang mở.';
const NO_SELECTED_SHOWTIMES_MESSAGE =
  'Hôm nay rạp chưa có suất chiếu nào đang mở. Mình gợi ý bạn xem các suất sắp tới bên dưới.';

type ShowtimeWithMovieAndRoom = Prisma.ShowtimeGetPayload<{
  include: {
    movie: true;
    room: true;
  };
}>;

interface AvailableShowtime {
  showtimeId: string;
  startTime: string;
  endTime: string;
  roomName: string;
  roomType: string;
  basePrice: number;
}

interface AvailableMovie {
  movieId: string;
  title: string;
  overview: string;
  genres: string[];
  ageRating: string;
  runtime: number;
  rating: number;
  status: string;
  showtimes: AvailableShowtime[];
  firstStartTimeMs: number;
}

interface TimeIntent {
  label: string;
  range: {
    start: Date;
    end: Date;
  };
  flags: {
    hasTodayIntent: boolean;
    hasSpecificPartOfDay: boolean;
    hasWeekendIntent: boolean;
    hasCurrentlyShowingIntent: boolean;
    hasRecommendationIntent: boolean;
  };
}

interface UserIntent {
  time: {
    label: string;
    start: string;
    end: string;
    hasTodayIntent: boolean;
    hasSpecificPartOfDay: boolean;
    hasWeekendIntent: boolean;
    hasCurrentlyShowingIntent: boolean;
    hasRecommendationIntent: boolean;
  };
  moods: string[];
  genres: string[];
  inferredPreferences: string[];
}

interface GeminiMovieRecommendation {
  movieId?: unknown;
  reason?: unknown;
}

interface GeminiChatResponse {
  message?: unknown;
  recommendedMovies?: unknown;
  nextAction?: unknown;
}

@Injectable()
export class AiService {
  constructor(private readonly prisma: PrismaService) {}

  async chat(userId: string, message: string): Promise<AiChatResponse> {
    const apiKey = process.env.GEMINI_API_KEY;

    if (!apiKey) {
      throw new InternalServerErrorException(
        'GEMINI_API_KEY is not configured',
      );
    }

    const timeIntent = this.detectTimeIntent(message);
    const userIntent = this.detectUserIntent(message, timeIntent);
    const availableMovies = await this.loadAvailableMovies(timeIntent);

    if (availableMovies.length === 0) {
      const upcomingMovies = await this.loadUpcomingAvailableMovies();

      if (upcomingMovies.length === 0) {
        return this.buildNoShowtimesResponse();
      }

      return this.buildFallbackResponse(
        upcomingMovies,
        NO_SELECTED_SHOWTIMES_MESSAGE,
      );
    }

    const userContext = await this.loadUserContext(userId);
    const prompt = this.buildPrompt(
      timeIntent,
      userIntent,
      availableMovies,
      userContext,
      message,
    );

    try {
      const ai = new GoogleGenAI({ apiKey });
      const response = await ai.models.generateContent({
        model: GEMINI_MODEL,
        contents: prompt,
        config: {
          temperature: 0.35,
          responseMimeType: 'application/json',
        },
      });

      const parsed = this.parseGeminiJson(response.text ?? '');

      if (!parsed) {
        return this.buildFallbackResponse(availableMovies);
      }

      return this.sanitizeGeminiResponse(parsed, availableMovies);
    } catch {
      return this.buildFallbackResponse(availableMovies);
    }
  }

  private detectTimeIntent(message: string): TimeIntent {
    const normalizedMessage = this.normalizeMessage(message);
    const now = new Date();
    const hasTonightIntent = this.containsAny(normalizedMessage, [
      'toi nay',
      'dem nay',
      'buoi toi',
      'tonight',
    ]);
    const hasMorningIntent = this.containsAny(normalizedMessage, [
      'sang nay',
      'buoi sang',
      'morning',
    ]);
    const hasAfternoonIntent = this.containsAny(normalizedMessage, [
      'chieu nay',
      'buoi chieu',
      'afternoon',
    ]);
    const hasTodayIntent =
      hasTonightIntent ||
      hasMorningIntent ||
      hasAfternoonIntent ||
      this.containsAny(normalizedMessage, [
        'hom nay',
        'today',
        'trua nay',
        'dem nay',
      ]);
    const hasWeekendIntent = this.containsAny(normalizedMessage, [
      'cuoi tuan',
      'weekend',
      'thu 7',
      'thu bay',
      'chu nhat',
    ]);
    const hasCurrentlyShowingIntent = this.containsAny(normalizedMessage, [
      'dang chieu',
      'rap dang chieu',
      'co phim nao',
      'phim nao dang co',
    ]);
    const hasRecommendationIntent = this.containsAny(normalizedMessage, [
      'goi y',
      'recommend',
      'nen xem',
      'phu hop',
      'muon xem',
    ]);

    if (hasWeekendIntent) {
      return this.buildTimeIntent('weekend', this.getWeekendRange(now), {
        hasTodayIntent,
        hasSpecificPartOfDay: false,
        hasWeekendIntent,
        hasCurrentlyShowingIntent,
        hasRecommendationIntent,
      });
    }

    if (hasTonightIntent) {
      return this.buildTimeIntent(
        'tonight',
        {
          start: this.withLocalTime(now, 18, 0, 0, 0),
          end: this.startOfTomorrow(now),
        },
        {
          hasTodayIntent: true,
          hasSpecificPartOfDay: true,
          hasWeekendIntent,
          hasCurrentlyShowingIntent,
          hasRecommendationIntent,
        },
      );
    }

    if (hasAfternoonIntent) {
      return this.buildTimeIntent(
        'afternoon',
        {
          start: this.withLocalTime(now, 12, 0, 0, 0),
          end: this.withLocalTime(now, 18, 0, 0, 0),
        },
        {
          hasTodayIntent: true,
          hasSpecificPartOfDay: true,
          hasWeekendIntent,
          hasCurrentlyShowingIntent,
          hasRecommendationIntent,
        },
      );
    }

    if (hasMorningIntent) {
      return this.buildTimeIntent(
        'morning',
        {
          start: this.startOfDay(now),
          end: this.withLocalTime(now, 12, 0, 0, 0),
        },
        {
          hasTodayIntent: true,
          hasSpecificPartOfDay: true,
          hasWeekendIntent,
          hasCurrentlyShowingIntent,
          hasRecommendationIntent,
        },
      );
    }

    if (hasTodayIntent) {
      return this.buildTimeIntent(
        'today',
        {
          start: this.startOfDay(now),
          end: this.startOfTomorrow(now),
        },
        {
          hasTodayIntent: true,
          hasSpecificPartOfDay: false,
          hasWeekendIntent,
          hasCurrentlyShowingIntent,
          hasRecommendationIntent,
        },
      );
    }

    return this.buildTimeIntent(
      hasCurrentlyShowingIntent ? 'currently_showing' : 'next_7_days',
      {
        start: now,
        end: this.addDays(now, 7),
      },
      {
        hasTodayIntent: false,
        hasSpecificPartOfDay: false,
        hasWeekendIntent,
        hasCurrentlyShowingIntent,
        hasRecommendationIntent,
      },
    );
  }

  private detectUserIntent(message: string, timeIntent: TimeIntent): UserIntent {
    const normalizedMessage = this.normalizeMessage(message);
    const moods = this.collectKeywordMatches(normalizedMessage, [
      'vui',
      'hai',
      'nhe nhang',
      'thu gian',
      'chua lanh',
      'cam dong',
      'buon',
      'lang man',
      'hoi hop',
      'cang thang',
      'kich tinh',
      'so',
      'kinh di',
      'hanh dong',
      'gia dinh',
      'tre em',
      'di cung ban be',
      'di cung nguoi yeu',
      'di cung gia dinh',
    ]);
    const genres = this.collectKeywordMatches(normalizedMessage, [
      'hai',
      'hoat hinh',
      'hanh dong',
      'kinh di',
      'tinh cam',
      'phieu luu',
      'gia dinh',
      'anime',
      'drama',
      'sci-fi',
      'fantasy',
    ]);
    const inferredPreferences = new Set<string>();

    if (this.containsAny(normalizedMessage, ['vui', 'hai', 'giai tri'])) {
      ['Comedy', 'Animation', 'Family'].forEach((item) =>
        inferredPreferences.add(item),
      );
    }

    if (
      this.containsAny(normalizedMessage, [
        'nhe nhang',
        'chua lanh',
        'thu gian',
      ])
    ) {
      ['Animation', 'Family', 'Romance', 'Drama'].forEach((item) =>
        inferredPreferences.add(item),
      );
    }

    if (
      this.containsAny(normalizedMessage, [
        'hoi hop',
        'cang thang',
        'kich tinh',
      ])
    ) {
      ['Thriller', 'Action', 'Adventure'].forEach((item) =>
        inferredPreferences.add(item),
      );
    }

    if (this.containsAny(normalizedMessage, ['so', 'kinh di'])) {
      ['Horror', 'Thriller'].forEach((item) => inferredPreferences.add(item));
    }

    if (
      this.containsAny(normalizedMessage, [
        'di voi gia dinh',
        'di cung gia dinh',
        'tre em',
      ])
    ) {
      ['Animation', 'Family', 'ageRating-safe'].forEach((item) =>
        inferredPreferences.add(item),
      );
    }

    if (
      this.containsAny(normalizedMessage, [
        'di voi nguoi yeu',
        'di cung nguoi yeu',
      ])
    ) {
      ['Romance', 'Drama', 'Comedy'].forEach((item) =>
        inferredPreferences.add(item),
      );
    }

    if (
      this.containsAny(normalizedMessage, [
        'ban be',
        'di voi ban be',
        'di cung ban be',
      ])
    ) {
      ['Comedy', 'Action', 'Adventure', 'Animation'].forEach((item) =>
        inferredPreferences.add(item),
      );
    }

    return {
      time: {
        label: timeIntent.label,
        start: timeIntent.range.start.toISOString(),
        end: timeIntent.range.end.toISOString(),
        ...timeIntent.flags,
      },
      moods,
      genres,
      inferredPreferences: Array.from(inferredPreferences),
    };
  }

  private async loadAvailableMovies(
    timeIntent: TimeIntent,
  ): Promise<AvailableMovie[]> {
    return this.loadAvailableMoviesFromRange(
      timeIntent.range.start,
      timeIntent.range.end,
    );
  }

  private async loadUpcomingAvailableMovies(): Promise<AvailableMovie[]> {
    const now = new Date();

    return this.loadAvailableMoviesFromRange(now, this.addDays(now, 7));
  }

  private async loadAvailableMoviesFromRange(
    start: Date,
    end: Date,
  ): Promise<AvailableMovie[]> {
    const showtimes = await this.prisma.showtime.findMany({
      where: {
        status: ShowtimeStatus.OPEN,
        startTime: {
          gte: start,
          lt: end,
        },
      },
      include: {
        movie: true,
        room: true,
      },
      orderBy: {
        startTime: 'asc',
      },
    });

    return this.groupShowtimesByMovie(showtimes);
  }

  private groupShowtimesByMovie(
    showtimes: ShowtimeWithMovieAndRoom[],
  ): AvailableMovie[] {
    const moviesById = new Map<string, AvailableMovie>();

    for (const showtime of showtimes) {
      const existingMovie = moviesById.get(showtime.movieId);
      const availableShowtime: AvailableShowtime = {
        showtimeId: showtime.id,
        startTime: showtime.startTime.toISOString(),
        endTime: showtime.endTime.toISOString(),
        roomName: showtime.room.name,
        roomType: showtime.room.type,
        basePrice: showtime.basePrice,
      };

      if (!existingMovie) {
        moviesById.set(showtime.movieId, {
          movieId: showtime.movie.id,
          title: showtime.movie.title,
          overview: showtime.movie.overview,
          genres: showtime.movie.genres,
          ageRating: showtime.movie.ageRating,
          runtime: showtime.movie.runtime,
          rating: showtime.movie.rating,
          status: showtime.movie.status,
          showtimes: [availableShowtime],
          firstStartTimeMs: showtime.startTime.getTime(),
        });
        continue;
      }

      if (existingMovie.showtimes.length < MAX_SHOWTIMES_PER_MOVIE) {
        existingMovie.showtimes.push(availableShowtime);
      }

      existingMovie.firstStartTimeMs = Math.min(
        existingMovie.firstStartTimeMs,
        showtime.startTime.getTime(),
      );
    }

    return Array.from(moviesById.values())
      .sort((a, b) => {
        const timeCompare = a.firstStartTimeMs - b.firstStartTimeMs;

        if (timeCompare !== 0) {
          return timeCompare;
        }

        return b.rating - a.rating;
      })
      .slice(0, MAX_CONTEXT_MOVIES);
  }

  private async loadUserContext(userId: string) {
    const recentBookings = await this.prisma.booking.findMany({
      where: {
        userId,
      },
      select: {
        status: true,
        showtime: {
          select: {
            startTime: true,
            movie: {
              select: {
                title: true,
                genres: true,
              },
            },
          },
        },
        seats: {
          select: {
            id: true,
          },
        },
      },
      orderBy: {
        createdAt: 'desc',
      },
      take: 10,
    });
    const recentGenres = new Set<string>();
    const recentMovieTitles = new Set<string>();

    for (const booking of recentBookings) {
      booking.showtime.movie.genres.forEach((genre) => recentGenres.add(genre));
      recentMovieTitles.add(booking.showtime.movie.title);
    }

    return {
      recentGenres: Array.from(recentGenres),
      recentMovieTitles: Array.from(recentMovieTitles),
      recentBookingCount: recentBookings.length,
      recentBookings: recentBookings.map((booking) => ({
        movieTitle: booking.showtime.movie.title,
        genres: booking.showtime.movie.genres,
        bookingStatus: booking.status,
        showtimeStartTime: booking.showtime.startTime.toISOString(),
        seatsCount: booking.seats.length,
      })),
    };
  }

  private buildPrompt(
    timeIntent: TimeIntent,
    userIntent: UserIntent,
    availableMovies: AvailableMovie[],
    userContext: unknown,
    userMessage: string,
  ) {
    const availableMovieContext = availableMovies.map((movie) => ({
      movieId: movie.movieId,
      title: movie.title,
      overview: movie.overview,
      genres: movie.genres,
      ageRating: movie.ageRating,
      runtime: movie.runtime,
      rating: movie.rating,
      status: movie.status,
      showtimes: movie.showtimes,
    }));
    const timeContext = {
      label: timeIntent.label,
      start: timeIntent.range.start.toISOString(),
      end: timeIntent.range.end.toISOString(),
      note:
        'AVAILABLE_MOVIES are derived from OPEN showtimes inside this selected time range.',
    };

    return `You are the AI movie assistant for EousX cinema.

Hard rules:
You may recommend ONLY movies listed in AVAILABLE_MOVIES.
AVAILABLE_MOVIES are movies that have actual OPEN showtimes in the selected time range.
Do not invent movies.
Do not invent movieId.
Do not recommend movies outside AVAILABLE_MOVIES.
Do not recommend a movie if it has no showtime in AVAILABLE_MOVIES.
If user asks about today, answer only from today's showtimes.
If the user asks for a movie that is not in AVAILABLE_MOVIES, say EousX currently does not have that movie in the selected time range and recommend available alternatives.
If no perfect match, recommend the closest available movie and explain why.
Return only valid JSON.
Do not wrap response in markdown.

TIME_CONTEXT:
${JSON.stringify(timeContext)}

USER_INTENT:
${JSON.stringify(userIntent)}

AVAILABLE_MOVIES:
${JSON.stringify(availableMovieContext)}

CURRENT_USER_CONTEXT:
${JSON.stringify(userContext)}

USER_MESSAGE:
${userMessage}

Expected JSON:
{
  "message": "string",
  "recommendedMovies": [
    {
      "movieId": "string",
      "reason": "string"
    }
  ],
  "nextAction": {
    "type": "OPEN_MOVIE_DETAIL" | "OPEN_SHOWTIMES" | "NONE",
    "movieId": "string or null"
  }
}`;
  }

  private parseGeminiJson(rawText: string): GeminiChatResponse | null {
    const cleanedText = rawText
      .trim()
      .replace(/^```json\s*/i, '')
      .replace(/^```\s*/i, '')
      .replace(/\s*```$/i, '')
      .trim();

    try {
      return JSON.parse(cleanedText) as GeminiChatResponse;
    } catch {
      return null;
    }
  }

  private sanitizeGeminiResponse(
    parsed: GeminiChatResponse,
    availableMovies: AvailableMovie[],
  ): AiChatResponse {
    const moviesById = new Map(
      availableMovies.map((movie) => [movie.movieId, movie]),
    );
    const rawRecommendations = Array.isArray(parsed.recommendedMovies)
      ? (parsed.recommendedMovies as GeminiMovieRecommendation[])
      : [];
    const seenMovieIds = new Set<string>();

    const recommendedMovies = rawRecommendations
      .map((recommendation) => {
        if (typeof recommendation.movieId !== 'string') {
          return null;
        }

        const movie = moviesById.get(recommendation.movieId);

        if (!movie || seenMovieIds.has(movie.movieId)) {
          return null;
        }

        seenMovieIds.add(movie.movieId);

        return this.toRecommendedMovie(movie, recommendation.reason);
      })
      .filter((movie): movie is NonNullable<typeof movie> => movie !== null)
      .slice(0, 3);

    if (recommendedMovies.length === 0) {
      return this.buildFallbackResponse(availableMovies);
    }

    return {
      message:
        typeof parsed.message === 'string' && parsed.message.trim().length > 0
          ? parsed.message.trim()
          : 'Mình tìm thấy một vài phim phù hợp với suất chiếu đang mở tại EousX.',
      recommendedMovies,
      nextAction: this.sanitizeNextAction(parsed.nextAction, recommendedMovies),
    };
  }

  private sanitizeNextAction(
    rawNextAction: unknown,
    recommendedMovies: AiChatResponse['recommendedMovies'],
  ): AiChatResponse['nextAction'] {
    const firstRecommendedMovieId = recommendedMovies[0]?.movieId ?? null;

    if (!this.isRecord(rawNextAction)) {
      return firstRecommendedMovieId
        ? {
            type: 'OPEN_SHOWTIMES',
            movieId: firstRecommendedMovieId,
          }
        : {
            type: 'NONE',
            movieId: null,
          };
    }

    const rawType = rawNextAction.type;
    const rawMovieId = rawNextAction.movieId;
    const allowedTypes: AiNextActionType[] = [
      'OPEN_MOVIE_DETAIL',
      'OPEN_SHOWTIMES',
      'NONE',
    ];
    const type: AiNextActionType = allowedTypes.includes(
      rawType as AiNextActionType,
    )
      ? (rawType as AiNextActionType)
      : 'OPEN_SHOWTIMES';
    const movieId =
      typeof rawMovieId === 'string' &&
      recommendedMovies.some((movie) => movie.movieId === rawMovieId)
        ? rawMovieId
        : firstRecommendedMovieId;

    if (type === 'NONE' || !movieId) {
      return {
        type: 'NONE',
        movieId: null,
      };
    }

    return {
      type,
      movieId,
    };
  }

  private buildFallbackResponse(
    availableMovies: AvailableMovie[],
    message = FALLBACK_MESSAGE,
  ): AiChatResponse {
    const fallbackMovies = availableMovies
      .slice(0, 3)
      .map((movie) => this.toRecommendedMovie(movie));
    const firstMovieId = fallbackMovies[0]?.movieId ?? null;

    return {
      message,
      recommendedMovies: fallbackMovies,
      nextAction: firstMovieId
        ? {
            type: 'OPEN_SHOWTIMES',
            movieId: firstMovieId,
          }
        : {
            type: 'NONE',
            movieId: null,
          },
    };
  }

  private buildNoShowtimesResponse(): AiChatResponse {
    return {
      message: NO_OPEN_SHOWTIMES_MESSAGE,
      recommendedMovies: [],
      nextAction: {
        type: 'NONE',
        movieId: null,
      },
    };
  }

  private toRecommendedMovie(
    movie: AvailableMovie,
    reason?: unknown,
  ): AiChatResponse['recommendedMovies'][number] {
    return {
      movieId: movie.movieId,
      title: movie.title,
      reason:
        typeof reason === 'string' && reason.trim().length > 0
          ? reason.trim()
          : 'Phim có suất chiếu phù hợp trong rạp EousX.',
      showtimes: movie.showtimes.map((showtime) => ({
        showtimeId: showtime.showtimeId,
        startTime: showtime.startTime,
        roomName: showtime.roomName,
        roomType: showtime.roomType,
        basePrice: showtime.basePrice,
      })),
    };
  }

  private buildTimeIntent(
    label: string,
    range: { start: Date; end: Date },
    flags: TimeIntent['flags'],
  ): TimeIntent {
    return {
      label,
      range,
      flags,
    };
  }

  private getWeekendRange(now: Date) {
    const day = now.getDay();
    const isWeekend = day === 0 || day === 6;

    if (isWeekend) {
      const start = now;
      const end = this.startOfDay(this.addDays(now, day === 6 ? 2 : 1));

      return { start, end };
    }

    const daysUntilSaturday = 6 - day;
    const saturday = this.startOfDay(this.addDays(now, daysUntilSaturday));

    return {
      start: saturday,
      end: this.addDays(saturday, 2),
    };
  }

  private startOfDay(date: Date) {
    return this.withLocalTime(date, 0, 0, 0, 0);
  }

  private startOfTomorrow(date: Date) {
    return this.startOfDay(this.addDays(date, 1));
  }

  private addDays(date: Date, days: number) {
    const result = new Date(date);
    result.setDate(result.getDate() + days);

    return result;
  }

  private withLocalTime(
    date: Date,
    hours: number,
    minutes: number,
    seconds: number,
    milliseconds: number,
  ) {
    const result = new Date(date);
    result.setHours(hours, minutes, seconds, milliseconds);

    return result;
  }

  private normalizeMessage(message: string) {
    return message
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd')
      .replace(/\s+/g, ' ')
      .trim();
  }

  private containsAny(message: string, keywords: string[]) {
    return keywords.some((keyword) => message.includes(keyword));
  }

  private collectKeywordMatches(message: string, keywords: string[]) {
    return keywords.filter((keyword) => message.includes(keyword));
  }

  private isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
  }
}
