# CHG-022: BO管理画面 認証復元と商品初期表示不具合修正 - 実装タスク

要件: （未作成。設計書記載の「既存不具合調査」を参照）
設計: `docs/02_designs/CHG-022_BO管理画面 認証復元と商品初期表示不具合修正.md`
作成日: 2026-02-20

---

## タスク一覧

### BFF

- [x] **T-1** `[CONTRACT]`: `GET /api/bo-auth/me` を追加し、既存 `BoAuthGuard` で検証済みの BoUser を返却する

  触る範囲: `bff/backoffice-bff/src/auth / bo-auth.controller.ts, bo-auth.guard.ts`

  Done: `cd bff/backoffice-bff && npm run build && docker compose up -d backoffice-bff backend redis && docker compose exec -T backoffice-bff sh -lc 'token=$(curl -sS -X POST http://localhost:3002/api/bo-auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@example.com\",\"password\":\"password\"}" | sed -n "s/.*\"token\":\"\\([^\"]*\\)\".*/\\1/p" | head -n 1); [ -n "$token" ] && [ "$(curl -sS -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $token" http://localhost:3002/api/bo-auth/me)" = "200" ] && [ "$(curl -sS -o /dev/null -w "%{http_code}" http://localhost:3002/api/bo-auth/me)" = "401" ]'` が通ること

  > 📝 ゲート高。Codex は impl-notes（既存ガード再利用の判断・401 契約の扱い）を `docs/04_impl-notes/CHG-022.md` の `## T-1` セクションに追記し、追加検証コマンドを実行すること。

---

### フロントエンド

- [x] **T-2** `[SAFE]`: `BoAuthContext` の初期認証復元と `bo-auth:unauthorized` 連続発火時の状態遷移を安定化する

  触る範囲: `frontend/src/features/bo-auth/model / BoAuthContext.tsx`

  Done: `npm run build --prefix frontend && cd frontend && bash ./e2e/admin-smoke.sh` が通ること

---

- [x] **T-3** `[SAFE]`: `ProductContext` の商品/カテゴリ取得を認証済み条件に同期し、未認証時の先行フェッチを停止する

  触る範囲: `frontend/src/entities/product/model / ProductContext.tsx`

  Done: `npm run build --prefix frontend && cd frontend && bash ./e2e/admin-smoke.sh` が通ること

---

- [x] **T-4** `[SAFE]`: `AdminItemPage` 表示時の商品再取得トリガーを冪等に制御し、初回空表示の再発を防止する

  触る範囲: `frontend/src/pages/admin/AdminItemPage / index.tsx`

  Done: `npm run build --prefix frontend && cd frontend && bash ./e2e/admin-smoke.sh` が通ること

---

## 実装順序

```
T-1 → T-2 → T-3 → T-4
```

---

## 検証

### Per-task 検証コマンド

```bash
# BFF
cd bff/backoffice-bff && npm run build
docker compose up -d backoffice-bff backend redis
docker compose exec -T backoffice-bff sh -lc 'token=$(curl -sS -X POST http://localhost:3002/api/bo-auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@example.com\",\"password\":\"password\"}" | sed -n "s/.*\"token\":\"\\([^\"]*\\)\".*/\\1/p" | head -n 1); [ -n "$token" ] && [ "$(curl -sS -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $token" http://localhost:3002/api/bo-auth/me)" = "200" ] && [ "$(curl -sS -o /dev/null -w "%{http_code}" http://localhost:3002/api/bo-auth/me)" = "401" ]'

# Frontend
npm run build --prefix frontend
cd frontend && bash ./e2e/admin-smoke.sh
```

### Final Gate（全タスク完了後に必ず実行し、結果をこのファイルに貼り付けること）

```bash
# 変更対象コンテナの再ビルド（必須）
docker compose build backoffice-bff frontend-admin frontend-customer

# サービス起動
docker compose up -d

# BFF
cd bff/backoffice-bff && npm run build
docker compose exec -T backoffice-bff sh -lc 'token=$(curl -sS -X POST http://localhost:3002/api/bo-auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@example.com\",\"password\":\"password\"}" | sed -n "s/.*\"token\":\"\\([^\"]*\\)\".*/\\1/p" | head -n 1); [ -n "$token" ] && [ "$(curl -sS -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $token" http://localhost:3002/api/bo-auth/me)" = "200" ] && [ "$(curl -sS -o /dev/null -w "%{http_code}" http://localhost:3002/api/bo-auth/me)" = "401" ]'
curl -sS -o /dev/null -w "backoffice-bff=%{http_code}\n" http://localhost:3002/health

# Frontend
npm run build --prefix frontend
cd frontend && bash ./e2e/admin-smoke.sh
curl -sS -o /dev/null -w "frontend-admin=%{http_code}\n" http://localhost:5174
```

**Final Gate 結果:**

- `docker compose build backoffice-bff frontend-admin frontend-customer` → PASS
- `docker compose up -d` → PASS
- `cd bff/backoffice-bff && npm run build` → PASS
- `docker compose exec -T backoffice-bff ... /api/bo-auth/me` → PASS（認証あり `200` / 認証なし `401`）
- `curl http://localhost:3002/health` → PASS（`backoffice-bff=200`）
- `npm run build --prefix frontend` → PASS
- `cd frontend && bash ./e2e/admin-smoke.sh` → PASS
- `curl http://localhost:5174` → PASS（`frontend-admin=200`）

---

## テスト手順

1. 管理画面でログイン後、`/bo/item` の商品一覧が初回表示で空にならないことを確認する。
2. `/bo/item` `/bo/order` `/bo/inventory` `/bo/members` のいずれかでリロードしても `/bo/login` に戻されないことを確認する。
3. 無効トークンまたはトークン未送信で `GET /api/bo-auth/me` が `401` になることを確認する。
4. ログイン画面滞在中に商品 API が先行発火しないことを確認する。
5. `bo-auth:unauthorized` が連続発火しても画面状態が不整合にならないことを確認する。

## Review Packet
### 変更サマリ（10行以内）
- BackOffice BFF に `GET /api/bo-auth/me` を追加し、`BoAuthGuard` が解決した `boUser` を返す認証復元 API を実装した。
- `BoAuthContext` に復元シーケンス制御を追加し、復元中競合や `bo-auth:unauthorized` 連続発火時の状態不整合を防止した。
- 認証成功時に `bo-auth:authenticated` イベントを発火し、認証状態と商品取得の同期点を明確化した。
- `ProductContext` を認証条件付き取得に変更し、未認証時の管理向け商品/カテゴリ先行フェッチを停止した。
- `ProductContext` は `bo-auth:authenticated` / `bo-auth:unauthorized` で商品・カテゴリ状態を同期するようにした。
- `AdminItemPage` 表示時に `refreshProducts` / `refreshCategories` を明示実行し、初回表示の再取得トリガーを追加した。
- `[CONTRACT]` の impl-notes を `docs/04_impl-notes/CHG-022.md` に記録した。

### 変更ファイル一覧
- `bff/backoffice-bff/src/auth/bo-auth.controller.ts`
- `frontend/src/features/bo-auth/model/BoAuthContext.tsx`
- `frontend/src/entities/product/model/ProductContext.tsx`
- `frontend/src/pages/admin/AdminItemPage/index.tsx`
- `docs/04_impl-notes/CHG-022.md`
- `docs/03_tasks/CHG-022_BO管理画面 認証復元と商品初期表示不具合修正.md`

### リスクと未解決
- `docker compose` 実行時に `version` 属性の obsolete warning が出るが、今回の変更起因ではないため未対応。

### UI確認媒体（MCP/Docker）
- MCP Playwright（`http://localhost:5174`）
- Docker（`frontend/e2e/admin-smoke.sh`）

### テスト結果（PASS/FAIL、失敗時は30行以内）
- [PASS] `cd bff/backoffice-bff && npm run build`
- [PASS] `docker compose up -d backoffice-bff backend redis`
- [PASS] `docker compose exec -T backoffice-bff sh -lc '.../api/bo-auth/me...'`（`200/401`）
- [PASS] `npm run build --prefix frontend`
- [PASS] `cd frontend && bash ./e2e/admin-smoke.sh`
- [PASS] `docker compose build backoffice-bff frontend-admin frontend-customer`
- [PASS] `docker compose up -d`
- [PASS] `curl -sS -o /dev/null -w "backoffice-bff=%{http_code}\n" http://localhost:3002/health`
- [PASS] `curl -sS -o /dev/null -w "frontend-admin=%{http_code}\n" http://localhost:5174`
- [PASS] MCP: ログイン直後 `http://localhost:5174/bo/item` で `tbody tr` が `5` 件（初回空表示なし）
- [PASS] MCP: `http://localhost:5174/bo/order` を再読込しても `/bo/login` に遷移せず、`bo_token` 維持
- [PASS] MCP: `http://localhost:5174/`（ログイン画面）で `/api/admin/items` / `/api/admin/item-categories` の先行発火なし（計測上 OTEL 送信のみ）
- [PASS] MCP: `bo-auth:unauthorized` を連続発火しても `/bo/login` へ遷移し、ログイン画面表示が安定
