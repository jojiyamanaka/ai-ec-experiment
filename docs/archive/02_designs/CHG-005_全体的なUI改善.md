# CHG-005: 全体的なUI改善（エディトリアルデザイン化） - 技術設計

## 1. デザインシステム設計

### 1.1 カラーパレット

既存の `blue-600` などのシステム色を廃止し、モノトーン＋ニュアンスカラーに統一する。

**ベースカラー（Tailwind CSS 4 カスタム設定）:**
```css
@theme {
  --color-brand-primary: #18181B;    /* zinc-900 相当 - メインテキスト、ボタン */
  --color-brand-secondary: #71717A;  /* zinc-500 相当 - 補助テキスト */
  --color-brand-accent: #E7E5E4;     /* stone-200 相当 - 境界線 */
  --color-brand-base: #F5F5F4;       /* stone-50 相当 - 背景 */
  --color-brand-light: #A8A29E;      /* stone-400 相当 - アイコン */
}
```

**廃止する色:**
- `blue-600`, `blue-500`, `blue-700` などの青系色
- `red-500` などの鮮やかな色（在庫バッジは `zinc-500`, `stone-400` で代替）

### 1.2 タイポグラフィ

**フォントファミリー:**
- **見出し（Heading）**: `'Playfair Display', 'Noto Serif JP', serif`
- **本文（Body）**: `'Inter', sans-serif`

**Google Fonts読み込み（index.html）:**
```html
<link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@600&family=Inter:wght@300;400;500&display=swap" rel="stylesheet">
```

**Tailwind CSS 4 カスタムフォント設定（index.css）:**
```css
@theme {
  --font-family-serif: 'Playfair Display', 'Noto Serif JP', serif;
  --font-family-sans: 'Inter', sans-serif;
}
```

**文字間隔（Letter Spacing）:**
- 見出し: `tracking-wider` (0.05em) または `tracking-widest` (0.1em)
- ラベル・小文字: `tracking-[0.2em]` または `tracking-[0.3em]`

**フォントウェイト:**
- 見出し: `font-semibold` (600)
- 本文: `font-light` (300) または `font-normal` (400)
- 価格: `font-normal` (400) - 太字を避ける

### 1.3 スペーシング

**セクション間余白:**
- 縦方向: `py-24` (6rem / 96px) 以上を基本とする
- 横方向: `px-6` (1.5rem / 24px) を基本とする

**グリッドギャップ:**
- 商品カード間: `gap-6` (1.5rem / 24px) または `gap-x-6 gap-y-12`

### 1.4 コンポーネント設計原則

**商品カード:**
- アスペクト比: `aspect-[3/4]` (縦長)
- 背景: `bg-stone-200` (ニュアンスカラー)
- 影: なし（`shadow-*` を使用しない）
- 境界線: なし、または `border border-stone-200` (1px の薄い線)
- 角丸: `rounded-none` または `rounded-sm`

**ホバーアニメーション:**
- 画像スケール: `group-hover:scale-105`
- トランジション時間: `duration-700` (0.7秒)

**ボタン:**
- 背景: `bg-zinc-900`
- テキスト: `text-white`
- 文字間隔: `tracking-[0.2em]`
- パディング: `px-12 py-4`

## 2. 実装方針

### 2.1 基盤整備

**ファイル: `frontend/index.html`**
- `<head>` 内に Google Fonts のリンクを追加

**ファイル: `frontend/src/index.css`**
- `@theme` ディレクティブでカスタムカラーとフォントを定義

### 2.2 共通レイアウト

**ファイル: `frontend/src/components/Layout.tsx`**

変更内容:
- ヘッダーを透過デザインに変更 (`bg-white/80 backdrop-blur-md`)
- ヘッダーを固定配置に変更 (`fixed`)
- ロゴを中央配置、左右にナビゲーション配置
- ロゴにセリフ体と `tracking-[0.2em]` を適用
- カートアイコンをシンプル化、バッジ色を `bg-zinc-900` に変更
- フッター背景を `bg-stone-50` に変更

### 2.3 商品カード

**ファイル: `frontend/src/components/ProductCard.tsx`**

変更内容:
- 画像アスペクト比を `aspect-[4/3]` から `aspect-[3/4]` に変更
- 背景色を `bg-stone-200` に変更
- 影を削除（`shadow-sm`, `hover:shadow-md` を削除）
- 境界線を薄くするか削除
- 商品名にセリフ体と `uppercase tracking-wider` を適用
- 価格の太字を削除し、色を `text-zinc-500` に変更、サイズを `text-xs` に縮小
- 在庫バッジの色を `zinc-*` / `stone-*` 系に変更
- ホバーアニメーションを `duration-700` に変更

### 2.4 ホームページ

**ファイル: `frontend/src/pages/HomePage.tsx`**

変更内容:
- バナー背景を `bg-gradient-to-r from-blue-500 to-purple-600` から単色 `bg-zinc-900` または画像背景に変更
- 見出しにセリフ体と `tracking-wider` を適用
- セクション余白を `py-24` 以上に拡大
- 見出しにセリフ体を適用

### 2.5 商品一覧ページ

**ファイル: `frontend/src/pages/ItemListPage.tsx`**

変更内容:
- 背景を `bg-stone-50` に変更（必要に応じて）
- 見出しにセリフ体を適用
- セクション余白を `py-24` に拡大
- グリッドギャップを `gap-x-6 gap-y-12` に変更

### 2.6 商品詳細ページ

**ファイル: `frontend/src/pages/ItemDetailPage.tsx`**

変更内容:
- 画像アスペクト比を `aspect-[4/3]` から `aspect-[3/4]` に変更
- 見出しにセリフ体を適用
- 価格の太字を削除し、色を `text-zinc-900` に変更
- ボタン背景を `bg-blue-600` から `bg-zinc-900` に変更
- ボタンに `tracking-[0.2em]` を適用
- セクション余白を拡大

### 2.7 カートページ

**ファイル: `frontend/src/pages/CartPage.tsx`**

変更内容:
- 背景を `bg-stone-50` に変更（必要に応じて）
- 見出しにセリフ体を適用
- 商品カードの影を削除または薄く変更
- ボタン背景を `bg-blue-600` から `bg-zinc-900` に変更
- 価格表示の太字を削除または軽減

## 3. 変更対象ファイル一覧

### フェーズ1: デザインシステム基盤
1. `frontend/index.html` - Google Fonts 追加
2. `frontend/src/index.css` - Tailwind カスタムテーマ設定

### フェーズ2: 共通コンポーネント
3. `frontend/src/components/Layout.tsx` - ヘッダー・フッター改善
4. `frontend/src/components/ProductCard.tsx` - 商品カード改善

### フェーズ3: ページコンポーネント
5. `frontend/src/pages/HomePage.tsx` - ホームページ改善
6. `frontend/src/pages/ItemListPage.tsx` - 商品一覧改善
7. `frontend/src/pages/ItemDetailPage.tsx` - 商品詳細改善
8. `frontend/src/pages/CartPage.tsx` - カート改善

### その他（必要に応じて）
9. `frontend/src/pages/OrderConfirmPage.tsx` - 注文確認改善
10. `frontend/src/pages/OrderCompletePage.tsx` - 注文完了改善

## 4. 既存パターンとの整合性

- **React 19 + Vite + Tailwind CSS 4** の構成を維持
- **Context API** (`ProductContext`, `CartContext`) は変更なし
- **React Router v7** のルーティング構造は維持
- **API層** (`src/lib/api.ts`) は変更なし
- **型定義** (`src/types/api.ts`) は変更なし

## 5. 実装の優先順位

### 高優先度（ビジュアルインパクト大）
1. デザインシステム基盤（index.html, index.css）
2. ProductCard - 商品カードの縦長化・色変更
3. Layout - ヘッダーの透過・ロゴ中央配置

### 中優先度
4. HomePage - バナー・見出しの改善
5. ItemListPage - 商品一覧の改善
6. ItemDetailPage - 商品詳細の改善

### 低優先度
7. CartPage - カートの改善
8. その他ページ - 注文確認・完了ページの改善

## 6. 参考実装

`docs/01_requirements/sample.html` を参考にした主要な実装パターン:

**透過ヘッダー:**
```tsx
<header className="fixed w-full z-50 bg-white/80 backdrop-blur-md border-b border-stone-200">
```

**セリフ体見出し:**
```tsx
<h1 className="font-serif text-2xl tracking-[0.2em]">
```

**商品カード:**
```tsx
<div className="aspect-[3/4] bg-stone-200 overflow-hidden">
  <img className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700" />
</div>
```

**ボタン:**
```tsx
<button className="bg-zinc-900 text-white px-12 py-4 text-xs tracking-[0.2em] hover:bg-zinc-800">
```

## 7. 注意事項

- すべての変更はフロントエンドのみで、バックエンドAPIは変更不要
- 既存の機能（カート追加、在庫管理など）は維持する
- レスポンシブデザインは既存のブレークポイント（sm, md, lg, xl）を維持する
- アクセシビリティ（aria-label など）は既存のものを維持する
