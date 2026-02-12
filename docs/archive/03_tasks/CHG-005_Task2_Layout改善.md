# CHG-005 Task2: Layout.tsx 改善（透過ヘッダー・ロゴ中央配置）

## 検証コマンド
```bash
cd frontend
npm run dev
# ブラウザで http://localhost:5173 を開き、以下を確認:
# - ヘッダーが透過で固定されている
# - ロゴが中央に配置されている
# - スクロール時にぼかし効果が見える
```

## 目的
ヘッダーを透過デザインに変更し、ロゴを中央配置する。ファッションECらしい洗練されたヘッダーに改善する。

## 変更対象ファイル
1. `frontend/src/components/Layout.tsx`

---

## 変更内容

### 1. ヘッダーの全面改修

**ファイル:** `frontend/src/components/Layout.tsx`

**変更前（8-40行目）:**
```tsx
<header className="bg-white shadow-sm">
  <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4">
    <Link to="/" className="text-2xl font-bold text-gray-900">
      AI EC Shop
    </Link>
    <Link
      to="/order/cart"
      className="relative flex items-center gap-2 rounded-lg px-4 py-2 hover:bg-gray-100"
    >
      {/* カートアイコンとバッジ */}
    </Link>
  </div>
</header>
```

**変更後:**
```tsx
<header className="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-md border-b border-stone-200">
  <div className="mx-auto flex max-w-7xl items-center justify-between px-6 h-20">
    {/* 左側ナビゲーション */}
    <nav className="hidden md:flex space-x-8 text-xs tracking-widest uppercase">
      <Link to="/item" className="hover:text-zinc-600 transition-colors">
        Collection
      </Link>
    </nav>

    {/* 中央ロゴ */}
    <Link to="/" className="font-serif text-2xl tracking-[0.2em] text-zinc-900">
      AI EC Shop
    </Link>

    {/* 右側ナビゲーション */}
    <div className="flex items-center space-x-6 text-xs uppercase tracking-widest">
      <Link
        to="/order/cart"
        className="relative hover:text-zinc-600 transition-colors"
      >
        Cart
        {totalQuantity > 0 && (
          <span className="absolute -right-3 -top-2 flex h-5 w-5 items-center justify-center rounded-full bg-zinc-900 text-xs font-bold text-white">
            {totalQuantity}
          </span>
        )}
      </Link>
    </div>
  </div>
</header>
```

---

### 2. メインコンテンツの調整

**ファイル:** `frontend/src/components/Layout.tsx`

**変更前（41-43行目）:**
```tsx
<main className="flex-1">
  <Outlet />
</main>
```

**変更後:**
```tsx
<main className="flex-1 pt-20">
  <Outlet />
</main>
```

**理由:** ヘッダーが `fixed` になったため、メインコンテンツに `pt-20` でヘッダー分の余白を追加

---

### 3. フッターの改善

**ファイル:** `frontend/src/components/Layout.tsx`

**変更前（44-58行目）:**
```tsx
<footer className="border-t border-gray-200 bg-white">
  <div className="mx-auto max-w-7xl px-4 py-6">
    <div className="flex items-center justify-between">
      <p className="text-sm text-gray-500">
        © 2025 AI EC Shop. All rights reserved.
      </p>
      <Link
        to="/bo/item"
        className="text-xs text-gray-400 hover:text-gray-600"
      >
        管理画面
      </Link>
    </div>
  </div>
</footer>
```

**変更後:**
```tsx
<footer className="border-t border-stone-200 bg-stone-50">
  <div className="mx-auto max-w-7xl px-6 py-6">
    <div className="flex items-center justify-between">
      <p className="text-sm text-zinc-500">
        © 2025 AI EC Shop. All rights reserved.
      </p>
      <Link
        to="/bo/item"
        className="text-xs text-zinc-400 hover:text-zinc-600"
      >
        管理画面
      </Link>
    </div>
  </div>
</footer>
```

**変更点:**
- `border-gray-200` → `border-stone-200`
- `bg-white` → `bg-stone-50`
- `text-gray-*` → `text-zinc-*`
- `px-4` → `px-6`

---

### 4. 背景色の調整

**ファイル:** `frontend/src/components/Layout.tsx`

**変更前（8行目）:**
```tsx
<div className="flex min-h-screen flex-col bg-gray-50">
```

**変更後:**
```tsx
<div className="flex min-h-screen flex-col bg-stone-50">
```

---

## 参考実装
`docs/01_requirements/sample.html` の 15-27行目:
```html
<header class="fixed w-full z-50 bg-white/80 backdrop-blur-md border-b border-stone-200">
    <div class="max-w-7xl mx-auto px-6 h-20 flex items-center justify-between">
        <nav class="hidden md:flex space-x-8 text-xs tracking-widest uppercase">
            <a href="#">Collection</a>
            <a href="#">About</a>
        </nav>
        <h1 class="serif text-2xl tracking-[0.2em]">ETHEREAL</h1>
        <div class="flex items-center space-x-6 text-xs uppercase tracking-widest">
            <a href="#">Search</a>
            <a href="#">Cart (0)</a>
        </div>
    </div>
</header>
```

---

## 確認事項
- [ ] ヘッダーが画面上部に固定されている
- [ ] ヘッダー背景が透過（白80%）でぼかし効果がある
- [ ] ロゴが中央に配置され、セリフ体で表示されている
- [ ] ロゴの文字間が広い（tracking-[0.2em]）
- [ ] 左側に「Collection」リンクがある（デスクトップのみ）
- [ ] カートバッジが黒背景（bg-zinc-900）になっている
- [ ] メインコンテンツがヘッダーに隠れていない
- [ ] フッターの色が stone/zinc 系に変更されている

---

## 次のタスク
Task3: ProductCard.tsx 改善（3:4縦長、セリフ体、色変更）
