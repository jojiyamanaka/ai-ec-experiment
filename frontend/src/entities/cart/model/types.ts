import type { Product } from '@entities/product'

export interface CartItem {
  id: number
  product: Product
  quantity: number
}

export interface Cart {
  items: CartItem[]
  totalQuantity: number
  totalPrice: number
}

export interface AddToCartRequest {
  productId: number
  quantity?: number
}

export interface UpdateQuantityRequest {
  quantity: number
}
