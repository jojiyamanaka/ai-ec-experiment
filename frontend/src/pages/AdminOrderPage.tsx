import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import * as api from '../lib/api'
import type { Order } from '../types/api'

export default function AdminOrderPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState<string>('ALL')

  useEffect(() => {
    const fetchOrders = async () => {
      const response = await api.getAllOrders()
      if (response.success && response.data) {
        setOrders(response.data)
      }
      setLoading(false)
    }
    fetchOrders()
  }, [])

  const handleStatusChange = async (orderId: number, action: 'confirm' | 'ship' | 'deliver' | 'cancel') => {
    if (!window.confirm(`この注文を${action === 'confirm' ? '確認' : action === 'ship' ? '発送' : action === 'deliver' ? '配達完了' : 'キャンセル'}しますか？`)) {
      return
    }

    let response
    switch (action) {
      case 'confirm':
        response = await api.confirmOrder(orderId)
        break
      case 'ship':
        response = await api.shipOrder(orderId)
        break
      case 'deliver':
        response = await api.deliverOrder(orderId)
        break
      case 'cancel':
        response = await api.cancelOrder(orderId)
        break
    }

    if (response.success) {
      // 注文一覧を再取得
      const refreshResponse = await api.getAllOrders()
      if (refreshResponse.success && refreshResponse.data) {
        setOrders(refreshResponse.data)
      }
      alert('ステータスを更新しました')
    } else {
      alert(response.error?.message || '更新に失敗しました')
    }
  }

  const filteredOrders = filter === 'ALL'
    ? orders
    : orders.filter(order => order.status === filter)

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-12">
        <div className="text-center">
          <p className="text-gray-600">読み込み中...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-7xl px-6 py-12">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-3xl font-bold text-gray-900">注文管理</h1>
        <Link
          to="/bo/item"
          className="rounded-lg bg-gray-200 px-4 py-2 hover:bg-gray-300"
        >
          商品管理へ
        </Link>
      </div>

      {/* フィルタ */}
      <div className="mb-6 flex gap-2">
        {['ALL', 'PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'].map(status => (
          <button
            key={status}
            onClick={() => setFilter(status)}
            className={`rounded-lg px-4 py-2 ${filter === status ? 'bg-blue-600 text-white' : 'bg-gray-200'}`}
          >
            {status === 'ALL' ? 'すべて' : status}
          </button>
        ))}
      </div>

      {/* 注文一覧テーブル */}
      <div className="overflow-hidden rounded-lg bg-white shadow">
        <table className="min-w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">
                注文番号
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">
                注文日時
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">
                合計金額
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">
                ステータス
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase text-gray-500">
                アクション
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {filteredOrders.map(order => (
              <tr key={order.orderId}>
                <td className="whitespace-nowrap px-6 py-4">
                  {order.orderNumber}
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  {new Date(order.createdAt).toLocaleString('ja-JP')}
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  ¥{order.totalPrice.toLocaleString()}
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <span className={`rounded-full px-2 py-1 text-xs ${
                    order.status === 'PENDING' ? 'bg-gray-100 text-gray-800' :
                    order.status === 'CONFIRMED' ? 'bg-blue-100 text-blue-800' :
                    order.status === 'SHIPPED' ? 'bg-purple-100 text-purple-800' :
                    order.status === 'DELIVERED' ? 'bg-green-100 text-green-800' :
                    'bg-red-100 text-red-800'
                  }`}>
                    {order.status}
                  </span>
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <div className="flex gap-2">
                    {order.status === 'PENDING' && (
                      <>
                        <button
                          onClick={() => handleStatusChange(order.orderId, 'confirm')}
                          className="rounded bg-blue-600 px-3 py-1 text-xs text-white hover:bg-blue-700"
                        >
                          確認
                        </button>
                        <button
                          onClick={() => handleStatusChange(order.orderId, 'cancel')}
                          className="rounded bg-red-600 px-3 py-1 text-xs text-white hover:bg-red-700"
                        >
                          キャンセル
                        </button>
                      </>
                    )}
                    {order.status === 'CONFIRMED' && (
                      <>
                        <button
                          onClick={() => handleStatusChange(order.orderId, 'ship')}
                          className="rounded bg-purple-600 px-3 py-1 text-xs text-white hover:bg-purple-700"
                        >
                          発送
                        </button>
                        <button
                          onClick={() => handleStatusChange(order.orderId, 'cancel')}
                          className="rounded bg-red-600 px-3 py-1 text-xs text-white hover:bg-red-700"
                        >
                          キャンセル
                        </button>
                      </>
                    )}
                    {order.status === 'SHIPPED' && (
                      <button
                        onClick={() => handleStatusChange(order.orderId, 'deliver')}
                        className="rounded bg-green-600 px-3 py-1 text-xs text-white hover:bg-green-700"
                      >
                        配達完了
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
