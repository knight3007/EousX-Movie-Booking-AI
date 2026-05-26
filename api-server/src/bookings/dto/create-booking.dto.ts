import {
  ArrayNotEmpty,
  ArrayUnique,
  IsArray,
  IsString,
} from 'class-validator';

export class CreateBookingDto {
  @IsString()
  showtimeId: string;

  @IsArray()
  @ArrayNotEmpty()
  @ArrayUnique()
  @IsString({ each: true })
  lockIds: string[];
}
