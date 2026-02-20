import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router'
import { getOrderById, cancelOrder } from '@entities/order'
import type { Order } from '@entities/order'

const statusConfig = {
  PENDING: { label: '作成済み', color: 'gray', icon: '⏳' },
  CONFIRMED: { label: '確認済み', color: 'blue', icon: '✓' },
  SHIPPED: { label: '発送済み', color: 'purple', icon: '📦' },
  DELIVERED: { label: '配達完了', color: 'green', icon: '✓' },
  CANCELLED: { label: 'キャンセル', color: 'red', icon: '✗' }
}

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [order, setOrder] = useState<Order | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isCancelling, setIsCancelling] = useState(false)

  useEffect(() => {
    if (!id) return

    const fetchOrder = async () => {
      const response = await getOrderById(Number(id))
      if (response.success && response.data) {
        setOrder(response.data)
      } else {
        setError(response.error?.message || '注文が見つかりません')
      }
      setLoading(false)
    }

    fetchOrder()
  }, [id])

  const handleCancel = async () => {
    if (!order || !window.confirm('本当にキャンセルしますか？\n\nキャンセル後は在庫が戻りますが、注文は復元できません。')) return

    setIsCancelling(true)
    try {
      const response = await cancelOrder(order.orderId)
      if (response.success && response.data) {
        setOrder(response.data)
        alert('注文をキャンセルしました')
      } else {
        throw new Error(response.error?.message || 'キャンセルに失敗しました')
      }
    } catch (error) {
      console.error('キャンセルエラー:', error)
      alert(error instanceof Error ? error.message : '通信エラーが発生しました')
    } finally {
      setIsCancelling(false)
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-12">
        <div className="text-center">
          <p className="text-gray-600">読み込み中...</p>
        </div>
      </div>
    )
  }

  if (error || !order) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-12">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-900">エラー</h2>
          <p className="mt-2 text-gray-600">{error || '注文が見つかりません'}</p>
          <Link
            to="/"
            className="mt-6 inline-block rounded-lg bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700"
          >
            TOPに戻る
          </Link>
        </div>
      </div>
    )
  }

  const status = statusConfig[order.status as keyof typeof statusConfig]
  const canCancel = order.status === 'PENDING' || order.status === 'CONFIRMED'

  return (
    <div className="mx-auto max-w-4xl px-4 py-12">
      <h1 className="mb-6 text-3xl font-bold text-gray-900">注文詳細</h1>

      {/* ステータスバッジ */}
      <div className="mb-6">
        <span className={`inline-flex items-center px-4 py-2 rounded-full text-sm font-semibold ${
          status.color === 'gray' ? 'bg-gray-100 text-gray-800' :
          status.color === 'blue' ? 'bg-blue-100 text-blue-800' :
          status.color === 'purple' ? 'bg-purple-100 text-purple-800' :
          status.color === 'green' ? 'bg-green-100 text-green-800' :
          'bg-red-100 text-red-800'
        }`}>
          <span className="mr-2">{status.icon}</span>
          {status.label}
        </span>
      </div>

      {/* 注文情報 */}
      <div className="mb-6 rounded-lg bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-xl font-semibold text-gray-900">注文情報</h2>
        <dl className="grid grid-cols-2 gap-4">
          <div>
            <dt className="text-sm text-gray-600">注文番号</dt>
            <dd className="font-semibold text-gray-900">{order.orderNumber}</dd>
          </div>
          <div>
            <dt className="text-sm text-gray-600">注文日時</dt>
            <dd className="font-semibold text-gray-900">
              {new Date(order.createdAt).toLocaleString('ja-JP')}
            </dd>
          </div>
          <div>
            <dt className="text-sm text-gray-600">合計金額</dt>
            <dd className="font-semibold text-gray-900">
              ¥{order.totalPrice.toLocaleString()}
            </dd>
          </div>
          <div>
            <dt className="text-sm text-gray-600">引当進捗</dt>
            <dd className="font-semibold text-gray-900">
              {order.allocatedQuantity} / {order.orderedQuantity}
            </dd>
          </div>
        </dl>
      </div>

      {/* 商品一覧 */}
      <div className="mb-6 rounded-lg bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-xl font-semibold text-gray-900">注文商品</h2>
        {order.items.map((item) => (
          <div
            key={item.product.id}
            className="flex items-center gap-4 border-b border-gray-200 py-4 last:border-b-0"
          >
            <div className="h-20 w-20 flex-shrink-0 overflow-hidden rounded-lg bg-gray-200">
              <img
                src={item.product.image}
                alt={item.product.name}
                className="h-full w-full object-cover"
              />
            </div>
            <div className="flex-1">
              <h3 className="font-semibold text-gray-900">{item.product.name}</h3>
              <p className="text-sm text-gray-600">数量: {item.quantity}</p>
              <p className="text-xs text-gray-500">引当進捗: {item.allocatedQuantity} / {item.orderedQuantity}</p>
            </div>
            <div className="text-right">
              <p className="font-semibold text-gray-900">
                ¥{item.subtotal.toLocaleString()}
              </p>
            </div>
          </div>
        ))}
      </div>

      {/* アクション */}
      <div className="flex gap-4">
        <Link
          to="/"
          className="rounded-lg bg-gray-200 px-6 py-2 text-gray-800 hover:bg-gray-300"
        >
          TOPに戻る
        </Link>
        {canCancel && (
          <button
            onClick={handleCancel}
            disabled={isCancelling}
            className="rounded-lg bg-red-600 px-6 py-2 text-white hover:bg-red-700 disabled:opacity-50"
          >
            {isCancelling ? 'キャンセル中...' : '注文をキャンセル'}
          </button>
        )}
      </div>
    </div>
  )
}
