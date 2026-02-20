import { ApiProperty } from '@nestjs/swagger';

export class ProductDto {
  @ApiProperty()
  id!: number;

  @ApiProperty()
  name!: string;

  @ApiProperty()
  price!: number;

  @ApiProperty()
  allocationType!: 'REAL' | 'FRAME';

  @ApiProperty()
  effectiveStock!: number;

  @ApiProperty()
  imageUrl!: string;

  @ApiProperty()
  description!: string;

  @ApiProperty()
  isPublic!: boolean;
}
