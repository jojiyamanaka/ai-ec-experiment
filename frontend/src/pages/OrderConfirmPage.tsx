import { Link, useNavigate } from 'react-router'
import { useCart } from '../contexts/CartContext'

export default function OrderConfirmPage() {
  const { items, getTotalPrice, clearCart } = useCart()
  const navigate = useNavigate()

  // カートが空の場合はカート画面にリダイレクト
  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-12">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-900">
            カートが空です
          </h2>
          <p className="mt-2 text-gray-600">
            注文する商品がありません
          </p>
          <Link
            to="/item"
            className="mt-6 inline-block rounded-lg bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700"
          >
            商品一覧へ
          </Link>
        </div>
      </div>
    )
  }

  const totalPrice = getTotalPrice()

  // 注文番号を生成する関数
  const generateOrderNumber = () => {
    const date = new Date()
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const random = String(Math.floor(Math.random() * 1000)).padStart(3, '0')
    return `ORD-${year}${month}${day}-${random}`
  }

  const handleConfirmOrder = () => {
    const orderNumber = generateOrderNumber()
    const orderItems = [...items] // カートの内容をコピー
    const orderTotal = totalPrice

    // カートをクリア
    clearCart()

    // 注文情報を持って完了画面へ遷移
    navigate('/order/complete', {
      state: {
        orderNumber,
        items: orderItems,
        totalPrice: orderTotal,
      },
    })
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-12">
      <h1 className="mb-8 text-3xl font-bold text-gray-900">注文内容の確認</h1>

      {/* 注文商品一覧 */}
      <div className="mb-8 rounded-lg bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-lg font-bold text-gray-900">ご注文商品</h2>
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
                    {item.product.description}
                  </p>
                </div>
                <div className="flex items-center justify-between">
                  <p className="text-sm text-gray-600">
                    数量: {item.quantity}
                  </p>
                  <p className="font-bold text-gray-900">
                    ¥{(item.product.price * item.quantity).toLocaleString()}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 合計金額 */}
      <div className="mb-8 rounded-lg bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-lg font-bold text-gray-900">お支払い金額</h2>
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">小計</span>
            <span className="font-medium text-gray-900">
              ¥{totalPrice.toLocaleString()}
            </span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">配送料</span>
            <span className="font-medium text-gray-900">¥0</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">手数料</span>
            <span className="font-medium text-gray-900">¥0</span>
          </div>
          <div className="border-t border-gray-200 pt-2">
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
      </div>

      {/* アクションボタン */}
      <div className="space-y-3">
        <button
          onClick={handleConfirmOrder}
          className="w-full rounded-lg bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700"
        >
          注文を確定する
        </button>
        <Link
          to="/order/cart"
          className="block text-center text-sm text-blue-600 hover:underline"
        >
          カートに戻る
        </Link>
      </div>
    </div>
  )
}
