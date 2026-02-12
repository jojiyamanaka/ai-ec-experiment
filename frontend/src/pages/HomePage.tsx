import { Link } from 'react-router'
import ProductCard from '../components/ProductCard'
import { useProducts } from '../contexts/ProductContext'

export default function HomePage() {
  const { getPublishedProducts } = useProducts()
  const recommendedProducts = getPublishedProducts().slice(0, 3)

  return (
    <div>
      {/* バナーエリア */}
      <section className="bg-zinc-900">
        <div className="mx-auto max-w-7xl px-6 py-32 text-center text-white">
          <span className="text-xs uppercase tracking-[0.3em] text-zinc-400">
            2026 Spring Collection
          </span>
          <h1 className="mt-4 font-serif text-5xl md:text-7xl leading-tight">
            AI がおすすめする最高の商品
          </h1>
          <p className="mt-6 text-zinc-300 leading-relaxed font-light max-w-2xl mx-auto">
            あなたにぴったりの商品を見つけよう
          </p>
        </div>
      </section>

      {/* おすすめ商品セクション */}
      <section className="mx-auto max-w-7xl px-6 py-24">
        <div className="mb-12 flex items-end justify-between">
          <h2 className="font-serif text-3xl text-zinc-900">おすすめ商品</h2>
          <Link
            to="/item"
            className="text-xs uppercase tracking-widest border-b border-zinc-900 pb-1 hover:text-zinc-600 transition-colors"
          >
            View All
          </Link>
        </div>
        <div className="grid gap-x-6 gap-y-12 sm:grid-cols-2 lg:grid-cols-3">
          {recommendedProducts.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </section>
    </div>
  )
}
