import { Module } from '@nestjs/common';
import { SeatLocksController } from './seat-locks.controller';
import { SeatLocksService } from './seat-locks.service';

@Module({
  controllers: [SeatLocksController],
  providers: [SeatLocksService],
  exports: [SeatLocksService],
})
export class SeatLocksModule {}