import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useCart } from '@features/cart'

export default function CartPage() {
  const { items, totalPrice, updateQuantity, removeFromCart } = useCart()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  // カートが空の場合
  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-7xl px-6 py-24">
        <div className="text-center">
          {/* SVG アイコン（色を変更） */}
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={1.5}
            stroke="currentColor"
            className="mx-auto h-24 w-24 text-zinc-400"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 0 0-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 0 0-16.536-1.84M7.5 14.25 5.106 5.272M6 20.25a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm12.75 0a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z"
            />
          </svg>
          <h2 className="mt-6 font-serif text-2xl text-zinc-900">
            カートは空です
          </h2>
          <p className="mt-2 text-zinc-600">
            商品を追加して、お買い物を始めましょう
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

  const handleQuantityChange = async (itemId: number, newQuantity: number) => {
    setError(null)
    try {
      if (newQuantity <= 0) {
        await removeFromCart(itemId)
      } else {
        const clamped = Math.min(newQuantity, 9)
        await updateQuantity(itemId, clamped)
      }
    } catch (err) {
      console.error('数量変更エラー:', err)
      setError(err instanceof Error ? err.message : '数量の変更に失敗しました')
    }
  }

  const handleCheckout = () => {
    navigate('/order/reg')
  }

  return (
    <div className="mx-auto max-w-7xl px-6 py-24">
      <h1 className="mb-12 font-serif text-3xl text-zinc-900">ショッピングカート</h1>

      {error && (
        <div className="mb-6 rounded-lg border border-red-300 bg-red-50 p-3">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      <div className="grid gap-8 lg:grid-cols-3">
        {/* カート商品一覧 */}
        <div className="lg:col-span-2">
          <div className="space-y-4">
            {items.map((item) => (
              <div
                key={item.product.id}
                className="flex gap-4 border border-stone-200 bg-white p-4"
              >
                {/* 商品画像 */}
                <Link
                  to={`/item/${item.product.id}`}
                  className="h-24 w-24 flex-shrink-0 overflow-hidden bg-stone-200"
                >
                  <img
                    src={item.product.image}
                    alt={item.product.name}
                    className="h-full w-full object-cover"
                  />
                </Link>

                {/* 商品情報 */}
                <div className="flex flex-1 flex-col">
                  <div className="flex justify-between">
                    <div>
                      <Link
                        to={`/item/${item.product.id}`}
                        className="font-serif text-sm uppercase tracking-wider text-zinc-900 hover:text-zinc-600"
                      >
                        {item.product.name}
                      </Link>
                      <p className="mt-1 text-xs text-zinc-500">
                        {item.product.description}
                      </p>
                    </div>
                    <button
                      onClick={() => removeFromCart(item.id)}
                      className="text-zinc-400 hover:text-red-600"
                      aria-label="削除"
                    >
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        fill="none"
                        viewBox="0 0 24 24"
                        strokeWidth={1.5}
                        stroke="currentColor"
                        className="h-5 w-5"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0"
                        />
                      </svg>
                    </button>
                  </div>

                  <div className="mt-4 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() =>
                          handleQuantityChange(item.id, item.quantity - 1)
                        }
                        className="flex h-8 w-8 items-center justify-center rounded-full border border-stone-300 hover:bg-stone-100"
                        aria-label="数量を減らす"
                      >
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          fill="none"
                          viewBox="0 0 24 24"
                          strokeWidth={2}
                          stroke="currentColor"
                          className="h-4 w-4"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            d="M5 12h14"
                          />
                        </svg>
                      </button>
                      <input
                        type="number"
                        min="1"
                        max="9"
                        value={item.quantity}
                        onChange={(e) =>
                          handleQuantityChange(
                            item.id,
                            parseInt(e.target.value) || 1,
                          )
                        }
                        className="w-16 rounded border border-stone-300 px-3 py-1 text-center"
                      />
                      <button
                        onClick={() =>
                          handleQuantityChange(item.id, item.quantity + 1)
                        }
                        disabled={item.quantity >= 9}
                        className="flex h-8 w-8 items-center justify-center rounded-full border border-stone-300 hover:bg-stone-100 disabled:cursor-not-allowed disabled:opacity-50"
                        aria-label="数量を増やす"
                      >
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          fill="none"
                          viewBox="0 0 24 24"
                          strokeWidth={2}
                          stroke="currentColor"
                          className="h-4 w-4"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            d="M12 4.5v15m7.5-7.5h-15"
                          />
                        </svg>
                      </button>
                    </div>
                    <p className="text-sm text-zinc-900">
                      ¥{(item.product.price * item.quantity).toLocaleString()}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 合計金額サマリー */}
        <div className="lg:col-span-1">
          <div className="sticky top-24 border border-stone-200 bg-white p-6">
            <h2 className="font-serif text-lg text-zinc-900">注文サマリー</h2>
            <div className="mt-4 space-y-2">
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
              <div className="border-t border-stone-200 pt-2">
                <div className="flex justify-between">
                  <span className="text-base font-serif text-zinc-900">
                    合計
                  </span>
                  <span className="text-xl text-zinc-900">
                    ¥{totalPrice.toLocaleString()}
                  </span>
                </div>
              </div>
            </div>
            <button
              onClick={handleCheckout}
              className="mt-6 w-full bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors"
            >
              レジに進む
            </button>
            <Link
              to="/item"
              className="mt-3 block text-center text-xs uppercase tracking-widest text-zinc-600 hover:text-zinc-900 transition-colors"
            >
              買い物を続ける
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
