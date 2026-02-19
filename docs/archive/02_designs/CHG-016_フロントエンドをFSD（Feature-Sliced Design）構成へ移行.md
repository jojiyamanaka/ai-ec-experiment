# CHG-016: フロントエンドをFSD（Feature-Sliced Design）構成へ移行 - 技術設計

要件: `docs/01_requirements/CHG-016_フロントエンドをFSD（Feature-Sliced Design）構成へ移行.md`
作成日: 2026-02-18

---

## 1. 設計方針

### 基本方針

- **一括移行**: ファイル移動と import パスの書き換えを一度に実施。段階移行より全体整合性が保ちやすく、プロジェクト規模（30ファイル程度）で十分扱える。
- **既存ロジックの保存**: ファイルを移動して import を書き直すのみ。Contextパターンや useEffect によるデータフェッチは維持し、ロジック変更はしない。
- **Public API（index.ts）**: 各スライスは `index.ts` のみを外部公開インターフェースとし、内部ファイルの直接 import を禁止する。
- **依存方向の強制**: `eslint-plugin-boundaries` を ESLint flat config に追加し、上位→下位の一方向依存を lint ルールで検出・防止する。

### FSDレイヤ採用範囲

```
src/
  app/        ← App.tsx（Provider + Router設定）
  pages/      ← 画面コンポーネント（customer/ と admin/ で分離）
  widgets/    ← 独立した複合UIブロック（Layout、Sidebar）
  features/   ← ユーザー操作（認証・カート操作・注文操作）
  entities/   ← ドメイン実体（型・API関数・最小UI）
  shared/     ← 共通基盤（APIクライアント・UI・型・utils）
```

依存方向: `app → pages → widgets → features → entities → shared`（上位から下位へのみ）

---

## 2. ディレクトリ設計

### 移行後の完全構造

```
frontend/src/
  main.tsx                        ← 変更なし
  index.css                       ← 変更なし

  app/
    App.tsx                       ← src/App.tsx を移動
    router/
      customer.tsx                ← 顧客向け Routes + ページ import を集約
      admin.tsx                   ← 管理向け Routes + ページ import を集約

  pages/
    customer/
      HomePage/
        index.tsx                 ← pages/HomePage.tsx を移動
      ItemListPage/
        index.tsx                 ← pages/ItemListPage.tsx を移動
      ItemDetailPage/
        index.tsx                 ← pages/ItemDetailPage.tsx を移動
      CartPage/
        index.tsx                 ← pages/CartPage.tsx を移動
      OrderConfirmPage/
        index.tsx                 ← pages/OrderConfirmPage.tsx を移動
      OrderCompletePage/
        index.tsx                 ← pages/OrderCompletePage.tsx を移動
      OrderDetailPage/
        index.tsx                 ← pages/OrderDetailPage.tsx を移動
      OrderHistoryPage/
        index.tsx                 ← pages/OrderHistoryPage.tsx を移動
      LoginPage/
        index.tsx                 ← pages/LoginPage.tsx を移動
      RegisterPage/
        index.tsx                 ← pages/RegisterPage.tsx を移動
    admin/
      AdminItemPage/
        index.tsx                 ← pages/AdminItemPage.tsx を移動
      AdminOrderPage/
        index.tsx                 ← pages/AdminOrderPage.tsx を移動
      AdminInventoryPage/
        index.tsx                 ← pages/AdminInventoryPage.tsx を移動
      AdminMembersPage/
        index.tsx                 ← pages/AdminMembersPage.tsx を移動
      BoLoginPage/
        index.tsx                 ← pages/BoLoginPage.tsx を移動

  widgets/
    CustomerLayout/
      CustomerLayout.tsx           ← components/Layout.tsx を移動（default export を維持）
      index.ts                     ← named export で公開: export { default as CustomerLayout }
    AdminLayout/
      AdminLayout.tsx              ← components/AdminLayout.tsx を移動
      index.ts
    AdminSidebar/
      AdminSidebar.tsx             ← components/AdminSidebar.tsx を移動
      index.ts

  features/
    auth/
      model/
        AuthContext.tsx            ← contexts/AuthContext.tsx を移動
      index.ts                     ← AuthProvider, useAuth を公開
    bo-auth/
      model/
        BoAuthContext.tsx          ← contexts/BoAuthContext.tsx を移動
      index.ts
    cart/
      model/
        CartContext.tsx            ← contexts/CartContext.tsx を移動
      index.ts                     ← CartProvider, useCart を公開

  entities/
    product/
      model/
        types.ts                  ← 商品関連型（Product, ProductListResponse, UpdateProductRequest）
        ProductContext.tsx         ← contexts/ProductContext.tsx を移動
        api.ts                    ← getItems, getItemById, updateItem
      ui/
        ProductCard.tsx            ← components/ProductCard.tsx を移動
      index.ts                     ← Product型, useProducts, ProductProvider, ProductCard を公開
    cart/
      model/
        types.ts                  ← Cart, CartItem, AddToCartRequest, UpdateQuantityRequest型
        api.ts                    ← getCart, addToCart, updateCartItemQuantity, removeFromCart
      index.ts
    order/
      model/
        types.ts                  ← Order, OrderItem, CreateOrderRequest型
        api.ts                    ← getOrderById, getOrderHistory, getAllOrders, createOrder, cancelOrder, confirmOrder, shipOrder, deliverOrder
      index.ts
    customer/
      model/
        types.ts                  ← User, AuthResponse, RegisterRequest, LoginRequest型
        api.ts                    ← register, login, logout, getCurrentUser, getSessionId
      index.ts
    bo-user/
      model/
        types.ts                  ← BoUser, BoAuthResponse, BoLoginRequest型
        api.ts                    ← boLogin, boLogout, getBoUser
      index.ts

  shared/
    api/
      client.ts                   ← 純粋HTTPクライアント（fetchApi, get, post, put）
                                    ※ トークン取得・認証コンテキスト判定は含まない（後述）
    ui/
      Pagination.tsx              ← components/Pagination.tsx を移動
    lib/
      errorMessages.ts            ← lib/errorMessages.ts を移動
    config/
      env.ts                      ← APP_MODE, API_BASE_URL 定数
    types/
      api.ts                      ← ApiResponse, StockShortageDetail, UnavailableProductDetail, ApiError型
```

---

## 3. ファイル移行マッピング

| 現在のパス | 移行先パス | 備考 |
|-----------|-----------|------|
| `src/App.tsx` | `src/app/App.tsx` | import パス変更のみ |
| `src/contexts/AuthContext.tsx` | `src/features/auth/model/AuthContext.tsx` | |
| `src/contexts/BoAuthContext.tsx` | `src/features/bo-auth/model/BoAuthContext.tsx` | |
| `src/contexts/CartContext.tsx` | `src/features/cart/model/CartContext.tsx` | |
| `src/contexts/ProductContext.tsx` | `src/entities/product/model/ProductContext.tsx` | |
| `src/components/Layout.tsx` | `src/widgets/CustomerLayout/index.tsx` | |
| `src/components/AdminLayout.tsx` | `src/widgets/AdminLayout/index.tsx` | |
| `src/components/AdminSidebar.tsx` | `src/widgets/AdminSidebar/index.tsx` | |
| `src/components/ProductCard.tsx` | `src/entities/product/ui/ProductCard.tsx` | |
| `src/components/Pagination.tsx` | `src/shared/ui/Pagination.tsx` | |
| `src/lib/api.ts` | 分割（後述） | |
| `src/lib/errorMessages.ts` | `src/shared/lib/errorMessages.ts` | |
| `src/types/api.ts` | 分割（後述） | |
| `src/pages/HomePage.tsx` | `src/pages/customer/HomePage/index.tsx` | |
| `src/pages/ItemListPage.tsx` | `src/pages/customer/ItemListPage/index.tsx` | |
| `src/pages/ItemDetailPage.tsx` | `src/pages/customer/ItemDetailPage/index.tsx` | |
| `src/pages/CartPage.tsx` | `src/pages/customer/CartPage/index.tsx` | |
| `src/pages/OrderConfirmPage.tsx` | `src/pages/customer/OrderConfirmPage/index.tsx` | |
| `src/pages/OrderCompletePage.tsx` | `src/pages/customer/OrderCompletePage/index.tsx` | |
| `src/pages/OrderDetailPage.tsx` | `src/pages/customer/OrderDetailPage/index.tsx` | |
| `src/pages/OrderHistoryPage.tsx` | `src/pages/customer/OrderHistoryPage/index.tsx` | |
| `src/pages/LoginPage.tsx` | `src/pages/customer/LoginPage/index.tsx` | |
| `src/pages/RegisterPage.tsx` | `src/pages/customer/RegisterPage/index.tsx` | |
| `src/pages/AdminItemPage.tsx` | `src/pages/admin/AdminItemPage/index.tsx` | |
| `src/pages/AdminOrderPage.tsx` | `src/pages/admin/AdminOrderPage/index.tsx` | |
| `src/pages/AdminInventoryPage.tsx` | `src/pages/admin/AdminInventoryPage/index.tsx` | |
| `src/pages/AdminMembersPage.tsx` | `src/pages/admin/AdminMembersPage/index.tsx` | |
| `src/pages/BoLoginPage.tsx` | `src/pages/admin/BoLoginPage/index.tsx` | |

---

## 4. フロントエンド実装

### 4-1. lib/api.ts の分割

現在 `lib/api.ts` に全API関数が集約されている。FSDでは以下のように分割する。

**`shared/api/client.ts`**（純粋HTTPクライアント）

`shared` は認証ドメインを知ってはならないため、トークン取得・`bo_token`/`authToken` の localStorage 操作・401イベント発火は `shared/api/client.ts` に含めない。これらは `features/auth` および `features/bo-auth` が担う。

Phase 1（今回）の暫定対応: 現行の `fetchApi` の認証ロジックはそのまま `shared/api/client.ts` に移動し、**既知の技術的負債**として明記する。Phase 2 で interceptor パターンへ分離する。

```ts
// 変更前: src/lib/api.ts 内に混在
// 変更後（Phase 1暫定）: src/shared/api/client.ts
// ⚠ 技術的負債: bo_token/authToken の localStorage 参照がここに残る
// → Phase 2 で token getter コールバックを注入する形に分離予定
export type AuthContext = 'auto' | 'customer' | 'bo'
export async function get<T>(...): Promise<ApiResponse<T>>
export async function post<T>(...): Promise<ApiResponse<T>>
export async function put<T>(...): Promise<ApiResponse<T>>
export { getSessionId }
```

**`entities/product/model/api.ts`**
```ts
import { get, put } from '@shared/api/client'
export async function getItems(...): Promise<ApiResponse<ProductListResponse>>
export async function getItemById(id: number): Promise<ApiResponse<Product>>
export async function updateItem(id: number, updates: UpdateProductRequest): Promise<ApiResponse<Product>>
```

**`entities/cart/model/api.ts`**
```ts
import { fetchApi } from '@shared/api/client'
export async function getCart(): Promise<ApiResponse<Cart>>
export async function addToCart(request: AddToCartRequest): Promise<ApiResponse<Cart>>
export async function updateCartItemQuantity(itemId: number, request: UpdateQuantityRequest): Promise<ApiResponse<Cart>>
export async function removeFromCart(itemId: number): Promise<ApiResponse<Cart>>
```

**`entities/order/model/api.ts`**
```ts
export async function createOrder(request: CreateOrderRequest): Promise<ApiResponse<Order>>
export async function getOrderById(id: number): Promise<ApiResponse<Order>>
export async function getOrderHistory(): Promise<ApiResponse<Order[]>>
export async function cancelOrder(orderId: number): Promise<ApiResponse<Order>>
export async function confirmOrder(orderId: number): Promise<ApiResponse<Order>>
export async function shipOrder(orderId: number): Promise<ApiResponse<Order>>
export async function deliverOrder(orderId: number): Promise<ApiResponse<Order>>
export async function getAllOrders(): Promise<ApiResponse<Order[]>>
```

**`entities/customer/model/api.ts`**
```ts
export async function register(request: RegisterRequest): Promise<ApiResponse<AuthResponse>>
export async function login(request: LoginRequest): Promise<ApiResponse<AuthResponse>>
export async function logout(): Promise<ApiResponse<{ message: string }>>
export async function getCurrentUser(): Promise<ApiResponse<User>>
```

**`entities/bo-user/model/api.ts`**
```ts
export async function boLogin(email: string, password: string): Promise<ApiResponse<BoAuthResponse>>
export async function boLogout(): Promise<ApiResponse<{ message: string }>>
export async function getBoUser(): Promise<ApiResponse<BoUser>>
```

### 4-2. types/api.ts の分割

現在 `types/api.ts` に全型が集約されている。FSDでは以下に分割する。

| 型 | 移行先 |
|---|--------|
| `ApiResponse`, `StockShortageDetail`, `UnavailableProductDetail`, `ApiError` | `shared/types/api.ts` |
| `Product`, `ProductListResponse`, `UpdateProductRequest` | `entities/product/model/types.ts` |
| `Cart`, `CartItem`, `AddToCartRequest`, `UpdateQuantityRequest` | `entities/cart/model/types.ts` |
| `Order`, `OrderItem`, `CreateOrderRequest` | `entities/order/model/types.ts` |
| `User`, `AuthResponse`, `RegisterRequest`, `LoginRequest` | `entities/customer/model/types.ts` |
| `BoUser`, `BoAuthResponse`, `BoLoginRequest` | `entities/bo-user/model/types.ts` |

### 4-3. Vite パスエイリアス設定

`frontend/vite.config.ts` にパスエイリアスを追加する。

```ts
// 変更前: エイリアスなし
// 変更後
import path from 'path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@app': path.resolve(__dirname, 'src/app'),
      '@pages': path.resolve(__dirname, 'src/pages'),
      '@widgets': path.resolve(__dirname, 'src/widgets'),
      '@features': path.resolve(__dirname, 'src/features'),
      '@entities': path.resolve(__dirname, 'src/entities'),
      '@shared': path.resolve(__dirname, 'src/shared'),
    },
  },
})
```

`tsconfig.app.json` にも同様のパス設定を追加する。

```json
{
  "compilerOptions": {
    "paths": {
      "@app/*": ["src/app/*"],
      "@pages/*": ["src/pages/*"],
      "@widgets/*": ["src/widgets/*"],
      "@features/*": ["src/features/*"],
      "@entities/*": ["src/entities/*"],
      "@shared/*": ["src/shared/*"]
    }
  }
}
```

### 4-4. ESLint 依存方向ルール

2種類のルールを組み合わせる。

- **`eslint-plugin-boundaries`**: レイヤー間の方向違反を検出（上位→下位のみ許可）
- **`eslint-plugin-import` の `no-internal-modules`**: index.ts を迂回した内部パス直接参照を禁止

```bash
npm install -D eslint-plugin-boundaries eslint-plugin-import
```

```js
// eslint.config.js（変更後）
import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import boundaries from 'eslint-plugin-boundaries'
import importPlugin from 'eslint-plugin-import'
import { defineConfig, globalIgnores } from 'eslint/config'

const FSD_LAYERS = ['app', 'pages', 'widgets', 'features', 'entities', 'shared']

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    plugins: { boundaries, import: importPlugin },
    settings: {
      'boundaries/elements': FSD_LAYERS.map((layer) => ({
        type: layer,
        pattern: `src/${layer}/*`,
      })),
    },
    rules: {
      // ルール①: レイヤー間の依存方向（上位→下位のみ）
      'boundaries/element-types': [
        'error',
        {
          default: 'disallow',
          rules: [
            { from: 'app',      allow: ['pages', 'widgets', 'features', 'entities', 'shared'] },
            { from: 'pages',    allow: ['widgets', 'features', 'entities', 'shared'] },
            { from: 'widgets',  allow: ['features', 'entities', 'shared'] },
            { from: 'features', allow: ['entities', 'shared'] },
            { from: 'entities', allow: ['shared'] },
            { from: 'shared',   allow: [] },
          ],
        },
      ],
      // ルール②: スライス内部パスへの直接アクセス禁止（index.ts 経由を強制）
      'import/no-internal-modules': [
        'error',
        {
          // @layer/slice までは許可（index.ts が解決）
          // @layer/slice/segment/file は禁止
          allow: [
            '@app/**',
            '@pages/*/*',     // @pages/customer/CartPage まで OK（index.tsx を指す）
            '@widgets/*',
            '@features/*',
            '@entities/*',
            '@shared/**',     // shared は内部構造を公開してよい
          ],
        },
      ],
    },
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
  },
])
```

**`no-internal-modules` の allow パターン補足**:

| import パス | 判定 | 理由 |
|------------|------|------|
| `@entities/product` | ✓ 許可 | index.ts 経由 |
| `@entities/product/model/types` | ✗ 禁止 | 内部ファイル直接参照 |
| `@shared/api/client` | ✓ 許可 | shared は内部公開可 |
| `@pages/customer/CartPage` | ✓ 許可 | ページ自体は app/router から参照 |

### 4-5. 各スライスの index.ts（Public API）

各スライスは `index.ts` のみを外部公開インターフェースとし、他レイヤーからは `index.ts` 経由でのみアクセスする。コンポーネントファイルの `export default` はそのまま維持し、`index.ts` では **named export** に統一して再公開する。

```ts
// entities/product/index.ts
export type { Product, ProductListResponse, UpdateProductRequest } from './model/types'
export { useProducts, ProductProvider } from './model/ProductContext'
export { getItems, getItemById, updateItem } from './model/api'
export { default as ProductCard } from './ui/ProductCard'  // named export に変換
```

```ts
// widgets/CustomerLayout/index.ts
export { default as CustomerLayout } from './CustomerLayout'  // named export に変換

// widgets/AdminLayout/index.ts
export { default as AdminLayout } from './AdminLayout'

// widgets/AdminSidebar/index.ts
export { default as AdminSidebar } from './AdminSidebar'
```

```ts
// features/auth/index.ts
export { useAuth, AuthProvider } from './model/AuthContext'

// features/bo-auth/index.ts
export { useBoAuth, BoAuthProvider } from './model/BoAuthContext'

// features/cart/index.ts
export { useCart, CartProvider } from './model/CartContext'
```

**消費側の import 例（named export 統一後）**:
```tsx
// ✓ 全て named import で統一される
import { CustomerLayout } from '@widgets/CustomerLayout'
import { AdminLayout } from '@widgets/AdminLayout'
import { ProductCard, useProducts } from '@entities/product'
import { useAuth } from '@features/auth'
```

### 4-6. app/router/ によるルーティング集約

`app/App.tsx` がページ import を直接持つと肥大化するため、ルーティング定義を `app/router/` に分離する。

```tsx
// 変更前: src/App.tsx（ページ import が全て App.tsx に集中）
import HomePage from './pages/HomePage'
import ItemListPage from './pages/ItemListPage'
// ... 15ページ分の import

// 変更後: src/app/router/customer.tsx
import { BrowserRouter, Routes, Route } from 'react-router'
import { AuthProvider } from '@features/auth'
import { CartProvider } from '@features/cart'
import { ProductProvider } from '@entities/product'
import { CustomerLayout } from '@widgets/CustomerLayout'
import HomePage from '@pages/customer/HomePage'
import ItemListPage from '@pages/customer/ItemListPage'
// ... 顧客向けページの import

export function CustomerRouter() {
  return (
    <AuthProvider>
      <ProductProvider>
        <CartProvider>
          <BrowserRouter>
            <Routes>
              <Route element={<CustomerLayout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/item" element={<ItemListPage />} />
                {/* ... */}
              </Route>
            </Routes>
          </BrowserRouter>
        </CartProvider>
      </ProductProvider>
    </AuthProvider>
  )
}
```

```tsx
// src/app/router/admin.tsx
import { BoAuthProvider } from '@features/bo-auth'
import { ProductProvider } from '@entities/product'
import { AdminLayout } from '@widgets/AdminLayout'
import AdminItemPage from '@pages/admin/AdminItemPage'
// ...

export function AdminRouter() { /* ... */ }
```

```tsx
// src/app/App.tsx（移行後: Router の切り替えのみ）
import { CustomerRouter } from './router/customer'
import { AdminRouter } from './router/admin'

export default function App() {
  const appMode = import.meta.env.VITE_APP_MODE === 'admin' ? 'admin' : 'customer'
  return appMode === 'admin' ? <AdminRouter /> : <CustomerRouter />
}
```

### 4-7. main.tsx の App.tsx 参照変更

```tsx
// 変更前: src/main.tsx
import App from './App.tsx'

// 変更後: src/main.tsx
import App from './app/App.tsx'
```

---

## 5. 処理フロー

### 依存方向の概念図

```
src/main.tsx
    │
    ▼
src/app/App.tsx
    │  uses: @pages/*, @widgets/*, @features/*, @entities/*, @shared/*
    │
    ├─► src/pages/customer/ItemListPage/  ─► @entities/product (ProductCard, getItems)
    │                                      ─► @shared/ui (Pagination)
    │
    ├─► src/pages/customer/ItemDetailPage/ ─► @entities/product (useProducts)
    │                                       ─► @features/cart (useCart)
    │
    ├─► src/widgets/CustomerLayout/        ─► @features/auth (useAuth)
    │                                      ─► @entities/cart (useCart)
    │
    └─► src/features/auth/                 ─► @entities/customer (getCurrentUser, User)
                                           ─► @shared/api/client

禁止された方向（ESLintが検出）:
  shared → entities  ✗
  entities → features ✗
  features → pages   ✗
```

### Public API 経由アクセスの例

```ts
// ✓ 正しい: index.ts 経由
import { ProductCard, useProducts } from '@entities/product'

// ✗ 禁止: 内部ファイル直接参照
import ProductCard from '@entities/product/ui/ProductCard'
import { useProducts } from '@entities/product/model/ProductContext'
```

---

## 6. 影響範囲

### 変更対象ファイル（全フロントエンドファイル）

| カテゴリ | ファイル数 | 内容 |
|---------|-----------|------|
| ファイル移動 | 29ファイル | 現存の全 `.tsx` ファイル |
| 新規作成 | 約25ファイル | `index.ts`, `types.ts`, `api.ts`, `env.ts` など |
| 設定変更 | 3ファイル | `vite.config.ts`, `tsconfig.app.json`, `eslint.config.js` |

### バックエンド・BFFへの影響

- **なし**: API仕様・エンドポイント・レスポンス形式はすべて変更なし。

### 機能への影響

- **なし**: ロジックの移動のみで、ユーザー向け機能は変化しない。

---

## 7. 設計上の判断事項・制約

### ProductContext の位置（entities vs features）

`ProductContext` は商品一覧の全件取得とローカルキャッシュを担っている。「ドメイン実体の取得」であるため `entities/product/model/` に配置する。

**共有キャッシュの責務範囲**:

| 項目 | 内容 |
|-----|------|
| キャッシュ対象 | 全商品リスト（起動時 1 回取得） |
| アクセス権 | app / pages / widgets / features 全レイヤーが `useProducts()` 経由で参照可 |
| キャッシュしないもの | ページネーション済みリスト（ItemListPage が独自で保持）、在庫リアルタイム情報（AdminInventoryPage が独自取得） |
| 無効化タイミング | `refreshProducts()` の明示的呼び出し時のみ（自動 polling なし） |
| 更新時の同期 | `updateProduct()` 成功後にローカル状態を差分更新（再 fetch なし） |

将来的にページネーションとの整合が必要になった場合は `features/` への移行を検討する。

### Context パターンの維持

FSD標準では Zustand/Jotai 等のストアを推奨するケースもあるが、本プロジェクトは既存の React Context + カスタムフックパターンを維持する。ロジック変更なしでの構造移行を優先する。

### eslint-plugin-boundaries のスライス内ルール

同一スライス内のセグメント間（例: `features/auth/model/` → `features/auth/ui/`）はlintルール対象外。FSD のセグメント間依存ルール（`model → ui` は禁止など）は今回は採用せず、レイヤー間のみ強制する。

### shared/api/client.ts の認証ロジック（Phase 2 課題）

現行の `fetchApi` は `localStorage` から `bo_token`/`authToken` を読み取り、401 時にカスタムイベントを発火する。これらは `features/auth` ドメインの知識であり `shared` に本来属さない。

**Phase 1（今回）**: 現行ロジックをそのまま `shared/api/client.ts` に移動。技術的負債として `⚠` コメントで明記する。

**Phase 2（別 CHG）**: `fetchApi` を token getter コールバック受け取り型に変更し、認証ロジックを `features/auth` / `features/bo-auth` に移動する。

### lib/api.ts の後方互換

一括移行のため `lib/api.ts` は削除する。移行後に参照エラーが残る場合は `tsc` エラーで検出できる。

---

## 8. テスト観点

| 観点 | 検証方法 |
|-----|---------|
| TypeScript ビルド通過 | `npm run build` でエラーなし |
| レイヤー依存方向違反なし | `npm run lint` で `boundaries/element-types` エラーが出ないこと |
| index.ts 迂回なし | `npm run lint` で `import/no-internal-modules` エラーが出ないこと |
| named export の統一 | widgets・entities の index.ts が全て named export になっていること |
| 顧客画面の基本動作 | `npm run dev:customer` → 商品一覧・詳細・カート・注文の画面遷移確認 |
| 管理画面の基本動作 | `npm run dev:admin` → 商品管理・注文管理の画面遷移確認 |
| 認証フロー | ログイン・ログアウト・未認証リダイレクト確認 |
