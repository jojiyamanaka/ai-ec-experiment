import { useEffect, useMemo, useState } from 'react'
import { getAllOrders, confirmOrder, shipOrder, deliverOrder, cancelOrder, retryAllocation } from '@entities/order'
import type { Order } from '@entities/order'
import {
  AdminFilterChips,
  AdminModalBase,
  AdminPageContainer,
  AdminPageHeader,
  AdminSearchBar,
  AdminTableShell,
} from '@shared/ui/admin'

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
    action: 'confirm' | 'ship' | 'deliver' | 'cancel' | 'retry'
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
                : action === 'retry'
                  ? '本引当再試行'
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
      case 'retry':
        response = await retryAllocation(orderId)
        break
    }

    if (response.success) {
      await fetchOrders()
      alert('ステータスを更新しました')
    } else {
      alert(response.error?.message || '更新に失敗しました')
    }
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
      <AdminPageContainer>
        <div className="text-center text-gray-600">読み込み中...</div>
      </AdminPageContainer>
    )
  }

  return (
    <AdminPageContainer>
      {/* ヘッダー */}
      <AdminPageHeader title="注文管理" />

      {/* 検索エリア */}
      <AdminSearchBar
        value={searchInput}
        onChange={setSearchInput}
        onSearch={(value) => setSearchQuery(value.trim())}
        placeholder="注文番号で検索"
      />

      {/* フィルタエリア */}
      <AdminFilterChips
        items={['ALL', 'PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'].map((status) => ({
          key: status,
          label: getStatusLabel(status),
        }))}
        selectedKey={filter}
        onSelect={setFilter}
      />

      {/* テーブル */}
      <AdminTableShell>
        <table className="w-full">
          <thead className="bg-gray-50 border-b">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">注文番号</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">注文日時</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">会員メール</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">会員表示名</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">ステータス</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">引当進捗</th>
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
                <td className="px-6 py-4 text-sm text-gray-900 tabular-nums">
                  {order.committedQuantity} / {order.orderedQuantity}
                </td>
                <td className="px-6 py-4 text-sm tabular-nums text-gray-900">
                  {order.totalPrice.toLocaleString()}円
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </AdminTableShell>

      <AdminModalBase
        isOpen={showDetailModal && Boolean(selectedOrder)}
        onClose={() => setShowDetailModal(false)}
        title="注文詳細"
      >
        {selectedOrder ? (
          <>
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
                <span className="font-medium text-gray-900">引当進捗:</span> {selectedOrder.committedQuantity} / {selectedOrder.orderedQuantity}
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
                    <th className="px-4 py-2 text-left text-xs font-medium uppercase text-gray-500">引当進捗</th>
                    <th className="px-4 py-2 text-left text-xs font-medium uppercase text-gray-500">小計</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {selectedOrder.items.map((item, idx) => (
                    <tr key={`${selectedOrder.orderId}-${idx}`}>
                      <td className="px-4 py-3 text-sm text-gray-900">{item.product.name}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 tabular-nums">{item.quantity}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 tabular-nums">{item.committedQuantity} / {item.orderedQuantity}</td>
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
                    className="rounded bg-zinc-900 px-4 py-2 text-sm text-white hover:bg-zinc-700"
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
                    className="rounded bg-zinc-900 px-4 py-2 text-sm text-white hover:bg-zinc-700"
                  >
                    発送
                  </button>
                  <button
                    onClick={() => handleStatusChange(selectedOrder.orderId, 'retry')}
                    className="rounded border border-zinc-300 px-4 py-2 text-sm text-zinc-700 hover:bg-zinc-100"
                  >
                    本引当再試行
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
          </>
        ) : null}
      </AdminModalBase>
    </AdminPageContainer>
  )
}
