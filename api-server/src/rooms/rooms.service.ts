import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../database/prisma.service';

@Injectable()
export class RoomsService {
  constructor(private readonly prisma: PrismaService) {}

  findAll() {
    return this.prisma.room.findMany({
      include: {
        cinema: true,
        _count: {
          select: {
            seats: true,
            showtimes: true,
          },
        },
      },
      orderBy: {
        name: 'asc',
      },
    });
  }

  async findOne(id: string) {
    const room = await this.prisma.room.findUnique({
      where: { id },
      include: {
        cinema: true,
        seats: {
          orderBy: [
            { row: 'asc' },
            { number: 'asc' },
          ],
        },
        _count: {
          select: {
            showtimes: true,
          },
        },
      },
    });

    if (!room) {
      throw new NotFoundException('Room not found');
    }

    return room;
  }
}