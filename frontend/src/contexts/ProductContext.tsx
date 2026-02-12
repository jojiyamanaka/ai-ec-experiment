import { createContext, useContext, useState, useEffect } from 'react'
import type { ReactNode } from 'react'
import type { Product, UpdateProductRequest } from '../types/api'
import * as api from '../lib/api'
import { getUserFriendlyMessage } from '../lib/errorMessages'

interface ProductContextType {
  products: Product[]
  loading: boolean
  error: string | null
  refreshProducts: () => Promise<void>
  getPublishedProducts: () => Product[]
  updateProduct: (id: number, updates: UpdateProductRequest) => Promise<void>
}

const ProductContext = createContext<ProductContextType | undefined>(undefined)

export function ProductProvider({ children }: { children: ReactNode }) {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // 商品一覧を取得
  const refreshProducts = async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.getItems()
      if (response.success && response.data) {
        setProducts(response.data.items)
      } else {
        const message = response.error?.code
          ? getUserFriendlyMessage(response.error.code)
          : '商品の取得に失敗しました'
        setError(message)
      }
    } catch (err) {
      setError('商品の取得中にエラーが発生しました')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  // 初回マウント時に商品一覧を取得
  useEffect(() => {
    refreshProducts()
  }, [])

  // 公開されている商品のみを取得
  const getPublishedProducts = () => {
    return products.filter((product) => product.isPublished)
  }

  // 商品情報を更新
  const updateProduct = async (id: number, updates: UpdateProductRequest) => {
    try {
      const response = await api.updateItem(id, updates)
      if (response.success && response.data) {
        // ローカル状態を更新
        setProducts((prevProducts) =>
          prevProducts.map((product) =>
            product.id === id ? response.data! : product
          )
        )
      } else {
        const message = response.error?.code
          ? getUserFriendlyMessage(response.error.code)
          : '商品の更新に失敗しました'
        throw new Error(message)
      }
    } catch (err) {
      console.error('商品更新エラー:', err)
      throw err
    }
  }

  return (
    <ProductContext.Provider
      value={{
        products,
        loading,
        error,
        refreshProducts,
        getPublishedProducts,
        updateProduct,
      }}
    >
      {children}
    </ProductContext.Provider>
  )
}

// カスタムフック
// eslint-disable-next-line react-refresh/only-export-components
export function useProducts() {
  const context = useContext(ProductContext)
  if (context === undefined) {
    throw new Error('useProducts must be used within a ProductProvider')
  }
  return context
}
