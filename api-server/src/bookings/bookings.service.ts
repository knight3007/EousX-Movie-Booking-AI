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
import { CreateBookingDto } from './dto/create-booking.dto';

@Injectable()
export class BookingsService {
  constructor(private readonly prisma: PrismaService) {}

  async create(userId: string, dto: CreateBookingDto) {
    const now = new Date();

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
          id: dto.showtimeId,
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

      // Dọn lock hết hạn trước khi tạo booking.
      await tx.seatLock.updateMany({
        where: {
          showtimeId: dto.showtimeId,
          status: SeatLockStatus.ACTIVE,
          lockedUntil: {
            lte: now,
          },
        },
        data: {
          status: SeatLockStatus.EXPIRED,
        },
      });

      const locks = await tx.seatLock.findMany({
        where: {
          id: {
            in: dto.lockIds,
          },
          userId,
          showtimeId: dto.showtimeId,
          status: SeatLockStatus.ACTIVE,
          lockedUntil: {
            gt: now,
          },
        },
        include: {
          seat: true,
        },
        orderBy: {
          createdAt: 'asc',
        },
      });

      if (locks.length !== dto.lockIds.length) {
        const validLockIds = locks.map((lock) => lock.id);
        const invalidLockIds = dto.lockIds.filter(
          (lockId) => !validLockIds.includes(lockId),
        );

        throw new BadRequestException(
          `Some locks are invalid, expired, or not owned by this user: ${invalidLockIds.join(
            ', ',
          )}`,
        );
      }

      const seats = locks.map((lock) => lock.seat);
      const seatIds = seats.map((seat) => seat.id);

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

      const soldBookingSeats = await tx.bookingSeat.findMany({
        where: {
          seatId: {
            in: seatIds,
          },
          booking: {
            showtimeId: dto.showtimeId,
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

      const totalAmount = seats.reduce((sum, seat) => {
        return sum + this.calculateSeatPrice(showtime.basePrice, seat.type);
      }, 0);

      const booking = await tx.booking.create({
        data: {
          userId,
          showtimeId: dto.showtimeId,
          code: this.generateBookingCode(),
          status: BookingStatus.WAITING_PAYMENT,
          totalAmount,
          seats: {
            create: seats.map((seat) => ({
              seatId: seat.id,
              price: this.calculateSeatPrice(showtime.basePrice, seat.type),
            })),
          },
        },
        include: this.getBookingInclude(),
      });

      return {
        message: 'Booking created successfully. Waiting for payment.',
        booking,
        nextStep: {
          method: 'POST',
          endpoint: '/payments/mock-success',
          note: 'Bước sau sẽ làm Mock Payment API để thanh toán booking này.',
        },
      };
    });
  }

  async findAll() {
    return this.prisma.booking.findMany({
      include: this.getBookingInclude(),
      orderBy: {
        createdAt: 'desc',
      },
    });
  }

  async findByUser(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: {
        id: userId,
      },
    });

    if (!user) {
      throw new NotFoundException('User not found');
    }

    return this.prisma.booking.findMany({
      where: {
        userId,
      },
      include: this.getBookingInclude(),
      orderBy: {
        createdAt: 'desc',
      },
    });
  }

  async findOne(id: string) {
    const booking = await this.prisma.booking.findUnique({
      where: {
        id,
      },
      include: this.getBookingInclude(),
    });

    if (!booking) {
      throw new NotFoundException('Booking not found');
    }

    return booking;
  }

  async findOneForUser(id: string, userId: string) {
    const booking = await this.findOne(id);

    if (booking.userId !== userId) {
      throw new ForbiddenException('You cannot access this booking');
    }

    return booking;
  }

  async cancel(id: string, userId?: string) {
    const booking = await this.prisma.booking.findUnique({
      where: {
        id,
      },
      include: {
        seats: true,
      },
    });

    if (!booking) {
      throw new NotFoundException('Booking not found');
    }

    if (userId && booking.userId !== userId) {
      throw new ForbiddenException('You cannot cancel this booking');
    }

    if (
      booking.status === BookingStatus.PAID ||
      booking.status === BookingStatus.CHECKED_IN
    ) {
      throw new BadRequestException(
        'Cannot cancel a paid or checked-in booking',
      );
    }

    const updatedBooking = await this.prisma.booking.update({
      where: {
        id,
      },
      data: {
        status: BookingStatus.CANCELLED,
      },
      include: this.getBookingInclude(),
    });

    await this.prisma.seatLock.updateMany({
      where: {
        userId: booking.userId,
        showtimeId: booking.showtimeId,
        seatId: {
          in: booking.seats.map((seat) => seat.seatId),
        },
        status: SeatLockStatus.ACTIVE,
      },
      data: {
        status: SeatLockStatus.RELEASED,
      },
    });

    return {
      message: 'Booking cancelled successfully',
      booking: updatedBooking,
    };
  }

  private calculateSeatPrice(basePrice: number, seatType: SeatType) {
    if (seatType === SeatType.DISABLED || seatType === SeatType.MAINTENANCE) {
      return 0;
    }

    // Hiện tại giữ đơn giản cho đồ án: mọi ghế active dùng basePrice.
    // Nếu sau này muốn VIP/Couple đắt hơn thì chỉnh ở đây.
    return basePrice;
  }

  private generateBookingCode() {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(2, 8).toUpperCase();

    return `EOUSX-${timestamp}-${random}`;
  }

  private getBookingInclude() {
    return {
      user: {
        select: {
          id: true,
          fullName: true,
          email: true,
          phone: true,
        },
      },
      showtime: {
        include: {
          movie: true,
          room: true,
        },
      },
      seats: {
        include: {
          seat: true,
        },
      },
      payment: true,
      ticket: true,
    };
  }
}
