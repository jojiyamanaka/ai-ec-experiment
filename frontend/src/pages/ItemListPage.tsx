import ProductCard from '../components/ProductCard'
import { useProducts } from '../contexts/ProductContext'

export default function ItemListPage() {
  const { getPublishedProducts } = useProducts()
  const publishedProducts = getPublishedProducts()

  return (
    <div className="mx-auto max-w-7xl px-4 py-12">
      <h1 className="mb-8 text-3xl font-bold text-gray-900">すべての商品</h1>
      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {publishedProducts.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
    </div>
  )
}
