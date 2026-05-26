import {
  IsDateString,
  IsEnum,
  IsInt,
  IsOptional,
  IsString,
  Min,
} from 'class-validator';
import { ShowtimeStatus } from '@prisma/client';

export class CreateShowtimeDto {
  @IsString()
  movieId: string;

  @IsString()
  roomId: string;

  @IsDateString()
  startTime: string;

  @IsOptional()
  @IsDateString()
  endTime?: string;

  @IsInt()
  @Min(0)
  basePrice: number;

  @IsOptional()
  @IsEnum(ShowtimeStatus)
  status?: ShowtimeStatus;
}