import { useEffect, useMemo, useState } from 'react'
import { useProducts } from '@entities/product'
import type { CreateProductRequest, ProductCategory, UpdateProductRequest } from '@entities/product'
import {
  AdminFilterChips,
  AdminModalBase,
  AdminPageContainer,
  AdminPageHeader,
  AdminSearchBar,
  AdminTableShell,
} from '@shared/ui/admin'

export default function AdminItemPage() {
  const {
    products,
    categories,
    refreshProducts,
    refreshCategories,
    createProduct,
    updateProduct,
    createCategory,
    updateCategory,
  } = useProducts()
  const [editedProducts, setEditedProducts] = useState<Record<number, UpdateProductRequest>>({})
  const [savedMessage, setSavedMessage] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [searchInput, setSearchInput] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [publishFilter, setPublishFilter] = useState<'ALL' | 'PUBLISHED' | 'UNPUBLISHED'>('ALL')
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [showCategoryModal, setShowCategoryModal] = useState(false)
  const [categoryDrafts, setCategoryDrafts] = useState<Record<number, Partial<ProductCategory>>>({})
  const [createCategoryName, setCreateCategoryName] = useState('')

  const [createForm, setCreateForm] = useState<CreateProductRequest>({
    productCode: '',
    name: '',
    description: '',
    categoryId: 0,
    price: 0,
    stock: 0,
    isPublished: true,
    publishStartAt: null,
    publishEndAt: null,
    saleStartAt: null,
    saleEndAt: null,
    image: '',
  })

  useEffect(() => {
    if (categories.length > 0 && createForm.categoryId === 0) {
      setCreateForm((prev) => ({ ...prev, categoryId: categories[0].id }))
    }
  }, [categories, createForm.categoryId])

  useEffect(() => {
    void refreshProducts()
    void refreshCategories()
  }, [refreshProducts, refreshCategories])

  const toDateTimeLocal = (value: string | number | null | undefined): string => {
    if (value === null || value === undefined || value === '') {
      return ''
    }
    const date = typeof value === 'number'
      ? new Date(value < 10_000_000_000 ? value * 1000 : value)
      : new Date(value)
    if (Number.isNaN(date.getTime())) {
      return ''
    }
    const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
    return localDate.toISOString().slice(0, 16)
  }

  const toIsoOrNull = (value: string | number | null | undefined): string | null => {
    if (value === null || value === undefined || value === '') {
      return null
    }
    const date = typeof value === 'number'
      ? new Date(value < 10_000_000_000 ? value * 1000 : value)
      : new Date(value)
    if (Number.isNaN(date.getTime())) {
      return null
    }
    return date.toISOString()
  }

  const handleEdit = (
    id: number,
    field: keyof UpdateProductRequest,
    value: string | number | boolean | null
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
        Object.entries(editedProducts).map(([id, updates]) => {
          const payload: UpdateProductRequest = {
            ...updates,
            publishStartAt: toIsoOrNull(updates.publishStartAt ?? null),
            publishEndAt: toIsoOrNull(updates.publishEndAt ?? null),
            saleStartAt: toIsoOrNull(updates.saleStartAt ?? null),
            saleEndAt: toIsoOrNull(updates.saleEndAt ?? null),
          }
          return updateProduct(Number(id), payload)
        })
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
    defaultValue: string | number | boolean | null
  ) => {
    return editedProducts[productId]?.[field] ?? defaultValue
  }

  const getNumberValue = (
    productId: number,
    field: 'price' | 'stock' | 'categoryId',
    defaultValue: number
  ) => {
    return Number(getValue(productId, field, defaultValue))
  }

  const getBooleanValue = (
    productId: number,
    field: 'isPublished',
    defaultValue: boolean
  ) => {
    return Boolean(getValue(productId, field, defaultValue))
  }

  const getDateTimeValue = (
    productId: number,
    field: 'publishStartAt' | 'publishEndAt' | 'saleStartAt' | 'saleEndAt',
    defaultValue: string | number | null
  ) => {
    return toDateTimeLocal(getValue(productId, field, defaultValue) as string | number | null)
  }

  const handleCreateProduct = async () => {
    try {
      await createProduct({
        ...createForm,
        publishStartAt: toIsoOrNull(createForm.publishStartAt),
        publishEndAt: toIsoOrNull(createForm.publishEndAt),
        saleStartAt: toIsoOrNull(createForm.saleStartAt),
        saleEndAt: toIsoOrNull(createForm.saleEndAt),
      })
      setShowCreateModal(false)
      setCreateForm({
        productCode: '',
        name: '',
        description: '',
        categoryId: categories[0]?.id ?? 0,
        price: 0,
        stock: 0,
        isPublished: true,
        publishStartAt: null,
        publishEndAt: null,
        saleStartAt: null,
        saleEndAt: null,
        image: '',
      })
      setSavedMessage(true)
      setTimeout(() => setSavedMessage(false), 3000)
    } catch (error) {
      const message = error instanceof Error ? error.message : '商品登録に失敗しました'
      alert(message)
    }
  }

  const handleCreateCategory = async () => {
    if (!createCategoryName.trim()) {
      alert('カテゴリ名を入力してください')
      return
    }
    try {
      await createCategory({
        name: createCategoryName.trim(),
        displayOrder: categories.length,
        isPublished: true,
      })
      setCreateCategoryName('')
    } catch (error) {
      const message = error instanceof Error ? error.message : 'カテゴリ登録に失敗しました'
      alert(message)
    }
  }

  const handleCategoryDraft = (id: number, patch: Partial<ProductCategory>) => {
    setCategoryDrafts((prev) => ({
      ...prev,
      [id]: {
        ...(prev[id] ?? {}),
        ...patch,
      },
    }))
  }

  const handleSaveCategory = async (category: ProductCategory) => {
    const draft = categoryDrafts[category.id]
    if (!draft) {
      return
    }
    try {
      await updateCategory(category.id, {
        name: draft.name ?? category.name,
        displayOrder: draft.displayOrder ?? category.displayOrder,
        isPublished: draft.isPublished ?? category.isPublished,
      })
      setCategoryDrafts((prev) => {
        const next = { ...prev }
        delete next[category.id]
        return next
      })
    } catch (error) {
      const message = error instanceof Error ? error.message : 'カテゴリ更新に失敗しました'
      alert(message)
    }
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
    <AdminPageContainer>
      <AdminPageHeader
        title="商品管理"
        subMessage={savedMessage ? <p className="text-sm font-medium text-emerald-700">保存しました</p> : null}
        actions={(
          <div className="flex gap-2">
            <button
              onClick={() => setShowCategoryModal(true)}
              className="rounded-lg border border-zinc-300 px-4 py-2 text-zinc-700 hover:bg-zinc-100"
            >
              カテゴリ管理
            </button>
            <button
              onClick={() => setShowCreateModal(true)}
              className="rounded-lg bg-zinc-900 px-4 py-2 text-white hover:bg-zinc-700"
            >
              新規登録
            </button>
          </div>
        )}
      />

      <AdminSearchBar
        value={searchInput}
        onChange={setSearchInput}
        onSearch={(value) => setSearchQuery(value.trim())}
        placeholder="商品名で検索"
      />

      <AdminFilterChips
        items={[
          { key: 'ALL', label: 'すべて' },
          { key: 'PUBLISHED', label: '公開' },
          { key: 'UNPUBLISHED', label: '非公開' },
        ]}
        selectedKey={publishFilter}
        onSelect={(key) => setPublishFilter(key as 'ALL' | 'PUBLISHED' | 'UNPUBLISHED')}
      />

      <AdminTableShell className="shadow-sm">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">商品ID</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">商品名</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">品番</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">カテゴリ</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">価格（円）</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">在庫数</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">公開状態</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">公開開始</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">公開終了</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">販売開始</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">販売終了</th>
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
                <td className="px-6 py-4 text-sm text-gray-700">{product.productCode}</td>
                <td className="px-6 py-4">
                  <select
                    value={getNumberValue(product.id, 'categoryId', product.categoryId ?? categories[0]?.id ?? 0)}
                    onChange={(e) => handleEdit(product.id, 'categoryId', parseInt(e.target.value, 10))}
                    className="w-40 rounded border border-gray-300 px-3 py-2 text-sm"
                  >
                    {categories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <input
                    type="number"
                    value={getNumberValue(product.id, 'price', product.price)}
                    onChange={(e) => handleEdit(product.id, 'price', parseInt(e.target.value, 10) || 0)}
                    className="w-32 rounded border border-gray-300 px-3 py-2 text-sm"
                  />
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <input
                    type="number"
                    value={getNumberValue(product.id, 'stock', product.stock)}
                    onChange={(e) => handleEdit(product.id, 'stock', parseInt(e.target.value, 10) || 0)}
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
                    {getBooleanValue(product.id, 'isPublished', product.isPublished) ? '公開' : '非公開'}
                  </span>
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <input
                    type="datetime-local"
                    value={getDateTimeValue(product.id, 'publishStartAt', product.publishStartAt)}
                    onChange={(e) => handleEdit(product.id, 'publishStartAt', e.target.value || null)}
                    className="w-48 rounded border border-gray-300 px-3 py-2 text-sm"
                  />
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <input
                    type="datetime-local"
                    value={getDateTimeValue(product.id, 'publishEndAt', product.publishEndAt)}
                    onChange={(e) => handleEdit(product.id, 'publishEndAt', e.target.value || null)}
                    className="w-48 rounded border border-gray-300 px-3 py-2 text-sm"
                  />
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <input
                    type="datetime-local"
                    value={getDateTimeValue(product.id, 'saleStartAt', product.saleStartAt)}
                    onChange={(e) => handleEdit(product.id, 'saleStartAt', e.target.value || null)}
                    className="w-48 rounded border border-gray-300 px-3 py-2 text-sm"
                  />
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <input
                    type="datetime-local"
                    value={getDateTimeValue(product.id, 'saleEndAt', product.saleEndAt)}
                    onChange={(e) => handleEdit(product.id, 'saleEndAt', e.target.value || null)}
                    className="w-48 rounded border border-gray-300 px-3 py-2 text-sm"
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </AdminTableShell>

      <div className="mt-6 flex justify-end">
        <button
          onClick={handleSave}
          disabled={!hasChanges || isSaving}
          className={`rounded-lg px-6 py-3 font-medium text-white ${
            hasChanges && !isSaving
              ? 'bg-zinc-900 hover:bg-zinc-700'
              : 'cursor-not-allowed bg-gray-400'
          }`}
        >
          {isSaving ? '保存中...' : '保存'}
        </button>
      </div>

      <AdminModalBase
        isOpen={Boolean(selectedProduct)}
        onClose={() => setSelectedProductId(null)}
        title="商品詳細"
        maxWidthClass="max-w-2xl"
      >
        {selectedProduct ? (
          <div className="space-y-2 text-sm text-gray-700">
            <p>
              <span className="font-medium text-gray-900">商品ID:</span>{' '}
              <span className="font-mono">{selectedProduct.id}</span>
            </p>
            <p>
              <span className="font-medium text-gray-900">商品名:</span> {selectedProduct.name}
            </p>
            <p>
              <span className="font-medium text-gray-900">品番:</span> {selectedProduct.productCode}
            </p>
            <p>
              <span className="font-medium text-gray-900">カテゴリ:</span> {selectedProduct.categoryName}
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
            <p>
              <span className="font-medium text-gray-900">公開期間:</span>{' '}
              {toDateTimeLocal(selectedProduct.publishStartAt) || '-'} 〜 {toDateTimeLocal(selectedProduct.publishEndAt) || '-'}
            </p>
            <p>
              <span className="font-medium text-gray-900">販売期間:</span>{' '}
              {toDateTimeLocal(selectedProduct.saleStartAt) || '-'} 〜 {toDateTimeLocal(selectedProduct.saleEndAt) || '-'}
            </p>
          </div>
        ) : null}
      </AdminModalBase>

      <AdminModalBase
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        title="商品新規登録"
        maxWidthClass="max-w-2xl"
      >
        <div className="space-y-3">
          <input
            type="text"
            placeholder="品番"
            value={createForm.productCode}
            onChange={(e) => setCreateForm((prev) => ({ ...prev, productCode: e.target.value }))}
            className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
          />
          <input
            type="text"
            placeholder="商品名"
            value={createForm.name}
            onChange={(e) => setCreateForm((prev) => ({ ...prev, name: e.target.value }))}
            className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
          />
          <textarea
            placeholder="説明"
            value={createForm.description}
            onChange={(e) => setCreateForm((prev) => ({ ...prev, description: e.target.value }))}
            className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
          />
          <select
            value={createForm.categoryId}
            onChange={(e) => setCreateForm((prev) => ({ ...prev, categoryId: parseInt(e.target.value, 10) }))}
            className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
          >
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
          <div className="grid grid-cols-2 gap-3">
            <input
              type="number"
              placeholder="価格"
              value={createForm.price}
              onChange={(e) => setCreateForm((prev) => ({ ...prev, price: parseInt(e.target.value, 10) || 0 }))}
              className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
            />
            <input
              type="number"
              placeholder="在庫"
              value={createForm.stock}
              onChange={(e) => setCreateForm((prev) => ({ ...prev, stock: parseInt(e.target.value, 10) || 0 }))}
              className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <input
              type="datetime-local"
              value={toDateTimeLocal(createForm.publishStartAt)}
              onChange={(e) => setCreateForm((prev) => ({ ...prev, publishStartAt: e.target.value || null }))}
              className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
            />
            <input
              type="datetime-local"
              value={toDateTimeLocal(createForm.publishEndAt)}
              onChange={(e) => setCreateForm((prev) => ({ ...prev, publishEndAt: e.target.value || null }))}
              className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
            />
            <input
              type="datetime-local"
              value={toDateTimeLocal(createForm.saleStartAt)}
              onChange={(e) => setCreateForm((prev) => ({ ...prev, saleStartAt: e.target.value || null }))}
              className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
            />
            <input
              type="datetime-local"
              value={toDateTimeLocal(createForm.saleEndAt)}
              onChange={(e) => setCreateForm((prev) => ({ ...prev, saleEndAt: e.target.value || null }))}
              className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
            />
          </div>
          <label className="flex items-center gap-2 text-sm text-gray-700">
            <input
              type="checkbox"
              checked={Boolean(createForm.isPublished)}
              onChange={(e) => setCreateForm((prev) => ({ ...prev, isPublished: e.target.checked }))}
            />
            公開
          </label>
          <button
            onClick={handleCreateProduct}
            className="rounded bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700"
          >
            登録
          </button>
        </div>
      </AdminModalBase>

      <AdminModalBase
        isOpen={showCategoryModal}
        onClose={() => setShowCategoryModal(false)}
        title="カテゴリ管理"
        maxWidthClass="max-w-2xl"
      >
        <div className="space-y-4">
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="新規カテゴリ名"
              value={createCategoryName}
              onChange={(e) => setCreateCategoryName(e.target.value)}
              className="flex-1 rounded border border-gray-300 px-3 py-2 text-sm"
            />
            <button
              onClick={handleCreateCategory}
              className="rounded bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700"
            >
              追加
            </button>
          </div>
          <div className="space-y-2">
            {categories.map((category) => {
              const draft = categoryDrafts[category.id] ?? {}
              const draftName = draft.name ?? category.name
              const draftDisplayOrder = draft.displayOrder ?? category.displayOrder
              const draftPublished = draft.isPublished ?? category.isPublished
              return (
                <div key={category.id} className="grid grid-cols-[1fr_120px_120px_100px] items-center gap-2">
                  <input
                    type="text"
                    value={draftName}
                    onChange={(e) => handleCategoryDraft(category.id, { name: e.target.value })}
                    className="rounded border border-gray-300 px-3 py-2 text-sm"
                  />
                  <input
                    type="number"
                    value={draftDisplayOrder}
                    onChange={(e) => handleCategoryDraft(category.id, { displayOrder: parseInt(e.target.value, 10) || 0 })}
                    className="rounded border border-gray-300 px-3 py-2 text-sm"
                  />
                  <label className="flex items-center gap-2 text-sm text-gray-700">
                    <input
                      type="checkbox"
                      checked={Boolean(draftPublished)}
                      onChange={(e) => handleCategoryDraft(category.id, { isPublished: e.target.checked })}
                    />
                    公開
                  </label>
                  <button
                    onClick={() => handleSaveCategory(category)}
                    className="rounded border border-zinc-300 px-3 py-2 text-sm hover:bg-zinc-100"
                  >
                    保存
                  </button>
                </div>
              )
            })}
          </div>
        </div>
      </AdminModalBase>
    </AdminPageContainer>
  )
}
