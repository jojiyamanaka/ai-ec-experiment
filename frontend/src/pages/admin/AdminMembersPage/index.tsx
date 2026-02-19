import { useEffect, useMemo, useState } from 'react'
import { get, put } from '@shared/api/client'
import type { User } from '@entities/customer'

interface MemberDetail extends User {
  orderSummary?: {
    totalOrders: number
    totalAmount: number
  }
}

export default function AdminMembersPage() {
  const [members, setMembers] = useState<User[]>([])
  const [selectedMember, setSelectedMember] = useState<MemberDetail | null>(null)
  const [showDetailModal, setShowDetailModal] = useState<boolean>(false)
  const [searchQuery, setSearchQuery] = useState<string>('')

  const fetchMembers = async () => {
    try {
      const response = await get<User[]>('/bo/admin/members')
      if (response.success && response.data) {
        setMembers(response.data)
      }
    } catch (error) {
      console.error('会員一覧取得エラー:', error)
    }
  }

  const fetchMemberDetail = async (id: number) => {
    try {
      const response = await get<MemberDetail>(`/bo/admin/members/${id}`)
      if (response.success && response.data) {
        setSelectedMember(response.data)
        setShowDetailModal(true)
      }
    } catch (error) {
      console.error('会員詳細取得エラー:', error)
    }
  }

  const updateStatus = async (id: number, isActive: boolean) => {
    try {
      const response = await put(`/bo/admin/members/${id}/status`, { isActive })
      if (response.success) {
        alert('会員状態を変更しました')
        await fetchMembers()
        if (selectedMember?.id === id) {
          await fetchMemberDetail(id)
        }
      }
    } catch (error) {
      console.error('会員状態変更エラー:', error)
      alert('会員状態の変更に失敗しました')
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchMembers()
  }, [])

  const filteredMembers = useMemo(() => {
    return members
      .filter((member) =>
        member.email.toLowerCase().includes(searchQuery.toLowerCase())
      )
  }, [members, searchQuery])

  return (
    <div className="p-8">
      {/* ヘッダー */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-zinc-900">会員管理</h1>
      </div>

      {/* 検索エリア */}
      <div className="mb-4">
        <input
          type="text"
          placeholder="メールアドレスで検索"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="px-4 py-2 border rounded-lg w-full max-w-md"
        />
      </div>

      {/* 会員一覧テーブル */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50 border-b">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">会員ID</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">メールアドレス</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">表示名</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">状態</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">登録日</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {filteredMembers.map((member) => (
              <tr key={member.id} className="hover:bg-gray-50">
                <td className="px-6 py-4">
                  <button
                    onClick={() => fetchMemberDetail(member.id)}
                    className="font-mono text-blue-600 hover:underline"
                  >
                    {member.id}
                  </button>
                </td>
                <td className="px-6 py-4 text-sm text-gray-900">{member.email}</td>
                <td className="px-6 py-4 text-sm text-gray-900">{member.displayName}</td>
                <td className="px-6 py-4">
                  <span className={`inline-flex px-2 py-1 text-xs font-medium rounded ${
                    member.isActive
                      ? 'bg-emerald-100 text-emerald-800'
                      : 'bg-gray-100 text-gray-800'
                  }`}>
                    {member.isActive ? '有効' : '無効'}
                  </span>
                </td>
                <td className="px-6 py-4 text-sm text-gray-700">
                  {new Date(member.createdAt).toLocaleDateString('ja-JP')}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* 会員詳細モーダル */}
      {showDetailModal && selectedMember && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-8 max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <h2 className="text-2xl font-bold mb-6">会員詳細</h2>

            {/* 基本情報 */}
            <div className="mb-6 space-y-2">
              <div className="flex">
                <span className="w-32 font-medium text-gray-700">会員ID:</span>
                <span className="font-mono">{selectedMember.id}</span>
              </div>
              <div className="flex">
                <span className="w-32 font-medium text-gray-700">メール:</span>
                <span>{selectedMember.email}</span>
              </div>
              <div className="flex">
                <span className="w-32 font-medium text-gray-700">表示名:</span>
                <span>{selectedMember.displayName}</span>
              </div>
              <div className="flex">
                <span className="w-32 font-medium text-gray-700">状態:</span>
                <span className={`inline-flex px-2 py-1 text-xs font-medium rounded ${
                  selectedMember.isActive
                    ? 'bg-emerald-100 text-emerald-800'
                    : 'bg-gray-100 text-gray-800'
                }`}>
                  {selectedMember.isActive ? '有効' : '無効'}
                </span>
              </div>
              <div className="flex">
                <span className="w-32 font-medium text-gray-700">登録日:</span>
                <span>{new Date(selectedMember.createdAt).toLocaleString('ja-JP')}</span>
              </div>
              {selectedMember.updatedAt && (
                <div className="flex">
                  <span className="w-32 font-medium text-gray-700">更新日:</span>
                  <span>{new Date(selectedMember.updatedAt).toLocaleString('ja-JP')}</span>
                </div>
              )}
            </div>

            {/* 注文サマリ */}
            {selectedMember.orderSummary && (
              <div className="mb-6 p-4 bg-gray-50 rounded-lg">
                <h3 className="font-bold mb-2">注文サマリ</h3>
                <div className="space-y-1">
                  <div className="flex">
                    <span className="w-32 text-gray-700">総注文数:</span>
                    <span className="tabular-nums">{selectedMember.orderSummary.totalOrders}件</span>
                  </div>
                  <div className="flex">
                    <span className="w-32 text-gray-700">総購入額:</span>
                    <span className="tabular-nums">{selectedMember.orderSummary.totalAmount.toLocaleString()}円</span>
                  </div>
                </div>
              </div>
            )}

            {/* アクションボタン */}
            <div className="flex gap-2 mb-4">
              <button
                onClick={() => updateStatus(selectedMember.id, !selectedMember.isActive)}
                className={`flex-1 px-4 py-2 rounded font-medium ${
                  selectedMember.isActive
                    ? 'bg-gray-600 text-white hover:bg-gray-700'
                    : 'bg-emerald-600 text-white hover:bg-emerald-700'
                }`}
              >
                {selectedMember.isActive ? '無効化' : '有効化'}
              </button>
            </div>

            <button
              onClick={() => setShowDetailModal(false)}
              className="w-full px-4 py-2 bg-gray-200 text-gray-700 rounded font-medium hover:bg-gray-300"
            >
              閉じる
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
