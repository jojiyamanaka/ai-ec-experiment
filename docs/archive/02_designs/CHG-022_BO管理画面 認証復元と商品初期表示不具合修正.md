# CHG-022: BO管理画面 認証復元と商品初期表示不具合修正 - 技術設計

要件: 既存不具合調査（管理画面ログイン後の商品初期表示不安定、およびリロード後の再ログイン要求）  
作成日: 2026-02-20

**SSOT（唯一の真実）**:
- API契約: このドキュメントの「API契約」セクション
- 認証トークン検証: `bff/backoffice-bff/src/auth/bo-auth.guard.ts`
- フロント認証状態管理: `frontend/src/features/bo-auth/model/BoAuthContext.tsx`
- 商品取得タイミング: `frontend/src/entities/product/model/ProductContext.tsx`

---

## 1. 設計方針

- 不具合を分離せず、「認証復元の契約不整合」と「商品のみの未認証先行フェッチ」を同時に解消する。
- 既存のトークン保存方式（`bo_token`）と既存ログイン/ログアウト API は維持し、破壊的変更を避ける。
- 修正は契約追加（`GET /api/bo-auth/me`）とフロントの取得タイミング制御に限定する。
- 注文/在庫/会員/商品の表示安定性を同一の認証復元フローで担保する。

---

## 2. API契約

### 2.1 BackOffice BFF（追加）

| エンドポイント | メソッド | 認証/認可 | 契約 |
|---|---|---|---|
| `/api/bo-auth/me` | GET | BoUser | 現在ログイン中の BoUser 情報を返却 |

レスポンス契約:
- `success: true`
- `data`: `BoUser`（`id`, `email`, `displayName`, `permissionLevel`, `isActive` など既存型に準拠）

エラー契約:
- `401 BFF_UNAUTHORIZED`（トークンなし）
- `401 BFF_INVALID_TOKEN`（トークン不正/期限切れ）
- `503/504`（Core API 疎通異常時、既存ガード挙動を維持）

### 2.2 既存契約の維持

- `POST /api/bo-auth/login` は変更しない。
- `POST /api/bo-auth/logout` は変更しない。
- `/api/admin/*` および `/api/bo/admin/*` の商品/注文/在庫/会員 API 契約は変更しない。

---

## 3. モジュール・レイヤ構成

```
backoffice-bff/
  auth/
    bo-auth.controller.ts   - GET /api/bo-auth/me を追加
    bo-auth.guard.ts        - 既存のトークン検証を継続利用

frontend/
  features/bo-auth/
    model/BoAuthContext.tsx - 初期認証復元と unauthorized 処理の安定化

  entities/product/
    model/ProductContext.tsx - 認証済み条件での商品/カテゴリ取得

  pages/admin/
    AdminItemPage/index.tsx - ページ表示時の再取得トリガー（冪等）
```

依存方向:
- Frontend は BFF 認証 API 契約に依存。
- BFF `GET /api/bo-auth/me` は `BoAuthGuard` に依存し、ガードが Core API `/api/bo-auth/me` 検証を実施。

---

## 4. 主要クラス/IFの責務

| クラス/IF | 責務 | レイヤ |
|---|---|---|
| `BoAuthController` | `GET /api/bo-auth/me` を提供し、認証済み BoUser を返す | bff/adapter |
| `BoAuthGuard` | Bearer token を検証し、`request.boUser` を確定する | bff/adapter |
| `BoAuthContext` | `bo_token` による認証復元、ログイン状態の単一管理 | frontend/features |
| `ProductContext` | 商品/カテゴリの取得条件を認証状態に同期する | frontend/entities |
| `AdminItemPage` | 商品一覧表示時の再取得トリガーを管理する | frontend/pages |

---

## 5. トランザクション・非同期方針

- 新規 DB トランザクションは追加しない。
- 認証トークンの永続化先（`bo_auth_tokens`）のスキーマ変更は行わない。
- BFF 認証検証は既存どおり Redis キャッシュ（`bo-auth:token:{hash}`、TTL 60秒）を利用する。
- フロントの再取得は冪等に実行し、同一データに対する多重更新副作用を持たないことを前提とする。

---

## 6. 処理フロー

### 6.1 リロード後の認証復元

```
Browser (bo_tokenあり)
  → Frontend BoAuthContext: GET /api/bo-auth/me
    → BackOffice BFF BoAuthController (GET /api/bo-auth/me)
      → BoAuthGuard (token検証 + request.boUser確定)
      → BoUser返却
  → RequireBoAuth 通過
  → 管理画面表示継続
```

### 6.2 商品初期表示

```
ログイン成功 or 認証復元完了
  → ProductContext が認証済み条件で refreshProducts/refreshCategories
  → AdminItemPage 表示時に冪等再取得
  → 商品一覧表示
```

---

## 7. 影響範囲

| 区分 | 対象（クラス名） | 変更概要 |
|---|---|---|
| 既存変更 | `bff/backoffice-bff/src/auth/bo-auth.controller.ts` | `GET /api/bo-auth/me` 追加 |
| 既存変更 | `frontend/src/features/bo-auth/model/BoAuthContext.tsx` | 認証復元/unauthorized の状態遷移安定化 |
| 既存変更 | `frontend/src/entities/product/model/ProductContext.tsx` | 未認証時先行フェッチ停止、認証済み条件で取得 |
| 既存変更 | `frontend/src/pages/admin/AdminItemPage/index.tsx` | 画面表示時の商品再取得を明示化 |
| 影響なし | `backend` DBスキーマ/Flyway | テーブル変更なし |
| 影響なし | `bo_auth_tokens` テーブル定義 | 既存設計を維持 |

---

## 8. テスト観点

- 正常系:
  - ログイン直後に `/bo/item` `/bo/order` `/bo/inventory` `/bo/members` が表示される。
  - 任意管理画面でリロード後も `/bo/login` に戻されず継続利用できる。
  - 商品一覧が初回表示で空にならない。

- 異常系:
  - 無効トークンで `GET /api/bo-auth/me` が 401 を返し、ログイン画面へ遷移する。
  - トークン未送信で `GET /api/bo-auth/me` が 401 を返す。

- 境界値:
  - `bo-auth:unauthorized` が連続発火しても状態が不整合にならない。
  - ログイン画面滞在中に商品 API が先行発火しない。
  - BFF ログに `Cannot GET /api/bo-auth/me` が出力されない。
