import { IsString } from 'class-validator';

export class CreateSepayPaymentDto {
  @IsString()
  bookingId: string;
}