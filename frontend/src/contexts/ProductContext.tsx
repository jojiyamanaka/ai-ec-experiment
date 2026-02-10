import { createContext, useContext, useState, ReactNode } from 'react'
import { mockProducts, type Product } from '../data/mockProducts'

interface ProductContextType {
  products: Product[]
  getPublishedProducts: () => Product[]
  updateProduct: (id: number, updates: Partial<Product>) => void
}

const ProductContext = createContext<ProductContextType | undefined>(undefined)

export function ProductProvider({ children }: { children: ReactNode }) {
  const [products, setProducts] = useState<Product[]>(mockProducts)

  // 公開されている商品のみを取得
  const getPublishedProducts = () => {
    return products.filter((product) => product.isPublished)
  }

  // 商品情報を更新
  const updateProduct = (id: number, updates: Partial<Product>) => {
    setProducts((prevProducts) =>
      prevProducts.map((product) =>
        product.id === id ? { ...product, ...updates } : product
      )
    )
  }

  return (
    <ProductContext.Provider
      value={{
        products,
        getPublishedProducts,
        updateProduct,
      }}
    >
      {children}
    </ProductContext.Provider>
  )
}

// カスタムフック
export function useProducts() {
  const context = useContext(ProductContext)
  if (context === undefined) {
    throw new Error('useProducts must be used within a ProductProvider')
  }
  return context
}
