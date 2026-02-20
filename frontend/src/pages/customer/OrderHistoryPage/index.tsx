import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '@features/auth'
import { getOrderHistory } from '@entities/order'
import type { Order } from '@entities/order'

const STATUS_LABELS: Record<string, string> = {
  PENDING: '作成済み',
  CONFIRMED: '確認済み',
  SHIPPED: '発送済み',
  DELIVERED: '配達完了',
  CANCELLED: 'キャンセル',
}

function getStatusLabel(status: string): string {
  return STATUS_LABELS[status] ?? status
}

export default function OrderHistoryPage() {
  const { isAuthenticated } = useAuth()
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!isAuthenticated) {
      return
    }

    const fetchOrders = async () => {
      setLoading(true)
      setError(null)
      try {
        const response = await getOrderHistory()
        if (response.success && response.data) {
          setOrders(response.data)
        } else {
          setError(response.error?.message || '注文履歴の取得に失敗しました')
        }
      } catch (err) {
        setError('注文履歴の取得中にエラーが発生しました')
        console.error(err)
      } finally {
        setLoading(false)
      }
    }

    fetchOrders()
  }, [isAuthenticated])

  if (!isAuthenticated) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-12">
        <p className="text-center text-zinc-600">
          注文履歴を表示するには
          <Link to="/auth/login" className="ml-2 text-zinc-900 underline">
            ログイン
          </Link>
          してください
        </p>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-12">
        <p className="text-center text-zinc-600">読み込み中...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-12">
        <p className="text-center text-red-600">{error}</p>
      </div>
    )
  }

  if (orders.length === 0) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-12">
        <h1 className="mb-8 text-center font-serif text-3xl tracking-wider">
          注文履歴
        </h1>
        <p className="text-center text-zinc-600">注文履歴がありません</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl px-6 py-12">
      <h1 className="mb-8 text-center font-serif text-3xl tracking-wider">
        注文履歴
      </h1>

      <div className="space-y-6">
        {orders.map((order) => (
          <div
            key={order.orderId}
            className="rounded border border-zinc-200 bg-white p-6"
          >
            <div className="mb-4 flex items-center justify-between">
              <div>
                <p className="text-sm text-zinc-500">注文番号</p>
                <p className="font-mono text-lg">{order.orderNumber}</p>
                <p className="text-xs text-zinc-500">
                  引当進捗: {order.allocatedQuantity} / {order.orderedQuantity}
                </p>
              </div>
              <div>
                <span
                  className={`rounded px-3 py-1 text-sm ${
                    order.status === 'DELIVERED'
                      ? 'bg-green-100 text-green-800'
                      : order.status === 'SHIPPED'
                      ? 'bg-blue-100 text-blue-800'
                      : order.status === 'CANCELLED'
                      ? 'bg-red-100 text-red-800'
                      : 'bg-zinc-100 text-zinc-800'
                  }`}
                >
                  {getStatusLabel(order.status)}
                </span>
              </div>
            </div>

            <div className="mb-4 space-y-2">
              {order.items.map((item, index) => (
                <div key={index} className="flex items-center space-x-4">
                  <img
                    src={item.product.image}
                    alt={item.product.name}
                    className="h-16 w-16 object-cover rounded"
                  />
                  <div className="flex-1">
                    <p className="font-medium">{item.product.name}</p>
                    <p className="text-sm text-zinc-600">
                      数量: {item.quantity} × ¥{item.product.price.toLocaleString()}
                    </p>
                    <p className="text-xs text-zinc-500">
                      引当進捗: {item.allocatedQuantity} / {item.orderedQuantity}
                    </p>
                  </div>
                  <p className="font-medium">
                    ¥{item.subtotal.toLocaleString()}
                  </p>
                </div>
              ))}
            </div>

            <div className="flex items-center justify-between border-t border-zinc-200 pt-4">
              <div>
                <p className="text-sm text-zinc-500">注文日時</p>
                <p className="text-sm">
                  {new Date(order.createdAt).toLocaleDateString('ja-JP')}
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm text-zinc-500">合計金額</p>
                <p className="text-xl font-bold">
                  ¥{order.totalPrice.toLocaleString()}
                </p>
              </div>
            </div>

            <div className="mt-4">
              <Link
                to={`/order/${order.orderId}`}
                className="block w-full rounded bg-zinc-900 px-4 py-2 text-center text-sm uppercase tracking-widest text-white hover:bg-zinc-800"
              >
                詳細を見る
              </Link>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
