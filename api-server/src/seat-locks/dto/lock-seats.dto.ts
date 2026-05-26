import {
  ArrayNotEmpty,
  ArrayUnique,
  IsArray,
  IsString,
} from 'class-validator';

export class LockSeatsDto {
  @IsArray()
  @ArrayNotEmpty()
  @ArrayUnique()
  @IsString({ each: true })
  seatIds: string[];
}
