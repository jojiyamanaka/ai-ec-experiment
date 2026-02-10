import { useState } from 'react'
import { useProducts } from '../contexts/ProductContext'
import type { Product } from '../data/mockProducts'

export default function AdminItemPage() {
  const { products, updateProduct } = useProducts()
  const [editedProducts, setEditedProducts] = useState<
    Record<number, Partial<Product>>
  >({})
  const [savedMessage, setSavedMessage] = useState(false)

  // 商品の編集内容を一時保存
  const handleEdit = (id: number, field: keyof Product, value: any) => {
    setEditedProducts((prev) => ({
      ...prev,
      [id]: {
        ...prev[id],
        [field]: value,
      },
    }))
  }

  // 変更を保存
  const handleSave = () => {
    Object.entries(editedProducts).forEach(([id, updates]) => {
      updateProduct(Number(id), updates)
    })
    setEditedProducts({})
    setSavedMessage(true)
    setTimeout(() => setSavedMessage(false), 3000)
  }

  // 編集された値または元の値を取得
  const getValue = (product: Product, field: keyof Product) => {
    return editedProducts[product.id]?.[field] ?? product[field]
  }

  // 変更があるかチェック
  const hasChanges = Object.keys(editedProducts).length > 0

  return (
    <div className="mx-auto max-w-7xl px-4 py-12">
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">商品管理</h1>
          <p className="mt-2 text-sm text-gray-600">
            商品の価格、在庫、公開状態を管理できます
          </p>
        </div>
        {savedMessage && (
          <div className="rounded-lg bg-green-50 px-4 py-2 text-sm font-medium text-green-800">
            ✓ 保存しました
          </div>
        )}
      </div>

      {/* 商品テーブル */}
      <div className="overflow-hidden rounded-lg bg-white shadow-sm">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                ID
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
            {products.map((product) => (
              <tr key={product.id} className="hover:bg-gray-50">
                <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-500">
                  {product.id}
                </td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-3">
                    <img
                      src={product.image}
                      alt={product.name}
                      className="h-10 w-10 rounded object-cover"
                    />
                    <div>
                      <div className="text-sm font-medium text-gray-900">
                        {product.name}
                      </div>
                      <div className="text-xs text-gray-500">
                        {product.description}
                      </div>
                    </div>
                  </div>
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <input
                    type="number"
                    value={getValue(product, 'price')}
                    onChange={(e) =>
                      handleEdit(
                        product.id,
                        'price',
                        parseInt(e.target.value) || 0
                      )
                    }
                    className="w-32 rounded border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  />
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <input
                    type="number"
                    value={getValue(product, 'stock')}
                    onChange={(e) =>
                      handleEdit(
                        product.id,
                        'stock',
                        parseInt(e.target.value) || 0
                      )
                    }
                    className="w-24 rounded border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  />
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <button
                    onClick={() =>
                      handleEdit(
                        product.id,
                        'isPublished',
                        !getValue(product, 'isPublished')
                      )
                    }
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${
                      getValue(product, 'isPublished')
                        ? 'bg-blue-600'
                        : 'bg-gray-200'
                    }`}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                        getValue(product, 'isPublished')
                          ? 'translate-x-6'
                          : 'translate-x-1'
                      }`}
                    />
                  </button>
                  <span className="ml-3 text-sm text-gray-700">
                    {getValue(product, 'isPublished') ? '公開' : '非公開'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* 保存ボタン */}
      <div className="mt-6 flex justify-end">
        <button
          onClick={handleSave}
          disabled={!hasChanges}
          className={`rounded-lg px-6 py-3 font-medium text-white ${
            hasChanges
              ? 'bg-blue-600 hover:bg-blue-700'
              : 'cursor-not-allowed bg-gray-400'
          }`}
        >
          保存
        </button>
      </div>
    </div>
  )
}
