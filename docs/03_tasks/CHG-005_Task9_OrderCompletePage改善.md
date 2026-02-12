# CHG-005 Task9: OrderCompletePage.tsx 改善（注文完了ページ）

## 検証コマンド
```bash
cd frontend
npm run dev
# 注文を完了してから http://localhost:5173/order/complete を開き、以下を確認:
# - 見出しがセリフ体で表示されている
# - カードの影が削除されている
# - ボタンが黒背景になっている
# - 青色・緑色が廃止されている
```

## 目的
注文完了ページをエディトリアルデザインに改善する。

## 変更対象ファイル
1. `frontend/src/pages/OrderCompletePage.tsx`

---

## 変更内容

### 1. エラー画面の改修

**ファイル:** `frontend/src/pages/OrderCompletePage.tsx`

**変更前（21-40行目）:**
```tsx
if (!state) {
  return (
    <div className="mx-auto max-w-7xl px-4 py-12">
      <div className="text-center">
        <h2 className="text-2xl font-bold text-gray-900">
          注文情報が見つかりません
        </h2>
        <p className="mt-2 text-gray-600">
          正しい手順で注文を完了してください
        </p>
        <Link
          to="/"
          className="mt-6 inline-block rounded-lg bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700"
        >
          TOPに戻る
        </Link>
      </div>
    </div>
  )
}
```

**変更後:**
```tsx
if (!state) {
  return (
    <div className="mx-auto max-w-7xl px-6 py-24">
      <div className="text-center">
        <h2 className="font-serif text-2xl text-zinc-900">
          注文情報が見つかりません
        </h2>
        <p className="mt-2 text-zinc-600">
          正しい手順で注文を完了してください
        </p>
        <Link
          to="/"
          className="mt-6 inline-block bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors"
        >
          TOPに戻る
        </Link>
      </div>
    </div>
  )
}
```

**変更点:**
- パディング: `px-4 py-12` → `px-6 py-24`
- 見出し: `text-2xl font-bold text-gray-900` → `font-serif text-2xl text-zinc-900`
- 本文色: `text-gray-600` → `text-zinc-600`
- ボタン: `rounded-lg bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700` → `bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors`

---

### 2. ページ全体とアイコンの改修

**ファイル:** `frontend/src/pages/OrderCompletePage.tsx`

**変更前（68-93行目）:**
```tsx
return (
  <div className="mx-auto max-w-4xl px-4 py-12">
    {/* 完了メッセージ */}
    <div className="mb-8 text-center">
      <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          strokeWidth={2}
          stroke="currentColor"
          className="h-8 w-8 text-green-600"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M4.5 12.75l6 6 9-13.5"
          />
        </svg>
      </div>
      <h1 className="text-3xl font-bold text-gray-900">
        ご注文ありがとうございます
      </h1>
      <p className="mt-2 text-gray-600">
        ご注文を承りました。商品の発送までしばらくお待ちください。
      </p>
    </div>
```

**変更後:**
```tsx
return (
  <div className="mx-auto max-w-4xl px-6 py-24">
    {/* 完了メッセージ */}
    <div className="mb-12 text-center">
      <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-zinc-100">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          strokeWidth={2}
          stroke="currentColor"
          className="h-8 w-8 text-zinc-700"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M4.5 12.75l6 6 9-13.5"
          />
        </svg>
      </div>
      <h1 className="font-serif text-3xl text-zinc-900">
        ご注文ありがとうございます
      </h1>
      <p className="mt-2 text-zinc-600">
        ご注文を承りました。商品の発送までしばらくお待ちください。
      </p>
    </div>
```

**変更点:**
- パディング: `px-4 py-12` → `px-6 py-24`
- メッセージ余白: `mb-8` → `mb-12`
- アイコン背景: `bg-green-100` → `bg-zinc-100` (緑色廃止)
- アイコン色: `text-green-600` → `text-zinc-700` (緑色廃止)
- アイコン余白: `mb-4` → `mb-6`
- 見出し: `text-3xl font-bold text-gray-900` → `font-serif text-3xl text-zinc-900`
- 本文色: `text-gray-600` → `text-zinc-600`

---

### 3. 注文番号カードの改修

**ファイル:** `frontend/src/pages/OrderCompletePage.tsx`

**変更前（96-99行目）:**
```tsx
<div className="mb-8 rounded-lg bg-blue-50 p-6 text-center">
  <p className="text-sm text-gray-600">注文番号</p>
  <p className="mt-1 text-2xl font-bold text-blue-600">{orderNumber}</p>
</div>
```

**変更後:**
```tsx
<div className="mb-8 border border-stone-200 bg-stone-50 p-6 text-center">
  <p className="text-xs uppercase tracking-widest text-zinc-500">注文番号</p>
  <p className="mt-2 font-serif text-2xl text-zinc-900">{orderNumber}</p>
</div>
```

**変更点:**
- カード: `rounded-lg bg-blue-50` → `border border-stone-200 bg-stone-50` (青色廃止、境界線追加、角丸削除)
- ラベル: `text-sm text-gray-600` → `text-xs uppercase tracking-widest text-zinc-500`
- 注文番号: `mt-1 text-2xl font-bold text-blue-600` → `mt-2 font-serif text-2xl text-zinc-900` (青色廃止、太字削除、セリフ体追加)

---

### 4. 注文内容カードの改修

**ファイル:** `frontend/src/pages/OrderCompletePage.tsx`

**変更前（102-150行目）:**
```tsx
<div className="mb-8 rounded-lg bg-white p-6 shadow-sm">
  <h2 className="mb-4 text-lg font-bold text-gray-900">
    ご注文内容
  </h2>
  <div className="space-y-4">
    {items.map((item) => (
      <div
        key={item.product.id}
        className="flex gap-4 border-b border-gray-200 pb-4 last:border-b-0 last:pb-0"
      >
        {/* 商品画像 */}
        <div className="h-20 w-20 flex-shrink-0 overflow-hidden rounded-lg bg-gray-200">
          <img
            src={item.product.image}
            alt={item.product.name}
            className="h-full w-full object-cover"
          />
        </div>

        {/* 商品情報 */}
        <div className="flex flex-1 flex-col justify-between">
          <div>
            <h3 className="font-medium text-gray-900">
              {item.product.name}
            </h3>
            <p className="mt-1 text-sm text-gray-500">
              単価: ¥{item.product.price.toLocaleString()} × {item.quantity}
            </p>
          </div>
          <p className="font-bold text-gray-900">
            ¥{(item.product.price * item.quantity).toLocaleString()}
          </p>
        </div>
      </div>
    ))}
  </div>

  {/* 合計金額 */}
  <div className="mt-6 border-t border-gray-200 pt-4">
    <div className="flex justify-between">
      <span className="text-lg font-bold text-gray-900">
        合計金額
      </span>
      <span className="text-2xl font-bold text-blue-600">
        ¥{totalPrice.toLocaleString()}
      </span>
    </div>
  </div>
</div>
```

**変更後:**
```tsx
<div className="mb-8 border border-stone-200 bg-white p-6">
  <h2 className="mb-4 font-serif text-lg text-zinc-900">
    ご注文内容
  </h2>
  <div className="space-y-4">
    {items.map((item) => (
      <div
        key={item.product.id}
        className="flex gap-4 border-b border-stone-200 pb-4 last:border-b-0 last:pb-0"
      >
        {/* 商品画像 */}
        <div className="h-20 w-20 flex-shrink-0 overflow-hidden bg-stone-200">
          <img
            src={item.product.image}
            alt={item.product.name}
            className="h-full w-full object-cover"
          />
        </div>

        {/* 商品情報 */}
        <div className="flex flex-1 flex-col justify-between">
          <div>
            <h3 className="font-serif text-sm uppercase tracking-wider text-zinc-900">
              {item.product.name}
            </h3>
            <p className="mt-1 text-xs text-zinc-500">
              単価: ¥{item.product.price.toLocaleString()} × {item.quantity}
            </p>
          </div>
          <p className="text-sm text-zinc-900">
            ¥{(item.product.price * item.quantity).toLocaleString()}
          </p>
        </div>
      </div>
    ))}
  </div>

  {/* 合計金額 */}
  <div className="mt-6 border-t border-stone-200 pt-4">
    <div className="flex justify-between">
      <span className="text-base font-serif text-zinc-900">
        合計金額
      </span>
      <span className="text-xl text-zinc-900">
        ¥{totalPrice.toLocaleString()}
      </span>
    </div>
  </div>
</div>
```

**変更点:**
- カード: `rounded-lg bg-white p-6 shadow-sm` → `border border-stone-200 bg-white p-6`
- 見出し: `text-lg font-bold text-gray-900` → `font-serif text-lg text-zinc-900`
- ボーダー色: `border-gray-200` → `border-stone-200`
- 画像背景: `rounded-lg bg-gray-200` → `bg-stone-200` (角丸削除)
- 商品名: `font-medium text-gray-900` → `font-serif text-sm uppercase tracking-wider text-zinc-900`
- 単価: `text-sm text-gray-500` → `text-xs text-zinc-500`
- 価格: `font-bold text-gray-900` → `text-sm text-zinc-900` (太字削除)
- 合計ラベル: `text-lg font-bold text-gray-900` → `text-base font-serif text-zinc-900`
- 合計金額: `text-2xl font-bold text-blue-600` → `text-xl text-zinc-900` (青色廃止、太字削除)

---

### 5. お知らせカードの改修

**ファイル:** `frontend/src/pages/OrderCompletePage.tsx`

**変更前（153-160行目）:**
```tsx
<div className="mb-8 rounded-lg border border-gray-200 bg-gray-50 p-6">
  <h3 className="mb-2 font-bold text-gray-900">ご注文完了のお知らせ</h3>
  <ul className="space-y-1 text-sm text-gray-600">
    <li>• ご登録のメールアドレスに注文確認メールをお送りしました</li>
    <li>• 商品の発送が完了しましたら、発送通知メールをお送りします</li>
    <li>• ご不明な点がございましたら、お問い合わせください</li>
  </ul>
</div>
```

**変更後:**
```tsx
<div className="mb-8 border border-stone-200 bg-stone-50 p-6">
  <h3 className="mb-2 font-serif text-base text-zinc-900">ご注文完了のお知らせ</h3>
  <ul className="space-y-1 text-sm text-zinc-600">
    <li>• ご登録のメールアドレスに注文確認メールをお送りしました</li>
    <li>• 商品の発送が完了しましたら、発送通知メールをお送りします</li>
    <li>• ご不明な点がございましたら、お問い合わせください</li>
  </ul>
</div>
```

**変更点:**
- カード: `rounded-lg border border-gray-200 bg-gray-50` → `border border-stone-200 bg-stone-50`
- 見出し: `font-bold text-gray-900` → `font-serif text-base text-zinc-900`
- リスト色: `text-gray-600` → `text-zinc-600`

---

### 6. ボタンの改修

**ファイル:** `frontend/src/pages/OrderCompletePage.tsx`

**変更前（163-186行目）:**
```tsx
<div className="text-center">
  <Link
    to="/"
    className="inline-block rounded-lg bg-blue-600 px-8 py-3 font-medium text-white hover:bg-blue-700"
  >
    TOPに戻る
  </Link>
  <Link
    to="/item"
    className="ml-4 inline-block rounded-lg border border-gray-300 bg-white px-8 py-3 font-medium text-gray-700 hover:bg-gray-50"
  >
    買い物を続ける
  </Link>
  {orderStatus === 'PENDING' && (
    <div className="mt-4">
      <button
        onClick={handleCancelOrder}
        disabled={isCancelling}
        className="text-sm text-red-600 hover:text-red-700 underline disabled:opacity-50"
      >
        {isCancelling ? 'キャンセル中...' : '注文をキャンセル'}
      </button>
    </div>
  )}
</div>
```

**変更後:**
```tsx
<div className="text-center">
  <Link
    to="/"
    className="inline-block bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors"
  >
    TOPに戻る
  </Link>
  <Link
    to="/item"
    className="ml-4 inline-block border border-stone-300 bg-white px-12 py-4 text-xs tracking-[0.2em] uppercase text-zinc-700 hover:bg-stone-50 transition-colors"
  >
    買い物を続ける
  </Link>
  {orderStatus === 'PENDING' && (
    <div className="mt-4">
      <button
        onClick={handleCancelOrder}
        disabled={isCancelling}
        className="text-xs uppercase tracking-widest text-red-600 hover:text-red-700 underline disabled:opacity-50"
      >
        {isCancelling ? 'キャンセル中...' : '注文をキャンセル'}
      </button>
    </div>
  )}
</div>
```

**変更点:**
- TOPボタン: `rounded-lg bg-blue-600 px-8 py-3 font-medium text-white hover:bg-blue-700` → `bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors`
- 買い物ボタン: `rounded-lg border border-gray-300 bg-white px-8 py-3 font-medium text-gray-700 hover:bg-gray-50` → `border border-stone-300 bg-white px-12 py-4 text-xs tracking-[0.2em] uppercase text-zinc-700 hover:bg-stone-50 transition-colors`
- キャンセルボタン: `text-sm` → `text-xs uppercase tracking-widest`

---

## 参考実装
`docs/02_designs/CHG-005_全体的なUI改善.md` の「ボタン」「カード」セクション参照

---

## 確認事項
- [ ] セクション余白が py-24 になっている
- [ ] 見出しがセリフ体になっている
- [ ] 見出しが太字ではない
- [ ] カードの影が削除され、境界線が追加されている
- [ ] 商品名がセリフ体、大文字、文字間広めになっている
- [ ] 価格が太字ではない
- [ ] ボタンが黒背景（bg-zinc-900）になっている
- [ ] ボタンに文字間隔（tracking-[0.2em]）が適用されている
- [ ] ボタンが大文字（uppercase）になっている
- [ ] 青色・緑色が全て廃止されている
- [ ] gray-* が全て zinc-* / stone-* に変更されている
- [ ] 完了アイコンが zinc 系の色になっている

---

## 完了
全9タスクの実装タスク定義が完了しました。CHG-005: 全体的なUI改善（エディトリアルデザイン化）の実装タスクはこれで終了です。
