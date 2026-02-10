import { Link } from 'react-router'
import type { Product } from '../data/mockProducts'

interface ProductCardProps {
  product: Product
}

export default function ProductCard({ product }: ProductCardProps) {
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
        <p className="mt-2 text-lg font-bold text-blue-600">
          ¥{product.price.toLocaleString()}
        </p>
      </div>
    </Link>
  )
}
