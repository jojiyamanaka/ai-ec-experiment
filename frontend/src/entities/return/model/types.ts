export interface ReturnShipmentItem {
  shipmentItemId: number
  orderItemId: number
  productId: number
  productName: string
  quantity: number
  unitPrice: number
  subtotal: number
}

export interface ReturnShipment {
  shipmentId: number
  orderId: number
  orderNumber: string
  status: string
  statusLabel: string
  reason: string
  rejectionReason?: string | null
  items: ReturnShipmentItem[]
  createdAt: string
  updatedAt: string
}

export interface ReturnShipmentSummary {
  shipmentId: number
  status: string
  statusLabel: string
  createdAt: string
}

export interface CreateReturnRequest {
  reason: string
  items: Array<{
    orderItemId: number
    quantity: number
  }>
}

export interface RejectReturnRequest {
  reason: string
}

export interface ReturnListResponse {
  returns: ReturnShipment[]
  pagination: {
    page: number
    limit: number
    totalCount: number
  }
}
