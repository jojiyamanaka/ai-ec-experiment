import { ApiProperty } from '@nestjs/swagger';

export class ProductDto {
  @ApiProperty()
  id!: number;

  @ApiProperty()
  name!: string;

  @ApiProperty()
  price!: number;

  @ApiProperty()
  stock!: number;

  @ApiProperty()
  imageUrl!: string;

  @ApiProperty()
  description!: string;

  @ApiProperty()
  isPublic!: boolean;
}
