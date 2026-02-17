import { useEffect, useMemo, useState } from 'react'
import * as api from '../lib/api'

interface InventoryItem {
  productId: number
  productName: string
  physicalStock: number
  tentativeReserved: number
  committedReserved: number
  availableStock: number
}

interface InventoryAdjustment {
  id: number
  productId: number
  productName?: string
  product?: {
    id: number
    name: string
  }
  quantityBefore: number
  quantityAfter: number
  quantityDelta: number
  reason: string
  adjustedBy: string
  adjustedAt: string
}

export default function AdminInventoryPage() {
  const [inventories, setInventories] = useState<InventoryItem[]>([])
  const [adjustments, setAdjustments] = useState<InventoryAdjustment[]>([])
  const [threshold, setThreshold] = useState<number>(10)
  const [showLowStock, setShowLowStock] = useState<boolean>(false)
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null)
  const [showAdjustModal, setShowAdjustModal] = useState<boolean>(false)
  const [adjustForm, setAdjustForm] = useState({ quantityDelta: 0, reason: '' })
  const [searchQuery, setSearchQuery] = useState<string>('')

  useEffect(() => {
    fetchInventories()
    fetchAdjustments()
  }, [])

  const fetchInventories = async () => {
    try {
      const response = await api.get<InventoryItem[]>('/bo/admin/inventory')
      if (response.success && response.data) {
        setInventories(response.data)
      }
    } catch (error) {
      console.error('在庫一覧取得エラー:', error)
    }
  }

  const fetchAdjustments = async () => {
    try {
      const response = await api.get<InventoryAdjustment[]>('/bo/admin/inventory/adjustments')
      if (response.success && response.data) {
        const normalized = response.data.map((adjustment: InventoryAdjustment) => ({
          ...adjustment,
          productId: adjustment.productId ?? adjustment.product?.id ?? 0,
          productName: adjustment.productName ?? adjustment.product?.name,
        }))
        setAdjustments(normalized)
      }
    } catch (error) {
      console.error('調整履歴取得エラー:', error)
    }
  }

  const openAdjustModal = (productId: number) => {
    setSelectedProductId(productId)
    setShowAdjustModal(true)
  }

  const handleAdjust = async () => {
    if (!selectedProductId) {
      return
    }
    if (!adjustForm.reason.trim()) {
      alert('調整理由を入力してください')
      return
    }

    try {
      const response = await api.post('/bo/admin/inventory/adjust', {
        productId: selectedProductId,
        quantityDelta: adjustForm.quantityDelta,
        reason: adjustForm.reason,
      })

      if (response.success) {
        alert('在庫を調整しました')
        setShowAdjustModal(false)
        setAdjustForm({ quantityDelta: 0, reason: '' })
        await fetchInventories()
        await fetchAdjustments()
      } else {
        alert(response.error?.message || '在庫調整に失敗しました')
      }
    } catch (error) {
      console.error('在庫調整エラー:', error)
      alert('在庫調整に失敗しました')
    }
  }

  const filteredInventories = useMemo(() => {
    return inventories
      .filter((item) =>
        item.productName.toLowerCase().includes(searchQuery.toLowerCase())
      )
      .filter((item) => {
        if (!showLowStock) return true
        return item.availableStock <= threshold
      })
  }, [inventories, searchQuery, showLowStock, threshold])

  const selectedProduct = useMemo(() => {
    return inventories.find((item) => item.productId === selectedProductId) ?? null
  }, [inventories, selectedProductId])

  return (
    <div className="p-8">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-zinc-900">在庫管理</h1>
      </div>

      <div className="mb-6 flex flex-wrap items-center gap-3">
        <input
          type="text"
          placeholder="商品名で検索"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="px-4 py-2 border rounded-lg w-full max-w-md"
        />
        <div className="flex items-center gap-2">
          <label className="text-sm text-gray-700">しきい値</label>
          <input
            type="number"
            value={threshold}
            onChange={(e) => setThreshold(parseInt(e.target.value) || 0)}
            className="w-20 px-3 py-2 border rounded"
          />
        </div>
        <button
          onClick={() => setShowLowStock((prev) => !prev)}
          className={`px-4 py-2 rounded-lg font-medium ${
            showLowStock
              ? 'bg-amber-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          {showLowStock ? '低在庫のみ表示中' : '低在庫のみ表示'}
        </button>
      </div>

      <div className="mb-8 bg-white rounded-lg shadow overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50 border-b">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">商品ID</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">商品名</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">物理在庫</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">仮引当</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">本引当</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">有効在庫</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {filteredInventories.map((item) => (
              <tr key={item.productId} className="hover:bg-gray-50">
                <td className="px-6 py-4 font-mono text-sm">{item.productId}</td>
                <td className="px-6 py-4 text-sm text-gray-900">{item.productName}</td>
                <td className="px-6 py-4 text-sm tabular-nums">{item.physicalStock}</td>
                <td className="px-6 py-4 text-sm tabular-nums text-amber-700">{item.tentativeReserved}</td>
                <td className="px-6 py-4 text-sm tabular-nums text-blue-700">{item.committedReserved}</td>
                <td className="px-6 py-4 text-sm">
                  <span className={`font-semibold tabular-nums ${
                    item.availableStock <= threshold
                      ? 'text-red-600'
                      : 'text-emerald-700'
                  }`}>
                    {item.availableStock}
                  </span>
                </td>
                <td className="px-6 py-4">
                  <button
                    onClick={() => openAdjustModal(item.productId)}
                    className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
                  >
                    調整
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="px-6 py-4 border-b">
          <h2 className="text-xl font-bold text-zinc-900">在庫調整履歴</h2>
        </div>
        <table className="w-full">
          <thead className="bg-gray-50 border-b">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">日時</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">商品</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">増減</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">調整前→調整後</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">理由</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">実施者</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {adjustments.map((adjustment) => (
              <tr key={adjustment.id} className="hover:bg-gray-50">
                <td className="px-6 py-4 text-sm text-gray-700">
                  {new Date(adjustment.adjustedAt).toLocaleString('ja-JP')}
                </td>
                <td className="px-6 py-4 text-sm text-gray-900">
                  {adjustment.productName || `#${adjustment.productId}`}
                </td>
                <td className="px-6 py-4 text-sm font-semibold">
                  <span className={adjustment.quantityDelta >= 0 ? 'text-emerald-700' : 'text-red-600'}>
                    {adjustment.quantityDelta >= 0 ? '+' : ''}{adjustment.quantityDelta}
                  </span>
                </td>
                <td className="px-6 py-4 text-sm tabular-nums text-gray-700">
                  {adjustment.quantityBefore} → {adjustment.quantityAfter}
                </td>
                <td className="px-6 py-4 text-sm text-gray-700">{adjustment.reason}</td>
                <td className="px-6 py-4 text-sm text-gray-700">{adjustment.adjustedBy}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showAdjustModal && selectedProduct && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-lg">
            <h2 className="text-2xl font-bold mb-4">在庫調整</h2>
            <div className="space-y-3 mb-6">
              <p className="text-sm text-gray-700">
                <span className="font-medium text-gray-900">商品:</span> {selectedProduct.productName}
              </p>
              <p className="text-sm text-gray-700">
                <span className="font-medium text-gray-900">現在在庫:</span>{' '}
                <span className="tabular-nums">{selectedProduct.physicalStock}</span>
              </p>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">増減量（正: 増加 / 負: 減少）</label>
                <input
                  type="number"
                  value={adjustForm.quantityDelta}
                  onChange={(e) =>
                    setAdjustForm((prev) => ({
                      ...prev,
                      quantityDelta: parseInt(e.target.value) || 0,
                    }))
                  }
                  className="w-full px-3 py-2 border rounded"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">調整理由</label>
                <input
                  type="text"
                  value={adjustForm.reason}
                  onChange={(e) =>
                    setAdjustForm((prev) => ({
                      ...prev,
                      reason: e.target.value,
                    }))
                  }
                  className="w-full px-3 py-2 border rounded"
                  placeholder="例: 棚卸調整"
                />
              </div>
              <div className="flex gap-2">
                <button
                  onClick={handleAdjust}
                  className="flex-1 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
                >
                  調整実行
                </button>
                <button
                  onClick={() => {
                    setShowAdjustModal(false)
                    setAdjustForm({ quantityDelta: 0, reason: '' })
                  }}
                  className="flex-1 px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
                >
                  キャンセル
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
