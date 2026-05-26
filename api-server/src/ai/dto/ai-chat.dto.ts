import { IsNotEmpty, IsString, MaxLength } from 'class-validator';

export class AiChatDto {
  @IsString()
  @IsNotEmpty()
  @MaxLength(1000)
  message: string;
}
