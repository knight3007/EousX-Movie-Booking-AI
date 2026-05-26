import { Injectable, NotFoundException } from '@nestjs/common';
import {
  BookingStatus,
  SeatLockStatus,
  SeatType,
} from '@prisma/client';
import { PrismaService } from '../database/prisma.service';

type SeatDisplayStatus = 'AVAILABLE' | 'LOCKED' | 'SOLD' | 'MAINTENANCE';

@Injectable()
export class SeatsService {
  constructor(private readonly prisma: PrismaService) {}

  async getSeatMapByShowtime(showtimeId: string) {
    const now = new Date();

    const showtime = await this.prisma.showtime.findUnique({
      where: {
        id: showtimeId,
      },
      include: {
        movie: true,
        room: {
          include: {
            seats: {
              orderBy: [
                {
                  row: 'asc',
                },
                {
                  number: 'asc',
                },
              ],
            },
          },
        },
      },
    });

    if (!showtime) {
      throw new NotFoundException('Showtime not found');
    }

    // Tự động đánh dấu các lock đã hết hạn thành EXPIRED.
    // Việc này giúp seat map không hiển thị ghế bị lock quá hạn.
    await this.prisma.seatLock.updateMany({
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

    const [soldBookingSeats, activeSeatLocks] = await Promise.all([
      this.prisma.bookingSeat.findMany({
        where: {
          booking: {
            showtimeId,
            status: {
              in: [BookingStatus.PAID, BookingStatus.CHECKED_IN],
            },
          },
        },
        include: {
          booking: {
            select: {
              id: true,
              code: true,
              status: true,
            },
          },
        },
      }),

      this.prisma.seatLock.findMany({
        where: {
          showtimeId,
          status: SeatLockStatus.ACTIVE,
          lockedUntil: {
            gt: now,
          },
        },
        include: {
          user: {
            select: {
              id: true,
              fullName: true,
              email: true,
            },
          },
        },
      }),
    ]);

    const soldSeatMap = new Map(
      soldBookingSeats.map((bookingSeat) => [
        bookingSeat.seatId,
        bookingSeat,
      ]),
    );

    const lockedSeatMap = new Map(
      activeSeatLocks.map((seatLock) => [
        seatLock.seatId,
        seatLock,
      ]),
    );

    const seats = showtime.room.seats.map((seat) => {
      const soldInfo = soldSeatMap.get(seat.id);
      const lockInfo = lockedSeatMap.get(seat.id);

      const status = this.resolveSeatStatus({
        isActive: seat.isActive,
        type: seat.type,
        isSold: Boolean(soldInfo),
        isLocked: Boolean(lockInfo),
      });

      return {
        id: seat.id,
        row: seat.row,
        number: seat.number,
        code: seat.code,
        type: seat.type,
        status,
        price: this.calculateSeatPrice(showtime.basePrice, seat.type),
        lock: lockInfo
          ? {
              id: lockInfo.id,
              lockedUntil: lockInfo.lockedUntil,
              user: lockInfo.user,
            }
          : null,
        booking: soldInfo
          ? {
              id: soldInfo.booking.id,
              code: soldInfo.booking.code,
              status: soldInfo.booking.status,
            }
          : null,
      };
    });

    const summary = this.buildSummary(seats);

    return {
      showtime: {
        id: showtime.id,
        startTime: showtime.startTime,
        endTime: showtime.endTime,
        basePrice: showtime.basePrice,
        status: showtime.status,
      },
      movie: {
        id: showtime.movie.id,
        title: showtime.movie.title,
        posterUrl: showtime.movie.posterUrl,
        runtime: showtime.movie.runtime,
        ageRating: showtime.movie.ageRating,
      },
      room: {
        id: showtime.room.id,
        name: showtime.room.name,
        type: showtime.room.type,
        rows: showtime.room.rows,
        columns: showtime.room.columns,
      },
      summary,
      seats,
    };
  }

  private resolveSeatStatus(params: {
    isActive: boolean;
    type: SeatType;
    isSold: boolean;
    isLocked: boolean;
  }): SeatDisplayStatus {
    if (
      !params.isActive ||
      params.type === SeatType.DISABLED ||
      params.type === SeatType.MAINTENANCE
    ) {
      return 'MAINTENANCE';
    }

    if (params.isSold) {
      return 'SOLD';
    }

    if (params.isLocked) {
      return 'LOCKED';
    }

    return 'AVAILABLE';
  }

  private calculateSeatPrice(basePrice: number, seatType: SeatType) {
    // MVP: dùng basePrice theo suất chiếu.
    // Sau này nếu muốn có phụ thu VIP/COUPLE thì sửa tại đây.
    if (seatType === SeatType.DISABLED || seatType === SeatType.MAINTENANCE) {
      return 0;
    }

    return basePrice;
  }

  private buildSummary(
    seats: Array<{
      status: SeatDisplayStatus;
    }>,
  ) {
    const summary = {
      total: seats.length,
      available: 0,
      locked: 0,
      sold: 0,
      maintenance: 0,
    };

    for (const seat of seats) {
      if (seat.status === 'AVAILABLE') {
        summary.available += 1;
      }

      if (seat.status === 'LOCKED') {
        summary.locked += 1;
      }

      if (seat.status === 'SOLD') {
        summary.sold += 1;
      }

      if (seat.status === 'MAINTENANCE') {
        summary.maintenance += 1;
      }
    }

    return summary;
  }
}