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

function getStatusBadgeClass(status: string): string {
  if (status === 'PENDING') {
    return 'bg-gray-100 text-gray-800'
  }
  if (status === 'CONFIRMED') {
    return 'bg-blue-100 text-blue-800'
  }
  if (status === 'SHIPPED') {
    return 'bg-purple-100 text-purple-800'
  }
  if (status === 'DELIVERED') {
    return 'bg-green-100 text-green-800'
  }
  return 'bg-red-100 text-red-800'
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
          <thead className="border-b bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">注文番号</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">注文日時</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">会員メール</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">会員表示名</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">ステータス</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">引当進捗</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">合計金額</th>
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
                  <span className={`inline-flex rounded-full px-2 py-1 text-xs font-medium ${getStatusBadgeClass(order.status)}`}>
                    {getStatusLabel(order.status)}
                  </span>
                </td>
                <td className="px-6 py-4 text-sm tabular-nums text-gray-900">
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
        maxWidthClass="max-w-4xl"
        bodyClassName="p-8"
      >
        {selectedOrder ? (
          <>
            <section className="space-y-3">
              <div>
                <h3 className="text-base font-bold text-zinc-900">注文情報</h3>
                <p className="mt-1 text-xs text-gray-500">注文番号・日時・ステータスを確認できます。</p>
              </div>
              <div className="grid gap-3 md:grid-cols-2">
                <label className="text-sm text-zinc-700">
                  注文番号
                  <input
                    value={selectedOrder.orderNumber}
                    readOnly
                    className="mt-1 w-full rounded border bg-white px-3 py-2 font-mono text-sm text-zinc-900"
                  />
                </label>
                <label className="text-sm text-zinc-700">
                  注文日時
                  <input
                    value={new Date(selectedOrder.createdAt).toLocaleString('ja-JP')}
                    readOnly
                    className="mt-1 w-full rounded border bg-white px-3 py-2 text-sm text-zinc-900"
                  />
                </label>
                <div className="text-sm text-zinc-700">
                  ステータス
                  <div className="mt-1">
                    <span className={`inline-flex rounded-full px-2 py-1 text-xs font-medium ${getStatusBadgeClass(selectedOrder.status)}`}>
                      {getStatusLabel(selectedOrder.status)}
                    </span>
                  </div>
                </div>
                <label className="text-sm text-zinc-700">
                  引当進捗
                  <input
                    value={`${selectedOrder.committedQuantity} / ${selectedOrder.orderedQuantity}`}
                    readOnly
                    className="mt-1 w-full rounded border bg-white px-3 py-2 text-sm tabular-nums text-zinc-900"
                  />
                </label>
              </div>
            </section>

            <section className="mt-6 space-y-3">
              <div>
                <h3 className="text-base font-bold text-zinc-900">会員情報・合計</h3>
                <p className="mt-1 text-xs text-gray-500">購入者情報と請求合計を表示します。</p>
              </div>
              <div className="grid gap-3 md:grid-cols-2">
                <label className="text-sm text-zinc-700">
                  会員メール
                  <input
                    value={selectedOrder.userEmail || 'ゲスト'}
                    readOnly
                    className="mt-1 w-full rounded border bg-white px-3 py-2 text-sm text-zinc-900"
                  />
                </label>
                <label className="text-sm text-zinc-700">
                  会員表示名
                  <input
                    value={selectedOrder.userDisplayName || '—'}
                    readOnly
                    className="mt-1 w-full rounded border bg-white px-3 py-2 text-sm text-zinc-900"
                  />
                </label>
                <label className="text-sm text-zinc-700 md:col-span-2">
                  合計金額
                  <input
                    value={`${selectedOrder.totalPrice.toLocaleString()}円`}
                    readOnly
                    className="mt-1 w-full rounded border bg-white px-3 py-2 text-sm font-semibold tabular-nums text-zinc-900"
                  />
                </label>
              </div>
            </section>

            <section className="mt-6 space-y-3">
              <div>
                <h3 className="text-base font-bold text-zinc-900">注文明細</h3>
                <p className="mt-1 text-xs text-gray-500">明細ごとの数量と引当進捗です。</p>
              </div>
              <div className="overflow-hidden rounded-lg border">
                <table className="w-full">
                  <thead className="border-b bg-gray-50">
                    <tr>
                      <th className="px-4 py-2 text-left text-xs font-medium uppercase tracking-wider text-gray-500">商品</th>
                      <th className="px-4 py-2 text-left text-xs font-medium uppercase tracking-wider text-gray-500">数量</th>
                      <th className="px-4 py-2 text-left text-xs font-medium uppercase tracking-wider text-gray-500">引当進捗</th>
                      <th className="px-4 py-2 text-left text-xs font-medium uppercase tracking-wider text-gray-500">小計</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {selectedOrder.items.map((item, idx) => (
                      <tr key={`${selectedOrder.orderId}-${idx}`} className="hover:bg-gray-50">
                        <td className="px-4 py-3 text-sm text-gray-900">{item.product.name}</td>
                        <td className="px-4 py-3 text-sm tabular-nums text-gray-900">{item.quantity}</td>
                        <td className="px-4 py-3 text-sm tabular-nums text-gray-900">
                          {item.committedQuantity} / {item.orderedQuantity}
                        </td>
                        <td className="px-4 py-3 text-sm tabular-nums text-gray-900">{item.subtotal.toLocaleString()}円</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>

            <div className="mt-6 flex flex-wrap gap-2">
              {selectedOrder.status === 'PENDING' && (
                <>
                  <button
                    onClick={() => handleStatusChange(selectedOrder.orderId, 'confirm')}
                    className="flex-1 rounded bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700"
                  >
                    確認
                  </button>
                  <button
                    onClick={() => handleStatusChange(selectedOrder.orderId, 'cancel')}
                    className="flex-1 rounded bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700"
                  >
                    キャンセル
                  </button>
                </>
              )}
              {selectedOrder.status === 'CONFIRMED' && (
                <>
                  <button
                    onClick={() => handleStatusChange(selectedOrder.orderId, 'ship')}
                    className="flex-1 rounded bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700"
                  >
                    発送
                  </button>
                  <button
                    onClick={() => handleStatusChange(selectedOrder.orderId, 'retry')}
                    className="flex-1 rounded border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-100"
                  >
                    本引当再試行
                  </button>
                  <button
                    onClick={() => handleStatusChange(selectedOrder.orderId, 'cancel')}
                    className="flex-1 rounded bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700"
                  >
                    キャンセル
                  </button>
                </>
              )}
              {selectedOrder.status === 'SHIPPED' && (
                <button
                  onClick={() => handleStatusChange(selectedOrder.orderId, 'deliver')}
                  className="flex-1 rounded bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700"
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
