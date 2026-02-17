# Frontend 規約

## API呼び出し

`src/lib/api.ts` の関数を使う。`fetch` を直接使わない。

## 型定義

`src/types/api.ts` に集約。レスポンス型は `ApiResponse<T>` で統一。

## 状態管理

- `ProductContext` — 商品データ
- `CartContext` — カート状態（Customer BFF と同期）

## API接続先（BFF構成）

- 顧客画面: `frontend (5173) -> customer-bff (3001) -> backend (8080/internal)`
- 管理画面: `frontend (5174) -> backoffice-bff (3002) -> backend (8080/internal)`
- ブラウザから Core API (`localhost:8080`) への直接アクセスは行わない

## コーディング

- TypeScript のみ（JS 禁止）
- スタイリングは Tailwind CSS のユーティリティクラス
