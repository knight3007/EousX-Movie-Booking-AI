import { Controller, Get, Param } from '@nestjs/common';
import { SeatsService } from './seats.service';

@Controller()
export class SeatsController {
  constructor(private readonly seatsService: SeatsService) {}

  @Get('showtimes/:showtimeId/seats')
  getSeatMapByShowtime(@Param('showtimeId') showtimeId: string) {
    return this.seatsService.getSeatMapByShowtime(showtimeId);
  }

  @Get('admin/showtimes/:showtimeId/seats')
  adminGetSeatMapByShowtime(@Param('showtimeId') showtimeId: string) {
    return this.seatsService.getSeatMapByShowtime(showtimeId);
  }
}