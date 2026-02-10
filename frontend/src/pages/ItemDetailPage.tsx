import { useParams } from 'react-router'
import { mockProducts } from '../data/mockProducts'

export default function ItemDetailPage() {
  const { id } = useParams<{ id: string }>()
  const product = mockProducts.find((p) => p.id === Number(id))

  if (!product) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-12">
        <p className="text-center text-gray-500">商品が見つかりません</p>
      </div>
    )
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
          <p className="mt-4 text-2xl font-bold text-blue-600">
            ¥{product.price.toLocaleString()}
          </p>
          <p className="mt-6 text-gray-600">{product.description}</p>
          <button className="mt-8 w-full rounded-lg bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700">
            カートに追加
          </button>
        </div>
      </div>
    </div>
  )
}
