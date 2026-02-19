import { createContext, useContext, useState, useEffect } from 'react'
import type { ReactNode } from 'react'
import type { Product } from '@entities/product'
import type { CartItem, AddToCartRequest, UpdateQuantityRequest } from '@entities/cart'
import { getCart, addToCart, updateCartItemQuantity, removeFromCart } from '@entities/cart'
import { getUserFriendlyMessage } from '@shared/lib/errorMessages'

interface CartContextType {
  items: CartItem[]
  totalQuantity: number
  totalPrice: number
  loading: boolean
  error: string | null
  addToCart: (product: Product, quantity?: number) => Promise<void>
  removeFromCart: (itemId: number) => Promise<void>
  updateQuantity: (itemId: number, quantity: number) => Promise<void>
  refreshCart: () => Promise<void>
  clearCart: () => void
}

const CartContext = createContext<CartContextType | undefined>(undefined)

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([])
  const [totalQuantity, setTotalQuantity] = useState(0)
  const [totalPrice, setTotalPrice] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const refreshCart = async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await getCart()
      if (response.success && response.data) {
        setItems(response.data.items)
        setTotalQuantity(response.data.totalQuantity)
        setTotalPrice(response.data.totalPrice)
      } else {
        const message = response.error?.code
          ? getUserFriendlyMessage(response.error.code)
          : 'カートの取得に失敗しました'
        setError(message)
      }
    } catch (err) {
      setError('カートの取得中にエラーが発生しました')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    refreshCart()
  }, [])

  const addToCartHandler = async (product: Product, quantity = 1) => {
    setLoading(true)
    setError(null)
    try {
      const request: AddToCartRequest = { productId: product.id, quantity }
      const response = await addToCart(request)
      if (response.success && response.data) {
        setItems(response.data.items)
        setTotalQuantity(response.data.totalQuantity)
        setTotalPrice(response.data.totalPrice)
      } else {
        const message = response.error?.code
          ? getUserFriendlyMessage(response.error.code)
          : 'カートへの追加に失敗しました'
        throw new Error(message)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'エラーが発生しました')
      console.error('カート追加エラー:', err)
      throw err
    } finally {
      setLoading(false)
    }
  }

  const removeFromCartHandler = async (itemId: number) => {
    setLoading(true)
    setError(null)
    try {
      const response = await removeFromCart(itemId)
      if (response.success && response.data) {
        setItems(response.data.items)
        setTotalQuantity(response.data.totalQuantity)
        setTotalPrice(response.data.totalPrice)
      } else {
        const message = response.error?.code
          ? getUserFriendlyMessage(response.error.code)
          : 'カートからの削除に失敗しました'
        throw new Error(message)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'エラーが発生しました')
      console.error('カート削除エラー:', err)
      throw err
    } finally {
      setLoading(false)
    }
  }

  const updateQuantity = async (itemId: number, quantity: number) => {
    if (quantity <= 0) {
      await removeFromCartHandler(itemId)
      return
    }
    if (quantity > 9) return

    setLoading(true)
    setError(null)
    try {
      const request: UpdateQuantityRequest = { quantity }
      const response = await updateCartItemQuantity(itemId, request)
      if (response.success && response.data) {
        setItems(response.data.items)
        setTotalQuantity(response.data.totalQuantity)
        setTotalPrice(response.data.totalPrice)
      } else {
        const message = response.error?.code
          ? getUserFriendlyMessage(response.error.code)
          : '数量の変更に失敗しました'
        throw new Error(message)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'エラーが発生しました')
      console.error('数量更新エラー:', err)
      throw err
    } finally {
      setLoading(false)
    }
  }

  const clearCart = () => {
    setItems([])
    setTotalQuantity(0)
    setTotalPrice(0)
  }

  return (
    <CartContext.Provider
      value={{
        items,
        totalQuantity,
        totalPrice,
        loading,
        error,
        addToCart: addToCartHandler,
        removeFromCart: removeFromCartHandler,
        updateQuantity,
        refreshCart,
        clearCart,
      }}
    >
      {children}
    </CartContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useCart() {
  const context = useContext(CartContext)
  if (context === undefined) {
    throw new Error('useCart must be used within a CartProvider')
  }
  return context
}
