import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class CreateReturnItemDto {
  @ApiProperty()
  orderItemId!: number;

  @ApiProperty()
  quantity!: number;
}

export class CreateReturnRequestDto {
  @ApiProperty()
  reason!: string;

  @ApiProperty({ type: () => CreateReturnItemDto, isArray: true })
  items!: CreateReturnItemDto[];
}

export class RejectReturnRequestDto {
  @ApiProperty()
  reason!: string;
}

export class ReturnShipmentItemDto {
  @ApiProperty()
  shipmentItemId!: number;

  @ApiProperty()
  orderItemId!: number;

  @ApiProperty()
  productId!: number;

  @ApiProperty()
  productName!: string;

  @ApiProperty()
  quantity!: number;

  @ApiProperty()
  unitPrice!: number;

  @ApiProperty()
  subtotal!: number;
}

export class ReturnShipmentDto {
  @ApiProperty()
  shipmentId!: number;

  @ApiProperty()
  orderId!: number;

  @ApiProperty()
  orderNumber!: string;

  @ApiProperty()
  status!: string;

  @ApiProperty()
  statusLabel!: string;

  @ApiProperty()
  reason!: string;

  @ApiPropertyOptional()
  rejectionReason?: string | null;

  @ApiProperty({ type: () => ReturnShipmentItemDto, isArray: true })
  items!: ReturnShipmentItemDto[];

  @ApiProperty()
  createdAt!: string;

  @ApiProperty()
  updatedAt!: string;
}

export class ReturnListPaginationDto {
  @ApiProperty()
  page!: number;

  @ApiProperty()
  limit!: number;

  @ApiProperty()
  totalCount!: number;
}

export class ReturnListResponseDto {
  @ApiProperty({ type: () => ReturnShipmentDto, isArray: true })
  returns!: ReturnShipmentDto[];

  @ApiProperty({ type: () => ReturnListPaginationDto })
  pagination!: ReturnListPaginationDto;
}
