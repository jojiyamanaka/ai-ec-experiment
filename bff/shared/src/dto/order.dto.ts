export interface OrderDto {
  id: number;
  orderNumber: string;
  userId?: number;
  status: string;
  totalAmount: number;
  createdAt: string;
  items: OrderItemDto[];
}

export interface OrderItemDto {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  price: number;
}
