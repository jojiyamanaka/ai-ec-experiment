export type { Product, ProductListResponse, UpdateProductRequest } from './model/types'
export { useProducts, ProductProvider } from './model/ProductContext'
export { getItems, getItemById, updateItem } from './model/api'
export { default as ProductCard } from './ui/ProductCard'
