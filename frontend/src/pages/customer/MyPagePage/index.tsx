import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '@features/auth'
import {
  addMyAddress,
  deleteMyAddress,
  updateMyAddress,
  updateMyProfile,
} from '@entities/customer'
import type { UpsertAddressRequest, UserAddress } from '@entities/customer'

const initialAddressForm: UpsertAddressRequest = {
  label: '',
  recipientName: '',
  recipientPhoneNumber: '',
  postalCode: '',
  prefecture: '',
  city: '',
  addressLine1: '',
  addressLine2: '',
  isDefault: false,
  addressOrder: 0,
}

export default function MyPagePage() {
  const { user, isAuthenticated, refreshUser } = useAuth()
  const [savingProfile, setSavingProfile] = useState(false)
  const [savingAddress, setSavingAddress] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const [displayName, setDisplayName] = useState('')
  const [fullName, setFullName] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [birthDate, setBirthDate] = useState('')
  const [newsletterOptIn, setNewsletterOptIn] = useState(false)

  const [addressForm, setAddressForm] = useState<UpsertAddressRequest>(initialAddressForm)

  useEffect(() => {
    setDisplayName(user?.displayName ?? '')
    setFullName(user?.fullName ?? '')
    setPhoneNumber(user?.phoneNumber ?? '')
    setBirthDate(user?.birthDate ?? '')
    setNewsletterOptIn(user?.newsletterOptIn ?? false)
  }, [user])

  const addresses = useMemo(() => {
    return [...(user?.addresses ?? [])].sort((a, b) => (a.addressOrder ?? 0) - (b.addressOrder ?? 0))
  }, [user?.addresses])

  if (!isAuthenticated) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16">
        <h1 className="text-3xl font-bold text-zinc-900">マイページ</h1>
        <p className="mt-4 text-zinc-700">マイページを利用するにはログインが必要です。</p>
        <Link
          to="/auth/login"
          className="mt-6 inline-flex rounded bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700"
        >
          ログインへ進む
        </Link>
      </div>
    )
  }

  const handleSaveProfile = async () => {
    setSavingProfile(true)
    setMessage(null)
    setError(null)
    try {
      const response = await updateMyProfile({
        displayName,
        fullName,
        phoneNumber,
        birthDate: birthDate || undefined,
        newsletterOptIn,
      })
      if (response.success) {
        await refreshUser()
        setMessage('プロフィールを更新しました')
      } else {
        setError(response.error?.message ?? 'プロフィール更新に失敗しました')
      }
    } finally {
      setSavingProfile(false)
    }
  }

  const handleAddAddress = async () => {
    setSavingAddress(true)
    setMessage(null)
    setError(null)
    try {
      const response = await addMyAddress({
        ...addressForm,
        recipientName: addressForm.recipientName ?? '',
        postalCode: addressForm.postalCode ?? '',
        prefecture: addressForm.prefecture ?? '',
        city: addressForm.city ?? '',
        addressLine1: addressForm.addressLine1 ?? '',
      })
      if (response.success) {
        await refreshUser()
        setAddressForm(initialAddressForm)
        setMessage('住所を追加しました')
      } else {
        setError(response.error?.message ?? '住所追加に失敗しました')
      }
    } finally {
      setSavingAddress(false)
    }
  }

  const handleDeleteAddress = async (addressId: number) => {
    setMessage(null)
    setError(null)
    const response = await deleteMyAddress(addressId)
    if (response.success) {
      await refreshUser()
      setMessage('住所を削除しました')
      return
    }
    setError(response.error?.message ?? '住所削除に失敗しました')
  }

  const handleSetDefault = async (address: UserAddress) => {
    setMessage(null)
    setError(null)
    const response = await updateMyAddress(address.id, {
      label: address.label,
      recipientName: address.recipientName,
      recipientPhoneNumber: address.recipientPhoneNumber,
      postalCode: address.postalCode,
      prefecture: address.prefecture,
      city: address.city,
      addressLine1: address.addressLine1,
      addressLine2: address.addressLine2,
      isDefault: true,
      addressOrder: address.addressOrder,
    })
    if (response.success) {
      await refreshUser()
      setMessage('デフォルト住所を更新しました')
      return
    }
    setError(response.error?.message ?? 'デフォルト住所の更新に失敗しました')
  }

  return (
    <div className="mx-auto max-w-5xl px-6 py-10">
      <h1 className="text-3xl font-bold text-zinc-900">マイページ</h1>

      {message && <p className="mt-4 rounded bg-emerald-100 px-4 py-2 text-emerald-700">{message}</p>}
      {error && <p className="mt-4 rounded bg-rose-100 px-4 py-2 text-rose-700">{error}</p>}

      <section className="mt-8 rounded-lg border border-zinc-200 bg-white p-6">
        <h2 className="text-xl font-semibold text-zinc-900">プロフィール</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <label className="block">
            <span className="mb-1 block text-sm text-zinc-700">表示名</span>
            <input
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="w-full rounded border border-zinc-300 px-3 py-2"
            />
          </label>
          <label className="block">
            <span className="mb-1 block text-sm text-zinc-700">氏名</span>
            <input
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="w-full rounded border border-zinc-300 px-3 py-2"
            />
          </label>
          <label className="block">
            <span className="mb-1 block text-sm text-zinc-700">電話番号</span>
            <input
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              className="w-full rounded border border-zinc-300 px-3 py-2"
            />
          </label>
          <label className="block">
            <span className="mb-1 block text-sm text-zinc-700">生年月日</span>
            <input
              type="date"
              value={birthDate}
              onChange={(e) => setBirthDate(e.target.value)}
              className="w-full rounded border border-zinc-300 px-3 py-2"
            />
          </label>
        </div>
        <label className="mt-4 inline-flex items-center gap-2 text-sm text-zinc-700">
          <input
            type="checkbox"
            checked={newsletterOptIn}
            onChange={(e) => setNewsletterOptIn(e.target.checked)}
          />
          メールマガジンを受け取る
        </label>
        <div className="mt-6">
          <button
            onClick={handleSaveProfile}
            disabled={savingProfile}
            className="rounded bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-60"
          >
            {savingProfile ? '更新中...' : 'プロフィールを保存'}
          </button>
        </div>
      </section>

      <section className="mt-8 rounded-lg border border-zinc-200 bg-white p-6">
        <h2 className="text-xl font-semibold text-zinc-900">住所管理</h2>

        <div className="mt-4 space-y-3">
          {addresses.map((address) => (
            <div key={address.id} className="rounded border border-zinc-200 p-4">
              <div className="flex items-center justify-between">
                <p className="font-medium text-zinc-900">{address.label || '住所'}</p>
                {address.isDefault && (
                  <span className="rounded bg-zinc-900 px-2 py-1 text-xs text-white">Default</span>
                )}
              </div>
              <p className="mt-2 text-sm text-zinc-700">
                {address.postalCode} {address.prefecture} {address.city} {address.addressLine1} {address.addressLine2}
              </p>
              <p className="text-sm text-zinc-700">受取人: {address.recipientName}</p>
              <div className="mt-3 flex gap-2">
                {!address.isDefault && (
                  <button
                    onClick={() => handleSetDefault(address)}
                    className="rounded bg-zinc-100 px-3 py-1 text-sm text-zinc-700 hover:bg-zinc-200"
                  >
                    デフォルトに設定
                  </button>
                )}
                <button
                  onClick={() => handleDeleteAddress(address.id)}
                  className="rounded bg-rose-100 px-3 py-1 text-sm text-rose-700 hover:bg-rose-200"
                >
                  削除
                </button>
              </div>
            </div>
          ))}
          {addresses.length === 0 && <p className="text-sm text-zinc-600">登録済み住所はありません。</p>}
        </div>

        <div className="mt-6 rounded border border-dashed border-zinc-300 p-4">
          <h3 className="font-medium text-zinc-900">住所を追加</h3>
          <div className="mt-3 grid gap-3 md:grid-cols-2">
            <input
              placeholder="ラベル"
              value={addressForm.label ?? ''}
              onChange={(e) => setAddressForm((prev) => ({ ...prev, label: e.target.value }))}
              className="rounded border border-zinc-300 px-3 py-2"
            />
            <input
              placeholder="受取人名"
              value={addressForm.recipientName ?? ''}
              onChange={(e) => setAddressForm((prev) => ({ ...prev, recipientName: e.target.value }))}
              className="rounded border border-zinc-300 px-3 py-2"
            />
            <input
              placeholder="受取人電話番号"
              value={addressForm.recipientPhoneNumber ?? ''}
              onChange={(e) => setAddressForm((prev) => ({ ...prev, recipientPhoneNumber: e.target.value }))}
              className="rounded border border-zinc-300 px-3 py-2"
            />
            <input
              placeholder="郵便番号"
              value={addressForm.postalCode ?? ''}
              onChange={(e) => setAddressForm((prev) => ({ ...prev, postalCode: e.target.value }))}
              className="rounded border border-zinc-300 px-3 py-2"
            />
            <input
              placeholder="都道府県"
              value={addressForm.prefecture ?? ''}
              onChange={(e) => setAddressForm((prev) => ({ ...prev, prefecture: e.target.value }))}
              className="rounded border border-zinc-300 px-3 py-2"
            />
            <input
              placeholder="市区町村"
              value={addressForm.city ?? ''}
              onChange={(e) => setAddressForm((prev) => ({ ...prev, city: e.target.value }))}
              className="rounded border border-zinc-300 px-3 py-2"
            />
            <input
              placeholder="住所1"
              value={addressForm.addressLine1 ?? ''}
              onChange={(e) => setAddressForm((prev) => ({ ...prev, addressLine1: e.target.value }))}
              className="rounded border border-zinc-300 px-3 py-2"
            />
            <input
              placeholder="住所2"
              value={addressForm.addressLine2 ?? ''}
              onChange={(e) => setAddressForm((prev) => ({ ...prev, addressLine2: e.target.value }))}
              className="rounded border border-zinc-300 px-3 py-2"
            />
          </div>
          <label className="mt-3 inline-flex items-center gap-2 text-sm text-zinc-700">
            <input
              type="checkbox"
              checked={addressForm.isDefault ?? false}
              onChange={(e) => setAddressForm((prev) => ({ ...prev, isDefault: e.target.checked }))}
            />
            デフォルト住所に設定
          </label>
          <div className="mt-4">
            <button
              onClick={handleAddAddress}
              disabled={savingAddress}
              className="rounded bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-60"
            >
              {savingAddress ? '追加中...' : '住所を追加'}
            </button>
          </div>
        </div>
      </section>
    </div>
  )
}
