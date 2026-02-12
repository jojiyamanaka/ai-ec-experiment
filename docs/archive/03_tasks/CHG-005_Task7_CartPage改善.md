# CHG-005 Task7: CartPage.tsx 改善

## 検証コマンド
```bash
cd frontend
npm run dev
# カートに商品を追加してから http://localhost:5173/order/cart を開き、以下を確認:
# - 見出しがセリフ体で表示されている
# - セクション余白が大きくなっている
# - ボタンが黒背景になっている
# - 価格表示が控えめになっている
```

## 目的
カートページをエディトリアルデザインに改善する。

## 変更対象ファイル
1. `frontend/src/pages/CartPage.tsx`

---

## 変更内容

### 1. 空カート画面の改修

**ファイル:** `frontend/src/pages/CartPage.tsx`

**変更前（12-43行目）:**
```tsx
if (items.length === 0) {
  return (
    <div className="mx-auto max-w-7xl px-4 py-12">
      <div className="text-center">
        {/* SVG アイコン */}
        <h2 className="mt-6 text-2xl font-bold text-gray-900">
          カートは空です
        </h2>
        <p className="mt-2 text-gray-600">
          商品を追加して、お買い物を始めましょう
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
        {/* SVG アイコン（色を変更） */}
        <svg
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          strokeWidth={1.5}
          stroke="currentColor"
          className="mx-auto h-24 w-24 text-zinc-400"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 0 0-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 0 0-16.536-1.84M7.5 14.25 5.106 5.272M6 20.25a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm12.75 0a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z"
          />
        </svg>
        <h2 className="mt-6 font-serif text-2xl text-zinc-900">
          カートは空です
        </h2>
        <p className="mt-2 text-zinc-600">
          商品を追加して、お買い物を始めましょう
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
- アイコン色: `text-gray-400` → `text-zinc-400`
- 見出し: `text-2xl font-bold text-gray-900` → `font-serif text-2xl text-zinc-900`
- 本文色: `text-gray-600` → `text-zinc-600`
- ボタン: `rounded-lg bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700` → `bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors`

---

### 2. カートページ本体の改修

**ファイル:** `frontend/src/pages/CartPage.tsx`

**変更前（65-67行目）:**
```tsx
return (
  <div className="mx-auto max-w-7xl px-4 py-12">
    <h1 className="mb-8 text-3xl font-bold text-gray-900">ショッピングカート</h1>
```

**変更後:**
```tsx
return (
  <div className="mx-auto max-w-7xl px-6 py-24">
    <h1 className="mb-12 font-serif text-3xl text-zinc-900">ショッピングカート</h1>
```

**変更点:**
- パディング: `px-4 py-12` → `px-6 py-24`
- 見出し余白: `mb-8` → `mb-12`
- 見出し: `text-3xl font-bold text-gray-900` → `font-serif text-3xl text-zinc-900`

---

### 3. 商品カードの改修

**ファイル:** `frontend/src/pages/CartPage.tsx`

**変更前（80-82, 88-89, 98-110, 112行目の一部）:**
```tsx
<div
  key={item.product.id}
  className="flex gap-4 rounded-lg bg-white p-4 shadow-sm"
>
  {/* 商品画像 */}
  <Link
    to={`/item/${item.product.id}`}
    className="h-24 w-24 flex-shrink-0 overflow-hidden rounded-lg bg-gray-200"
  >
    {/* ... */}
  </Link>

  {/* 商品情報 */}
  <div className="flex flex-1 flex-col">
    <div className="flex justify-between">
      <div>
        <Link
          to={`/item/${item.product.id}`}
          className="font-medium text-gray-900 hover:text-blue-600"
        >
          {item.product.name}
        </Link>
        <p className="mt-1 text-sm text-gray-500">
          {item.product.description}
        </p>
      </div>
      <button
        onClick={() => removeFromCart(item.id)}
        className="text-gray-400 hover:text-red-600"
```

**変更後:**
```tsx
<div
  key={item.product.id}
  className="flex gap-4 border border-stone-200 bg-white p-4"
>
  {/* 商品画像 */}
  <Link
    to={`/item/${item.product.id}`}
    className="h-24 w-24 flex-shrink-0 overflow-hidden bg-stone-200"
  >
    {/* ... */}
  </Link>

  {/* 商品情報 */}
  <div className="flex flex-1 flex-col">
    <div className="flex justify-between">
      <div>
        <Link
          to={`/item/${item.product.id}`}
          className="font-serif text-sm uppercase tracking-wider text-zinc-900 hover:text-zinc-600"
        >
          {item.product.name}
        </Link>
        <p className="mt-1 text-xs text-zinc-500">
          {item.product.description}
        </p>
      </div>
      <button
        onClick={() => removeFromCart(item.id)}
        className="text-zinc-400 hover:text-red-600"
```

**変更点:**
- カード: `rounded-lg bg-white p-4 shadow-sm` → `border border-stone-200 bg-white p-4` (影削除、境界線追加、角丸削除)
- サムネイル: `rounded-lg bg-gray-200` → `bg-stone-200` (角丸削除)
- 商品名: `font-medium text-gray-900 hover:text-blue-600` → `font-serif text-sm uppercase tracking-wider text-zinc-900 hover:text-zinc-600`
- 説明: `text-sm text-gray-500` → `text-xs text-zinc-500`
- 削除ボタン: `text-gray-400` → `text-zinc-400`

---

### 4. 価格表示の改修

**ファイル:** `frontend/src/pages/CartPage.tsx`

**変更前（193行目）:**
```tsx
<p className="font-bold text-gray-900">
  ¥{(item.product.price * item.quantity).toLocaleString()}
</p>
```

**変更後:**
```tsx
<p className="text-sm text-zinc-900">
  ¥{(item.product.price * item.quantity).toLocaleString()}
</p>
```

**変更点:**
- `font-bold text-gray-900` → `text-sm text-zinc-900` (太字削除、サイズ縮小)

---

### 5. サマリーの改修

**ファイル:** `frontend/src/pages/CartPage.tsx`

**変更前（204-242行目）:**
```tsx
<div className="lg:col-span-1">
  <div className="sticky top-4 rounded-lg bg-white p-6 shadow-sm">
    <h2 className="text-lg font-bold text-gray-900">注文サマリー</h2>
    <div className="mt-4 space-y-2">
      <div className="flex justify-between text-sm">
        <span className="text-gray-600">小計</span>
        <span className="font-medium text-gray-900">
          ¥{totalPrice.toLocaleString()}
        </span>
      </div>
      {/* ... */}
      <div className="border-t border-gray-200 pt-2">
        <div className="flex justify-between">
          <span className="text-base font-bold text-gray-900">
            合計
          </span>
          <span className="text-xl font-bold text-blue-600">
            ¥{totalPrice.toLocaleString()}
          </span>
        </div>
      </div>
    </div>
    <button
      onClick={handleCheckout}
      className="mt-6 w-full rounded-lg bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700"
    >
      レジに進む
    </button>
    <Link
      to="/item"
      className="mt-3 block text-center text-sm text-blue-600 hover:underline"
    >
      買い物を続ける
    </Link>
  </div>
</div>
```

**変更後:**
```tsx
<div className="lg:col-span-1">
  <div className="sticky top-24 border border-stone-200 bg-white p-6">
    <h2 className="font-serif text-lg text-zinc-900">注文サマリー</h2>
    <div className="mt-4 space-y-2">
      <div className="flex justify-between text-sm">
        <span className="text-zinc-600">小計</span>
        <span className="text-zinc-900">
          ¥{totalPrice.toLocaleString()}
        </span>
      </div>
      {/* ... */}
      <div className="border-t border-stone-200 pt-2">
        <div className="flex justify-between">
          <span className="text-base font-serif text-zinc-900">
            合計
          </span>
          <span className="text-xl text-zinc-900">
            ¥{totalPrice.toLocaleString()}
          </span>
        </div>
      </div>
    </div>
    <button
      onClick={handleCheckout}
      className="mt-6 w-full bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors"
    >
      レジに進む
    </button>
    <Link
      to="/item"
      className="mt-3 block text-center text-xs uppercase tracking-widest text-zinc-600 hover:text-zinc-900 transition-colors"
    >
      買い物を続ける
    </Link>
  </div>
</div>
```

**変更点:**
- スティッキー位置: `top-4` → `top-24` (固定ヘッダー分の調整)
- カード: `rounded-lg bg-white p-6 shadow-sm` → `border border-stone-200 bg-white p-6` (影削除、境界線追加、角丸削除)
- 見出し: `text-lg font-bold text-gray-900` → `font-serif text-lg text-zinc-900`
- テキスト色: `text-gray-*` → `text-zinc-*`
- 太字削除: 小計・合計の `font-medium` / `font-bold` を削除
- 合計ラベル: `font-serif` 追加
- 合計価格: `text-xl font-bold text-blue-600` → `text-xl text-zinc-900` (青色廃止、太字削除)
- ボタン: `rounded-lg bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700` → `bg-zinc-900 px-12 py-4 text-xs tracking-[0.2em] uppercase text-white hover:bg-zinc-800 transition-colors`
- リンク: `text-sm text-blue-600 hover:underline` → `text-xs uppercase tracking-widest text-zinc-600 hover:text-zinc-900 transition-colors`

---

### 6. 数量ボタンの小調整

**ファイル:** `frontend/src/pages/CartPage.tsx`

**変更前（134-175行目の一部）:**
```tsx
<button
  onClick={() =>
    handleQuantityChange(item.id, item.quantity - 1)
  }
  className="flex h-8 w-8 items-center justify-center rounded-full border border-gray-300 hover:bg-gray-100"
```

**変更後:**
```tsx
<button
  onClick={() =>
    handleQuantityChange(item.id, item.quantity - 1)
  }
  className="flex h-8 w-8 items-center justify-center rounded-full border border-stone-300 hover:bg-stone-100"
```

**変更点:**
- `border-gray-300` → `border-stone-300`
- `hover:bg-gray-100` → `hover:bg-stone-100`
- 同様に「+」ボタンも変更

---

### 7. 入力フィールドの小調整

**ファイル:** `frontend/src/pages/CartPage.tsx`

**変更前（156-168行目）:**
```tsx
<input
  type="number"
  min="1"
  max="9"
  value={item.quantity}
  onChange={(e) =>
    handleQuantityChange(
      item.id,
      parseInt(e.target.value) || 1,
    )
  }
  className="w-16 rounded border border-gray-300 px-3 py-1 text-center"
/>
```

**変更後:**
```tsx
<input
  type="number"
  min="1"
  max="9"
  value={item.quantity}
  onChange={(e) =>
    handleQuantityChange(
      item.id,
      parseInt(e.target.value) || 1,
    )
  }
  className="w-16 rounded border border-stone-300 px-3 py-1 text-center"
/>
```

**変更点:**
- `border-gray-300` → `border-stone-300`

---

## 参考実装
`docs/02_designs/CHG-005_全体的なUI改善.md` の「ボタン」セクション参照

---

## 確認事項
- [ ] セクション余白が py-24 になっている
- [ ] 見出しがセリフ体になっている
- [ ] 見出しが太字ではない
- [ ] 商品カードの影が削除され、境界線が追加されている
- [ ] 商品名がセリフ体、大文字、文字間広めになっている
- [ ] 価格が太字ではない
- [ ] サマリーカードの影が削除され、境界線が追加されている
- [ ] サマリーのスティッキー位置が top-24 になっている
- [ ] ボタンが黒背景（bg-zinc-900）になっている
- [ ] ボタンに文字間隔（tracking-[0.2em]）が適用されている
- [ ] ボタンが大文字（uppercase）になっている
- [ ] 青色が全て廃止されている
- [ ] gray-* が全て zinc-* / stone-* に変更されている

---

## 完了
全7タスクの実装が完了しました。CHG-005: 全体的なUI改善（エディトリアルデザイン化）の実装タスクはこれで終了です。
