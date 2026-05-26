import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { BookingStatus, TicketStatus } from '@prisma/client';
import { PrismaService } from '../database/prisma.service';

@Injectable()
export class TicketsService {
  constructor(private readonly prisma: PrismaService) {}

  async findByBooking(bookingId: string) {
    const ticket = await this.prisma.ticket.findUnique({
      where: { bookingId },
      include: this.getTicketInclude(),
    });

    if (!ticket) {
      throw new NotFoundException('Ticket not found for this booking');
    }

    return ticket;
  }

  async findByBookingForUser(bookingId: string, userId: string) {
    const ticket = await this.findByBooking(bookingId);

    if (ticket.booking.userId !== userId) {
      throw new ForbiddenException('You cannot access this ticket');
    }

    return ticket;
  }

  async verify(qrCode: string) {
    const ticket = await this.prisma.ticket.findFirst({
      where: { qrCode },
      include: this.getTicketInclude(),
    });

    if (!ticket) {
      return {
        status: 'INVALID',
        message: 'Ticket is invalid',
        ticket: null,
      };
    }

    if (ticket.status === TicketStatus.USED) {
      return {
        status: 'ALREADY_USED',
        message: 'Ticket was already checked in',
        ticket,
      };
    }

    if (ticket.status !== TicketStatus.VALID) {
      return {
        status: ticket.status,
        message: `Ticket is not valid. Current status: ${ticket.status}`,
        ticket,
      };
    }

    return {
      status: 'VALID',
      message: 'Ticket is valid',
      ticket,
    };
  }

  async checkIn(ticketId: string) {
    const ticket = await this.prisma.ticket.findUnique({
      where: { id: ticketId },
      include: {
        booking: true,
      },
    });

    if (!ticket) {
      throw new NotFoundException('Ticket not found');
    }

    if (ticket.status === TicketStatus.USED) {
      throw new BadRequestException('Ticket was already checked in');
    }

    if (ticket.status !== TicketStatus.VALID) {
      throw new BadRequestException(
        `Ticket is not valid. Current status: ${ticket.status}`,
      );
    }

    if (ticket.booking.status !== BookingStatus.PAID) {
      throw new BadRequestException(
        `Booking is not paid. Current status: ${ticket.booking.status}`,
      );
    }

    const updatedTicket = await this.prisma.ticket.update({
      where: { id: ticketId },
      data: {
        status: TicketStatus.USED,
        usedAt: new Date(),
      },
      include: this.getTicketInclude(),
    });

    await this.prisma.booking.update({
      where: { id: ticket.bookingId },
      data: {
        status: BookingStatus.CHECKED_IN,
      },
    });

    return {
      message: 'Ticket checked in successfully',
      ticket: updatedTicket,
      note: 'Nút này chỉ dành cho nhân viên rạp xác nhận vé.',
    };
  }

  private getTicketInclude() {
    return {
      booking: {
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
        },
      },
    };
  }
}
