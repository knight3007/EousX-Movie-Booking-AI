import { Controller, Get, Param } from '@nestjs/common';
import { RoomsService } from './rooms.service';

@Controller()
export class RoomsController {
  constructor(private readonly roomsService: RoomsService) {}

  @Get('rooms')
  findAll() {
    return this.roomsService.findAll();
  }

  @Get('rooms/:id')
  findOne(@Param('id') id: string) {
    return this.roomsService.findOne(id);
  }

  @Get('admin/rooms')
  adminFindAll() {
    return this.roomsService.findAll();
  }

  @Get('admin/rooms/:id')
  adminFindOne(@Param('id') id: string) {
    return this.roomsService.findOne(id);
  }
}