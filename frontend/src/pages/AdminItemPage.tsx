import { useMemo, useState } from 'react'
import { useProducts } from '../contexts/ProductContext'
import type { UpdateProductRequest } from '../types/api'

export default function AdminItemPage() {
  const { products, updateProduct } = useProducts()
  const [editedProducts, setEditedProducts] = useState<
    Record<number, UpdateProductRequest>
  >({})
  const [savedMessage, setSavedMessage] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [searchInput, setSearchInput] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [publishFilter, setPublishFilter] = useState<'ALL' | 'PUBLISHED' | 'UNPUBLISHED'>('ALL')
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null)

  const handleEdit = (
    id: number,
    field: keyof UpdateProductRequest,
    value: number | boolean
  ) => {
    setEditedProducts((prev) => ({
      ...prev,
      [id]: {
        ...prev[id],
        [field]: value,
      },
    }))
  }

  const handleSave = async () => {
    setIsSaving(true)
    try {
      await Promise.all(
        Object.entries(editedProducts).map(([id, updates]) =>
          updateProduct(Number(id), updates)
        )
      )
      setEditedProducts({})
      setSavedMessage(true)
      setTimeout(() => setSavedMessage(false), 3000)
    } catch (error) {
      console.error('保存エラー:', error)
      const message = error instanceof Error ? error.message : '保存に失敗しました'
      alert(message)
    } finally {
      setIsSaving(false)
    }
  }

  const getValue = (
    productId: number,
    field: keyof UpdateProductRequest,
    defaultValue: number | boolean
  ) => {
    return editedProducts[productId]?.[field] ?? defaultValue
  }

  const getNumberValue = (
    productId: number,
    field: 'price' | 'stock',
    defaultValue: number
  ) => {
    return getValue(productId, field, defaultValue) as number
  }

  const getBooleanValue = (
    productId: number,
    field: 'isPublished',
    defaultValue: boolean
  ) => {
    return getValue(productId, field, defaultValue) as boolean
  }

  const handleSearch = () => {
    setSearchQuery(searchInput.trim())
  }

  const filteredProducts = useMemo(() => {
    return products
      .filter((product) =>
        product.name.toLowerCase().includes(searchQuery.toLowerCase())
      )
      .filter((product) => {
        if (publishFilter === 'ALL') return true
        if (publishFilter === 'PUBLISHED') return product.isPublished
        return !product.isPublished
      })
  }, [products, searchQuery, publishFilter])

  const selectedProduct = useMemo(() => {
    return products.find((product) => product.id === selectedProductId) ?? null
  }, [products, selectedProductId])

  const hasChanges = Object.keys(editedProducts).length > 0

  return (
    <div className="mx-auto max-w-7xl px-6 py-8">
      {/* ヘッダー */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-zinc-900">商品管理</h1>
          {savedMessage && (
            <p className="mt-2 text-sm font-medium text-emerald-700">保存しました</p>
          )}
        </div>
        <button className="rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700">
          新規登録
        </button>
      </div>

      {/* 検索エリア */}
      <div className="mb-4 flex gap-2">
        <input
          type="text"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              handleSearch()
            }
          }}
          placeholder="商品名で検索"
          className="w-full max-w-md rounded-lg border px-4 py-2"
        />
        <button
          onClick={handleSearch}
          className="rounded-lg bg-gray-800 px-4 py-2 text-white hover:bg-gray-900"
        >
          検索
        </button>
      </div>

      {/* フィルタエリア */}
      <div className="mb-6 flex gap-2">
        <button
          onClick={() => setPublishFilter('ALL')}
          className={`rounded-lg px-4 py-2 font-medium ${
            publishFilter === 'ALL'
              ? 'bg-blue-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          すべて
        </button>
        <button
          onClick={() => setPublishFilter('PUBLISHED')}
          className={`rounded-lg px-4 py-2 font-medium ${
            publishFilter === 'PUBLISHED'
              ? 'bg-blue-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          公開
        </button>
        <button
          onClick={() => setPublishFilter('UNPUBLISHED')}
          className={`rounded-lg px-4 py-2 font-medium ${
            publishFilter === 'UNPUBLISHED'
              ? 'bg-blue-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          非公開
        </button>
      </div>

      {/* テーブル */}
      <div className="overflow-hidden rounded-lg bg-white shadow-sm">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                商品ID
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                商品名
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                価格（円）
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                在庫数
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                公開状態
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 bg-white">
            {filteredProducts.map((product) => (
              <tr key={product.id} className="hover:bg-gray-50">
                <td className="whitespace-nowrap px-6 py-4 text-sm">
                  <button
                    onClick={() => setSelectedProductId(product.id)}
                    className="font-mono text-blue-600 hover:underline"
                  >
                    {product.id}
                  </button>
                </td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-3">
                    <img
                      src={product.image}
                      alt={product.name}
                      className="h-10 w-10 rounded object-cover"
                    />
                    <div>
                      <div className="text-sm font-medium text-gray-900">{product.name}</div>
                      <div className="text-xs text-gray-500">{product.description}</div>
                    </div>
                  </div>
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <input
                    type="number"
                    value={getNumberValue(product.id, 'price', product.price)}
                    onChange={(e) =>
                      handleEdit(
                        product.id,
                        'price',
                        parseInt(e.target.value) || 0
                      )
                    }
                    className="w-32 rounded border border-gray-300 px-3 py-2 text-sm"
                  />
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <input
                    type="number"
                    value={getNumberValue(product.id, 'stock', product.stock)}
                    onChange={(e) =>
                      handleEdit(
                        product.id,
                        'stock',
                        parseInt(e.target.value) || 0
                      )
                    }
                    className="w-24 rounded border border-gray-300 px-3 py-2 text-sm"
                  />
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <button
                    onClick={() =>
                      handleEdit(
                        product.id,
                        'isPublished',
                        !getBooleanValue(product.id, 'isPublished', product.isPublished)
                      )
                    }
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                      getBooleanValue(product.id, 'isPublished', product.isPublished)
                        ? 'bg-blue-600'
                        : 'bg-gray-200'
                    }`}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                        getBooleanValue(product.id, 'isPublished', product.isPublished)
                          ? 'translate-x-6'
                          : 'translate-x-1'
                      }`}
                    />
                  </button>
                  <span className="ml-3 text-sm text-gray-700">
                    {getBooleanValue(product.id, 'isPublished', product.isPublished)
                      ? '公開'
                      : '非公開'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-6 flex justify-end">
        <button
          onClick={handleSave}
          disabled={!hasChanges || isSaving}
          className={`rounded-lg px-6 py-3 font-medium text-white ${
            hasChanges && !isSaving
              ? 'bg-blue-600 hover:bg-blue-700'
              : 'cursor-not-allowed bg-gray-400'
          }`}
        >
          {isSaving ? '保存中...' : '保存'}
        </button>
      </div>

      {selectedProduct && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-2xl rounded-lg bg-white p-6">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-2xl font-bold text-zinc-900">商品詳細</h2>
              <button
                onClick={() => setSelectedProductId(null)}
                className="rounded bg-gray-200 px-3 py-1 text-sm hover:bg-gray-300"
              >
                閉じる
              </button>
            </div>
            <div className="space-y-2 text-sm text-gray-700">
              <p>
                <span className="font-medium text-gray-900">商品ID:</span>{' '}
                <span className="font-mono">{selectedProduct.id}</span>
              </p>
              <p>
                <span className="font-medium text-gray-900">商品名:</span> {selectedProduct.name}
              </p>
              <p>
                <span className="font-medium text-gray-900">説明:</span> {selectedProduct.description}
              </p>
              <p>
                <span className="font-medium text-gray-900">価格:</span>{' '}
                {selectedProduct.price.toLocaleString()}円
              </p>
              <p>
                <span className="font-medium text-gray-900">在庫:</span> {selectedProduct.stock}
              </p>
              <p>
                <span className="font-medium text-gray-900">公開状態:</span>{' '}
                {selectedProduct.isPublished ? '公開' : '非公開'}
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
