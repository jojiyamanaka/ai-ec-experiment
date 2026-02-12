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
      <div className="mx-auto max-w-7xl px-6 py-24">
        <div className="text-center">
          <h2 className="font-serif text-2xl text-zinc-900">
            注文情報が見つかりません
          </h2>
          <p className="mt-2 text-zinc-600">
            正しい手順で注文を完了してください
          </p>
          <Link
            to="/"
            className="mt-6 inline-block bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors"
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
    <div className="mx-auto max-w-4xl px-6 py-24">
      {/* 完了メッセージ */}
      <div className="mb-12 text-center">
        <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-zinc-100">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={2}
            stroke="currentColor"
            className="h-8 w-8 text-zinc-700"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M4.5 12.75l6 6 9-13.5"
            />
          </svg>
        </div>
        <h1 className="font-serif text-3xl text-zinc-900">
          ご注文ありがとうございます
        </h1>
        <p className="mt-2 text-zinc-600">
          ご注文を承りました。商品の発送までしばらくお待ちください。
        </p>
      </div>

      {/* 注文番号 */}
      <div className="mb-8 border border-stone-200 bg-stone-50 p-6 text-center">
        <p className="text-xs uppercase tracking-widest text-zinc-500">注文番号</p>
        <p className="mt-2 font-serif text-2xl text-zinc-900">{orderNumber}</p>
      </div>

      {/* 注文内容サマリー */}
      <div className="mb-8 border border-stone-200 bg-white p-6">
        <h2 className="mb-4 font-serif text-lg text-zinc-900">
          ご注文内容
        </h2>
        <div className="space-y-4">
          {items.map((item) => (
            <div
              key={item.product.id}
              className="flex gap-4 border-b border-stone-200 pb-4 last:border-b-0 last:pb-0"
            >
              {/* 商品画像 */}
              <div className="h-20 w-20 flex-shrink-0 overflow-hidden bg-stone-200">
                <img
                  src={item.product.image}
                  alt={item.product.name}
                  className="h-full w-full object-cover"
                />
              </div>

              {/* 商品情報 */}
              <div className="flex flex-1 flex-col justify-between">
                <div>
                  <h3 className="font-serif text-sm uppercase tracking-wider text-zinc-900">
                    {item.product.name}
                  </h3>
                  <p className="mt-1 text-xs text-zinc-500">
                    単価: ¥{item.product.price.toLocaleString()} × {item.quantity}
                  </p>
                </div>
                <p className="text-sm text-zinc-900">
                  ¥{(item.product.price * item.quantity).toLocaleString()}
                </p>
              </div>
            </div>
          ))}
        </div>

        {/* 合計金額 */}
        <div className="mt-6 border-t border-stone-200 pt-4">
          <div className="flex justify-between">
            <span className="text-base font-serif text-zinc-900">
              合計金額
            </span>
            <span className="text-xl text-zinc-900">
              ¥{totalPrice.toLocaleString()}
            </span>
          </div>
        </div>
      </div>

      {/* お知らせ */}
      <div className="mb-8 border border-stone-200 bg-stone-50 p-6">
        <h3 className="mb-2 font-serif text-base text-zinc-900">ご注文完了のお知らせ</h3>
        <ul className="space-y-1 text-sm text-zinc-600">
          <li>• ご登録のメールアドレスに注文確認メールをお送りしました</li>
          <li>• 商品の発送が完了しましたら、発送通知メールをお送りします</li>
          <li>• ご不明な点がございましたら、お問い合わせください</li>
        </ul>
      </div>

      {/* アクションボタン */}
      <div className="text-center">
        <Link
          to="/"
          className="inline-block bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors"
        >
          TOPに戻る
        </Link>
        <Link
          to="/item"
          className="ml-4 inline-block border border-stone-300 bg-white px-12 py-4 text-xs tracking-[0.2em] uppercase text-zinc-700 hover:bg-stone-50 transition-colors"
        >
          買い物を続ける
        </Link>
        {orderStatus === 'PENDING' && (
          <div className="mt-4">
            <button
              onClick={handleCancelOrder}
              disabled={isCancelling}
              className="text-xs uppercase tracking-widest text-red-600 hover:text-red-700 underline disabled:opacity-50"
            >
              {isCancelling ? 'キャンセル中...' : '注文をキャンセル'}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
