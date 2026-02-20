import { useEffect, useMemo, useState } from 'react'
import { useProducts, getAdminItemInventory, updateAdminItemInventory } from '@entities/product'
import type {
  AllocationType,
  CreateProductRequest,
  ProductCategory,
  ProductInventory,
  UpdateProductInventoryRequest,
} from '@entities/product'
import {
  AdminFilterChips,
  AdminModalBase,
  AdminPageContainer,
  AdminPageHeader,
  AdminSearchBar,
  AdminTableShell,
} from '@shared/ui/admin'

type DetailTab = 'PRODUCT' | 'INVENTORY' | 'SALES'

interface ProductDetailForm {
  name: string
  description: string
  categoryId: number
  price: number
  image: string
}

interface SalesForm {
  isPublished: boolean
  publishStartAt: string
  publishEndAt: string
  saleStartAt: string
  saleEndAt: string
}

interface InventoryForm {
  allocationType: AllocationType
  availableQty: number
  frameLimitQty: number
  committedQty: number
  realRemainingQty: number
  consumedQty: number
  frameRemainingQty: number
}

const INITIAL_INVENTORY_FORM: InventoryForm = {
  allocationType: 'REAL',
  availableQty: 0,
  frameLimitQty: 0,
  committedQty: 0,
  realRemainingQty: 0,
  consumedQty: 0,
  frameRemainingQty: 0,
}

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
  const [savedMessage, setSavedMessage] = useState(false)
  const [searchInput, setSearchInput] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [publishFilter, setPublishFilter] = useState<'ALL' | 'PUBLISHED' | 'UNPUBLISHED'>('ALL')
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null)
  const [activeTab, setActiveTab] = useState<DetailTab>('PRODUCT')
  const [detailForm, setDetailForm] = useState<ProductDetailForm | null>(null)
  const [salesForm, setSalesForm] = useState<SalesForm | null>(null)
  const [inventoryForm, setInventoryForm] = useState<InventoryForm>(INITIAL_INVENTORY_FORM)
  const [inventorySnapshot, setInventorySnapshot] = useState<ProductInventory | null>(null)
  const [isDetailSaving, setIsDetailSaving] = useState(false)
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
    allocationType: 'REAL',
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

  useEffect(() => {
    const selectedProduct = products.find((product) => product.id === selectedProductId) ?? null
    if (!selectedProduct) {
      setDetailForm(null)
      setSalesForm(null)
      setInventoryForm(INITIAL_INVENTORY_FORM)
      setInventorySnapshot(null)
      return
    }

    setDetailForm({
      name: selectedProduct.name,
      description: selectedProduct.description,
      categoryId: selectedProduct.categoryId ?? categories[0]?.id ?? 0,
      price: selectedProduct.price,
      image: selectedProduct.image,
    })
    setSalesForm({
      isPublished: selectedProduct.isPublished,
      publishStartAt: toDateTimeLocal(selectedProduct.publishStartAt),
      publishEndAt: toDateTimeLocal(selectedProduct.publishEndAt),
      saleStartAt: toDateTimeLocal(selectedProduct.saleStartAt),
      saleEndAt: toDateTimeLocal(selectedProduct.saleEndAt),
    })

    const loadInventory = async () => {
      const response = await getAdminItemInventory(selectedProduct.id)
      if (response.success && response.data) {
        setInventorySnapshot(response.data)
        setInventoryForm({
          allocationType: response.data.allocationType,
          availableQty: response.data.locationStock.availableQty,
          frameLimitQty: response.data.salesLimit.frameLimitQty,
          committedQty: response.data.locationStock.committedQty,
          realRemainingQty: response.data.locationStock.remainingQty,
          consumedQty: response.data.salesLimit.consumedQty,
          frameRemainingQty: response.data.salesLimit.remainingQty,
        })
      }
    }

    void loadInventory()
  }, [categories, products, selectedProductId])

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
        allocationType: 'REAL',
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

  const handleDetailField = <K extends keyof ProductDetailForm>(
    field: K,
    value: ProductDetailForm[K]
  ) => {
    setDetailForm((prev) => (prev ? { ...prev, [field]: value } : prev))
  }

  const handleSalesField = <K extends keyof SalesForm>(field: K, value: SalesForm[K]) => {
    setSalesForm((prev) => (prev ? { ...prev, [field]: value } : prev))
  }

  const handleInventoryField = <K extends keyof InventoryForm>(field: K, value: InventoryForm[K]) => {
    setInventoryForm((prev) => ({ ...prev, [field]: value }))
  }

  const handleSaveDetail = async () => {
    if (!selectedProductId || !detailForm || !salesForm) {
      return
    }

    setIsDetailSaving(true)
    try {
      if (activeTab === 'PRODUCT') {
        await updateProduct(selectedProductId, {
          name: detailForm.name,
          description: detailForm.description,
          categoryId: detailForm.categoryId > 0 ? detailForm.categoryId : undefined,
          price: detailForm.price,
          image: detailForm.image,
        })
      }

      if (activeTab === 'INVENTORY') {
        const payload: UpdateProductInventoryRequest = {
          allocationType: inventoryForm.allocationType,
          locationStock: {
            availableQty: inventoryForm.availableQty,
          },
          salesLimit: {
            frameLimitQty: inventoryForm.frameLimitQty,
          },
        }
        const response = await updateAdminItemInventory(selectedProductId, payload)
        if (!response.success || !response.data) {
          throw new Error(response.error?.message || '在庫情報の保存に失敗しました')
        }
        setInventorySnapshot(response.data)
        setInventoryForm({
          allocationType: response.data.allocationType,
          availableQty: response.data.locationStock.availableQty,
          frameLimitQty: response.data.salesLimit.frameLimitQty,
          committedQty: response.data.locationStock.committedQty,
          realRemainingQty: response.data.locationStock.remainingQty,
          consumedQty: response.data.salesLimit.consumedQty,
          frameRemainingQty: response.data.salesLimit.remainingQty,
        })
      }

      if (activeTab === 'SALES') {
        await updateProduct(selectedProductId, {
          isPublished: salesForm.isPublished,
          publishStartAt: toIsoOrNull(salesForm.publishStartAt),
          publishEndAt: toIsoOrNull(salesForm.publishEndAt),
          saleStartAt: toIsoOrNull(salesForm.saleStartAt),
          saleEndAt: toIsoOrNull(salesForm.saleEndAt),
        })
      }

      await refreshProducts()
      setSavedMessage(true)
      setTimeout(() => setSavedMessage(false), 3000)
    } catch (error) {
      console.error('保存エラー:', error)
      const message = error instanceof Error ? error.message : '保存に失敗しました'
      alert(message)
    } finally {
      setIsDetailSaving(false)
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

  const selectedDetailCategory = useMemo(() => {
    if (!detailForm) {
      return null
    }
    return categories.find((category) => category.id === detailForm.categoryId) ?? null
  }, [categories, detailForm])

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
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">引当区分</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">有効在庫</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">公開状態</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 bg-white">
            {filteredProducts.map((product) => (
              <tr key={product.id} className="hover:bg-gray-50">
                <td className="whitespace-nowrap px-6 py-4 text-sm">
                  <button
                    onClick={() => {
                      setSelectedProductId(product.id)
                      setActiveTab('PRODUCT')
                    }}
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
                <td className="px-6 py-4 text-sm text-gray-700">{product.categoryName}</td>
                <td className="whitespace-nowrap px-6 py-4 text-sm tabular-nums text-gray-900">{product.price.toLocaleString()}円</td>
                <td className="px-6 py-4 text-sm text-gray-700">{product.allocationType}</td>
                <td className="whitespace-nowrap px-6 py-4 text-sm tabular-nums text-gray-900">{product.effectiveStock}</td>
                <td className="whitespace-nowrap px-6 py-4">
                  <span
                    className={`rounded-full px-2 py-1 text-xs ${
                      product.isPublished
                        ? 'bg-blue-100 text-blue-800'
                        : 'bg-gray-100 text-gray-800'
                    }`}
                  >
                    {product.isPublished ? '公開' : '非公開'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </AdminTableShell>

      <AdminModalBase
        isOpen={Boolean(selectedProduct)}
        onClose={() => setSelectedProductId(null)}
        title="商品詳細"
        maxWidthClass="max-w-4xl"
        bodyClassName="p-8"
      >
        {selectedProduct && detailForm && salesForm ? (
          <>
            <div className="mb-4 flex gap-2">
              {([
                { key: 'PRODUCT', label: '商品' },
                { key: 'INVENTORY', label: '在庫' },
                { key: 'SALES', label: '販売' },
              ] as const).map((tab) => (
                <button
                  key={tab.key}
                  onClick={() => setActiveTab(tab.key)}
                  className={`rounded px-4 py-2 text-sm ${
                    activeTab === tab.key
                      ? 'bg-zinc-900 text-white'
                      : 'bg-zinc-100 text-zinc-700 hover:bg-zinc-200'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            {activeTab === 'PRODUCT' && (
              <div>
                <h3 className="mb-3 font-bold text-zinc-900">商品情報</h3>
                <div className="grid gap-3 md:grid-cols-2">
                  <label className="text-sm text-zinc-700">
                    品番
                    <input
                      value={selectedProduct.productCode}
                      readOnly
                      className="mt-1 w-full rounded border bg-gray-50 px-3 py-2 text-gray-700"
                    />
                  </label>
                  <label className="text-sm text-zinc-700">
                    商品名
                    <input
                      value={detailForm.name}
                      onChange={(e) => handleDetailField('name', e.target.value)}
                      className="mt-1 w-full rounded border px-3 py-2"
                    />
                  </label>
                  <label className="text-sm text-zinc-700">
                    カテゴリ
                    <select
                      value={detailForm.categoryId}
                      onChange={(e) => handleDetailField('categoryId', parseInt(e.target.value, 10))}
                      className="mt-1 w-full rounded border px-3 py-2"
                    >
                      {categories.map((category) => (
                        <option key={category.id} value={category.id}>
                          {category.name}（ID:{category.id}）
                        </option>
                      ))}
                    </select>
                    <p className="mt-1 text-xs text-gray-500">
                      現在のカテゴリID: {selectedDetailCategory?.id ?? detailForm.categoryId}
                    </p>
                  </label>
                  <label className="text-sm text-zinc-700">
                    価格
                    <input
                      type="number"
                      value={detailForm.price}
                      onChange={(e) => handleDetailField('price', parseInt(e.target.value, 10) || 0)}
                      className="mt-1 w-full rounded border px-3 py-2"
                    />
                  </label>
                  <label className="text-sm text-zinc-700 md:col-span-2">
                    画像URL
                    <input
                      type="text"
                      value={detailForm.image}
                      onChange={(e) => handleDetailField('image', e.target.value)}
                      className="mt-1 w-full rounded border px-3 py-2"
                    />
                  </label>
                  <label className="text-sm text-zinc-700 md:col-span-2">
                    説明
                    <textarea
                      value={detailForm.description}
                      onChange={(e) => handleDetailField('description', e.target.value)}
                      className="mt-1 w-full rounded border px-3 py-2"
                    />
                  </label>
                </div>
              </div>
            )}

            {activeTab === 'INVENTORY' && (
              <div>
                <h3 className="mb-3 font-bold text-zinc-900">在庫情報</h3>
                <div className="mb-3">
                  <label className="text-sm text-zinc-700">
                    引当区分
                    <select
                      value={inventoryForm.allocationType}
                      onChange={(e) => handleInventoryField('allocationType', e.target.value as AllocationType)}
                      className="mt-1 w-full rounded border px-3 py-2"
                    >
                      <option value="REAL">REAL</option>
                      <option value="FRAME">FRAME</option>
                    </select>
                  </label>
                </div>
                <div className="grid gap-3 md:grid-cols-2">
                  <div className={`rounded border p-3 ${inventoryForm.allocationType === 'REAL' ? 'border-zinc-900 bg-white' : 'border-zinc-200 bg-gray-50'}`}>
                    <p className="mb-2 text-sm font-semibold text-zinc-900">実在庫</p>
                    <label className="text-sm text-zinc-700">
                      在庫総量
                      <input
                        type="number"
                        value={inventoryForm.availableQty}
                        onChange={(e) => handleInventoryField('availableQty', parseInt(e.target.value, 10) || 0)}
                        className="mt-1 w-full rounded border px-3 py-2"
                      />
                    </label>
                    <div className="mt-3 text-sm text-gray-700">
                      <p>本引当済: {inventoryForm.committedQty}</p>
                      <p>引当可能数: {inventoryForm.realRemainingQty}</p>
                    </div>
                  </div>
                  <div className={`rounded border p-3 ${inventoryForm.allocationType === 'FRAME' ? 'border-zinc-900 bg-white' : 'border-zinc-200 bg-gray-50'}`}>
                    <p className="mb-2 text-sm font-semibold text-zinc-900">枠在庫</p>
                    <label className="text-sm text-zinc-700">
                      販売上限
                      <input
                        type="number"
                        value={inventoryForm.frameLimitQty}
                        onChange={(e) => handleInventoryField('frameLimitQty', parseInt(e.target.value, 10) || 0)}
                        className="mt-1 w-full rounded border px-3 py-2"
                      />
                    </label>
                    <div className="mt-3 text-sm text-gray-700">
                      <p>枠消費: {inventoryForm.consumedQty}</p>
                      <p>枠残数: {inventoryForm.frameRemainingQty}</p>
                    </div>
                  </div>
                </div>
                {inventorySnapshot ? (
                  <p className="mt-2 text-xs text-gray-500">
                    最新反映: allocationType={inventorySnapshot.allocationType},
                    realRemaining={inventorySnapshot.locationStock.remainingQty},
                    frameRemaining={inventorySnapshot.salesLimit.remainingQty}
                  </p>
                ) : null}
              </div>
            )}

            {activeTab === 'SALES' && (
              <div className="rounded-lg bg-gray-50 p-4">
                <h3 className="mb-3 font-bold text-zinc-900">販売設定</h3>
                <div className="grid gap-3 md:grid-cols-2">
                  <label className="text-sm text-zinc-700">
                    公開開始
                    <input
                      type="datetime-local"
                      value={salesForm.publishStartAt}
                      onChange={(e) => handleSalesField('publishStartAt', e.target.value)}
                      className="mt-1 w-full rounded border px-3 py-2"
                    />
                  </label>
                  <label className="text-sm text-zinc-700">
                    公開終了
                    <input
                      type="datetime-local"
                      value={salesForm.publishEndAt}
                      onChange={(e) => handleSalesField('publishEndAt', e.target.value)}
                      className="mt-1 w-full rounded border px-3 py-2"
                    />
                  </label>
                  <label className="text-sm text-zinc-700">
                    販売開始
                    <input
                      type="datetime-local"
                      value={salesForm.saleStartAt}
                      onChange={(e) => handleSalesField('saleStartAt', e.target.value)}
                      className="mt-1 w-full rounded border px-3 py-2"
                    />
                  </label>
                  <label className="text-sm text-zinc-700">
                    販売終了
                    <input
                      type="datetime-local"
                      value={salesForm.saleEndAt}
                      onChange={(e) => handleSalesField('saleEndAt', e.target.value)}
                      className="mt-1 w-full rounded border px-3 py-2"
                    />
                  </label>
                </div>
                <label className="mt-3 inline-flex items-center gap-2 text-sm text-zinc-700">
                  <input
                    type="checkbox"
                    checked={salesForm.isPublished}
                    onChange={(e) => handleSalesField('isPublished', e.target.checked)}
                  />
                  公開
                </label>
              </div>
            )}

            <div className="mt-6 flex gap-2">
              <button
                onClick={handleSaveDetail}
                disabled={isDetailSaving}
                className="flex-1 rounded bg-zinc-900 px-4 py-2 font-medium text-white hover:bg-zinc-700 disabled:opacity-60"
              >
                {isDetailSaving ? '保存中...' : activeTab === 'PRODUCT' ? '商品情報を保存' : activeTab === 'INVENTORY' ? '在庫情報を保存' : '販売設定を保存'}
              </button>
            </div>

            <button
              onClick={() => setSelectedProductId(null)}
              className="mt-4 w-full rounded bg-gray-200 px-4 py-2 font-medium text-gray-700 hover:bg-gray-300"
            >
              閉じる
            </button>
          </>
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
          <select
            value={createForm.allocationType}
            onChange={(e) => setCreateForm((prev) => ({ ...prev, allocationType: e.target.value as AllocationType }))}
            className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
          >
            <option value="REAL">REAL</option>
            <option value="FRAME">FRAME</option>
          </select>
          <input
            type="number"
            placeholder="価格"
            value={createForm.price}
            onChange={(e) => setCreateForm((prev) => ({ ...prev, price: parseInt(e.target.value, 10) || 0 }))}
            className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
          />
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
