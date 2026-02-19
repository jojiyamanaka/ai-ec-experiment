import { ApiProperty } from '@nestjs/swagger';

export class InventoryDto {
  @ApiProperty()
  id!: number;

  @ApiProperty()
  productName!: string;

  @ApiProperty()
  stock!: number;

  @ApiProperty()
  reservedStock!: number;

  @ApiProperty()
  availableStock!: number;

  @ApiProperty()
  lastUpdated!: string;
}
