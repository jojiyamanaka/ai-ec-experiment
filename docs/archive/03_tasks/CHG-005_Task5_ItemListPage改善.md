# CHG-005 Task5: ItemListPage.tsx 改善

## 検証コマンド
```bash
cd frontend
npm run dev
# ブラウザで http://localhost:5173/item を開き、以下を確認:
# - 見出しがセリフ体で表示されている
# - セクション余白が大きくなっている
# - グリッドギャップが適切に設定されている
```

## 目的
商品一覧ページをエディトリアルデザインに改善する。

## 変更対象ファイル
1. `frontend/src/pages/ItemListPage.tsx`

---

## 変更内容

### 1. ページ全体の改修

**ファイル:** `frontend/src/pages/ItemListPage.tsx`

**変更前（8-18行目）:**
```tsx
return (
  <div className="mx-auto max-w-7xl px-4 py-12">
    <h1 className="mb-8 text-3xl font-bold text-gray-900">すべての商品</h1>
    <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {publishedProducts.map((product) => (
        <ProductCard key={product.id} product={product} />
      ))}
    </div>
  </div>
)
```

**変更後:**
```tsx
return (
  <div className="mx-auto max-w-7xl px-6 py-24">
    <h1 className="mb-12 font-serif text-3xl text-zinc-900">すべての商品</h1>
    <div className="grid gap-x-6 gap-y-12 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {publishedProducts.map((product) => (
        <ProductCard key={product.id} product={product} />
      ))}
    </div>
  </div>
)
```

**主な変更点:**
1. パディング: `px-4 py-12` → `px-6 py-24` (余白2倍)
2. 見出し余白: `mb-8` → `mb-12`
3. 見出し: `text-3xl font-bold text-gray-900` → `font-serif text-3xl text-zinc-900` (セリフ体、太字削除)
4. グリッドギャップ: `gap-6` → `gap-x-6 gap-y-12` (縦方向のギャップ拡大)

---

## 参考実装
`docs/01_requirements/sample.html` の 46-51行目:
```html
<section class="py-24 px-6 max-w-7xl mx-auto">
    <div class="flex justify-between items-end mb-12">
        <h3 class="serif text-3xl">New Arrivals</h3>
    </div>

    <div class="grid grid-cols-2 md:grid-cols-4 gap-x-6 gap-y-12">
```

---

## 確認事項
- [ ] セクション余白が py-24 に拡大されている
- [ ] セクション横余白が px-6 になっている
- [ ] 見出しがセリフ体になっている
- [ ] 見出しが太字ではない
- [ ] 見出しの色が zinc-900 になっている
- [ ] 見出し下の余白が mb-12 になっている
- [ ] グリッドの縦方向ギャップが gap-y-12 になっている
- [ ] グリッドの横方向ギャップが gap-x-6 になっている

---

## 次のタスク
Task6: ItemDetailPage.tsx 改善
