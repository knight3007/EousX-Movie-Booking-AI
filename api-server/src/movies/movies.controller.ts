import { Body, Controller, Delete, Get, Param, Post, Put } from '@nestjs/common';
import { MoviesService } from './movies.service';
import { CreateMovieDto } from './dto/create-movie.dto';
import { UpdateMovieDto } from './dto/update-movie.dto';

@Controller()
export class MoviesController {
  constructor(private readonly moviesService: MoviesService) {}

  @Get('movies')
  findAll() {
    return this.moviesService.findAll();
  }

  @Get('movies/now-showing')
  findNowShowing() {
    return this.moviesService.findNowShowing();
  }

  @Get('movies/upcoming')
  findUpcoming() {
    return this.moviesService.findUpcoming();
  }

  @Get('movies/:id')
  findOne(@Param('id') id: string) {
    return this.moviesService.findOne(id);
  }

  @Post('admin/movies')
  create(@Body() dto: CreateMovieDto) {
    return this.moviesService.create(dto);
  }

  @Put('admin/movies/:id')
  update(@Param('id') id: string, @Body() dto: UpdateMovieDto) {
    return this.moviesService.update(id, dto);
  }

  @Delete('admin/movies/:id')
  remove(@Param('id') id: string) {
    return this.moviesService.remove(id);
  }
}