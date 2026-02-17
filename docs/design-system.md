# デザインシステム仕様書

## 概要

AI EC Experimentのデザインシステム。エディトリアルデザインコンセプトに基づき、モノトーン＋ニュアンスカラー、セリフ体タイポグラフィ、広い余白による高級感と洗練性を実現する。

---

## 1. デザインコンセプト

**エディトリアルデザイン**: 雑誌や書籍のような洗練されたビジュアルデザイン

**設計原則**:
1. **Less is More**: 最小限の要素で最大の効果
2. **Typography First**: タイポグラフィを主役に
3. **Whitespace as Design**: 余白をデザイン要素として活用
4. **Subtle Interactions**: 控えめで洗練されたアニメーション
5. **Consistency**: 全画面で統一されたルール

---

## 2. カラーパレット

### ベースカラー（Tailwind CSS 4 カスタムテーマ `frontend/src/index.css`）

| 用途 | カラー | CSS変数 | Tailwindクラス |
|------|--------|---------|---------------|
| メインテキスト・ボタン | #18181B | `--color-brand-primary` | `text-zinc-900`, `bg-zinc-900` |
| 補助テキスト | #71717A | `--color-brand-secondary` | `text-zinc-500` |
| 境界線・商品カード背景 | #E7E5E4 | `--color-brand-accent` | `border-stone-200`, `bg-stone-200` |
| 背景（セカンダリ） | #F5F5F4 | `--color-brand-base` | `bg-stone-50` |
| アイコン | #A8A29E | `--color-brand-light` | `text-stone-400` |

### 状態別カラー

| 対象 | 状態 | Tailwindクラス |
|------|------|---------------|
| 在庫バッジ | 在庫あり(6+) | `bg-zinc-700 text-white` |
| 在庫バッジ | 残りわずか(1-5) | `bg-zinc-500 text-white` |
| 在庫バッジ | 売り切れ(0) | `bg-zinc-400 text-white` |
| ボタン | デフォルト | `bg-zinc-900 text-white` |
| ボタン | ホバー | `hover:bg-zinc-800` |
| ボタン | 無効 | `bg-zinc-400 text-white` |

**廃止した色**: `blue-*`, `red-*`, `green-*`, `orange-*` 系

---

## 3. タイポグラフィ

### フォントファミリー（`frontend/src/index.css`）

| 用途 | フォント | CSS変数 | Tailwindクラス |
|------|---------|---------|---------------|
| 見出し | Playfair Display | `--font-family-serif` | `font-serif` |
| 本文 | Inter | `--font-family-sans` | `font-sans` |

Google Fonts: `Playfair+Display:wght@600` + `Inter:wght@300;400;500`

### タイポグラフィスケール

| レベル | Tailwind | フォント | ウェイト | 用途 |
|--------|---------|---------|---------|------|
| H1 | `text-3xl` | serif | semibold(600) | ページタイトル |
| H2 | `text-2xl` | serif | semibold(600) | セクション見出し |
| H3 | `text-xl` | serif | semibold(600) | サブセクション |
| Body | `text-base` | sans | light(300) | 本文 |
| Small | `text-sm` | sans | light(300) | 補足情報 |
| Caption | `text-xs` | sans | normal(400) | ラベル・価格 |

### 文字間隔

| 用途 | Tailwindクラス |
|------|---------------|
| 見出し | `tracking-wider` (0.05em) |
| ボタン・ナビ | `tracking-[0.2em]` |
| ロゴ・キャッチ | `tracking-[0.3em]` |
| 本文 | デフォルト |

---

## 4. スペーシング

| 用途 | Tailwindクラス | px |
|------|---------------|-----|
| 大セクション間 | `py-24` | 96px |
| 中セクション間 | `py-16` | 64px |
| ページ余白（横） | `px-6` | 24px |
| 商品カード間 | `gap-6` or `gap-x-6 gap-y-12` | 24px / 24x48px |
| ボタン | `px-12 py-4` | 48x16px |
| 入力欄 | `px-4 py-3` | 16x12px |

---

## 5. コンポーネント仕様

### 商品カード

- アスペクト比: `aspect-[3/4]`（縦長）、背景: `bg-stone-200`
- 影なし、角丸なし（`rounded-none` or `rounded-sm`）
- ホバー: 画像スケール `group-hover:scale-105`, `duration-700`
- 商品名: `font-serif text-sm uppercase tracking-wider`
- 価格: `text-xs text-zinc-500`

### ボタン

- プライマリ: `bg-zinc-900 text-white text-xs uppercase tracking-[0.2em] px-12 py-4 hover:bg-zinc-800 transition-colors`
- セカンダリ: `border border-stone-300 bg-white text-zinc-700 text-xs uppercase tracking-[0.2em] px-12 py-4 hover:bg-stone-50 transition-colors`

### ヘッダー

- `fixed top-0 w-full z-50 bg-white/80 backdrop-blur-md border-b border-stone-200`
- ロゴ: `font-serif text-2xl tracking-[0.2em]`
- ナビ: `text-sm tracking-wider`

### フッター

- `bg-stone-50 border-t border-stone-200 py-12`
- テキスト: `text-xs text-zinc-500`

### 入力フォーム

- `border border-stone-300 px-4 py-3 text-sm focus:border-zinc-900 transition-colors`

---

## 6. アニメーション

| 対象 | アニメーション | Tailwindクラス |
|------|---------------|---------------|
| 商品カード画像 | スケール拡大 | `group-hover:scale-105 transition-transform duration-700` |
| ボタン | 背景色変化 | `transition-colors`（デフォルト200ms） |
| リンク | テキスト色変化 | `hover:text-zinc-900 transition-colors` |

**原則**: 控えめ・スムーズ・長め（画像スケールは0.7秒で優雅に）

---

## 7. レスポンシブデザイン

| デバイス | プレフィックス | 幅 | 商品グリッド |
|---------|---------------|-----|-------------|
| モバイル | (なし) | < 640px | 1列 |
| タブレット | `sm:` | ≥ 640px | 2列 |
| PC(小) | `md:` | ≥ 768px | 3列 |
| PC(中) | `lg:` | ≥ 1024px | 3列 |
| PC(大) | `xl:` | ≥ 1280px | 3列 |

---

## 8. 実装チェックリスト

新しいコンポーネント実装時:
- カラーは zinc / stone 系のみ使用
- 見出しに `font-serif`、本文に `font-sans` を使用
- 文字間隔（`tracking-*`）を適切に設定
- 余白は `py-24` 以上を基本
- 影（`shadow-*`）は使用しない
- レスポンシブ対応（sm:, md: ブレークポイント）を確認
