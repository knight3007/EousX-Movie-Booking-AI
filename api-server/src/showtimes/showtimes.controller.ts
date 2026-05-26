import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Post,
  Put,
  Query,
} from '@nestjs/common';
import { ShowtimesService } from './showtimes.service';
import { CreateShowtimeDto } from './dto/create-showtime.dto';
import { UpdateShowtimeDto } from './dto/update-showtime.dto';

@Controller()
export class ShowtimesController {
  constructor(private readonly showtimesService: ShowtimesService) {}

  @Get('movies/:movieId/showtimes')
  findByMovie(
    @Param('movieId') movieId: string,
    @Query('date') date?: string,
  ) {
    return this.showtimesService.findByMovie(movieId, date);
  }

  @Get('showtimes')
  findAll(@Query('date') date?: string) {
    return this.showtimesService.findAll(date);
  }

  @Get('showtimes/:id')
  findOne(@Param('id') id: string) {
    return this.showtimesService.findOne(id);
  }

  @Get('admin/showtimes')
  adminFindAll(@Query('date') date?: string) {
    return this.showtimesService.findAll(date);
  }

  @Post('admin/showtimes')
  create(@Body() dto: CreateShowtimeDto) {
    return this.showtimesService.create(dto);
  }

  @Put('admin/showtimes/:id')
  update(@Param('id') id: string, @Body() dto: UpdateShowtimeDto) {
    return this.showtimesService.update(id, dto);
  }

  @Delete('admin/showtimes/:id')
  cancel(@Param('id') id: string) {
    return this.showtimesService.cancel(id);
  }
}