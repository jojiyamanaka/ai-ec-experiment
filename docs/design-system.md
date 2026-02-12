# デザインシステム仕様書

作成日: 2026-02-12
バージョン: 1.0

## 概要

本仕様書は、AI EC Experimentのデザインシステムを定義する。
エディトリアルデザインコンセプトに基づき、モノトーン＋ニュアンスカラー、セリフ体タイポグラフィ、広い余白による高級感と洗練性を実現する。

**関連変更**: CHG-005（全体的なUI改善 - エディトリアルデザイン化）

---

## 1. デザインコンセプト

### 1-1. 基本方針

**エディトリアルデザイン**: 雑誌や書籍のような洗練されたビジュアルデザイン
- **視覚的なノイズの排除**: 鮮やかな色、過度な影、装飾を避ける
- **高級感の演出**: セリフ体、広い余白、モノトーンカラーで品位を保つ
- **読みやすさ重視**: 適切な文字間隔、軽量フォント、明確な階層構造

### 1-2. 設計原則

1. **Less is More**: 最小限の要素で最大の効果を
2. **Typography First**: タイポグラフィを主役に
3. **Whitespace as Design**: 余白をデザイン要素として活用
4. **Subtle Interactions**: 控えめで洗練されたアニメーション
5. **Consistency**: 全画面で統一されたルールを適用

---

## 2. カラーパレット

### 2-1. ベースカラー

**Tailwind CSS 4 カスタムテーマ設定** (`frontend/src/index.css`):
```css
@theme {
  --color-brand-primary: #18181B;    /* zinc-900 */
  --color-brand-secondary: #71717A;  /* zinc-500 */
  --color-brand-accent: #E7E5E4;     /* stone-200 */
  --color-brand-base: #F5F5F4;       /* stone-50 */
  --color-brand-light: #A8A29E;      /* stone-400 */
}
```

### 2-2. カラー使用ガイドライン

| 用途 | カラー | Tailwindクラス | 説明 |
|------|--------|---------------|------|
| メインテキスト | #18181B | `text-zinc-900` | 見出し、ボタンテキスト、重要な情報 |
| 補助テキスト | #71717A | `text-zinc-500` | 説明文、補足情報、ラベル |
| 境界線 | #E7E5E4 | `border-stone-200` | カード、セクション、入力欄の枠線 |
| 背景（ベース） | #FFFFFF | `bg-white` | メインコンテンツ背景 |
| 背景（セカンダリ） | #F5F5F4 | `bg-stone-50` | セクション背景、フッター |
| 商品カード背景 | #E7E5E4 | `bg-stone-200` | 商品画像のプレースホルダー背景 |
| アイコン | #A8A29E | `text-stone-400` | 装飾アイコン、非活性状態 |

### 2-3. 状態別カラー

#### 在庫バッジ
| 状態 | カラー | Tailwindクラス | 説明 |
|------|--------|---------------|------|
| 在庫あり | #3F3F46 | `bg-zinc-700 text-white` | 6個以上 |
| 残りわずか | #71717A | `bg-zinc-500 text-white` | 1〜5個 |
| 売り切れ | #A1A1AA | `bg-zinc-400 text-white` | 0個 |

#### ボタン
| 状態 | カラー | Tailwindクラス |
|------|--------|---------------|
| デフォルト | #18181B | `bg-zinc-900 text-white` |
| ホバー | #27272A | `hover:bg-zinc-800` |
| 無効 | #A1A1AA | `bg-zinc-400 text-white` |

### 2-4. 廃止した色

以下の色はエディトリアルデザイン化に伴い廃止:
- `blue-*` 系（`blue-500`, `blue-600`, `blue-700`など）
- `red-*` 系（`red-500`など）
- `green-*` 系（`green-500`など）
- `orange-*` 系（`orange-500`など）

---

## 3. タイポグラフィ

### 3-1. フォントファミリー

**Google Fonts 読み込み** (`frontend/index.html`):
```html
<link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@600&family=Inter:wght@300;400;500&display=swap" rel="stylesheet">
```

**Tailwind CSS 4 カスタムフォント** (`frontend/src/index.css`):
```css
@theme {
  --font-family-serif: 'Playfair Display', 'Noto Serif JP', serif;
  --font-family-sans: 'Inter', sans-serif;
}
```

#### 見出し（Heading）
- **フォント**: `font-serif` (Playfair Display)
- **用途**: ページタイトル、セクション見出し、ロゴ
- **特徴**: 伝統的なセリフ体、エレガントで格式高い印象

#### 本文（Body）
- **フォント**: `font-sans` (Inter)
- **用途**: 本文、説明文、ボタンラベル、フォーム入力
- **特徴**: モダンなサンセリフ体、高い可読性

### 3-2. タイポグラフィスケール

| レベル | サイズ | Tailwindクラス | 用途 | フォント | ウェイト |
|--------|--------|---------------|------|----------|----------|
| H1 | 30px | `text-3xl` | ページタイトル | serif | semibold (600) |
| H2 | 24px | `text-2xl` | セクション見出し | serif | semibold (600) |
| H3 | 20px | `text-xl` | サブセクション見出し | serif | semibold (600) |
| Body Large | 18px | `text-lg` | 強調テキスト | sans | normal (400) |
| Body | 16px | `text-base` | 本文 | sans | light (300) |
| Body Small | 14px | `text-sm` | 補足情報 | sans | light (300) |
| Caption | 12px | `text-xs` | ラベル、価格 | sans | normal (400) |

### 3-3. 文字間隔（Letter Spacing）

| 用途 | Tailwindクラス | 値 | 使用場面 |
|------|---------------|-----|----------|
| 見出し | `tracking-wider` | 0.05em | セリフ体見出し |
| 大文字ラベル | `tracking-[0.2em]` | 0.2em | ボタン、ナビゲーション |
| 特別な強調 | `tracking-[0.3em]` | 0.3em | ロゴ、キャッチコピー |
| 通常 | (デフォルト) | 0em | 本文、説明文 |

**組み合わせ例**:
```tsx
// 見出し
<h1 className="font-serif text-3xl tracking-wider">
  AIがおすすめする商品
</h1>

// ボタンラベル（大文字）
<button className="font-sans text-xs uppercase tracking-[0.2em]">
  カートに追加
</button>

// 商品名（セリフ体、大文字、文字間隔広め）
<h3 className="font-serif text-sm uppercase tracking-wider">
  Premium Coffee Beans
</h3>
```

### 3-4. 行間（Line Height）

| 用途 | Tailwindクラス | 値 |
|------|---------------|-----|
| 見出し | `leading-tight` | 1.25 |
| 本文 | `leading-relaxed` | 1.625 |
| 補足 | `leading-normal` | 1.5 |

---

## 4. スペーシングシステム

### 4-1. セクション間余白

| 用途 | Tailwindクラス | ピクセル値 | 使用場面 |
|------|---------------|-----------|----------|
| 大セクション | `py-24` | 96px | ページセクション間 |
| 中セクション | `py-16` | 64px | サブセクション間 |
| 小セクション | `py-12` | 48px | カード内のセクション間 |

**縦方向の基本原則**: `py-24` (96px) 以上を基本とし、広い余白でゆとりのあるレイアウトを実現

### 4-2. 要素間余白

| 用途 | Tailwindクラス | ピクセル値 |
|------|---------------|-----------|
| 見出し→本文 | `mt-2`, `mt-4` | 8px, 16px |
| パラグラフ間 | `mb-4`, `mb-6` | 16px, 24px |
| フォーム要素間 | `mb-4` | 16px |
| ボタン間 | `gap-4` | 16px |

### 4-3. グリッドギャップ

| 用途 | Tailwindクラス | ピクセル値 | 説明 |
|------|---------------|-----------|------|
| 商品カード（縦横同じ） | `gap-6` | 24px | 均等な間隔 |
| 商品カード（縦横異なる） | `gap-x-6 gap-y-12` | 横24px, 縦48px | 縦長カードで縦を広く |

### 4-4. パディング

| 用途 | Tailwindクラス | ピクセル値 |
|------|---------------|-----------|
| ページ全体（横） | `px-6` | 24px |
| カード内 | `p-6` | 24px |
| ボタン | `px-12 py-4` | 横48px, 縦16px |
| 入力欄 | `px-4 py-3` | 横16px, 縦12px |

---

## 5. コンポーネント仕様

### 5-1. 商品カード

**デザイン原則**: 縦長フォーマット、影なし、ミニマルなデザイン

```tsx
<div className="group">
  {/* 画像エリア */}
  <div className="aspect-[3/4] bg-stone-200 overflow-hidden">
    <img
      src={product.image}
      alt={product.name}
      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
    />
  </div>

  {/* 商品情報 */}
  <div className="mt-4 space-y-1">
    <h3 className="font-serif text-sm uppercase tracking-wider text-zinc-900">
      {product.name}
    </h3>
    <p className="text-xs text-zinc-500">
      ¥{product.price.toLocaleString()}
    </p>
  </div>
</div>
```

**仕様**:
- アスペクト比: `aspect-[3/4]` (縦長)
- 背景: `bg-stone-200`
- 影: なし
- 境界線: なし、または `border border-stone-200`
- 角丸: `rounded-none` または `rounded-sm`
- ホバーアニメーション: 画像スケール `group-hover:scale-105`, `duration-700`

### 5-2. ボタン

**プライマリボタン**:
```tsx
<button className="bg-zinc-900 text-white px-12 py-4 text-xs tracking-[0.2em] uppercase hover:bg-zinc-800 transition-colors">
  カートに追加
</button>
```

**セカンダリボタン**:
```tsx
<button className="border border-stone-300 bg-white text-zinc-700 px-12 py-4 text-xs tracking-[0.2em] uppercase hover:bg-stone-50 transition-colors">
  買い物を続ける
</button>
```

**仕様**:
- 背景: `bg-zinc-900` (プライマリ), `bg-white border` (セカンダリ)
- テキスト: `text-xs uppercase tracking-[0.2em]`
- パディング: `px-12 py-4`
- ホバー: `hover:bg-zinc-800` (プライマリ), `hover:bg-stone-50` (セカンダリ)
- トランジション: `transition-colors`
- 角丸: 最小限（デフォルトまたは `rounded-sm`）

### 5-3. ヘッダー

**固定ヘッダー（透過・ぼかし効果）**:
```tsx
<header className="fixed top-0 w-full z-50 bg-white/80 backdrop-blur-md border-b border-stone-200">
  <div className="mx-auto max-w-7xl px-6 py-4 flex items-center justify-between">
    {/* ロゴ（中央配置の場合） */}
    <h1 className="font-serif text-2xl tracking-[0.2em] text-zinc-900">
      AI EC
    </h1>

    {/* ナビゲーション */}
    <nav className="flex items-center gap-8">
      <a href="/" className="text-sm tracking-wider text-zinc-700 hover:text-zinc-900">
        HOME
      </a>
      <a href="/item" className="text-sm tracking-wider text-zinc-700 hover:text-zinc-900">
        SHOP
      </a>
    </nav>

    {/* カートアイコン */}
    <div className="relative">
      <ShoppingCart className="h-5 w-5 text-zinc-700" />
      <span className="absolute -top-2 -right-2 bg-zinc-900 text-white text-xs w-5 h-5 flex items-center justify-center rounded-full">
        3
      </span>
    </div>
  </div>
</header>
```

**仕様**:
- 配置: `fixed top-0 w-full z-50`
- 背景: `bg-white/80 backdrop-blur-md` (透過80%、ぼかし効果)
- 境界線: `border-b border-stone-200`
- ロゴ: セリフ体、`tracking-[0.2em]`
- ナビゲーション: `text-sm tracking-wider`

### 5-4. フッター

```tsx
<footer className="bg-stone-50 border-t border-stone-200 py-12">
  <div className="mx-auto max-w-7xl px-6">
    <div className="flex justify-between items-center">
      <p className="text-xs text-zinc-500">
        © 2026 AI EC. All rights reserved.
      </p>
      <a href="/bo/item" className="text-xs tracking-wider text-zinc-700 hover:text-zinc-900">
        管理画面
      </a>
    </div>
  </div>
</footer>
```

**仕様**:
- 背景: `bg-stone-50`
- 境界線: `border-t border-stone-200`
- テキスト: `text-xs text-zinc-500`

### 5-5. 入力フォーム

```tsx
<input
  type="text"
  className="w-full border border-stone-300 px-4 py-3 text-sm focus:outline-none focus:border-zinc-900 transition-colors"
  placeholder="メールアドレス"
/>
```

**仕様**:
- 境界線: `border border-stone-300`
- パディング: `px-4 py-3`
- フォーカス: `focus:border-zinc-900`
- 角丸: 最小限（デフォルト）

---

## 6. アニメーション・トランジション

### 6-1. ホバーアニメーション

| 対象 | アニメーション | Tailwindクラス | 時間 |
|------|---------------|---------------|------|
| 商品カード画像 | スケール拡大 | `group-hover:scale-105` | 0.7秒 |
| ボタン | 背景色変化 | `hover:bg-zinc-800` | デフォルト |
| リンク | テキスト色変化 | `hover:text-zinc-900` | デフォルト |

**トランジション指定**:
- 画像スケール: `transition-transform duration-700`
- 色変化: `transition-colors` (duration-200がデフォルト)

### 6-2. 原則

- **控えめに**: 派手なアニメーションは避ける
- **スムーズに**: イージング関数はデフォルト（ease）
- **長めに**: 画像スケールなど目立つアニメーションは0.7秒と長めに設定し、優雅な印象を与える

---

## 7. レスポンシブデザイン

### 7-1. ブレークポイント

Tailwind CSS のデフォルトブレークポイントを使用:

| 名称 | プレフィックス | 幅 | デバイス |
|------|---------------|-----|----------|
| モバイル | (なし) | < 640px | スマートフォン |
| タブレット | `sm:` | ≥ 640px | タブレット縦 |
| デスクトップ（小） | `md:` | ≥ 768px | タブレット横、小型PC |
| デスクトップ（中） | `lg:` | ≥ 1024px | PC |
| デスクトップ（大） | `xl:` | ≥ 1280px | 大型PC |

### 7-2. グリッドレイアウト

**商品一覧**:
```tsx
<div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
  {/* 商品カード */}
</div>
```

| デバイス | カラム数 | Tailwindクラス |
|---------|---------|---------------|
| モバイル | 1 | `grid-cols-1` |
| タブレット | 2 | `sm:grid-cols-2` |
| PC | 3 | `md:grid-cols-3` |

### 7-3. 余白調整

| 用途 | モバイル | デスクトップ |
|------|---------|-------------|
| ページ余白（横） | `px-6` (24px) | `px-6` (24px) |
| セクション余白（縦） | `py-16` (64px) | `py-24` (96px) |

**例**:
```tsx
<section className="py-16 md:py-24 px-6">
  {/* コンテンツ */}
</section>
```

---

## 8. 実装ガイドライン

### 8-1. セットアップ手順

1. **Google Fonts 読み込み** (`frontend/index.html`):
```html
<link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@600&family=Inter:wght@300;400;500&display=swap" rel="stylesheet">
```

2. **Tailwind CSS カスタムテーマ** (`frontend/src/index.css`):
```css
@theme {
  --color-brand-primary: #18181B;
  --color-brand-secondary: #71717A;
  --color-brand-accent: #E7E5E4;
  --color-brand-base: #F5F5F4;
  --color-brand-light: #A8A29E;

  --font-family-serif: 'Playfair Display', 'Noto Serif JP', serif;
  --font-family-sans: 'Inter', sans-serif;
}
```

### 8-2. コンポーネント実装チェックリスト

新しいコンポーネントを実装する際のチェックリスト:

- [ ] カラーは zinc / stone 系のみ使用
- [ ] 見出しにはセリフ体（`font-serif`）を使用
- [ ] 本文にはサンセリフ体（`font-sans`）を使用
- [ ] 文字間隔（`tracking-*`）を適切に設定
- [ ] 余白は十分に確保（`py-24` 以上を基本）
- [ ] 影（`shadow-*`）は使用しない
- [ ] ボタンは `bg-zinc-900` を使用
- [ ] ホバーアニメーションは控えめに（0.7秒、スケール1.05程度）
- [ ] レスポンシブ対応を確認（sm:, md: ブレークポイント）

### 8-3. 既存コンポーネントの移行

既存コンポーネントをエディトリアルデザインに移行する手順:

1. **カラー置換**:
   - `blue-*` → `zinc-900`
   - `green-*` / `orange-*` / `red-*` → `zinc-*` / `stone-*`
   - `gray-*` → `zinc-*` / `stone-*`

2. **タイポグラフィ更新**:
   - 見出しに `font-serif` を追加
   - `tracking-wider` または `tracking-[0.2em]` を追加
   - 太字（`font-bold`）を `font-semibold` に軽減

3. **スペーシング拡大**:
   - `py-8` → `py-16` または `py-24`
   - セクション間余白を2倍程度に拡大

4. **影の削除**:
   - `shadow-sm`, `shadow-md`, `shadow-lg` を削除
   - 必要に応じて `border border-stone-200` で代替

5. **アニメーション調整**:
   - `duration-300` → `duration-700`（画像スケール等）
   - スケール値を控えめに（`scale-110` → `scale-105`）

---

## 9. デザイントークン（参考）

将来的にデザイントークンとして管理する場合の定義:

```json
{
  "color": {
    "primary": "#18181B",
    "secondary": "#71717A",
    "accent": "#E7E5E4",
    "base": "#F5F5F4",
    "light": "#A8A29E"
  },
  "font": {
    "family": {
      "serif": "'Playfair Display', 'Noto Serif JP', serif",
      "sans": "'Inter', sans-serif"
    },
    "weight": {
      "light": 300,
      "normal": 400,
      "semibold": 600
    }
  },
  "spacing": {
    "section": "96px",
    "subsection": "64px",
    "element": "24px"
  },
  "animation": {
    "duration": {
      "fast": "200ms",
      "normal": "300ms",
      "slow": "700ms"
    }
  }
}
```

---

## 関連ドキュメント

- **UI仕様**: [ui/customer-ui.md](./ui/customer-ui.md) - UI/UX設計思想セクション
- **技術仕様**: [SPEC.md](./SPEC.md) - フロントエンド技術スタック
- **要件定義**: [01_requirements/CHG-005_全体的なUI改善.md](./01_requirements/CHG-005_全体的なUI改善.md)
- **設計**: [02_designs/CHG-005_全体的なUI改善.md](./02_designs/CHG-005_全体的なUI改善.md)
