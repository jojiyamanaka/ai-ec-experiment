import { ApiProperty } from '@nestjs/swagger';

export class BoUserDto {
  @ApiProperty()
  id!: number;

  @ApiProperty()
  username!: string;

  @ApiProperty()
  email!: string;

  @ApiProperty()
  createdAt!: string;
}
