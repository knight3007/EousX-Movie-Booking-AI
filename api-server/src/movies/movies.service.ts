import { Injectable, NotFoundException } from '@nestjs/common';
import { MovieStatus } from '@prisma/client';
import { PrismaService } from '../database/prisma.service';
import { CreateMovieDto } from './dto/create-movie.dto';
import { UpdateMovieDto } from './dto/update-movie.dto';

@Injectable()
export class MoviesService {
  constructor(private readonly prisma: PrismaService) {}

  findAll() {
    return this.prisma.movie.findMany({
      orderBy: {
        createdAt: 'desc',
      },
    });
  }

  findNowShowing() {
    return this.prisma.movie.findMany({
      where: {
        status: MovieStatus.NOW_SHOWING,
      },
      orderBy: {
        createdAt: 'desc',
      },
    });
  }

  findUpcoming() {
    return this.prisma.movie.findMany({
      where: {
        status: MovieStatus.UPCOMING,
      },
      orderBy: {
        createdAt: 'desc',
      },
    });
  }

  async findOne(id: string) {
    const movie = await this.prisma.movie.findUnique({
      where: { id },
    });

    if (!movie) {
      throw new NotFoundException('Movie not found');
    }

    return movie;
  }

  create(dto: CreateMovieDto) {
    return this.prisma.movie.create({
      data: dto,
    });
  }

  async update(id: string, dto: UpdateMovieDto) {
    await this.findOne(id);

    return this.prisma.movie.update({
      where: { id },
      data: dto,
    });
  }

  async remove(id: string) {
    await this.findOne(id);

    return this.prisma.movie.delete({
      where: { id },
    });
  }
}