import { useEffect, useMemo, useState } from 'react'
import { getAllOrders, confirmOrder, shipOrder, deliverOrder, cancelOrder } from '@entities/order'
import type { Order } from '@entities/order'

const STATUS_LABELS: Record<string, string> = {
  ALL: 'すべて',
  PENDING: '作成済み',
  CONFIRMED: '確認済み',
  SHIPPED: '発送済み',
  DELIVERED: '配達完了',
  CANCELLED: 'キャンセル',
}

function getStatusLabel(status: string): string {
  return STATUS_LABELS[status] ?? status
}

export default function AdminOrderPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState<string>('ALL')
  const [searchInput, setSearchInput] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null)
  const [showDetailModal, setShowDetailModal] = useState(false)

  const fetchOrders = async () => {
    const response = await getAllOrders()
    if (response.success && response.data) {
      setOrders(response.data)
    }
    setLoading(false)
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchOrders()
  }, [])

  const handleStatusChange = async (
    orderId: number,
    action: 'confirm' | 'ship' | 'deliver' | 'cancel'
  ) => {
    if (
      !window.confirm(
        `この注文を${
          action === 'confirm'
            ? '確認'
            : action === 'ship'
              ? '発送'
              : action === 'deliver'
                ? '配達完了'
                : 'キャンセル'
        }しますか？`
      )
    ) {
      return
    }

    let response
    switch (action) {
      case 'confirm':
        response = await confirmOrder(orderId)
        break
      case 'ship':
        response = await shipOrder(orderId)
        break
      case 'deliver':
        response = await deliverOrder(orderId)
        break
      case 'cancel':
        response = await cancelOrder(orderId)
        break
    }

    if (response.success) {
      await fetchOrders()
      alert('ステータスを更新しました')
    } else {
      alert(response.error?.message || '更新に失敗しました')
    }
  }

  const handleSearch = () => {
    setSearchQuery(searchInput.trim())
  }

  const handleOrderClick = (orderId: number) => {
    setSelectedOrderId(orderId)
    setShowDetailModal(true)
  }

  const filteredOrders = useMemo(() => {
    return orders
      .filter((order) => filter === 'ALL' || order.status === filter)
      .filter((order) =>
        order.orderNumber.toLowerCase().includes(searchQuery.toLowerCase())
      )
  }, [orders, filter, searchQuery])

  const selectedOrder = useMemo(() => {
    return orders.find((order) => order.orderId === selectedOrderId) ?? null
  }, [orders, selectedOrderId])

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-6 py-8">
        <div className="text-center text-gray-600">読み込み中...</div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-7xl px-6 py-8">
      {/* ヘッダー */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-zinc-900">注文管理</h1>
      </div>

      {/* 検索エリア */}
      <div className="mb-4 flex gap-2">
        <input
          type="text"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              handleSearch()
            }
          }}
          placeholder="注文番号で検索"
          className="w-full max-w-md rounded-lg border px-4 py-2"
        />
        <button
          onClick={handleSearch}
          className="rounded-lg bg-gray-800 px-4 py-2 text-white hover:bg-gray-900"
        >
          検索
        </button>
      </div>

      {/* フィルタエリア */}
      <div className="mb-6 flex gap-2">
        {['ALL', 'PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'].map((status) => (
          <button
            key={status}
            onClick={() => setFilter(status)}
            className={`rounded-lg px-4 py-2 font-medium ${
              filter === status
                ? 'bg-blue-600 text-white'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            {getStatusLabel(status)}
          </button>
        ))}
      </div>

      {/* テーブル */}
      <div className="overflow-hidden rounded-lg bg-white shadow">
        <table className="w-full">
          <thead className="bg-gray-50 border-b">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">注文番号</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">注文日時</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">会員メール</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">会員表示名</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">ステータス</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">合計金額</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {filteredOrders.map((order) => (
              <tr key={order.orderId} className="hover:bg-gray-50">
                <td className="px-6 py-4">
                  <button
                    onClick={() => handleOrderClick(order.orderId)}
                    className="font-mono text-blue-600 hover:underline"
                  >
                    {order.orderNumber}
                  </button>
                </td>
                <td className="px-6 py-4 text-sm text-gray-900">
                  {new Date(order.createdAt).toLocaleString('ja-JP')}
                </td>
                <td className="px-6 py-4 text-sm text-gray-900">{order.userEmail || 'ゲスト'}</td>
                <td className="px-6 py-4 text-sm text-gray-900">{order.userDisplayName || '—'}</td>
                <td className="px-6 py-4">
                  <span className={`rounded-full px-2 py-1 text-xs ${
                    order.status === 'PENDING'
                      ? 'bg-gray-100 text-gray-800'
                      : order.status === 'CONFIRMED'
                        ? 'bg-blue-100 text-blue-800'
                        : order.status === 'SHIPPED'
                          ? 'bg-purple-100 text-purple-800'
                          : order.status === 'DELIVERED'
                            ? 'bg-green-100 text-green-800'
                            : 'bg-red-100 text-red-800'
                  }`}>
                    {getStatusLabel(order.status)}
                  </span>
                </td>
                <td className="px-6 py-4 text-sm tabular-nums text-gray-900">
                  {order.totalPrice.toLocaleString()}円
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showDetailModal && selectedOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-3xl rounded-lg bg-white p-6 max-h-[90vh] overflow-y-auto">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-2xl font-bold text-zinc-900">注文詳細</h2>
              <button
                onClick={() => setShowDetailModal(false)}
                className="rounded bg-gray-200 px-3 py-1 text-sm hover:bg-gray-300"
              >
                閉じる
              </button>
            </div>

            <div className="mb-6 grid gap-2 text-sm text-gray-700">
              <p>
                <span className="font-medium text-gray-900">注文番号:</span>{' '}
                <span className="font-mono">{selectedOrder.orderNumber}</span>
              </p>
              <p>
                <span className="font-medium text-gray-900">注文日時:</span>{' '}
                {new Date(selectedOrder.createdAt).toLocaleString('ja-JP')}
              </p>
              <p>
                <span className="font-medium text-gray-900">会員メール:</span>{' '}
                {selectedOrder.userEmail || 'ゲスト'}
              </p>
              <p>
                <span className="font-medium text-gray-900">会員表示名:</span>{' '}
                {selectedOrder.userDisplayName || '—'}
              </p>
              <p>
                <span className="font-medium text-gray-900">ステータス:</span> {getStatusLabel(selectedOrder.status)}
              </p>
              <p>
                <span className="font-medium text-gray-900">合計金額:</span>{' '}
                {selectedOrder.totalPrice.toLocaleString()}円
              </p>
            </div>

            <div className="mb-6 overflow-hidden rounded border">
              <table className="w-full">
                <thead className="bg-gray-50 border-b">
                  <tr>
                    <th className="px-4 py-2 text-left text-xs font-medium uppercase text-gray-500">商品</th>
                    <th className="px-4 py-2 text-left text-xs font-medium uppercase text-gray-500">数量</th>
                    <th className="px-4 py-2 text-left text-xs font-medium uppercase text-gray-500">小計</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {selectedOrder.items.map((item, idx) => (
                    <tr key={`${selectedOrder.orderId}-${idx}`}>
                      <td className="px-4 py-3 text-sm text-gray-900">{item.product.name}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 tabular-nums">{item.quantity}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 tabular-nums">{item.subtotal.toLocaleString()}円</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex gap-2">
              {selectedOrder.status === 'PENDING' && (
                <>
                  <button
                    onClick={() => handleStatusChange(selectedOrder.orderId, 'confirm')}
                    className="rounded bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700"
                  >
                    確認
                  </button>
                  <button
                    onClick={() => handleStatusChange(selectedOrder.orderId, 'cancel')}
                    className="rounded bg-red-600 px-4 py-2 text-sm text-white hover:bg-red-700"
                  >
                    キャンセル
                  </button>
                </>
              )}
              {selectedOrder.status === 'CONFIRMED' && (
                <>
                  <button
                    onClick={() => handleStatusChange(selectedOrder.orderId, 'ship')}
                    className="rounded bg-purple-600 px-4 py-2 text-sm text-white hover:bg-purple-700"
                  >
                    発送
                  </button>
                  <button
                    onClick={() => handleStatusChange(selectedOrder.orderId, 'cancel')}
                    className="rounded bg-red-600 px-4 py-2 text-sm text-white hover:bg-red-700"
                  >
                    キャンセル
                  </button>
                </>
              )}
              {selectedOrder.status === 'SHIPPED' && (
                <button
                  onClick={() => handleStatusChange(selectedOrder.orderId, 'deliver')}
                  className="rounded bg-green-600 px-4 py-2 text-sm text-white hover:bg-green-700"
                >
                  配達完了
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
