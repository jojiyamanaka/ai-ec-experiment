import { Link } from 'react-router'
import ProductCard from '../components/ProductCard'
import { useProducts } from '../contexts/ProductContext'

export default function HomePage() {
  const { getPublishedProducts } = useProducts()
  const recommendedProducts = getPublishedProducts().slice(0, 3)

  return (
    <div>
      {/* バナーエリア */}
      <section className="bg-gradient-to-r from-blue-500 to-purple-600">
        <div className="mx-auto max-w-7xl px-4 py-24 text-center text-white">
          <h1 className="text-4xl font-bold md:text-5xl">
            AI がおすすめする最高の商品
          </h1>
          <p className="mt-4 text-lg md:text-xl">
            あなたにぴったりの商品を見つけよう
          </p>
        </div>
      </section>

      {/* おすすめ商品セクション */}
      <section className="mx-auto max-w-7xl px-4 py-12">
        <div className="mb-6 flex items-center justify-between">
          <h2 className="text-2xl font-bold text-gray-900">おすすめ商品</h2>
          <Link
            to="/item"
            className="text-sm font-medium text-blue-600 hover:underline"
          >
            すべて見る →
          </Link>
        </div>
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {recommendedProducts.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </section>
    </div>
  )
}
