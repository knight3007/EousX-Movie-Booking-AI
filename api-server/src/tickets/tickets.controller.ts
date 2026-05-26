import { Controller, Get, Param, Post, UseGuards } from '@nestjs/common';
import { AuthUser } from '../auth/auth.types';
import { CurrentUser } from '../auth/current-user.decorator';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { TicketsService } from './tickets.service';

@Controller()
export class TicketsController {
  constructor(private readonly ticketsService: TicketsService) {}

  @Get('bookings/:bookingId/ticket')
  @UseGuards(JwtAuthGuard)
  findByBooking(
    @Param('bookingId') bookingId: string,
    @CurrentUser() user: AuthUser,
  ) {
    return this.ticketsService.findByBookingForUser(bookingId, user.id);
  }

  @Get('tickets/verify/:qrCode')
  verify(@Param('qrCode') qrCode: string) {
    return this.ticketsService.verify(qrCode);
  }

  @Post('tickets/:ticketId/check-in')
  checkIn(@Param('ticketId') ticketId: string) {
    return this.ticketsService.checkIn(ticketId);
  }
}
