import type { ProductDto } from './product.dto';

export interface CartDto {
  items: CartItemDto[];
  totalAmount: number;
  totalItems: number;
}

export interface CartItemDto {
  id: number;
  productId: number;
  quantity: number;
  product?: ProductDto;
  subtotal?: number;
}
