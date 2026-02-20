import { useEffect, useMemo, useState } from 'react'
import {
  createAdminMember,
  getAdminMemberById,
  getAdminMembers,
  updateAdminMember,
  updateAdminMemberStatus,
} from '@entities/customer'
import type { CreateMemberRequest, MemberRank, UpdateMemberRequest, UpsertAddressRequest, User } from '@entities/customer'
import {
  AdminDrawerBase,
  AdminModalBase,
  AdminPageContainer,
  AdminPageHeader,
  AdminSearchBar,
  AdminTableShell,
} from '@shared/ui/admin'

interface MemberDetail extends User {
  orderSummary?: {
    totalOrders: number
    totalAmount: number
  }
}

const rankOptions: MemberRank[] = ['STANDARD', 'SILVER', 'GOLD', 'PLATINUM']

const initialCreateForm: CreateMemberRequest = {
  email: '',
  displayName: '',
  password: '',
  fullName: '',
  phoneNumber: '',
  birthDate: '',
  newsletterOptIn: false,
  memberRank: 'STANDARD',
  loyaltyPoints: 0,
  isActive: true,
  deactivationReason: '',
  addresses: [],
}

export default function AdminMembersPage() {
  const [members, setMembers] = useState<User[]>([])
  const [selectedMember, setSelectedMember] = useState<MemberDetail | null>(null)
  const [showDetailModal, setShowDetailModal] = useState<boolean>(false)
  const [searchQuery, setSearchQuery] = useState<string>('')

  const [createForm, setCreateForm] = useState<CreateMemberRequest>(initialCreateForm)
  const [creating, setCreating] = useState(false)
  const [showCreateDrawer, setShowCreateDrawer] = useState(false)

  const [editing, setEditing] = useState(false)
  const [editForm, setEditForm] = useState<UpdateMemberRequest>({})
  const [editAddresses, setEditAddresses] = useState<UpsertAddressRequest[]>([])

  const fetchMembers = async () => {
    try {
      const response = await getAdminMembers()
      if (response.success && response.data) {
        setMembers(response.data)
      }
    } catch (error) {
      console.error('会員一覧取得エラー:', error)
    }
  }

  const fetchMemberDetail = async (id: number) => {
    try {
      const response = await getAdminMemberById(id)
      if (response.success && response.data) {
        setSelectedMember(response.data)
        setEditForm({
          displayName: response.data.displayName,
          fullName: response.data.fullName,
          phoneNumber: response.data.phoneNumber,
          birthDate: response.data.birthDate,
          newsletterOptIn: response.data.newsletterOptIn,
          memberRank: response.data.memberRank,
          loyaltyPoints: response.data.loyaltyPoints,
          deactivationReason: response.data.deactivationReason,
          isActive: response.data.isActive,
        })
        setEditAddresses((response.data.addresses ?? []).map((address) => ({
          id: address.id,
          label: address.label,
          recipientName: address.recipientName,
          recipientPhoneNumber: address.recipientPhoneNumber,
          postalCode: address.postalCode,
          prefecture: address.prefecture,
          city: address.city,
          addressLine1: address.addressLine1,
          addressLine2: address.addressLine2,
          isDefault: address.isDefault,
          addressOrder: address.addressOrder,
        })))
        setShowDetailModal(true)
      }
    } catch (error) {
      console.error('会員詳細取得エラー:', error)
    }
  }

  const updateStatus = async (id: number, isActive: boolean) => {
    try {
      const response = await updateAdminMemberStatus(id, isActive)
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

  const handleCreateMember = async () => {
    setCreating(true)
    try {
      const normalizedAddresses = (createForm.addresses ?? []).map((address, index) => ({
        ...address,
        addressOrder: index,
      }))
      const payload: CreateMemberRequest = {
        ...createForm,
        loyaltyPoints: Number(createForm.loyaltyPoints ?? 0),
        addresses: normalizedAddresses,
      }
      const response = await createAdminMember(payload)
      if (response.success) {
        alert('会員を作成しました')
        setCreateForm(initialCreateForm)
        setShowCreateDrawer(false)
        await fetchMembers()
      } else {
        alert(response.error?.message ?? '会員作成に失敗しました')
      }
    } catch (error) {
      console.error('会員作成エラー:', error)
      alert('会員作成に失敗しました')
    } finally {
      setCreating(false)
    }
  }

  const handleSaveMember = async () => {
    if (!selectedMember) {
      return
    }
    setEditing(true)
    try {
      const payload: UpdateMemberRequest = {
        ...editForm,
        loyaltyPoints: Number(editForm.loyaltyPoints ?? 0),
        addresses: editAddresses,
      }
      const response = await updateAdminMember(selectedMember.id, payload)
      if (response.success && response.data) {
        alert('会員情報を更新しました')
        await fetchMembers()
        await fetchMemberDetail(selectedMember.id)
      } else {
        alert(response.error?.message ?? '会員更新に失敗しました')
      }
    } catch (error) {
      console.error('会員更新エラー:', error)
      alert('会員更新に失敗しました')
    } finally {
      setEditing(false)
    }
  }

  const addAddressRow = () => {
    setEditAddresses((prev) => [
      ...prev,
      {
        label: '',
        recipientName: '',
        recipientPhoneNumber: '',
        postalCode: '',
        prefecture: '',
        city: '',
        addressLine1: '',
        addressLine2: '',
        isDefault: false,
        addressOrder: prev.length,
      },
    ])
  }

  const updateAddressField = (index: number, patch: Partial<UpsertAddressRequest>) => {
    setEditAddresses((prev) => prev.map((address, currentIndex) => {
      if (currentIndex !== index) {
        return address
      }
      if (patch.isDefault) {
        return { ...address, ...patch }
      }
      return { ...address, ...patch }
    }))

    if (patch.isDefault) {
      setEditAddresses((prev) => prev.map((address, currentIndex) => {
        if (currentIndex === index) {
          return { ...address, ...patch, isDefault: true }
        }
        return { ...address, isDefault: false }
      }))
    }
  }

  const removeAddressRow = (index: number) => {
    setEditAddresses((prev) => {
      const current = prev[index]
      if (current?.id) {
        return prev.map((address, currentIndex) => (
          currentIndex === index ? { ...address, deleted: true } : address
        ))
      }
      return prev.filter((_, currentIndex) => currentIndex !== index)
    })
  }

  const addCreateAddressRow = () => {
    setCreateForm((prev) => ({
      ...prev,
      addresses: [
        ...(prev.addresses ?? []),
        {
          label: '',
          recipientName: '',
          recipientPhoneNumber: '',
          postalCode: '',
          prefecture: '',
          city: '',
          addressLine1: '',
          addressLine2: '',
          isDefault: false,
          addressOrder: (prev.addresses ?? []).length,
        },
      ],
    }))
  }

  const updateCreateAddressField = (index: number, patch: Partial<UpsertAddressRequest>) => {
    setCreateForm((prev) => {
      const nextAddresses = (prev.addresses ?? []).map((address, currentIndex) => {
        if (currentIndex !== index) {
          return address
        }
        return { ...address, ...patch }
      })

      if (patch.isDefault) {
        return {
          ...prev,
          addresses: nextAddresses.map((address, currentIndex) => {
            if (currentIndex === index) {
              return { ...address, isDefault: true }
            }
            return { ...address, isDefault: false }
          }),
        }
      }

      return { ...prev, addresses: nextAddresses }
    })
  }

  const removeCreateAddressRow = (index: number) => {
    setCreateForm((prev) => ({
      ...prev,
      addresses: (prev.addresses ?? []).filter((_, currentIndex) => currentIndex !== index),
    }))
  }

  useEffect(() => {
    fetchMembers()
  }, [])

  const filteredMembers = useMemo(() => {
    return members
      .filter((member) =>
        member.email.toLowerCase().includes(searchQuery.toLowerCase())
      )
  }, [members, searchQuery])

  return (
    <AdminPageContainer>
      <AdminPageHeader
        title="会員管理"
        actions={(
          <button
            onClick={() => setShowCreateDrawer(true)}
            className="rounded-lg bg-zinc-900 px-4 py-2 text-white hover:bg-zinc-700"
          >
            新規登録
          </button>
        )}
      />

      <AdminSearchBar
        value={searchQuery}
        onChange={setSearchQuery}
        onSearch={setSearchQuery}
        placeholder="メールアドレスで検索"
        mode="instant"
      />

      <AdminTableShell>
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
      </AdminTableShell>

      <AdminDrawerBase
        isOpen={showCreateDrawer}
        onClose={() => setShowCreateDrawer(false)}
        title="会員新規登録"
        widthClass="max-w-2xl"
        footer={(
          <div className="flex gap-2">
            <button
              onClick={() => setShowCreateDrawer(false)}
              className="flex-1 rounded bg-gray-200 px-4 py-2 font-medium text-gray-700 hover:bg-gray-300"
            >
              キャンセル
            </button>
            <button
              onClick={handleCreateMember}
              disabled={creating || !createForm.email || !createForm.displayName || !createForm.password}
              className="flex-1 rounded bg-zinc-900 px-4 py-2 text-white hover:bg-zinc-700 disabled:opacity-60"
            >
              {creating ? '作成中...' : '会員を作成'}
            </button>
          </div>
        )}
      >
        <div className="space-y-8">
          <section className="space-y-4">
            <div>
              <h3 className="text-base font-bold text-zinc-900">基本情報</h3>
              <p className="mt-1 text-xs text-gray-500">会員のログインとプロフィールに使用する情報です。</p>
            </div>
            <div className="grid gap-4 md:grid-cols-2">
              <label className="text-sm text-zinc-700">
                <span className="flex items-center gap-2">
                  メールアドレス
                  <span className="rounded bg-rose-100 px-2 py-0.5 text-xs font-medium text-rose-700">必須</span>
                </span>
                <input
                  type="email"
                  value={createForm.email}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, email: e.target.value }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                />
              </label>
              <label className="text-sm text-zinc-700">
                <span className="flex items-center gap-2">
                  表示名
                  <span className="rounded bg-rose-100 px-2 py-0.5 text-xs font-medium text-rose-700">必須</span>
                </span>
                <input
                  type="text"
                  value={createForm.displayName}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, displayName: e.target.value }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                />
              </label>
              <label className="text-sm text-zinc-700">
                <span className="flex items-center gap-2">
                  パスワード
                  <span className="rounded bg-rose-100 px-2 py-0.5 text-xs font-medium text-rose-700">必須</span>
                </span>
                <input
                  type="password"
                  value={createForm.password}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, password: e.target.value }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                />
                <p className="mt-1 text-xs text-gray-500">ログイン時に利用します。</p>
              </label>
              <label className="text-sm text-zinc-700">
                氏名
                <input
                  type="text"
                  value={createForm.fullName ?? ''}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, fullName: e.target.value }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                />
              </label>
              <label className="text-sm text-zinc-700">
                電話番号
                <input
                  type="text"
                  value={createForm.phoneNumber ?? ''}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, phoneNumber: e.target.value }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                />
              </label>
              <label className="text-sm text-zinc-700">
                生年月日
                <input
                  type="date"
                  value={createForm.birthDate ?? ''}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, birthDate: e.target.value }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                />
              </label>
            </div>
            <label className="inline-flex items-center gap-2 text-sm text-zinc-700">
              <input
                type="checkbox"
                checked={createForm.newsletterOptIn ?? false}
                onChange={(e) => setCreateForm((prev) => ({ ...prev, newsletterOptIn: e.target.checked }))}
              />
              メルマガ購読
            </label>
          </section>

          <section className="space-y-4">
            <div>
              <h3 className="text-base font-bold text-zinc-900">運用設定</h3>
              <p className="mt-1 text-xs text-gray-500">会員の運用区分と初期状態を設定します。</p>
            </div>
            <div className="grid gap-4 md:grid-cols-2">
              <label className="text-sm text-zinc-700">
                会員ランク
                <select
                  value={createForm.memberRank}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, memberRank: e.target.value as MemberRank }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                >
                  {rankOptions.map((rank) => (
                    <option key={rank} value={rank}>{rank}</option>
                  ))}
                </select>
              </label>
              <label className="text-sm text-zinc-700">
                ポイント
                <input
                  type="number"
                  value={createForm.loyaltyPoints}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, loyaltyPoints: Number(e.target.value || 0) }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                />
              </label>
              <label className="text-sm text-zinc-700">
                状態
                <select
                  value={createForm.isActive ? 'active' : 'inactive'}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, isActive: e.target.value === 'active' }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                >
                  <option value="active">有効</option>
                  <option value="inactive">無効</option>
                </select>
              </label>
            </div>
            <label className="block text-sm text-zinc-700">
              停止理由
              <textarea
                value={createForm.deactivationReason ?? ''}
                onChange={(e) => setCreateForm((prev) => ({ ...prev, deactivationReason: e.target.value }))}
                className="mt-1 w-full rounded border px-3 py-2"
              />
            </label>
          </section>

          <section className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-zinc-900">住所情報</h3>
                <p className="mt-1 text-xs text-gray-500">住所が必要な場合のみ追加してください。</p>
              </div>
              <button
                onClick={addCreateAddressRow}
                className="rounded bg-zinc-900 px-3 py-1 text-sm text-white hover:bg-zinc-700"
              >
                住所追加
              </button>
            </div>
            {(createForm.addresses ?? []).length === 0 ? (
              <p className="rounded border border-dashed px-3 py-4 text-sm text-gray-500">
                住所は未設定です。
              </p>
            ) : (
              <div className="space-y-3">
                {(createForm.addresses ?? []).map((address, index) => (
                  <div key={`create-address-${index}`} className="rounded border bg-white p-3">
                    <div className="grid gap-3 md:grid-cols-2">
                      <label className="text-sm text-zinc-700">
                        ラベル
                        <input
                          value={address.label ?? ''}
                          onChange={(e) => updateCreateAddressField(index, { label: e.target.value })}
                          className="mt-1 w-full rounded border px-2 py-1"
                        />
                      </label>
                      <label className="text-sm text-zinc-700">
                        受取人名
                        <input
                          value={address.recipientName ?? ''}
                          onChange={(e) => updateCreateAddressField(index, { recipientName: e.target.value })}
                          className="mt-1 w-full rounded border px-2 py-1"
                        />
                      </label>
                      <label className="text-sm text-zinc-700">
                        受取人電話番号
                        <input
                          value={address.recipientPhoneNumber ?? ''}
                          onChange={(e) => updateCreateAddressField(index, { recipientPhoneNumber: e.target.value })}
                          className="mt-1 w-full rounded border px-2 py-1"
                        />
                      </label>
                      <label className="text-sm text-zinc-700">
                        郵便番号
                        <input
                          value={address.postalCode ?? ''}
                          onChange={(e) => updateCreateAddressField(index, { postalCode: e.target.value })}
                          className="mt-1 w-full rounded border px-2 py-1"
                        />
                      </label>
                      <label className="text-sm text-zinc-700">
                        都道府県
                        <input
                          value={address.prefecture ?? ''}
                          onChange={(e) => updateCreateAddressField(index, { prefecture: e.target.value })}
                          className="mt-1 w-full rounded border px-2 py-1"
                        />
                      </label>
                      <label className="text-sm text-zinc-700">
                        市区町村
                        <input
                          value={address.city ?? ''}
                          onChange={(e) => updateCreateAddressField(index, { city: e.target.value })}
                          className="mt-1 w-full rounded border px-2 py-1"
                        />
                      </label>
                      <label className="text-sm text-zinc-700 md:col-span-2">
                        住所1
                        <input
                          value={address.addressLine1 ?? ''}
                          onChange={(e) => updateCreateAddressField(index, { addressLine1: e.target.value })}
                          className="mt-1 w-full rounded border px-2 py-1"
                        />
                      </label>
                      <label className="text-sm text-zinc-700 md:col-span-2">
                        住所2
                        <input
                          value={address.addressLine2 ?? ''}
                          onChange={(e) => updateCreateAddressField(index, { addressLine2: e.target.value })}
                          className="mt-1 w-full rounded border px-2 py-1"
                        />
                      </label>
                    </div>
                    <div className="mt-3 flex items-center justify-between">
                      <label className="inline-flex items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={address.isDefault ?? false}
                          onChange={(e) => updateCreateAddressField(index, { isDefault: e.target.checked })}
                        />
                        デフォルト住所
                      </label>
                      <button
                        onClick={() => removeCreateAddressRow(index)}
                        className="rounded bg-rose-100 px-2 py-1 text-sm text-rose-700 hover:bg-rose-200"
                      >
                        削除
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>
      </AdminDrawerBase>

      <AdminModalBase
        isOpen={showDetailModal && Boolean(selectedMember)}
        onClose={() => setShowDetailModal(false)}
        title="会員詳細"
        maxWidthClass="max-w-4xl"
        bodyClassName="p-8"
      >
        {selectedMember ? (
          <>
            <div className="grid gap-3 md:grid-cols-2">
              <label className="text-sm text-zinc-700">
                表示名
                <input
                  value={editForm.displayName ?? ''}
                  onChange={(e) => setEditForm((prev) => ({ ...prev, displayName: e.target.value }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                />
              </label>
              <label className="text-sm text-zinc-700">
                氏名
                <input
                  value={editForm.fullName ?? ''}
                  onChange={(e) => setEditForm((prev) => ({ ...prev, fullName: e.target.value }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                />
              </label>
              <label className="text-sm text-zinc-700">
                電話番号
                <input
                  value={editForm.phoneNumber ?? ''}
                  onChange={(e) => setEditForm((prev) => ({ ...prev, phoneNumber: e.target.value }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                />
              </label>
              <label className="text-sm text-zinc-700">
                生年月日
                <input
                  type="date"
                  value={editForm.birthDate ?? ''}
                  onChange={(e) => setEditForm((prev) => ({ ...prev, birthDate: e.target.value }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                />
              </label>
              <label className="text-sm text-zinc-700">
                会員ランク
                <select
                  value={editForm.memberRank ?? 'STANDARD'}
                  onChange={(e) => setEditForm((prev) => ({ ...prev, memberRank: e.target.value as MemberRank }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                >
                  {rankOptions.map((rank) => (
                    <option key={rank} value={rank}>{rank}</option>
                  ))}
                </select>
              </label>
              <label className="text-sm text-zinc-700">
                ポイント
                <input
                  type="number"
                  value={editForm.loyaltyPoints ?? 0}
                  onChange={(e) => setEditForm((prev) => ({ ...prev, loyaltyPoints: Number(e.target.value || 0) }))}
                  className="mt-1 w-full rounded border px-3 py-2"
                />
              </label>
            </div>

            <label className="mt-4 inline-flex items-center gap-2 text-sm text-zinc-700">
              <input
                type="checkbox"
                checked={editForm.newsletterOptIn ?? false}
                onChange={(e) => setEditForm((prev) => ({ ...prev, newsletterOptIn: e.target.checked }))}
              />
              メルマガ購読
            </label>

            <label className="mt-3 block text-sm text-zinc-700">
              停止理由
              <textarea
                value={editForm.deactivationReason ?? ''}
                onChange={(e) => setEditForm((prev) => ({ ...prev, deactivationReason: e.target.value }))}
                className="mt-1 w-full rounded border px-3 py-2"
              />
            </label>

            <div className="mt-6 rounded-lg bg-gray-50 p-4">
              <div className="mb-3 flex items-center justify-between">
                <h3 className="font-bold">住所管理</h3>
                <button
                  onClick={addAddressRow}
                  className="rounded bg-zinc-900 px-3 py-1 text-sm text-white hover:bg-zinc-700"
                >
                  住所追加
                </button>
              </div>
              <div className="space-y-3">
                {editAddresses.filter((address) => !address.deleted).map((address, index) => (
                  <div key={`${address.id ?? 'new'}-${index}`} className="rounded border bg-white p-3">
                    <div className="grid gap-2 md:grid-cols-2">
                      <input
                        placeholder="ラベル"
                        value={address.label ?? ''}
                        onChange={(e) => updateAddressField(index, { label: e.target.value })}
                        className="rounded border px-2 py-1"
                      />
                      <input
                        placeholder="受取人名"
                        value={address.recipientName ?? ''}
                        onChange={(e) => updateAddressField(index, { recipientName: e.target.value })}
                        className="rounded border px-2 py-1"
                      />
                      <input
                        placeholder="郵便番号"
                        value={address.postalCode ?? ''}
                        onChange={(e) => updateAddressField(index, { postalCode: e.target.value })}
                        className="rounded border px-2 py-1"
                      />
                      <input
                        placeholder="都道府県"
                        value={address.prefecture ?? ''}
                        onChange={(e) => updateAddressField(index, { prefecture: e.target.value })}
                        className="rounded border px-2 py-1"
                      />
                      <input
                        placeholder="市区町村"
                        value={address.city ?? ''}
                        onChange={(e) => updateAddressField(index, { city: e.target.value })}
                        className="rounded border px-2 py-1"
                      />
                      <input
                        placeholder="住所1"
                        value={address.addressLine1 ?? ''}
                        onChange={(e) => updateAddressField(index, { addressLine1: e.target.value })}
                        className="rounded border px-2 py-1"
                      />
                    </div>
                    <div className="mt-2 flex items-center justify-between">
                      <label className="inline-flex items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={address.isDefault ?? false}
                          onChange={(e) => updateAddressField(index, { isDefault: e.target.checked })}
                        />
                        デフォルト住所
                      </label>
                      <button
                        onClick={() => removeAddressRow(index)}
                        className="rounded bg-rose-100 px-2 py-1 text-sm text-rose-700 hover:bg-rose-200"
                      >
                        削除
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {selectedMember.orderSummary && (
              <div className="mt-4 p-4 bg-gray-50 rounded-lg">
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

            <div className="mt-6 flex gap-2">
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
              <button
                onClick={handleSaveMember}
                disabled={editing}
                className="flex-1 rounded bg-zinc-900 px-4 py-2 font-medium text-white hover:bg-zinc-700 disabled:opacity-60"
              >
                {editing ? '保存中...' : '会員情報を保存'}
              </button>
            </div>

            <button
              onClick={() => setShowDetailModal(false)}
              className="mt-4 w-full px-4 py-2 bg-gray-200 text-gray-700 rounded font-medium hover:bg-gray-300"
            >
              閉じる
            </button>
          </>
        ) : null}
      </AdminModalBase>
    </AdminPageContainer>
  )
}
