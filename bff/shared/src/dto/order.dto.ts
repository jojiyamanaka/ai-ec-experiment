import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class OrderDto {
  @ApiProperty()
  id!: number;

  @ApiProperty()
  orderNumber!: string;

  @ApiPropertyOptional()
  userId?: number;

  @ApiProperty()
  status!: string;

  @ApiProperty()
  totalAmount!: number;

  @ApiProperty()
  orderedQuantity!: number;

  @ApiProperty()
  allocatedQuantity!: number;

  @ApiProperty()
  createdAt!: string;

  @ApiProperty({ type: () => OrderItemDto, isArray: true })
  items!: OrderItemDto[];
}

export class OrderItemDto {
  @ApiProperty()
  id!: number;

  @ApiProperty()
  productId!: number;

  @ApiProperty()
  productName!: string;

  @ApiProperty()
  quantity!: number;

  @ApiProperty()
  orderedQuantity!: number;

  @ApiProperty()
  allocatedQuantity!: number;

  @ApiProperty()
  price!: number;
}
