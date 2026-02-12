# CHG-005 Task4: HomePage.tsx 改善

## 検証コマンド
```bash
cd frontend
npm run dev
# ブラウザで http://localhost:5173 を開き、以下を確認:
# - バナー背景が青グラデーションから黒に変更されている
# - 見出しがセリフ体で表示されている
# - セクション余白が大きくなっている
```

## 目的
ホームページのバナーとセクション見出しをエディトリアルデザインに改善する。

## 変更対象ファイル
1. `frontend/src/pages/HomePage.tsx`

---

## 変更内容

### 1. バナーセクションの改修

**ファイル:** `frontend/src/pages/HomePage.tsx`

**変更前（11-21行目）:**
```tsx
<section className="bg-gradient-to-r from-blue-500 to-purple-600">
  <div className="mx-auto max-w-7xl px-4 py-24 text-center text-white">
    <h1 className="text-4xl font-bold md:text-5xl">
      AI がおすすめする最高の商品
    </h1>
    <p className="mt-4 text-lg md:text-xl">
      あなたにぴったりの商品を見つけよう
    </p>
  </div>
</section>
```

**変更後:**
```tsx
<section className="bg-zinc-900">
  <div className="mx-auto max-w-7xl px-6 py-32 text-center text-white">
    <span className="text-xs uppercase tracking-[0.3em] text-zinc-400">
      2026 Spring Collection
    </span>
    <h1 className="mt-4 font-serif text-5xl md:text-7xl leading-tight">
      AI がおすすめする最高の商品
    </h1>
    <p className="mt-6 text-zinc-300 leading-relaxed font-light max-w-2xl mx-auto">
      あなたにぴったりの商品を見つけよう
    </p>
  </div>
</section>
```

**主な変更点:**
1. 背景: `bg-gradient-to-r from-blue-500 to-purple-600` → `bg-zinc-900` (黒背景)
2. パディング: `px-4 py-24` → `px-6 py-32` (余白拡大)
3. ラベル追加: `text-xs uppercase tracking-[0.3em] text-zinc-400`
4. 見出し: `text-4xl font-bold md:text-5xl` → `mt-4 font-serif text-5xl md:text-7xl leading-tight` (セリフ体、サイズ拡大)
5. 本文: `mt-4 text-lg md:text-xl` → `mt-6 text-zinc-300 leading-relaxed font-light max-w-2xl mx-auto` (細字、最大幅制限)

---

### 2. おすすめ商品セクションの改修

**ファイル:** `frontend/src/pages/HomePage.tsx`

**変更前（24-38行目）:**
```tsx
<section className="mx-auto max-w-7xl px-4 py-12">
  <div className="mb-6 flex items-center justify-between">
    <h2 className="text-2xl font-bold text-gray-900">おすすめ商品</h2>
    <Link
      to="/item"
      className="text-sm font-medium text-blue-600 hover:underline"
    >
      すべて見る →
    </Link>
  </div>
  <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
    {recommendedProducts.map((product) => (
      <ProductCard key={product.id} product={product} />
    ))}
  </div>
</section>
```

**変更後:**
```tsx
<section className="mx-auto max-w-7xl px-6 py-24">
  <div className="mb-12 flex items-end justify-between">
    <h2 className="font-serif text-3xl text-zinc-900">おすすめ商品</h2>
    <Link
      to="/item"
      className="text-xs uppercase tracking-widest border-b border-zinc-900 pb-1 hover:text-zinc-600 transition-colors"
    >
      View All
    </Link>
  </div>
  <div className="grid gap-x-6 gap-y-12 sm:grid-cols-2 lg:grid-cols-3">
    {recommendedProducts.map((product) => (
      <ProductCard key={product.id} product={product} />
    ))}
  </div>
</section>
```

**主な変更点:**
1. パディング: `px-4 py-12` → `px-6 py-24` (余白2倍)
2. ヘッダー余白: `mb-6` → `mb-12`
3. アライメント: `items-center` → `items-end`
4. 見出し: `text-2xl font-bold text-gray-900` → `font-serif text-3xl text-zinc-900` (セリフ体、太字削除)
5. リンク: `text-sm font-medium text-blue-600 hover:underline` → `text-xs uppercase tracking-widest border-b border-zinc-900 pb-1 hover:text-zinc-600 transition-colors` (大文字、下線、青色廃止)
6. グリッドギャップ: `gap-6` → `gap-x-6 gap-y-12` (縦方向のギャップ拡大)

---

## 参考実装
`docs/01_requirements/sample.html` の 30-44行目（バナーセクション）:
```html
<section class="py-24 px-6 max-w-7xl mx-auto grid md:grid-cols-12 gap-12 items-center">
    <div class="md:col-span-5 space-y-8">
        <span class="text-xs uppercase tracking-[0.3em] text-zinc-500">2026 Spring Collection</span>
        <h2 class="serif text-5xl md:text-7xl leading-tight text-zinc-900">The Art of <br>Simplicity</h2>
        <p class="text-zinc-600 leading-relaxed max-w-sm font-light">
            細部に宿る美しさと、選び抜かれた素材。日常を少しだけ特別にする、現代のスタンダード。
        </p>
    </div>
</section>
```

`docs/01_requirements/sample.html` の 46-50行目（セクション見出し）:
```html
<section class="py-24 px-6 max-w-7xl mx-auto">
    <div class="flex justify-between items-end mb-12">
        <h3 class="serif text-3xl">New Arrivals</h3>
        <a href="#" class="text-xs uppercase tracking-widest border-b border-zinc-900 pb-1">View All</a>
    </div>
</section>
```

---

## 確認事項
- [ ] バナー背景が黒（bg-zinc-900）になっている
- [ ] バナーにラベル「2026 Spring Collection」が追加されている
- [ ] バナー見出しがセリフ体で大きく表示されている
- [ ] バナー本文が細字（font-light）で表示されている
- [ ] セクション余白が py-24 に拡大されている
- [ ] セクション見出しがセリフ体になっている
- [ ] 「すべて見る」リンクが「View All」に変更され、大文字・下線になっている
- [ ] 青色が廃止されている
- [ ] グリッドの縦方向ギャップが拡大されている

---

## 次のタスク
Task5: ItemListPage.tsx 改善
