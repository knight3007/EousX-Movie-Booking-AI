import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Post,
  UseGuards,
} from '@nestjs/common';
import { AuthUser } from '../auth/auth.types';
import { CurrentUser } from '../auth/current-user.decorator';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { SeatLocksService } from './seat-locks.service';
import { LockSeatsDto } from './dto/lock-seats.dto';

@Controller()
export class SeatLocksController {
  constructor(private readonly seatLocksService: SeatLocksService) {}

  @Post('showtimes/:showtimeId/seat-locks')
  @UseGuards(JwtAuthGuard)
  lockSeats(
    @Param('showtimeId') showtimeId: string,
    @Body() dto: LockSeatsDto,
    @CurrentUser() user: AuthUser,
  ) {
    return this.seatLocksService.lockSeats(showtimeId, user.id, dto);
  }

  @Get('seat-locks/:lockId')
  getLock(@Param('lockId') lockId: string) {
    return this.seatLocksService.getLock(lockId);
  }

  @Delete('seat-locks/:lockId')
  @UseGuards(JwtAuthGuard)
  releaseLock(
    @Param('lockId') lockId: string,
    @CurrentUser() user: AuthUser,
  ) {
    return this.seatLocksService.releaseLock(lockId, user.id);
  }
}
