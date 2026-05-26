import {
  Body,
  Controller,
  Get,
  Param,
  Patch,
  Post,
  UseGuards,
} from '@nestjs/common';
import { AuthUser } from '../auth/auth.types';
import { CurrentUser } from '../auth/current-user.decorator';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { BookingsService } from './bookings.service';
import { CreateBookingDto } from './dto/create-booking.dto';

@Controller()
export class BookingsController {
  constructor(private readonly bookingsService: BookingsService) {}

  @Post('bookings')
  @UseGuards(JwtAuthGuard)
  create(@Body() dto: CreateBookingDto, @CurrentUser() user: AuthUser) {
    return this.bookingsService.create(user.id, dto);
  }

  @Get('bookings/me')
  @UseGuards(JwtAuthGuard)
  findMyBookings(@CurrentUser() user: AuthUser) {
    return this.bookingsService.findByUser(user.id);
  }

  @Get('bookings/:id')
  @UseGuards(JwtAuthGuard)
  findOne(@Param('id') id: string, @CurrentUser() user: AuthUser) {
    return this.bookingsService.findOneForUser(id, user.id);
  }

  @Patch('bookings/:id/cancel')
  @UseGuards(JwtAuthGuard)
  cancel(@Param('id') id: string, @CurrentUser() user: AuthUser) {
    return this.bookingsService.cancel(id, user.id);
  }

  @Get('admin/bookings')
  adminFindAll() {
    return this.bookingsService.findAll();
  }

  @Get('admin/bookings/:id')
  adminFindOne(@Param('id') id: string) {
    return this.bookingsService.findOne(id);
  }
}
