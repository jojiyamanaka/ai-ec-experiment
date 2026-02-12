import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useCart } from '../contexts/CartContext'
import * as api from '../lib/api'
import type { ApiError, StockShortageDetail, UnavailableProductDetail } from '../types/api'
import { getUserFriendlyMessage } from '../lib/errorMessages'

export default function OrderConfirmPage() {
  const { items, totalPrice, clearCart } = useCart()
  const navigate = useNavigate()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)

  // カートが空の場合はカート画面にリダイレクト
  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-7xl px-6 py-24">
        <div className="text-center">
          <h2 className="font-serif text-2xl text-zinc-900">
            カートが空です
          </h2>
          <p className="mt-2 text-zinc-600">
            注文する商品がありません
          </p>
          <Link
            to="/item"
            className="mt-6 inline-block bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors"
          >
            商品一覧へ
          </Link>
        </div>
      </div>
    )
  }

  const handleConfirmOrder = async () => {
    setIsSubmitting(true)
    setError(null) // エラーをクリア
    try {
      // 注文を作成
      const response = await api.createOrder({
        cartId: api.getSessionId(),
      })

      if (response.success && response.data) {
        // カートをクリア
        clearCart()

        // 注文情報を持って完了画面へ遷移
        navigate('/order/complete', {
          state: {
            orderNumber: response.data.orderNumber,
            items: response.data.items,
            totalPrice: response.data.totalPrice,
            orderId: response.data.orderId,
            status: response.data.status,
          },
        })
      } else {
        // エラー情報を設定
        if (response.error) {
          setError(response.error)
        } else {
          setError({
            code: 'UNKNOWN_ERROR',
            message: '注文の作成に失敗しました',
          })
        }
      }
    } catch (error) {
      console.error('注文作成エラー:', error)
      setError({
        code: 'NETWORK_ERROR',
        message: error instanceof Error ? error.message : '注文の作成に失敗しました。もう一度お試しください。',
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-4xl px-6 py-24">
      <h1 className="mb-12 font-serif text-3xl text-zinc-900">注文内容の確認</h1>

      {/* 注文商品一覧 */}
      <div className="mb-8 border border-stone-200 bg-white p-6">
        <h2 className="mb-4 font-serif text-lg text-zinc-900">ご注文商品</h2>
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
                    {item.product.description}
                  </p>
                </div>
                <div className="flex items-center justify-between">
                  <p className="text-xs text-zinc-600">
                    数量: {item.quantity}
                  </p>
                  <p className="text-sm text-zinc-900">
                    ¥{(item.product.price * item.quantity).toLocaleString()}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 合計金額 */}
      <div className="mb-8 border border-stone-200 bg-white p-6">
        <h2 className="mb-4 font-serif text-lg text-zinc-900">お支払い金額</h2>
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-zinc-600">小計</span>
            <span className="text-zinc-900">
              ¥{totalPrice.toLocaleString()}
            </span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-zinc-600">配送料</span>
            <span className="text-zinc-900">¥0</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-zinc-600">手数料</span>
            <span className="text-zinc-900">¥0</span>
          </div>
          <div className="border-t border-stone-200 pt-2">
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
      </div>

      {/* エラー表示 */}
      {error && (
        <div className="mb-6 rounded-lg border border-red-300 bg-red-50 p-4">
          <div className="flex items-start">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={1.5}
              stroke="currentColor"
              className="h-5 w-5 text-red-600"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z"
              />
            </svg>
            <div className="ml-3 flex-1">
              <h3 className="font-medium text-red-800">
                {getUserFriendlyMessage(error.code)}
              </h3>
              {error.code === 'OUT_OF_STOCK' && error.details && error.details.length > 0 && (
                <div className="mt-2">
                  <p className="text-sm text-red-700">以下の商品の在庫が不足しています：</p>
                  <ul className="mt-2 space-y-1 text-sm text-red-700">
                    {(error.details as StockShortageDetail[]).map((detail) => (
                      <li key={detail.productId} className="flex items-center">
                        <span className="font-medium">{detail.productName}</span>
                        <span className="ml-2">
                          （要求: {detail.requestedQuantity}個、在庫: {detail.availableStock}個）
                        </span>
                      </li>
                    ))}
                  </ul>
                  <p className="mt-2 text-sm text-red-700">
                    カートに戻って数量を調整してください。
                  </p>
                </div>
              )}
              {error.code === 'ITEM_NOT_AVAILABLE' && error.details && error.details.length > 0 && (
                <div className="mt-2">
                  <p className="text-sm text-red-700">以下の商品は現在購入できません：</p>
                  <ul className="mt-2 space-y-1 text-sm text-red-700">
                    {(error.details as UnavailableProductDetail[]).map((detail) => (
                      <li key={detail.productId} className="flex items-center">
                        <span className="font-medium">{detail.productName}</span>
                      </li>
                    ))}
                  </ul>
                  <p className="mt-2 text-sm text-red-700">
                    カートに戻って該当商品を削除してください。
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* アクションボタン */}
      <div className="space-y-3">
        <button
          onClick={handleConfirmOrder}
          disabled={isSubmitting}
          className="w-full bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors disabled:cursor-not-allowed disabled:bg-stone-400"
        >
          {isSubmitting ? '処理中...' : '注文を確定する'}
        </button>
        <Link
          to="/order/cart"
          className="block text-center text-xs uppercase tracking-widest text-zinc-600 hover:text-zinc-900 transition-colors"
        >
          カートに戻る
        </Link>
      </div>
    </div>
  )
}
