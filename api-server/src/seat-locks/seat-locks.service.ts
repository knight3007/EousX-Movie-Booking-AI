import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import {
  BookingStatus,
  SeatLockStatus,
  SeatType,
  ShowtimeStatus,
} from '@prisma/client';
import { PrismaService } from '../database/prisma.service';
import { LockSeatsDto } from './dto/lock-seats.dto';

const LOCK_DURATION_MINUTES = 3;

@Injectable()
export class SeatLocksService {
  constructor(private readonly prisma: PrismaService) {}

  async lockSeats(showtimeId: string, userId: string, dto: LockSeatsDto) {
    const now = new Date();
    const lockedUntil = this.addMinutes(now, LOCK_DURATION_MINUTES);

    return this.prisma.$transaction(async (tx) => {
      const user = await tx.user.findUnique({
        where: {
          id: userId,
        },
      });

      if (!user) {
        throw new NotFoundException('User not found');
      }

      const showtime = await tx.showtime.findUnique({
        where: {
          id: showtimeId,
        },
        include: {
          movie: true,
          room: true,
        },
      });

      if (!showtime) {
        throw new NotFoundException('Showtime not found');
      }

      if (showtime.status !== ShowtimeStatus.OPEN) {
        throw new BadRequestException('Showtime is not open for booking');
      }

      // Dọn lock hết hạn của suất chiếu này trước khi kiểm tra.
      await tx.seatLock.updateMany({
        where: {
          showtimeId,
          status: SeatLockStatus.ACTIVE,
          lockedUntil: {
            lte: now,
          },
        },
        data: {
          status: SeatLockStatus.EXPIRED,
        },
      });

      const seats = await tx.seat.findMany({
        where: {
          id: {
            in: dto.seatIds,
          },
        },
        orderBy: [
          {
            row: 'asc',
          },
          {
            number: 'asc',
          },
        ],
      });

      if (seats.length !== dto.seatIds.length) {
        throw new BadRequestException('Some seats were not found');
      }

      const wrongRoomSeats = seats.filter(
        (seat) => seat.roomId !== showtime.roomId,
      );

      if (wrongRoomSeats.length > 0) {
        throw new BadRequestException(
          `Some seats do not belong to this showtime room: ${wrongRoomSeats
            .map((seat) => seat.code)
            .join(', ')}`,
        );
      }

      const unavailableSeats = seats.filter(
        (seat) =>
          !seat.isActive ||
          seat.type === SeatType.DISABLED ||
          seat.type === SeatType.MAINTENANCE,
      );

      if (unavailableSeats.length > 0) {
        throw new BadRequestException(
          `Some seats are not available: ${unavailableSeats
            .map((seat) => seat.code)
            .join(', ')}`,
        );
      }

      const soldBookingSeats = await tx.bookingSeat.findMany({
        where: {
          seatId: {
            in: dto.seatIds,
          },
          booking: {
            showtimeId,
            status: {
              in: [BookingStatus.PAID, BookingStatus.CHECKED_IN],
            },
          },
        },
        include: {
          seat: true,
          booking: true,
        },
      });

      if (soldBookingSeats.length > 0) {
        throw new BadRequestException(
          `Some seats are already sold: ${soldBookingSeats
            .map((bookingSeat) => bookingSeat.seat.code)
            .join(', ')}`,
        );
      }

      const activeLocks = await tx.seatLock.findMany({
        where: {
          showtimeId,
          seatId: {
            in: dto.seatIds,
          },
          status: SeatLockStatus.ACTIVE,
          lockedUntil: {
            gt: now,
          },
        },
        include: {
          seat: true,
          user: {
            select: {
              id: true,
              fullName: true,
              email: true,
            },
          },
        },
      });

      if (activeLocks.length > 0) {
        throw new BadRequestException(
          `Some seats are currently locked: ${activeLocks
            .map((lock) => lock.seat.code)
            .join(', ')}`,
        );
      }

      const createdLocks = await Promise.all(
        seats.map((seat) =>
          tx.seatLock.create({
            data: {
              userId,
              showtimeId,
              seatId: seat.id,
              status: SeatLockStatus.ACTIVE,
              lockedUntil,
            },
            include: {
              seat: true,
            },
          }),
        ),
      );

      const totalAmount = seats.reduce((sum, seat) => {
        return sum + this.calculateSeatPrice(showtime.basePrice, seat.type);
      }, 0);

      return {
        message: `Seats locked for ${LOCK_DURATION_MINUTES} minutes`,
        lockDurationMinutes: LOCK_DURATION_MINUTES,
        lockDurationSeconds: LOCK_DURATION_MINUTES * 60,
        showtime: {
          id: showtime.id,
          movieTitle: showtime.movie.title,
          roomName: showtime.room.name,
          startTime: showtime.startTime,
          endTime: showtime.endTime,
          basePrice: showtime.basePrice,
        },
        user: {
          id: user.id,
          fullName: user.fullName,
          email: user.email,
        },
        lockedUntil,
        totalAmount,
        locks: createdLocks.map((lock) => ({
          id: lock.id,
          seatId: lock.seatId,
          seatCode: lock.seat.code,
          status: lock.status,
          lockedUntil: lock.lockedUntil,
        })),
      };
    });
  }

  async releaseLock(lockId: string, userId?: string) {
    const lock = await this.prisma.seatLock.findUnique({
      where: {
        id: lockId,
      },
      include: {
        seat: true,
        showtime: {
          include: {
            movie: true,
            room: true,
          },
        },
      },
    });

    if (!lock) {
      throw new NotFoundException('Seat lock not found');
    }

    if (userId && lock.userId !== userId) {
      throw new ForbiddenException('You cannot release this seat lock');
    }

    if (lock.status !== SeatLockStatus.ACTIVE) {
      return {
        message: 'Seat lock is not active',
        lock,
      };
    }

    const updatedLock = await this.prisma.seatLock.update({
      where: {
        id: lockId,
      },
      data: {
        status: SeatLockStatus.RELEASED,
      },
      include: {
        seat: true,
        showtime: {
          include: {
            movie: true,
            room: true,
          },
        },
      },
    });

    return {
      message: 'Seat lock released successfully',
      lock: updatedLock,
    };
  }

  async getLock(lockId: string) {
    const lock = await this.prisma.seatLock.findUnique({
      where: {
        id: lockId,
      },
      include: {
        user: {
          select: {
            id: true,
            fullName: true,
            email: true,
            phone: true,
            avatarUrl: true,
          },
        },
        seat: true,
        showtime: {
          include: {
            movie: true,
            room: true,
          },
        },
      },
    });

    if (!lock) {
      throw new NotFoundException('Seat lock not found');
    }

    return lock;
  }

  private calculateSeatPrice(basePrice: number, seatType: SeatType) {
    if (seatType === SeatType.DISABLED || seatType === SeatType.MAINTENANCE) {
      return 0;
    }

    return basePrice;
  }

  private addMinutes(date: Date, minutes: number) {
    return new Date(date.getTime() + minutes * 60 * 1000);
  }
}
