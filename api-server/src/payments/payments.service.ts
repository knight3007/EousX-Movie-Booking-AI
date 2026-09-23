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
import { ConfigService } from '@nestjs/config';
import { CreateSepayPaymentDto } from './dto/create-sepay-payment.dto';

@Injectable()
export class PaymentsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly configService: ConfigService,
  ) {}

  async createSepayPayment(dto: CreateSepayPaymentDto, userId: string) {
    const booking = await this.prisma.booking.findUnique({
      where: {
        id: dto.bookingId,
      },
      include: {
        payment: true,
        ticket: true,
        seats: {
          include: {
            seat: true,
          },
        },
      },
    });

    if (!booking) {
      throw new NotFoundException('Booking not found');
    }

    if (booking.userId !== userId) {
      throw new BadRequestException('You cannot pay for another user booking');
    }

    if (booking.status === BookingStatus.CANCELLED) {
      throw new BadRequestException('Cannot pay a cancelled booking');
    }

    if (booking.status === BookingStatus.EXPIRED) {
      throw new BadRequestException('Cannot pay an expired booking');
    }

    if (
      booking.status === BookingStatus.PAID ||
      booking.status === BookingStatus.CHECKED_IN
    ) {
      return {
        message: 'Booking was already paid',
        provider: booking.payment?.provider ?? 'SEPAY',
        paymentCode: booking.payment?.transactionId ?? null,
        bookingId: booking.id,
        amount: booking.totalAmount,
        paymentStatus: booking.payment?.status ?? 'SUCCESS',
        bookingStatus: booking.status,
        qrImageUrl: null,
        hasTicket: Boolean(booking.ticket),
      };
    }

    if (booking.status !== BookingStatus.WAITING_PAYMENT) {
      throw new BadRequestException(
        `Booking is not waiting for payment. Current status: ${booking.status}`,
      );
    }

    if (booking.seats.length === 0) {
      throw new BadRequestException('Booking has no seats');
    }

    const existingPendingSepayPaymentCode =
      booking.payment &&
      booking.payment.provider === 'SEPAY' &&
      booking.payment.status === PaymentStatus.PENDING &&
      booking.payment.transactionId
        ? booking.payment.transactionId
        : null;

    const paymentCode =
      existingPendingSepayPaymentCode ?? this.generateSepayPaymentCode();

    const qrImageUrl = this.buildSepayQrImageUrl(
      booking.totalAmount,
      paymentCode,
    );

    const payment = await this.prisma.payment.upsert({
      where: {
        bookingId: booking.id,
      },
      create: {
        bookingId: booking.id,
        provider: 'SEPAY',
        transactionId: paymentCode,
        amount: booking.totalAmount,
        status: PaymentStatus.PENDING,
      },
      update: {
        provider: 'SEPAY',
        transactionId: paymentCode,
        amount: booking.totalAmount,
        status: PaymentStatus.PENDING,
        paidAt: null,
      },
    });

    return {
      message: 'SePay payment created. Waiting for webhook confirmation.',
      provider: payment.provider,
      paymentCode,
      bookingId: booking.id,
      amount: booking.totalAmount,
      paymentStatus: payment.status,
      bookingStatus: booking.status,
      qrImageUrl,
      hasTicket: Boolean(booking.ticket),
    };
  }

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

  async findSepayStatusForUser(paymentCode: string, userId: string) {
    const payment = await this.prisma.payment.findFirst({
      where: {
        provider: 'SEPAY',
        transactionId: paymentCode,
      },
      include: {
        booking: {
          include: {
            ticket: true,
          },
        },
      },
    });

    if (!payment) {
      throw new NotFoundException('SePay payment not found');
    }

    if (payment.booking.userId !== userId) {
      throw new ForbiddenException('You cannot access this payment');
    }

    return {
      paymentCode,
      paymentStatus: payment.status,
      bookingStatus: payment.booking.status,
      bookingId: payment.booking.id,
      hasTicket: Boolean(payment.booking.ticket),
    };
  }

  async handleSepayWebhook(body: Record<string, unknown>) {
    const transferType = String(body.transferType ?? '').toLowerCase();

    if (transferType !== 'in') {
      return {
        success: true,
        ignored: true,
        reason: 'Transfer type is not incoming',
      };
    }

    const paymentCode =
      this.extractSepayPaymentCode(String(body.content ?? '')) ??
      this.extractSepayPaymentCode(String(body.description ?? ''));

    if (!paymentCode) {
      return {
        success: true,
        ignored: true,
        reason: 'Payment code was not found in content or description',
      };
    }

    const confirmedPaymentCode = paymentCode;

    const transferAmount = Number(body.transferAmount);

    if (!Number.isFinite(transferAmount)) {
      return {
        success: true,
        ignored: true,
        reason: 'Transfer amount is invalid',
        paymentCode: confirmedPaymentCode,
      };
    }

    const now = new Date();

    return this.prisma.$transaction(async (tx) => {
      const payment = await tx.payment.findFirst({
        where: {
          provider: 'SEPAY',
          transactionId: confirmedPaymentCode,
        },
        include: {
          booking: {
            include: {
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
        return {
          success: true,
          ignored: true,
          reason: 'No matching SePay payment was found',
          paymentCode: confirmedPaymentCode,
        };
      }

      const booking = payment.booking;

      if (transferAmount !== booking.totalAmount) {
        return {
          success: true,
          ignored: true,
          reason: 'Transfer amount does not match booking total',
          paymentCode: confirmedPaymentCode,
          bookingId: booking.id,
          amount: transferAmount,
          expectedAmount: booking.totalAmount,
        };
      }

      if (
        payment.status === PaymentStatus.SUCCESS &&
        (booking.status === BookingStatus.PAID ||
          booking.status === BookingStatus.CHECKED_IN)
      ) {
        return {
          success: true,
          ignored: true,
          reason: 'Payment was already confirmed',
          paymentCode: confirmedPaymentCode,
          bookingId: booking.id,
          amount: booking.totalAmount,
          ticketId: booking.ticket?.id ?? null,
        };
      }

      if (booking.status !== BookingStatus.WAITING_PAYMENT) {
        return {
          success: true,
          ignored: true,
          reason: `Booking is not waiting for payment. Current status: ${booking.status}`,
          paymentCode: confirmedPaymentCode,
          bookingId: booking.id,
        };
      }

      const seatIds = booking.seats.map((bookingSeat) => bookingSeat.seatId);

      if (seatIds.length === 0) {
        return {
          success: true,
          ignored: true,
          reason: 'Booking has no seats',
          paymentCode: confirmedPaymentCode,
          bookingId: booking.id,
        };
      }

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
        },
      });

      if (soldByOtherBooking.length > 0) {
        return {
          success: true,
          ignored: true,
          reason: `Some seats are already sold: ${soldByOtherBooking
            .map((bookingSeat) => bookingSeat.seat.code)
            .join(', ')}`,
          paymentCode: confirmedPaymentCode,
          bookingId: booking.id,
        };
      }

      await tx.payment.update({
        where: {
          id: payment.id,
        },
        data: {
          amount: booking.totalAmount,
          status: PaymentStatus.SUCCESS,
          paidAt: now,
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

      await tx.seatLock.updateMany({
        where: {
          userId: booking.userId,
          showtimeId: booking.showtimeId,
          seatId: {
            in: seatIds,
          },
          status: SeatLockStatus.ACTIVE,
        },
        data: {
          status: SeatLockStatus.CONVERTED_TO_BOOKING,
        },
      });

      return {
        success: true,
        message: 'SePay payment confirmed. Booking is paid and ticket is created.',
        paymentCode: confirmedPaymentCode,
        bookingId: booking.id,
        amount: booking.totalAmount,
        ticketId: ticket.id,
      };
    });
  }

  async findByBookingForUser(bookingId: string, userId: string) {
    const payment = await this.findByBooking(bookingId);

    if (payment.booking.userId !== userId) {
      throw new ForbiddenException('You cannot access this payment');
    }

    return payment;
  }

  private extractSepayPaymentCode(text: string) {
    const match = text.toUpperCase().match(/\bEOUSX\d{8}[A-Z0-9]{6}\b/);

    return match?.[0] ?? null;
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
  private generateSepayPaymentCode() {
    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    const random = Math.random().toString(36).substring(2, 8).toUpperCase();

    return `EOUSX${yyyy}${mm}${dd}${random}`;
  }

  private buildSepayQrImageUrl(amount: number, paymentCode: string) {
    const qrBaseUrl =
      this.configService.get<string>('SEPAY_QR_BASE_URL') ??
      'https://qr.sepay.vn/img';

    const bankCode = this.configService.get<string>('SEPAY_BANK_CODE');
    const accountNumber = this.configService.get<string>(
      'SEPAY_BANK_ACCOUNT_NUMBER',
    );

    if (!bankCode || !accountNumber) {
      throw new BadRequestException('SePay bank configuration is missing');
    }

    const params = new URLSearchParams({
      bank: bankCode,
      acc: accountNumber,
      amount: String(amount),
      des: paymentCode,
    });

    return `${qrBaseUrl}?${params.toString()}`;
  }
}
