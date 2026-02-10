import { useParams, useNavigate } from 'react-router'
import { useCart } from '../contexts/CartContext'
import { useProducts } from '../contexts/ProductContext'

// 在庫状態を判定する関数
function getStockStatus(stock: number) {
  if (stock === 0) {
    return {
      text: '売り切れ',
      color: 'bg-gray-400 text-white',
    }
  } else if (stock >= 1 && stock <= 5) {
    return {
      text: '残りわずか',
      color: 'bg-orange-500 text-white',
    }
  } else {
    return {
      text: '在庫あり',
      color: 'bg-green-500 text-white',
    }
  }
}

export default function ItemDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { addToCart } = useCart()
  const { products } = useProducts()
  const product = products.find((p) => p.id === Number(id))

  if (!product) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-12">
        <p className="text-center text-gray-500">商品が見つかりません</p>
      </div>
    )
  }

  const stockStatus = getStockStatus(product.stock)
  const isSoldOut = product.stock === 0

  const handleAddToCart = () => {
    addToCart(product)
    navigate('/order/cart')
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-12">
      <div className="grid gap-8 md:grid-cols-2">
        <div className="aspect-[4/3] overflow-hidden rounded-lg bg-gray-200">
          <img
            src={product.image}
            alt={product.name}
            className="h-full w-full object-cover"
          />
        </div>
        <div>
          <h1 className="text-3xl font-bold text-gray-900">{product.name}</h1>
          <div className="mt-4 flex items-center gap-4">
            <p className="text-2xl font-bold text-blue-600">
              ¥{product.price.toLocaleString()}
            </p>
            <span
              className={`rounded-full px-4 py-1 text-sm font-medium ${stockStatus.color}`}
            >
              {stockStatus.text}
            </span>
          </div>
          <p className="mt-6 leading-relaxed text-gray-600">
            {product.description}
          </p>
          <button
            onClick={handleAddToCart}
            disabled={isSoldOut}
            className={`mt-8 w-full rounded-lg px-6 py-3 font-medium text-white ${
              isSoldOut
                ? 'cursor-not-allowed bg-gray-400'
                : 'bg-blue-600 hover:bg-blue-700'
            }`}
          >
            {isSoldOut ? '売り切れ' : 'カートに追加'}
          </button>
        </div>
      </div>
    </div>
  )
}
