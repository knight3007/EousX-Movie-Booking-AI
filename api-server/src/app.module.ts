import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { PrismaModule } from './database/prisma.module';
import { AppController } from './app.controller';
import { MoviesModule } from './movies/movies.module';
import { RoomsModule } from './rooms/rooms.module';
import { ShowtimesModule } from './showtimes/showtimes.module';
import { SeatsModule } from './seats/seats.module';
import { SeatLocksModule } from './seat-locks/seat-locks.module';
import { BookingsModule } from './bookings/bookings.module';
import { PaymentsModule } from './payments/payments.module';
import { TicketsModule } from './tickets/tickets.module';
import { AuthModule } from './auth/auth.module';
import { AiModule } from './ai/ai.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    PrismaModule,
    MoviesModule,
    RoomsModule,
    ShowtimesModule,
    SeatsModule,
    SeatLocksModule,
    BookingsModule,
    PaymentsModule,
    TicketsModule,
    AuthModule,
    AiModule,
  ],
  controllers: [AppController],
})
export class AppModule {}
