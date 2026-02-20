import { useState } from 'react'
import { useParams, useNavigate } from 'react-router'
import { useCart } from '@features/cart'
import { useProducts } from '@entities/product'

// 在庫状態を判定する関数
function getStockStatus(stock: number) {
  if (stock === 0) {
    return {
      text: '売り切れ',
      color: 'bg-stone-400 text-white',
    }
  } else if (stock >= 1 && stock <= 5) {
    return {
      text: '残りわずか',
      color: 'bg-zinc-500 text-white',
    }
  } else {
    return {
      text: '在庫あり',
      color: 'bg-zinc-700 text-white',
    }
  }
}

export default function ItemDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { addToCart } = useCart()
  const { products } = useProducts()
  const product = products.find((p) => p.id === Number(id))
  const [isAdding, setIsAdding] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!product) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-12">
        <p className="text-center text-gray-500">商品が見つかりません</p>
      </div>
    )
  }

  const stockStatus = getStockStatus(product.effectiveStock)
  const isSoldOut = product.effectiveStock === 0

  const handleAddToCart = async () => {
    setIsAdding(true)
    setError(null)
    try {
      await addToCart(product)
      navigate('/order/cart')
    } catch (err) {
      console.error('カート追加エラー:', err)
      setError(err instanceof Error ? err.message : 'カートへの追加に失敗しました')
    } finally {
      setIsAdding(false)
    }
  }

  return (
    <div className="mx-auto max-w-7xl px-6 py-24">
      <div className="grid gap-12 md:grid-cols-2">
        <div className="aspect-[3/4] overflow-hidden bg-stone-200">
          <img
            src={product.image}
            alt={product.name}
            className="h-full w-full object-cover"
          />
        </div>
        <div className="space-y-8">
          <div>
            <h1 className="font-serif text-4xl md:text-5xl leading-tight text-zinc-900">
              {product.name}
            </h1>
            <div className="mt-6 flex items-center gap-4">
              <p className="text-sm text-zinc-500">
                ¥{product.price.toLocaleString()}
              </p>
              <span
                className={`rounded-full px-4 py-1 text-xs font-medium ${stockStatus.color}`}
              >
                {stockStatus.text}
              </span>
            </div>
          </div>
          <p className="leading-relaxed text-zinc-600 font-light">
            {product.description}
          </p>
          {error && (
            <div className="rounded-lg border border-red-300 bg-red-50 p-3">
              <p className="text-sm text-red-700">{error}</p>
            </div>
          )}
          <button
            onClick={handleAddToCart}
            disabled={isSoldOut || isAdding}
            className={`w-full px-12 py-4 text-xs tracking-[0.2em] uppercase transition-colors ${
              isSoldOut || isAdding
                ? 'cursor-not-allowed bg-stone-400 text-white'
                : 'bg-zinc-900 text-white hover:bg-zinc-800'
            }`}
          >
            {isSoldOut ? '売り切れ' : isAdding ? '追加中...' : 'カートに追加'}
          </button>
        </div>
      </div>
    </div>
  )
}
