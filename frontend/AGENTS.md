# Frontend 規約

## ディレクトリ構成（FSD）

`app / pages / widgets / features / entities / shared` の FSD レイヤ構成。
依存方向は上位→下位のみ（`app` → `pages` → `widgets` → `features` → `entities` → `shared`）。
ESLint `boundaries/element-types` ルールで違反を検出。

## API呼び出し

`fetch` を直接使わない。
- HTTPプリミティブ（`get` / `post` / `put` / `fetchApi`）: `@shared/api/client`
- エンティティ固有API: 各 `@entities/*/model/api.ts`

## 型定義

- 共通型（`ApiResponse` / `ApiError` 等）: `@shared/types/api`
- エンティティ型: 各 `@entities/*/model/types.ts`

## 状態管理

- `useProducts` / `ProductProvider` — 商品データ（`@entities/product`）
- `useCart` / `CartProvider` — カート状態（`@features/cart`）
- `useAuth` / `AuthProvider` — 顧客認証（`@features/auth`）
- `useBoAuth` / `BoAuthProvider` — 管理者認証（`@features/bo-auth`）

## API接続先（BFF構成）

- 顧客画面: `frontend (5173) -> customer-bff (3001) -> backend (8080/internal)`
- 管理画面: `frontend (5174) -> backoffice-bff (3002) -> backend (8080/internal)`
- ブラウザから Core API (`localhost:8080`) への直接アクセスは行わない

## コーディング

- TypeScript のみ（JS 禁止）
- スタイリングは Tailwind CSS のユーティリティクラス
