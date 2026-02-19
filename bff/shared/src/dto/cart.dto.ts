import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { ProductDto } from './product.dto';

export class CartDto {
  @ApiProperty({ type: () => CartItemDto, isArray: true })
  items!: CartItemDto[];

  @ApiProperty()
  totalAmount!: number;

  @ApiProperty()
  totalItems!: number;
}

export class CartItemDto {
  @ApiProperty()
  id!: number;

  @ApiProperty()
  productId!: number;

  @ApiProperty()
  quantity!: number;

  @ApiPropertyOptional({ type: () => ProductDto })
  product?: ProductDto;

  @ApiPropertyOptional()
  subtotal?: number;
}
