import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router'
import ProductCard from '../components/ProductCard'
import Pagination from '../components/Pagination'
import * as api from '../lib/api'
import type { Product } from '../types/api'

const ITEMS_PER_PAGE = 12

export default function ItemListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [products, setProducts] = useState<Product[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // URLからページ番号を取得（不正値は1にフォールバック）
  const currentPage = Math.max(1, parseInt(searchParams.get('page') || '1') || 1)

  const totalPages = Math.ceil(total / ITEMS_PER_PAGE)

  useEffect(() => {
    const fetchProducts = async () => {
      setLoading(true)
      setError(null)
      try {
        const response = await api.getItems(currentPage, ITEMS_PER_PAGE)
        if (response.success && response.data) {
          setProducts(response.data.items)
          setTotal(response.data.total)
        } else {
          setError('商品の取得に失敗しました')
        }
      } catch {
        setError('商品の取得中にエラーが発生しました')
      } finally {
        setLoading(false)
      }
    }
    fetchProducts()
  }, [currentPage])

  const handlePageChange = (page: number) => {
    if (page === 1) {
      setSearchParams({})
    } else {
      setSearchParams({ page: String(page) })
    }
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-6 py-24">
        <h1 className="mb-12 font-serif text-3xl text-zinc-900">すべての商品</h1>
        <p className="text-sm text-zinc-500">読み込み中...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="mx-auto max-w-7xl px-6 py-24">
        <h1 className="mb-12 font-serif text-3xl text-zinc-900">すべての商品</h1>
        <p className="text-sm text-zinc-500">{error}</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-7xl px-6 py-24">
      <h1 className="mb-12 font-serif text-3xl text-zinc-900">すべての商品</h1>
      <div className="grid gap-x-6 gap-y-12 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {products.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
      {totalPages > 1 && (
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={handlePageChange}
        />
      )}
    </div>
  )
}
