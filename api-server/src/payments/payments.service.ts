import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import {
  BookingStatus,
  PaymentStatus,
  SeatLockStatus,
  TicketStatus,
} from '@prisma/client';
import { PrismaService } from '../database/prisma.service';
import { MockPaymentSuccessDto } from './dto/mock-payment-success.dto';

@Injectable()
export class PaymentsService {
  constructor(private readonly prisma: PrismaService) {}

  async mockSuccess(userId: string, dto: MockPaymentSuccessDto) {
    const now = new Date();

    return this.prisma.$transaction(async (tx) => {
      const booking = await tx.booking.findUnique({
        where: {
          id: dto.bookingId,
        },
        include: {
          user: {
            select: this.getUserSelect(),
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
        },
      });

      if (!booking) {
        throw new NotFoundException('Booking not found');
      }

      if (booking.userId !== userId) {
        throw new ForbiddenException('You cannot pay this booking');
      }

      if (
        booking.status === BookingStatus.PAID ||
        booking.status === BookingStatus.CHECKED_IN
      ) {
        return {
          message: 'Booking was already paid',
          booking,
          payment: booking.payment,
          ticket: booking.ticket,
        };
      }

      if (booking.status === BookingStatus.CANCELLED) {
        throw new BadRequestException('Cannot pay a cancelled booking');
      }

      if (booking.status === BookingStatus.EXPIRED) {
        throw new BadRequestException('Cannot pay an expired booking');
      }

      if (booking.status !== BookingStatus.WAITING_PAYMENT) {
        throw new BadRequestException(
          `Booking is not waiting for payment. Current status: ${booking.status}`,
        );
      }

      const seatIds = booking.seats.map((bookingSeat) => bookingSeat.seatId);

      if (seatIds.length === 0) {
        throw new BadRequestException('Booking has no seats');
      }

      // Dọn lock hết hạn của suất chiếu này trước khi thanh toán.
      await tx.seatLock.updateMany({
        where: {
          showtimeId: booking.showtimeId,
          status: SeatLockStatus.ACTIVE,
          lockedUntil: {
            lte: now,
          },
        },
        data: {
          status: SeatLockStatus.EXPIRED,
        },
      });

      // Kiểm tra các ghế của booking vẫn còn được user này giữ.
      const activeLocks = await tx.seatLock.findMany({
        where: {
          userId: booking.userId,
          showtimeId: booking.showtimeId,
          seatId: {
            in: seatIds,
          },
          status: SeatLockStatus.ACTIVE,
          lockedUntil: {
            gt: now,
          },
        },
        include: {
          seat: true,
        },
      });

      if (activeLocks.length !== seatIds.length) {
        await tx.booking.update({
          where: {
            id: booking.id,
          },
          data: {
            status: BookingStatus.EXPIRED,
          },
        });

        throw new BadRequestException(
          'Seat lock has expired. Booking is marked as EXPIRED.',
        );
      }

      // Chặn trường hợp ghế đã bị booking khác thanh toán trước.
      const soldByOtherBooking = await tx.bookingSeat.findMany({
        where: {
          seatId: {
            in: seatIds,
          },
          booking: {
            id: {
              not: booking.id,
            },
            showtimeId: booking.showtimeId,
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

      if (soldByOtherBooking.length > 0) {
        throw new BadRequestException(
          `Some seats are already sold: ${soldByOtherBooking
            .map((bookingSeat) => bookingSeat.seat.code)
            .join(', ')}`,
        );
      }

      const transactionId = this.generateTransactionId();
      const provider = dto.provider ?? 'MOCK';

      const payment = await tx.payment.upsert({
        where: {
          bookingId: booking.id,
        },
        create: {
          bookingId: booking.id,
          provider,
          transactionId,
          amount: booking.totalAmount,
          status: PaymentStatus.SUCCESS,
          paidAt: now,
        },
        update: {
          provider,
          transactionId,
          amount: booking.totalAmount,
          status: PaymentStatus.SUCCESS,
          paidAt: now,
        },
      });

      const ticket = await tx.ticket.upsert({
        where: {
          bookingId: booking.id,
        },
        create: {
          bookingId: booking.id,
          qrCode: this.generateQrCode(booking.code),
          status: TicketStatus.VALID,
        },
        update: {
          status: TicketStatus.VALID,
        },
      });

      await tx.booking.update({
        where: {
          id: booking.id,
        },
        data: {
          status: BookingStatus.PAID,
        },
      });

      await tx.seatLock.updateMany({
        where: {
          id: {
            in: activeLocks.map((lock) => lock.id),
          },
        },
        data: {
          status: SeatLockStatus.CONVERTED_TO_BOOKING,
        },
      });

      const paidBooking = await tx.booking.findUnique({
        where: {
          id: booking.id,
        },
        include: {
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
        },
      });

      return {
        message: 'Mock payment success. Booking is paid and ticket is created.',
        booking: paidBooking,
        payment,
        ticket,
      };
    });
  }

  async findOne(id: string) {
    const payment = await this.prisma.payment.findUnique({
      where: {
        id,
      },
      include: {
        booking: {
          include: {
            user: {
              select: this.getUserSelect(),
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
            ticket: true,
          },
        },
      },
    });

    if (!payment) {
      throw new NotFoundException('Payment not found');
    }

    return payment;
  }

  async findByBooking(bookingId: string) {
    const payment = await this.prisma.payment.findUnique({
      where: {
        bookingId,
      },
      include: {
        booking: {
          include: {
            user: {
              select: this.getUserSelect(),
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
            ticket: true,
          },
        },
      },
    });

    if (!payment) {
      throw new NotFoundException('Payment not found for this booking');
    }

    return payment;
  }

  async findByBookingForUser(bookingId: string, userId: string) {
    const payment = await this.findByBooking(bookingId);

    if (payment.booking.userId !== userId) {
      throw new ForbiddenException('You cannot access this payment');
    }

    return payment;
  }

  private generateTransactionId() {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(2, 8).toUpperCase();

    return `MOCK-TXN-${timestamp}-${random}`;
  }

  private generateQrCode(bookingCode: string) {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(2, 8).toUpperCase();

    return `EOUSX-QR-${bookingCode}-${timestamp}-${random}`;
  }

  private getUserSelect() {
    return {
      id: true,
      fullName: true,
      email: true,
      phone: true,
      avatarUrl: true,
    };
  }
}
