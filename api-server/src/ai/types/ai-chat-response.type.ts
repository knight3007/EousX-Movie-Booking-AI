export type AiNextActionType =
  | 'OPEN_MOVIE_DETAIL'
  | 'OPEN_SHOWTIMES'
  | 'NONE';

export interface AiRecommendedMovie {
  movieId: string;
  title: string;
  reason: string;
  showtimes: {
    showtimeId: string;
    startTime: string;
    roomName: string;
    roomType: string;
    basePrice: number;
  }[];
}

export interface AiChatResponse {
  message: string;
  recommendedMovies: AiRecommendedMovie[];
  nextAction: {
    type: AiNextActionType;
    movieId: string | null;
  };
}
