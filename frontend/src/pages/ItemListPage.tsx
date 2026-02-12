import ProductCard from '../components/ProductCard'
import { useProducts } from '../contexts/ProductContext'

export default function ItemListPage() {
  const { getPublishedProducts } = useProducts()
  const publishedProducts = getPublishedProducts()

  return (
    <div className="mx-auto max-w-7xl px-6 py-24">
      <h1 className="mb-12 font-serif text-3xl text-zinc-900">すべての商品</h1>
      <div className="grid gap-x-6 gap-y-12 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {publishedProducts.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
    </div>
  )
}
