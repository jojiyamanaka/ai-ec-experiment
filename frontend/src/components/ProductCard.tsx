import { Link } from 'react-router'
import type { Product } from '../types/api'

interface ProductCardProps {
  product: Product
}

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

export default function ProductCard({ product }: ProductCardProps) {
  const stockStatus = getStockStatus(product.stock)

  return (
    <Link
      to={`/item/${product.id}`}
      className="group block cursor-pointer"
    >
      <div className="aspect-[3/4] overflow-hidden bg-stone-200 mb-4">
        <img
          src={product.image}
          alt={product.name}
          className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
        />
      </div>
      <div>
        <h3 className="font-serif text-sm mb-1 uppercase tracking-wider text-zinc-900">
          {product.name}
        </h3>
        <p className="text-xs text-zinc-500 mb-2">{product.description}</p>
        <div className="flex items-center justify-between">
          <p className="text-xs text-zinc-500">
            ¥{product.price.toLocaleString()}
          </p>
          <span
            className={`rounded-full px-3 py-1 text-xs font-medium ${stockStatus.color}`}
          >
            {stockStatus.text}
          </span>
        </div>
      </div>
    </Link>
  )
}
