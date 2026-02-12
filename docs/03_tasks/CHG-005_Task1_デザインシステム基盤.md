# CHG-005 Task1: デザインシステム基盤整備

## 検証コマンド
```bash
cd frontend
npm run dev
# ブラウザで http://localhost:5173 を開き、フォントとカラーが適用されていることを確認
```

## 目的
Google Fonts を追加し、Tailwind CSS 4 のカスタムテーマを設定する。エディトリアルデザインの基盤となるフォントとカラーパレットを整備する。

## 変更対象ファイル
1. `frontend/index.html`
2. `frontend/src/index.css`

---

## 変更内容

### 1. Google Fonts の追加

**ファイル:** `frontend/index.html`

**挿入位置:** `<head>` タグ内、`<meta name="viewport">` の直後

**変更内容:**
```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@600&family=Inter:wght@300;400;500&display=swap" rel="stylesheet" />
    <title>frontend</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

**追加する行:**
```html
<link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@600&family=Inter:wght@300;400;500&display=swap" rel="stylesheet" />
```

---

### 2. Tailwind CSS 4 カスタムテーマ設定

**ファイル:** `frontend/src/index.css`

**現在の内容:**
```css
@import "tailwindcss";
```

**変更後の内容:**
```css
@import "tailwindcss";

@theme {
  /* カラーパレット */
  --color-brand-primary: #18181B;
  --color-brand-secondary: #71717A;
  --color-brand-accent: #E7E5E4;
  --color-brand-base: #F5F5F4;
  --color-brand-light: #A8A29E;

  /* フォントファミリー */
  --font-family-serif: 'Playfair Display', 'Noto Serif JP', serif;
  --font-family-sans: 'Inter', sans-serif;
}

/* ベーススタイル */
body {
  font-family: var(--font-family-sans);
  background-color: var(--color-brand-base);
  color: var(--color-brand-primary);
}
```

---

## 参考実装
`docs/01_requirements/sample.html` の 8-11行目:
```html
<style>
    body { font-family: 'Inter', sans-serif; background-color: #F5F5F4; color: #18181B; }
    .serif { font-family: 'Playfair Display', serif; }
</style>
```

---

## 確認事項
- [ ] Google Fonts が正しく読み込まれている（DevTools の Network タブで確認）
- [ ] body 要素のフォントが Inter に変更されている
- [ ] body 要素の背景色が #F5F5F4 (stone-50相当) になっている
- [ ] セリフ体クラス (`font-serif`) が使用可能になっている
- [ ] カスタムカラー変数が定義されている

---

## 次のタスク
Task2: Layout.tsx 改善（透過ヘッダー、ロゴ中央配置）
