import type { Product } from '@entities/product'

export interface OrderItem {
  product: Product
  quantity: number
  orderedQuantity: number
  committedQuantity: number
  subtotal: number
}

export interface Order {
  orderId: number
  orderNumber: string
  userId?: number
  userEmail?: string
  userDisplayName?: string
  items: OrderItem[]
  totalPrice: number
  orderedQuantity: number
  committedQuantity: number
  status: string
  statusLabel?: string
  createdAt: string
  updatedAt?: string
}

export interface CreateOrderRequest {
  cartId: string
}
