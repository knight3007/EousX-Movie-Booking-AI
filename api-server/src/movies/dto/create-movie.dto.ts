import {
  IsArray,
  IsEnum,
  IsInt,
  IsNumber,
  IsOptional,
  IsString,
  Min,
} from 'class-validator';
import { MovieStatus } from '@prisma/client';

export class CreateMovieDto {
  @IsString()
  title: string;

  @IsOptional()
  @IsString()
  originalTitle?: string;

  @IsString()
  overview: string;

  @IsOptional()
  @IsString()
  posterUrl?: string;

  @IsOptional()
  @IsString()
  backdropUrl?: string;

  @IsOptional()
  @IsString()
  trailerKey?: string;

  @IsInt()
  @Min(1)
  runtime: number;

  @IsArray()
  @IsString({ each: true })
  genres: string[];

  @IsNumber()
  rating: number;

  @IsString()
  ageRating: string;

  @IsEnum(MovieStatus)
  status: MovieStatus;
}