// 在庫不足商品の詳細情報
export interface StockShortageDetail {
  productId: number
  productName: string
  requestedQuantity: number
  availableStock: number
}

export interface UnavailableProductDetail {
  productId: number
  productName: string
}

// APIエラー情報
export interface ApiError {
  code: string
  message: string
  details?: StockShortageDetail[] | UnavailableProductDetail[]
}

// APIレスポンス共通型
export interface ApiResponse<T = unknown> {
  success: boolean
  data?: T
  error?: ApiError
}
