import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import {
  BookingStatus,
  Prisma,
  SeatLockStatus,
  SeatType,
  ShowtimeStatus,
} from '@prisma/client';
import { PrismaService } from '../database/prisma.service';
import { CreateShowtimeDto } from './dto/create-showtime.dto';
import { UpdateShowtimeDto } from './dto/update-showtime.dto';

@Injectable()
export class ShowtimesService {
  constructor(private readonly prisma: PrismaService) {}

  async findAll(date?: string) {
    const where: Prisma.ShowtimeWhereInput = {
      ...this.buildDateWhere(date),
    };

    return this.prisma.showtime.findMany({
      where,
      include: {
        movie: true,
        room: true,
      },
      orderBy: {
        startTime: 'asc',
      },
    });
  }

  async findOne(id: string) {
    const showtime = await this.prisma.showtime.findUnique({
      where: { id },
      include: {
        movie: true,
        room: true,
      },
    });

    if (!showtime) {
      throw new NotFoundException('Showtime not found');
    }

    return showtime;
  }

  async findByMovie(movieId: string, date?: string) {
    const movie = await this.prisma.movie.findUnique({
      where: { id: movieId },
    });

    if (!movie) {
      throw new NotFoundException('Movie not found');
    }

    const where: Prisma.ShowtimeWhereInput = {
      movieId,
      status: ShowtimeStatus.OPEN,
      ...this.buildDateWhere(date),
    };

    const showtimes = await this.prisma.showtime.findMany({
      where,
      include: {
        movie: true,
        room: true,
      },
      orderBy: {
        startTime: 'asc',
      },
    });

    const result = await Promise.all(
      showtimes.map(async (showtime) => {
        const availableSeats = await this.countAvailableSeats(
          showtime.id,
          showtime.roomId,
        );

        return {
          id: showtime.id,
          movieId: showtime.movieId,
          movieTitle: showtime.movie.title,
          roomId: showtime.roomId,
          roomName: showtime.room.name,
          roomType: showtime.room.type,
          startTime: showtime.startTime,
          endTime: showtime.endTime,
          basePrice: showtime.basePrice,
          status: showtime.status,
          availableSeats,
        };
      }),
    );

    return {
      movieId,
      movieTitle: movie.title,
      date: date ?? null,
      showtimes: result,
    };
  }

  async create(dto: CreateShowtimeDto) {
    const movie = await this.prisma.movie.findUnique({
      where: { id: dto.movieId },
    });

    if (!movie) {
      throw new NotFoundException('Movie not found');
    }

    const room = await this.prisma.room.findUnique({
      where: { id: dto.roomId },
    });

    if (!room) {
      throw new NotFoundException('Room not found');
    }

    const startTime = this.parseDate(dto.startTime, 'Invalid startTime');
    const endTime = dto.endTime
      ? this.parseDate(dto.endTime, 'Invalid endTime')
      : this.addMinutes(startTime, movie.runtime);

    this.validateTimeRange(startTime, endTime);

    await this.ensureNoRoomScheduleConflict(dto.roomId, startTime, endTime);

    return this.prisma.showtime.create({
      data: {
        movieId: dto.movieId,
        roomId: dto.roomId,
        startTime,
        endTime,
        basePrice: dto.basePrice,
        status: dto.status ?? ShowtimeStatus.OPEN,
      },
      include: {
        movie: true,
        room: true,
      },
    });
  }

  async update(id: string, dto: UpdateShowtimeDto) {
    const current = await this.prisma.showtime.findUnique({
      where: { id },
      include: {
        movie: true,
        room: true,
      },
    });

    if (!current) {
      throw new NotFoundException('Showtime not found');
    }

    const movieId = dto.movieId ?? current.movieId;
    const roomId = dto.roomId ?? current.roomId;

    const movie = await this.prisma.movie.findUnique({
      where: { id: movieId },
    });

    if (!movie) {
      throw new NotFoundException('Movie not found');
    }

    const room = await this.prisma.room.findUnique({
      where: { id: roomId },
    });

    if (!room) {
      throw new NotFoundException('Room not found');
    }

    const startTime = dto.startTime
      ? this.parseDate(dto.startTime, 'Invalid startTime')
      : current.startTime;

    const endTime = dto.endTime
      ? this.parseDate(dto.endTime, 'Invalid endTime')
      : dto.startTime
        ? this.addMinutes(startTime, movie.runtime)
        : current.endTime;

    this.validateTimeRange(startTime, endTime);

    await this.ensureNoRoomScheduleConflict(roomId, startTime, endTime, id);

    return this.prisma.showtime.update({
      where: { id },
      data: {
        movieId,
        roomId,
        startTime,
        endTime,
        basePrice: dto.basePrice ?? current.basePrice,
        status: dto.status ?? current.status,
      },
      include: {
        movie: true,
        room: true,
      },
    });
  }

  async cancel(id: string) {
    await this.findOne(id);

    return this.prisma.showtime.update({
      where: { id },
      data: {
        status: ShowtimeStatus.CANCELLED,
      },
      include: {
        movie: true,
        room: true,
      },
    });
  }

  private async ensureNoRoomScheduleConflict(
    roomId: string,
    startTime: Date,
    endTime: Date,
    excludeShowtimeId?: string,
  ) {
    const where: Prisma.ShowtimeWhereInput = {
      roomId,
      status: {
        not: ShowtimeStatus.CANCELLED,
      },
      startTime: {
        lt: endTime,
      },
      endTime: {
        gt: startTime,
      },
    };

    if (excludeShowtimeId) {
      where.id = {
        not: excludeShowtimeId,
      };
    }

    const conflict = await this.prisma.showtime.findFirst({
      where,
      include: {
        movie: true,
        room: true,
      },
    });

    if (conflict) {
      throw new BadRequestException(
        `Room already has a showtime in this time range: ${conflict.movie.title}`,
      );
    }
  }

  private async countAvailableSeats(showtimeId: string, roomId: string) {
    const now = new Date();

    const [totalSeats, soldSeats, lockedSeats] = await Promise.all([
      this.prisma.seat.count({
        where: {
          roomId,
          isActive: true,
          type: {
            notIn: [SeatType.DISABLED, SeatType.MAINTENANCE],
          },
        },
      }),

      this.prisma.bookingSeat.count({
        where: {
          booking: {
            showtimeId,
            status: {
              in: [BookingStatus.PAID, BookingStatus.CHECKED_IN],
            },
          },
        },
      }),

      this.prisma.seatLock.count({
        where: {
          showtimeId,
          status: SeatLockStatus.ACTIVE,
          lockedUntil: {
            gt: now,
          },
        },
      }),
    ]);

    return Math.max(totalSeats - soldSeats - lockedSeats, 0);
  }

  private buildDateWhere(date?: string): Prisma.ShowtimeWhereInput {
    if (!date) {
      return {};
    }

    const parts = date.split('-').map(Number);

    if (parts.length !== 3 || parts.some((part) => Number.isNaN(part))) {
      throw new BadRequestException('Date must use format YYYY-MM-DD');
    }

    const [year, month, day] = parts;
    const start = new Date(year, month - 1, day, 0, 0, 0, 0);
    const end = new Date(year, month - 1, day + 1, 0, 0, 0, 0);

    return {
      startTime: {
        gte: start,
        lt: end,
      },
    };
  }

  private parseDate(value: string, message: string) {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      throw new BadRequestException(message);
    }

    return date;
  }

  private validateTimeRange(startTime: Date, endTime: Date) {
    if (endTime <= startTime) {
      throw new BadRequestException('endTime must be after startTime');
    }
  }

  private addMinutes(date: Date, minutes: number) {
    return new Date(date.getTime() + minutes * 60 * 1000);
  }
}