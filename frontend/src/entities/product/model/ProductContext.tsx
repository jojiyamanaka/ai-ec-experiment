import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import type { ReactNode } from 'react'
import type {
  CreateProductCategoryRequest,
  CreateProductRequest,
  Product,
  ProductCategory,
  UpdateProductCategoryRequest,
  UpdateProductRequest,
} from './types'
import {
  createAdminItem,
  createAdminItemCategory,
  getAdminItemCategories,
  getAdminItems,
  getItems,
  updateAdminItem,
  updateAdminItemCategory,
  updateItem,
} from './api'
import { getUserFriendlyMessage } from '@shared/lib/errorMessages'
import { APP_MODE } from '@shared/config/env'

interface ProductContextType {
  products: Product[]
  categories: ProductCategory[]
  loading: boolean
  error: string | null
  refreshProducts: () => Promise<void>
  refreshCategories: () => Promise<void>
  getPublishedProducts: () => Product[]
  createProduct: (payload: CreateProductRequest) => Promise<void>
  updateProduct: (id: number, updates: UpdateProductRequest) => Promise<void>
  createCategory: (payload: CreateProductCategoryRequest) => Promise<void>
  updateCategory: (id: number, updates: UpdateProductCategoryRequest) => Promise<void>
}

const ProductContext = createContext<ProductContextType | undefined>(undefined)

function hasBoToken(): boolean {
  if (typeof window === 'undefined') {
    return false
  }
  return Boolean(window.localStorage.getItem('bo_token'))
}

export function ProductProvider({ children }: { children: ReactNode }) {
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<ProductCategory[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const isAdminMode = APP_MODE === 'admin'

  // 商品一覧を取得
  const refreshProducts = useCallback(async () => {
    if (isAdminMode && !hasBoToken()) {
      setProducts([])
      setLoading(false)
      setError(null)
      return
    }

    setLoading(true)
    setError(null)
    try {
      const response = isAdminMode ? await getAdminItems() : await getItems()
      if (response.success && response.data) {
        if (!isAdminMode || hasBoToken()) {
          setProducts(response.data.items)
        }
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
  }, [isAdminMode])

  const refreshCategories = useCallback(async () => {
    if (!isAdminMode) {
      setCategories([])
      return
    }
    if (!hasBoToken()) {
      setCategories([])
      return
    }
    try {
      const response = await getAdminItemCategories()
      if (response.success && response.data) {
        if (hasBoToken()) {
          setCategories(response.data)
        }
      }
    } catch (err) {
      console.error(err)
    }
  }, [isAdminMode])

  useEffect(() => {
    if (!isAdminMode) {
      void refreshProducts()
      return
    }

    if (!hasBoToken()) {
      setProducts([])
      setCategories([])
      setLoading(false)
      setError(null)
      return
    }

    void refreshProducts()
    void refreshCategories()
  }, [isAdminMode, refreshProducts, refreshCategories])

  useEffect(() => {
    if (!isAdminMode) {
      return
    }

    const handleAuthenticated = () => {
      void refreshProducts()
      void refreshCategories()
    }
    const handleUnauthorized = () => {
      setProducts([])
      setCategories([])
      setLoading(false)
      setError(null)
    }

    window.addEventListener('bo-auth:authenticated', handleAuthenticated)
    window.addEventListener('bo-auth:unauthorized', handleUnauthorized)
    return () => {
      window.removeEventListener('bo-auth:authenticated', handleAuthenticated)
      window.removeEventListener('bo-auth:unauthorized', handleUnauthorized)
    }
  }, [isAdminMode, refreshProducts, refreshCategories])

  // 公開されている商品のみを取得
  const getPublishedProducts = () => {
    return products.filter((product) => product.isPublished)
  }

  // 商品情報を更新
  const updateProduct = async (id: number, updates: UpdateProductRequest) => {
    try {
      const response = isAdminMode
        ? await updateAdminItem(id, updates)
        : await updateItem(id, updates)
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

  const createProduct = async (payload: CreateProductRequest) => {
    if (!isAdminMode) {
      return
    }
    const response = await createAdminItem(payload)
    if (response.success && response.data) {
      setProducts((prevProducts) => [response.data!, ...prevProducts])
      return
    }
    const message = response.error?.code
      ? getUserFriendlyMessage(response.error.code)
      : '商品の登録に失敗しました'
    throw new Error(message)
  }

  const createCategory = async (payload: CreateProductCategoryRequest) => {
    if (!isAdminMode) {
      return
    }
    const response = await createAdminItemCategory(payload)
    if (response.success) {
      await refreshCategories()
      return
    }
    const message = response.error?.code
      ? getUserFriendlyMessage(response.error.code)
      : 'カテゴリの登録に失敗しました'
    throw new Error(message)
  }

  const updateCategory = async (id: number, updates: UpdateProductCategoryRequest) => {
    if (!isAdminMode) {
      return
    }
    const response = await updateAdminItemCategory(id, updates)
    if (response.success) {
      await refreshCategories()
      return
    }
    const message = response.error?.code
      ? getUserFriendlyMessage(response.error.code)
      : 'カテゴリの更新に失敗しました'
    throw new Error(message)
  }

  return (
    <ProductContext.Provider
      value={{
        products,
        categories,
        loading,
        error,
        refreshProducts,
        refreshCategories,
        getPublishedProducts,
        createProduct,
        updateProduct,
        createCategory,
        updateCategory,
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
