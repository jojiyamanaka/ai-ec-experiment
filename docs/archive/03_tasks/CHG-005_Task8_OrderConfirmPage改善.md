# CHG-005 Task8: OrderConfirmPage.tsx 改善（注文確認ページ）

## 検証コマンド
```bash
cd frontend
npm run dev
# カートに商品を追加してから http://localhost:5173/order/reg を開き、以下を確認:
# - 見出しがセリフ体で表示されている
# - カードの影が削除されている
# - ボタンが黒背景になっている
# - 価格表示が控えめになっている
```

## 目的
注文確認ページをエディトリアルデザインに改善する。

## 変更対象ファイル
1. `frontend/src/pages/OrderConfirmPage.tsx`

---

## 変更内容

### 1. 空カート画面の改修

**ファイル:** `frontend/src/pages/OrderConfirmPage.tsx`

**変更前（16-33行目）:**
```tsx
if (items.length === 0) {
  return (
    <div className="mx-auto max-w-7xl px-4 py-12">
      <div className="text-center">
        <h2 className="text-2xl font-bold text-gray-900">
          カートが空です
        </h2>
        <p className="mt-2 text-gray-600">
          注文する商品がありません
        </p>
        <Link
          to="/item"
          className="mt-6 inline-block rounded-lg bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700"
        >
          商品一覧へ
        </Link>
      </div>
    </div>
  )
}
```

**変更後:**
```tsx
if (items.length === 0) {
  return (
    <div className="mx-auto max-w-7xl px-6 py-24">
      <div className="text-center">
        <h2 className="font-serif text-2xl text-zinc-900">
          カートが空です
        </h2>
        <p className="mt-2 text-zinc-600">
          注文する商品がありません
        </p>
        <Link
          to="/item"
          className="mt-6 inline-block bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors"
        >
          商品一覧へ
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

### 2. ページ全体の改修

**ファイル:** `frontend/src/pages/OrderConfirmPage.tsx`

**変更前（82-83行目）:**
```tsx
return (
  <div className="mx-auto max-w-4xl px-4 py-12">
    <h1 className="mb-8 text-3xl font-bold text-gray-900">注文内容の確認</h1>
```

**変更後:**
```tsx
return (
  <div className="mx-auto max-w-4xl px-6 py-24">
    <h1 className="mb-12 font-serif text-3xl text-zinc-900">注文内容の確認</h1>
```

**変更点:**
- パディング: `px-4 py-12` → `px-6 py-24`
- 見出し余白: `mb-8` → `mb-12`
- 見出し: `text-3xl font-bold text-gray-900` → `font-serif text-3xl text-zinc-900`

---

### 3. 商品一覧カードの改修

**ファイル:** `frontend/src/pages/OrderConfirmPage.tsx`

**変更前（86-125行目）:**
```tsx
<div className="mb-8 rounded-lg bg-white p-6 shadow-sm">
  <h2 className="mb-4 text-lg font-bold text-gray-900">ご注文商品</h2>
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
              {item.product.description}
            </p>
          </div>
          <div className="flex items-center justify-between">
            <p className="text-sm text-gray-600">
              数量: {item.quantity}
            </p>
            <p className="font-bold text-gray-900">
              ¥{(item.product.price * item.quantity).toLocaleString()}
            </p>
          </div>
        </div>
      </div>
    ))}
  </div>
</div>
```

**変更後:**
```tsx
<div className="mb-8 border border-stone-200 bg-white p-6">
  <h2 className="mb-4 font-serif text-lg text-zinc-900">ご注文商品</h2>
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
              {item.product.description}
            </p>
          </div>
          <div className="flex items-center justify-between">
            <p className="text-xs text-zinc-600">
              数量: {item.quantity}
            </p>
            <p className="text-sm text-zinc-900">
              ¥{(item.product.price * item.quantity).toLocaleString()}
            </p>
          </div>
        </div>
      </div>
    ))}
  </div>
</div>
```

**変更点:**
- カード: `rounded-lg bg-white p-6 shadow-sm` → `border border-stone-200 bg-white p-6`
- 見出し: `text-lg font-bold text-gray-900` → `font-serif text-lg text-zinc-900`
- ボーダー色: `border-gray-200` → `border-stone-200`
- 画像背景: `rounded-lg bg-gray-200` → `bg-stone-200` (角丸削除)
- 商品名: `font-medium text-gray-900` → `font-serif text-sm uppercase tracking-wider text-zinc-900`
- 説明: `text-sm text-gray-500` → `text-xs text-zinc-500`
- 数量: `text-sm text-gray-600` → `text-xs text-zinc-600`
- 価格: `font-bold text-gray-900` → `text-sm text-zinc-900` (太字削除、サイズ縮小)

---

### 4. 合計金額カードの改修

**ファイル:** `frontend/src/pages/OrderConfirmPage.tsx`

**変更前（128-156行目）:**
```tsx
<div className="mb-8 rounded-lg bg-white p-6 shadow-sm">
  <h2 className="mb-4 text-lg font-bold text-gray-900">お支払い金額</h2>
  <div className="space-y-2">
    <div className="flex justify-between text-sm">
      <span className="text-gray-600">小計</span>
      <span className="font-medium text-gray-900">
        ¥{totalPrice.toLocaleString()}
      </span>
    </div>
    <div className="flex justify-between text-sm">
      <span className="text-gray-600">配送料</span>
      <span className="font-medium text-gray-900">¥0</span>
    </div>
    <div className="flex justify-between text-sm">
      <span className="text-gray-600">手数料</span>
      <span className="font-medium text-gray-900">¥0</span>
    </div>
    <div className="border-t border-gray-200 pt-2">
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
</div>
```

**変更後:**
```tsx
<div className="mb-8 border border-stone-200 bg-white p-6">
  <h2 className="mb-4 font-serif text-lg text-zinc-900">お支払い金額</h2>
  <div className="space-y-2">
    <div className="flex justify-between text-sm">
      <span className="text-zinc-600">小計</span>
      <span className="text-zinc-900">
        ¥{totalPrice.toLocaleString()}
      </span>
    </div>
    <div className="flex justify-between text-sm">
      <span className="text-zinc-600">配送料</span>
      <span className="text-zinc-900">¥0</span>
    </div>
    <div className="flex justify-between text-sm">
      <span className="text-zinc-600">手数料</span>
      <span className="text-zinc-900">¥0</span>
    </div>
    <div className="border-t border-stone-200 pt-2">
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
</div>
```

**変更点:**
- カード: `rounded-lg bg-white p-6 shadow-sm` → `border border-stone-200 bg-white p-6`
- 見出し: `text-lg font-bold text-gray-900` → `font-serif text-lg text-zinc-900`
- テキスト色: `text-gray-*` → `text-zinc-*`
- 太字削除: `font-medium` / `font-bold` を削除
- ボーダー色: `border-gray-200` → `border-stone-200`
- 合計ラベル: `text-lg font-bold text-gray-900` → `text-base font-serif text-zinc-900`
- 合計金額: `text-2xl font-bold text-blue-600` → `text-xl text-zinc-900` (青色廃止、太字削除)

---

### 5. ボタンとリンクの改修

**ファイル:** `frontend/src/pages/OrderConfirmPage.tsx`

**変更前（219-233行目）:**
```tsx
<div className="space-y-3">
  <button
    onClick={handleConfirmOrder}
    disabled={isSubmitting}
    className="w-full rounded-lg bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-400"
  >
    {isSubmitting ? '処理中...' : '注文を確定する'}
  </button>
  <Link
    to="/order/cart"
    className="block text-center text-sm text-blue-600 hover:underline"
  >
    カートに戻る
  </Link>
</div>
```

**変更後:**
```tsx
<div className="space-y-3">
  <button
    onClick={handleConfirmOrder}
    disabled={isSubmitting}
    className="w-full bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors disabled:cursor-not-allowed disabled:bg-stone-400"
  >
    {isSubmitting ? '処理中...' : '注文を確定する'}
  </button>
  <Link
    to="/order/cart"
    className="block text-center text-xs uppercase tracking-widest text-zinc-600 hover:text-zinc-900 transition-colors"
  >
    カートに戻る
  </Link>
</div>
```

**変更点:**
- ボタン: `rounded-lg bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700 disabled:bg-gray-400` → `bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors disabled:bg-stone-400`
- リンク: `text-sm text-blue-600 hover:underline` → `text-xs uppercase tracking-widest text-zinc-600 hover:text-zinc-900 transition-colors`

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
- [ ] 青色が全て廃止されている
- [ ] gray-* が全て zinc-* / stone-* に変更されている

---

## 次のタスク
Task9: OrderCompletePage.tsx 改善（注文完了ページ）
