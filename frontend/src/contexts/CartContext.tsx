import { createContext, useContext, useState, useEffect } from 'react'
import type { ReactNode } from 'react'
import type { Product, CartItem as ApiCartItem } from '../types/api'
import * as api from '../lib/api'
import { getUserFriendlyMessage } from '../lib/errorMessages'

// カート内の商品アイテム（APIの型を再エクスポート）
export type CartItem = ApiCartItem

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

  // カート情報を取得
  const refreshCart = async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.getCart()
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

  // 初回マウント時にカートを取得
  useEffect(() => {
    refreshCart()
  }, [])

  // カートに商品を追加
  const addToCart = async (product: Product, quantity = 1) => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.addToCart({
        productId: product.id,
        quantity,
      })
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

  // カートから商品を削除
  const removeFromCart = async (itemId: number) => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.removeFromCart(itemId)
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

  // 数量を更新
  const updateQuantity = async (itemId: number, quantity: number) => {
    if (quantity <= 0) {
      await removeFromCart(itemId)
      return
    }
    if (quantity > 9) {
      return
    }

    setLoading(true)
    setError(null)
    try {
      const response = await api.updateCartItemQuantity(itemId, { quantity })
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

  // カートをクリア（ローカルのみ）
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
        addToCart,
        removeFromCart,
        updateQuantity,
        refreshCart,
        clearCart,
      }}
    >
      {children}
    </CartContext.Provider>
  )
}

// カスタムフック
// eslint-disable-next-line react-refresh/only-export-components
export function useCart() {
  const context = useContext(CartContext)
  if (context === undefined) {
    throw new Error('useCart must be used within a CartProvider')
  }
  return context
}
