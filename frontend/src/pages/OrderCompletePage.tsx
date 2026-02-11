import { useState } from 'react'
import { Link, useLocation } from 'react-router'
import type { CartItem } from '../contexts/CartContext'
import * as api from '../lib/api'

interface OrderCompleteState {
  orderNumber: string
  items: CartItem[]
  totalPrice: number
  orderId: number
  status: string
}

export default function OrderCompletePage() {
  const location = useLocation()
  const state = location.state as OrderCompleteState | null
  const [orderStatus, setOrderStatus] = useState<string>(state?.status || 'PENDING')
  const [isCancelling, setIsCancelling] = useState(false)

  // 注文情報がない場合（直接URLアクセスなど）
  if (!state) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-12">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-900">
            注文情報が見つかりません
          </h2>
          <p className="mt-2 text-gray-600">
            正しい手順で注文を完了してください
          </p>
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

  const { orderNumber, items, totalPrice } = state

  const handleCancelOrder = async () => {
    if (!state?.orderId) return
    if (!window.confirm('本当にキャンセルしますか？\n\nキャンセル後は在庫が戻りますが、注文は復元できません。')) {
      return
    }

    setIsCancelling(true)
    try {
      const response = await api.cancelOrder(state.orderId)
      if (response.success && response.data) {
        setOrderStatus(response.data.status)
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

  return (
    <div className="mx-auto max-w-4xl px-4 py-12">
      {/* 完了メッセージ */}
      <div className="mb-8 text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={2}
            stroke="currentColor"
            className="h-8 w-8 text-green-600"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M4.5 12.75l6 6 9-13.5"
            />
          </svg>
        </div>
        <h1 className="text-3xl font-bold text-gray-900">
          ご注文ありがとうございます
        </h1>
        <p className="mt-2 text-gray-600">
          ご注文を承りました。商品の発送までしばらくお待ちください。
        </p>
      </div>

      {/* 注文番号 */}
      <div className="mb-8 rounded-lg bg-blue-50 p-6 text-center">
        <p className="text-sm text-gray-600">注文番号</p>
        <p className="mt-1 text-2xl font-bold text-blue-600">{orderNumber}</p>
      </div>

      {/* 注文内容サマリー */}
      <div className="mb-8 rounded-lg bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-lg font-bold text-gray-900">
          ご注文内容
        </h2>
        <div className="space-y-4">
          {items.map((item) => (
            <div
              key={item.product.id}
              className="flex gap-4 border-b border-gray-200 pb-4 last:border-b-0 last:pb-0"
            >
              {/* 商品画像 */}
              <div className="h-20 w-20 flex-shrink-0 overflow-hidden rounded-lg bg-gray-200">
                <img
                  src={item.product.image}
                  alt={item.product.name}
                  className="h-full w-full object-cover"
                />
              </div>

              {/* 商品情報 */}
              <div className="flex flex-1 flex-col justify-between">
                <div>
                  <h3 className="font-medium text-gray-900">
                    {item.product.name}
                  </h3>
                  <p className="mt-1 text-sm text-gray-500">
                    単価: ¥{item.product.price.toLocaleString()} × {item.quantity}
                  </p>
                </div>
                <p className="font-bold text-gray-900">
                  ¥{(item.product.price * item.quantity).toLocaleString()}
                </p>
              </div>
            </div>
          ))}
        </div>

        {/* 合計金額 */}
        <div className="mt-6 border-t border-gray-200 pt-4">
          <div className="flex justify-between">
            <span className="text-lg font-bold text-gray-900">
              合計金額
            </span>
            <span className="text-2xl font-bold text-blue-600">
              ¥{totalPrice.toLocaleString()}
            </span>
          </div>
        </div>
      </div>

      {/* お知らせ */}
      <div className="mb-8 rounded-lg border border-gray-200 bg-gray-50 p-6">
        <h3 className="mb-2 font-bold text-gray-900">ご注文完了のお知らせ</h3>
        <ul className="space-y-1 text-sm text-gray-600">
          <li>• ご登録のメールアドレスに注文確認メールをお送りしました</li>
          <li>• 商品の発送が完了しましたら、発送通知メールをお送りします</li>
          <li>• ご不明な点がございましたら、お問い合わせください</li>
        </ul>
      </div>

      {/* アクションボタン */}
      <div className="text-center">
        <Link
          to="/"
          className="inline-block rounded-lg bg-blue-600 px-8 py-3 font-medium text-white hover:bg-blue-700"
        >
          TOPに戻る
        </Link>
        <Link
          to="/item"
          className="ml-4 inline-block rounded-lg border border-gray-300 bg-white px-8 py-3 font-medium text-gray-700 hover:bg-gray-50"
        >
          買い物を続ける
        </Link>
        {orderStatus === 'PENDING' && (
          <div className="mt-4">
            <button
              onClick={handleCancelOrder}
              disabled={isCancelling}
              className="text-sm text-red-600 hover:text-red-700 underline disabled:opacity-50"
            >
              {isCancelling ? 'キャンセル中...' : '注文をキャンセル'}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
