# CHG-005 Task3: ProductCard.tsx 改善（3:4縦長・セリフ体・色変更）

## 検証コマンド
```bash
cd frontend
npm run dev
# ブラウザで http://localhost:5173 を開き、以下を確認:
# - 商品カードが縦長（3:4）になっている
# - 商品名がセリフ体で表示されている
# - 価格が控えめに表示されている
# - ホバー時のアニメーションがゆっくり（0.7秒）
```

## 目的
商品カードをファッションECらしい縦長デザインに変更し、セリフ体と控えめな価格表示で洗練度を向上させる。

## 変更対象ファイル
1. `frontend/src/components/ProductCard.tsx`

---

## 変更内容

### 1. 在庫バッジの色変更

**ファイル:** `frontend/src/components/ProductCard.tsx`

**変更前（9-26行目）:**
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

### 2. 商品カード全体の改修

**ファイル:** `frontend/src/components/ProductCard.tsx`

**変更前（32-58行目）:**
```tsx
<Link
  to={`/item/${product.id}`}
  className="group block overflow-hidden rounded-lg bg-white shadow-sm transition hover:shadow-md"
>
  <div className="aspect-[4/3] overflow-hidden bg-gray-200">
    <img
      src={product.image}
      alt={product.name}
      className="h-full w-full object-cover transition group-hover:scale-105"
    />
  </div>
  <div className="p-4">
    <h3 className="font-medium text-gray-900">{product.name}</h3>
    <p className="mt-1 text-sm text-gray-500">{product.description}</p>
    <div className="mt-3 flex items-center justify-between">
      <p className="text-lg font-bold text-blue-600">
        ¥{product.price.toLocaleString()}
      </p>
      <span
        className={`rounded-full px-3 py-1 text-xs font-medium ${stockStatus.color}`}
      >
        {stockStatus.text}
      </span>
    </div>
  </div>
</Link>
```

**変更後:**
```tsx
<Link
  to={`/item/${product.id}`}
  className="group block cursor-pointer"
>
  <div className="aspect-[3/4] overflow-hidden bg-stone-200 mb-4">
    <img
      src={product.image}
      alt={product.name}
      className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
    />
  </div>
  <div>
    <h3 className="font-serif text-sm mb-1 uppercase tracking-wider text-zinc-900">
      {product.name}
    </h3>
    <p className="text-xs text-zinc-500 mb-2">{product.description}</p>
    <div className="flex items-center justify-between">
      <p className="text-xs text-zinc-500">
        ¥{product.price.toLocaleString()}
      </p>
      <span
        className={`rounded-full px-3 py-1 text-xs font-medium ${stockStatus.color}`}
      >
        {stockStatus.text}
      </span>
    </div>
  </div>
</Link>
```

**主な変更点:**
1. **カードラッパー:**
   - `rounded-lg bg-white shadow-sm transition hover:shadow-md` を削除
   - シンプルに `cursor-pointer` のみ

2. **画像コンテナ:**
   - `aspect-[4/3]` → `aspect-[3/4]` (縦長に変更)
   - `bg-gray-200` → `bg-stone-200`
   - `mb-4` を追加（画像と詳細の間に余白）

3. **画像アニメーション:**
   - `transition` → `transition-transform duration-700` (0.7秒のゆっくりしたアニメーション)

4. **商品名:**
   - `font-medium text-gray-900` → `font-serif text-sm mb-1 uppercase tracking-wider text-zinc-900`
   - セリフ体、大文字、文字間広め

5. **説明文:**
   - `mt-1 text-sm text-gray-500` → `text-xs text-zinc-500 mb-2`
   - サイズ縮小、zinc色に変更

6. **価格:**
   - `text-lg font-bold text-blue-600` → `text-xs text-zinc-500`
   - 太字を削除、控えめなサイズと色に変更
   - 青色を廃止

7. **コンテナパディング:**
   - `p-4` を削除（余白をシンプルに）

---

## 参考実装
`docs/01_requirements/sample.html` の 53-59行目:
```html
<div class="group cursor-pointer">
    <div class="aspect-[3/4] bg-stone-200 overflow-hidden mb-4">
        <div class="w-full h-full group-hover:scale-105 transition-transform duration-700 ...">Product Image</div>
    </div>
    <h4 class="serif text-sm mb-1 uppercase tracking-wider">Minimal Wool Coat</h4>
    <p class="text-xs text-zinc-500">¥42,000</p>
</div>
```

---

## 確認事項
- [ ] 商品カードの画像が 3:4 の縦長になっている
- [ ] 商品カードの影が削除されている
- [ ] 商品名がセリフ体で表示されている
- [ ] 商品名が大文字（uppercase）で表示されている
- [ ] 商品名の文字間が広い（tracking-wider）
- [ ] 価格が小さく控えめに表示されている（text-xs）
- [ ] 価格が太字ではない
- [ ] 価格が青色ではなく zinc-500 になっている
- [ ] 在庫バッジが zinc/stone 系の色になっている
- [ ] ホバー時のアニメーションが0.7秒でゆっくり
- [ ] 背景色が stone-200 になっている

---

## 次のタスク
Task4: HomePage.tsx 改善
