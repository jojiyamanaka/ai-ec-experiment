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

export default function ProductCard({ product }: ProductCardProps) {
  const stockStatus = getStockStatus(product.stock)

  return (
    <Link
      to={`/item/${product.id}`}
      className="group block overflow-hidden rounded-lg bg-white shadow-sm transition hover:shadow-md"
    >
      <div className="aspect-[4/3] overflow-hidden bg-gray-200">
        <img
          src={product.image}
          alt={product.name}
          className="h-full w-full object-cover transition group-hover:scale-105"
        />
      </div>
      <div className="p-4">
        <h3 className="font-medium text-gray-900">{product.name}</h3>
        <p className="mt-1 text-sm text-gray-500">{product.description}</p>
        <div className="mt-3 flex items-center justify-between">
          <p className="text-lg font-bold text-blue-600">
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
