import { IsOptional, IsString } from 'class-validator';

export class MockPaymentSuccessDto {
  @IsString()
  bookingId: string;

  @IsOptional()
  @IsString()
  provider?: string;
}