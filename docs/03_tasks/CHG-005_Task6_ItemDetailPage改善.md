# CHG-005 Task6: ItemDetailPage.tsx 改善

## 検証コマンド
```bash
cd frontend
npm run dev
# ブラウザで http://localhost:5173/item/1 を開き、以下を確認:
# - 商品画像が縦長（3:4）になっている
# - 見出しがセリフ体で表示されている
# - 価格が控えめに表示されている
# - ボタンが黒背景になっている
```

## 目的
商品詳細ページをエディトリアルデザインに改善する。

## 変更対象ファイル
1. `frontend/src/pages/ItemDetailPage.tsx`

---

## 変更内容

### 1. 在庫バッジの色変更

**ファイル:** `frontend/src/pages/ItemDetailPage.tsx`

**変更前（7-24行目）:**
```tsx
function getStockStatus(stock: number) {
  if (stock === 0) {
    return {
      text: '売り切れ',
      color: 'bg-gray-400 text-white',
    }
  } else if (stock >= 1 && stock <= 5) {
    return {
      text: '残りわずか',
      color: 'bg-orange-500 text-white',
    }
  } else {
    return {
      text: '在庫あり',
      color: 'bg-green-500 text-white',
    }
  }
}
```

**変更後:**
```tsx
function getStockStatus(stock: number) {
  if (stock === 0) {
    return {
      text: '売り切れ',
      color: 'bg-stone-400 text-white',
    }
  } else if (stock >= 1 && stock <= 5) {
    return {
      text: '残りわずか',
      color: 'bg-zinc-500 text-white',
    }
  } else {
    return {
      text: '在庫あり',
      color: 'bg-zinc-700 text-white',
    }
  }
}
```

**変更点:**
- `bg-gray-400` → `bg-stone-400`
- `bg-orange-500` → `bg-zinc-500`
- `bg-green-500` → `bg-zinc-700`

---

### 2. ページコンテンツの改修

**ファイル:** `frontend/src/pages/ItemDetailPage.tsx`

**変更前（60-104行目）:**
```tsx
return (
  <div className="mx-auto max-w-7xl px-4 py-12">
    <div className="grid gap-8 md:grid-cols-2">
      <div className="aspect-[4/3] overflow-hidden rounded-lg bg-gray-200">
        <img
          src={product.image}
          alt={product.name}
          className="h-full w-full object-cover"
        />
      </div>
      <div>
        <h1 className="text-3xl font-bold text-gray-900">{product.name}</h1>
        <div className="mt-4 flex items-center gap-4">
          <p className="text-2xl font-bold text-blue-600">
            ¥{product.price.toLocaleString()}
          </p>
          <span
            className={`rounded-full px-4 py-1 text-sm font-medium ${stockStatus.color}`}
          >
            {stockStatus.text}
          </span>
        </div>
        <p className="mt-6 leading-relaxed text-gray-600">
          {product.description}
        </p>
        {error && (
          <div className="mt-6 rounded-lg border border-red-300 bg-red-50 p-3">
            <p className="text-sm text-red-700">{error}</p>
          </div>
        )}
        <button
          onClick={handleAddToCart}
          disabled={isSoldOut || isAdding}
          className={`mt-8 w-full rounded-lg px-6 py-3 font-medium text-white ${
            isSoldOut || isAdding
              ? 'cursor-not-allowed bg-gray-400'
              : 'bg-blue-600 hover:bg-blue-700'
          }`}
        >
          {isSoldOut ? '売り切れ' : isAdding ? '追加中...' : 'カートに追加'}
        </button>
      </div>
    </div>
  </div>
)
```

**変更後:**
```tsx
return (
  <div className="mx-auto max-w-7xl px-6 py-24">
    <div className="grid gap-12 md:grid-cols-2">
      <div className="aspect-[3/4] overflow-hidden bg-stone-200">
        <img
          src={product.image}
          alt={product.name}
          className="h-full w-full object-cover"
        />
      </div>
      <div className="space-y-8">
        <div>
          <h1 className="font-serif text-4xl md:text-5xl leading-tight text-zinc-900">
            {product.name}
          </h1>
          <div className="mt-6 flex items-center gap-4">
            <p className="text-sm text-zinc-500">
              ¥{product.price.toLocaleString()}
            </p>
            <span
              className={`rounded-full px-4 py-1 text-xs font-medium ${stockStatus.color}`}
            >
              {stockStatus.text}
            </span>
          </div>
        </div>
        <p className="leading-relaxed text-zinc-600 font-light">
          {product.description}
        </p>
        {error && (
          <div className="rounded-lg border border-red-300 bg-red-50 p-3">
            <p className="text-sm text-red-700">{error}</p>
          </div>
        )}
        <button
          onClick={handleAddToCart}
          disabled={isSoldOut || isAdding}
          className={`w-full px-12 py-4 text-xs tracking-[0.2em] uppercase transition-colors ${
            isSoldOut || isAdding
              ? 'cursor-not-allowed bg-stone-400 text-white'
              : 'bg-zinc-900 text-white hover:bg-zinc-800'
          }`}
        >
          {isSoldOut ? '売り切れ' : isAdding ? '追加中...' : 'カートに追加'}
        </button>
      </div>
    </div>
  </div>
)
```

**主な変更点:**

1. **コンテナ:**
   - パディング: `px-4 py-12` → `px-6 py-24`
   - グリッドギャップ: `gap-8` → `gap-12`

2. **画像コンテナ:**
   - アスペクト比: `aspect-[4/3]` → `aspect-[3/4]`
   - 角丸削除: `rounded-lg` を削除
   - 背景色: `bg-gray-200` → `bg-stone-200`

3. **右側コンテナ:**
   - `space-y-8` を追加（セクション間の一貫した余白）

4. **商品名:**
   - `text-3xl font-bold text-gray-900` → `font-serif text-4xl md:text-5xl leading-tight text-zinc-900`
   - セリフ体、サイズ拡大、太字削除

5. **価格エリア:**
   - 余白: `mt-4` → `mt-6`

6. **価格:**
   - `text-2xl font-bold text-blue-600` → `text-sm text-zinc-500`
   - 控えめなサイズ、太字削除、青色廃止

7. **バッジサイズ:**
   - `text-sm` → `text-xs`

8. **説明文:**
   - `mt-6 leading-relaxed text-gray-600` → `leading-relaxed text-zinc-600 font-light`
   - 細字追加

9. **ボタン:**
   - 角丸削除: `rounded-lg` を削除
   - パディング: `px-6 py-3` → `px-12 py-4`
   - フォント: `font-medium` → `text-xs tracking-[0.2em] uppercase`
   - 背景（有効時）: `bg-blue-600 hover:bg-blue-700` → `bg-zinc-900 hover:bg-zinc-800`
   - 背景（無効時）: `bg-gray-400` → `bg-stone-400`
   - 余白: `mt-8` を削除（space-y-8で制御）

---

## 参考実装
`docs/01_requirements/sample.html` の 40-42行目（ボタン）:
```html
<button class="bg-zinc-900 text-white px-12 py-4 text-xs tracking-[0.2em] hover:bg-zinc-800 transition-colors">
    SHOP THE LOOK
</button>
```

---

## 確認事項
- [ ] セクション余白が py-24 になっている
- [ ] 画像が 3:4 の縦長になっている
- [ ] 画像の角丸が削除されている
- [ ] 商品名がセリフ体で大きく表示されている
- [ ] 商品名が太字ではない
- [ ] 価格が控えめ（text-sm）に表示されている
- [ ] 価格が太字ではない
- [ ] 価格が青色ではなく zinc-500 になっている
- [ ] 説明文が細字（font-light）になっている
- [ ] ボタンが黒背景（bg-zinc-900）になっている
- [ ] ボタンに文字間隔（tracking-[0.2em]）が適用されている
- [ ] ボタンが大文字（uppercase）になっている
- [ ] 在庫バッジが zinc/stone 系の色になっている

---

## 次のタスク
Task7: CartPage.tsx 改善
