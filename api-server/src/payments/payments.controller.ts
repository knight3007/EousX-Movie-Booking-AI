import { Body, Controller, Get, Param, Post, UseGuards } from '@nestjs/common';
import { AuthUser } from '../auth/auth.types';
import { CurrentUser } from '../auth/current-user.decorator';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { PaymentsService } from './payments.service';
import { MockPaymentSuccessDto } from './dto/mock-payment-success.dto';

@Controller()
export class PaymentsController {
  constructor(private readonly paymentsService: PaymentsService) {}

  @Post('payments/mock-success')
  @UseGuards(JwtAuthGuard)
  mockSuccess(
    @Body() dto: MockPaymentSuccessDto,
    @CurrentUser() user: AuthUser,
  ) {
    return this.paymentsService.mockSuccess(user.id, dto);
  }

  @Get('payments/:id/status')
  findOne(@Param('id') id: string) {
    return this.paymentsService.findOne(id);
  }

  @Get('bookings/:bookingId/payment')
  @UseGuards(JwtAuthGuard)
  findByBooking(
    @Param('bookingId') bookingId: string,
    @CurrentUser() user: AuthUser,
  ) {
    return this.paymentsService.findByBookingForUser(bookingId, user.id);
  }
}
