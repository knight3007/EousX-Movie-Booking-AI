import {
  Body,
  Controller,
  Get,
  Headers,
  Param,
  Post,
  UnauthorizedException,
  UseGuards,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AuthUser } from '../auth/auth.types';
import { CurrentUser } from '../auth/current-user.decorator';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { MockPaymentSuccessDto } from './dto/mock-payment-success.dto';
import { PaymentsService } from './payments.service';
import { CreateSepayPaymentDto } from './dto/create-sepay-payment.dto';

@Controller()
export class PaymentsController {
  constructor(
    private readonly paymentsService: PaymentsService,
    private readonly configService: ConfigService,
  ) {}

  @Post('payments/mock-success')
  @UseGuards(JwtAuthGuard)
  mockSuccess(
    @Body() dto: MockPaymentSuccessDto,
    @CurrentUser() user: AuthUser,
  ) {
    return this.paymentsService.mockSuccess(user.id, dto);
  }
  @Post('payments/sepay/create')
  @UseGuards(JwtAuthGuard)
  createSepayPayment(
    @Body() dto: CreateSepayPaymentDto,
    @CurrentUser() user: AuthUser,
  ) {
    return this.paymentsService.createSepayPayment(dto, user.id);
  }

  @Post('payments/webhook/sepay')
  sepayWebhook(
    @Body() body: Record<string, unknown>,
    @Headers() headers: Record<string, string | string[] | undefined>,
  ) {
    this.verifySePayWebhookApiKey(headers);

    console.log(
      '[SePay Webhook] received payload:',
      JSON.stringify(body, null, 2),
    );

    return this.paymentsService.handleSepayWebhook(body);
  }

  @Get('payments/sepay/status/:paymentCode')
  @UseGuards(JwtAuthGuard)
  findSepayStatus(
    @Param('paymentCode') paymentCode: string,
    @CurrentUser() user: AuthUser,
  ) {
    return this.paymentsService.findSepayStatusForUser(paymentCode, user.id);
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

  private verifySePayWebhookApiKey(
    headers: Record<string, string | string[] | undefined>,
  ) {
    const expectedApiKey = this.configService.get<string>(
      'SEPAY_WEBHOOK_API_KEY',
    );

    if (!expectedApiKey) {
      throw new UnauthorizedException(
        'SePay webhook API key is not configured',
      );
    }

    const authorizationHeader = this.getSingleHeader(headers, 'authorization');
    const xApiKeyHeader = this.getSingleHeader(headers, 'x-api-key');

    const providedApiKey =
      this.extractApiKeyFromAuthorization(authorizationHeader) ??
      xApiKeyHeader;

    if (providedApiKey !== expectedApiKey) {
      throw new UnauthorizedException('Invalid SePay webhook API key');
    }
  }

  private getSingleHeader(
    headers: Record<string, string | string[] | undefined>,
    name: string,
  ) {
    const value = headers[name.toLowerCase()];

    if (Array.isArray(value)) {
      return value[0];
    }

    return value;
  }

  private extractApiKeyFromAuthorization(header?: string) {
    if (!header) {
      return undefined;
    }

    const value = header.trim();

    if (value.toLowerCase().startsWith('apikey ')) {
      return value.slice('apikey '.length).trim();
    }

    if (value.toLowerCase().startsWith('bearer ')) {
      return value.slice('bearer '.length).trim();
    }

    return value;
  }
}
