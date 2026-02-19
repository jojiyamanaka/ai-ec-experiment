# CHG-020: User データモデル項目追加と画面反映 - 実装タスク

要件: `docs/01_requirements/CHG-020_User データモデル項目追加と画面反映.md`
設計: `docs/02_designs/CHG-020_User データモデル項目追加と画面反映.md`
作成日: 2026-02-19

---

## タスク一覧

### バックエンド

- [x] **T-1** `[CONTRACT]`: Flyway V9 を作成し、`users` 拡張と `user_addresses` 新設を反映する

  触る範囲: `backend/src/main/resources/db/flyway / V9__extend_user_profile_and_addresses.sql`

  Done: `docker compose up -d && docker compose logs backend | rg "V9__extend_user_profile_and_addresses|Successfully applied"` で Flyway V9 の適用を確認できること

  > 📝 ゲート高。Codex は impl-notes（DDL 上の制約設計・既存データ互換の判断）を `docs/impl-notes/CHG-020.md` の `## T-1` セクションに追記し、追加検証コマンドを実行すること。

---

- [x] **T-2** `[ARCH]`: `UserAddress` 系エンティティ/リポジトリと `UserProfileService` を追加し、プロフィール/住所更新のトランザクション境界を実装する

  触る範囲: `backend/src/main/java/com/example/aiec/modules/customer/domain / entity/User.java, entity/UserAddress.java, repository/UserAddressRepository.java, service/UserProfileService.java, service/UserService.java`

  Done: `cd backend && ./mvnw test -Dtest=UserProfileServiceTest` が通ること（`is_default` の会員内一意制約、他会員住所の更新拒否を検証するケースを含むこと）

  > 📝 ゲート高。Codex は impl-notes（トランザクション境界・`is_default` 正規化戦略の判断根拠）を `docs/impl-notes/CHG-020.md` の `## T-2` セクションに追記し、追加検証コマンドを実行すること。

---

- [x] **T-3** `[CONTRACT]`: 顧客向け `/api/auth/me` 系 API の DTO/Request/Controller を拡張し、許可外項目更新を拒否する

  触る範囲: `backend/src/main/java/com/example/aiec/modules/customer/adapter / dto/UserDto.java, dto/UserAddressDto.java, dto/UpdateMyProfileRequest.java, rest/AuthController.java`

  Done: `cd backend && ./mvnw test -Dtest=AuthControllerContractTest` が通ること（顧客更新禁止項目送信時の `INVALID_REQUEST` と住所 CRUD の認可境界を検証するケースを含むこと）

  > 📝 ゲート高。Codex は impl-notes（顧客公開項目の境界定義・拒否方針）を `docs/impl-notes/CHG-020.md` の `## T-3` セクションに追記し、追加検証コマンドを実行すること。

---

- [x] **T-4** `[CONTRACT]`: BO 向け会員新規登録/FULL更新 API を追加し、更新禁止項目と重複メールを契約どおり拒否する

  触る範囲: `backend/src/main/java/com/example/aiec/modules/backoffice/adapter / dto/MemberDetailDto.java, dto/CreateMemberRequest.java, dto/UpdateMemberRequest.java, rest/BoAdminController.java`、`backend/src/main/java/com/example/aiec/modules/customer/domain/service / UserService.java, UserProfileService.java`

  Done: `cd backend && ./mvnw test -Dtest=BoAdminControllerContractTest` が通ること（`EMAIL_ALREADY_EXISTS` と BO 更新禁止項目拒否を検証するケースを含むこと）

  > 📝 ゲート高。Codex は impl-notes（BO FULL 更新許可項目の判断・既存 `/api/admin/members/*` 互換維持方針）を `docs/impl-notes/CHG-020.md` の `## T-4` セクションに追記し、追加検証コマンドを実行すること。

---

### BFF

- [x] **T-5** `[CONTRACT]`: Customer BFF の members API を拡張し、`/api/members/me` 更新と住所 CRUD を Core API にマッピングする

  触る範囲: `bff/customer-bff/src/members / members.controller.ts, members.service.ts`

  Done: `cd bff/customer-bff && npm run build` が通ること

  > 📝 ゲート高。Codex は impl-notes（BFF DTO マッピング方針・Core API エラー透過方針）を `docs/impl-notes/CHG-020.md` の `## T-5` セクションに追記し、追加検証コマンドを実行すること。

---

- [x] **T-6** `[CONTRACT]`: BackOffice BFF の members API を拡張し、会員新規登録と FULL 更新を Core API にマッピングする

  触る範囲: `bff/backoffice-bff/src/members / members.controller.ts, members.service.ts`

  Done: `cd bff/backoffice-bff && npm run build` が通ること

  > 📝 ゲート高。Codex は impl-notes（`/api/admin/members/*` と `/api/bo/admin/members/*` の互換方針）を `docs/impl-notes/CHG-020.md` の `## T-6` セクションに追記し、追加検証コマンドを実行すること。

---

### フロントエンド

- [x] **T-7** `[SAFE]`: `entities/customer` の型定義と API クライアントを User 拡張項目・住所操作に対応させる

  触る範囲: `frontend/src/entities/customer/model / types.ts, api.ts`

  Done: `npm run build --prefix frontend` が通ること

---

- [x] **T-8** `[SAFE]`: 顧客向けマイページを新規追加し、ルーティングとレイアウト導線を更新する

  触る範囲: `frontend/src/pages/customer/MyPagePage / index.tsx`、`frontend/src/app/router / customer.tsx`、`frontend/src/widgets/CustomerLayout / CustomerLayout.tsx`

  Done: `npm run build --prefix frontend` が通ること

---

- [x] **T-9** `[SAFE]`: 管理画面の会員管理 UI を拡張し、会員新規登録/FULL更新/住所管理フローを反映する

  触る範囲: `frontend/src/pages/admin/AdminMembersPage / index.tsx`

  Done: `npm run build --prefix frontend` が通ること

---

### ドキュメント

- [x] **T-10** `[SAFE]`: データモデル・OpenAPI・UI仕様・業務要件ドキュメントへ User 拡張仕様を反映する

  触る範囲: `docs/data-model.md`、`docs/api/customer-bff-openapi.json`、`docs/api/backoffice-bff-openapi.json`、`docs/api/openapi.json`、`docs/ui/customer-ui.md`、`docs/ui/admin-ui.md`、`docs/requirements.md`

  Done: `rg -n "/api/members/me/addresses|/api/admin/members|user_addresses|memberRank|deactivationReason" docs/api/customer-bff-openapi.json docs/api/backoffice-bff-openapi.json docs/api/openapi.json docs/data-model.md docs/ui/customer-ui.md docs/ui/admin-ui.md docs/requirements.md` で反映箇所を確認できること

---

## 実装順序

```
T-1 → T-2 → T-3 → T-4 → T-5 → T-6 → T-7 → T-8 → T-9 → T-10
```

---

## 検証

### Per-task 検証コマンド

```bash
# Backend
cd backend && ./mvnw compile
cd backend && ./mvnw test -Dtest=UserProfileServiceTest
cd backend && ./mvnw test -Dtest=AuthControllerContractTest
cd backend && ./mvnw test -Dtest=BoAdminControllerContractTest

# BFF
cd bff/customer-bff && npm run build
cd bff/backoffice-bff && npm run build

# Frontend
npm run build --prefix frontend

# Docs
rg -n "/api/members/me/addresses|/api/admin/members|user_addresses|memberRank|deactivationReason" \
  docs/api/customer-bff-openapi.json \
  docs/api/backoffice-bff-openapi.json \
  docs/api/openapi.json \
  docs/data-model.md \
  docs/ui/customer-ui.md \
  docs/ui/admin-ui.md \
  docs/requirements.md
```

### Final Gate（全タスク完了後に必ず実行し、結果をこのファイルに貼り付けること）

```bash
# Backend
cd backend && ./mvnw compile
cd backend && ./mvnw test

# BFF
cd bff/customer-bff && npm run build
cd bff/backoffice-bff && npm run build

# Frontend
npm run build --prefix frontend

# E2E smoke
bash -lc 'cd "$(git rev-parse --show-toplevel)/frontend" && bash ./e2e/customer-smoke.sh'
bash -lc 'cd "$(git rev-parse --show-toplevel)/frontend" && bash ./e2e/admin-smoke.sh'

# コンテナ起動確認
docker compose up -d
curl -sS -o /dev/null -w "customer-bff=%{http_code}\n" http://localhost:3001/health
curl -sS -o /dev/null -w "backoffice-bff=%{http_code}\n" http://localhost:3002/health
curl -sS -o /dev/null -w "backend=%{http_code}\n" http://localhost:8080/actuator/health
```

**Final Gate 結果:**
- `cd backend && ./mvnw compile`: PASS
- `cd backend && ./mvnw test`: PASS（84 tests, 0 failures）
- `cd bff/customer-bff && npm run build`: PASS
- `cd bff/backoffice-bff && npm run build`: PASS
- `npm run build --prefix frontend`: PASS
- `bash ./frontend/e2e/customer-smoke.sh`: FAIL（`open /Users/george/workspace/docker-compose.yml: no such file or directory`）
- `cd frontend && bash ./e2e/customer-smoke.sh`: PASS（実行ディレクトリ補正）
- `cd frontend && bash ./e2e/admin-smoke.sh`: PASS
- `docker compose up -d`: PASS
- `curl -sS -o /dev/null -w "customer-bff=%{http_code}\n" http://localhost:3001/health`: PASS（`200`）
- `curl -sS -o /dev/null -w "backoffice-bff=%{http_code}\n" http://localhost:3002/health`: PASS（`200`）
- `curl -sS -o /dev/null -w "backend=%{http_code}\n" http://localhost:8080/actuator/health`: FAIL（`backend=000`）
- 補足確認: `docker compose exec -T backend sh -lc "curl ... http://localhost:8080/actuator/health"` は `backend-internal=200`

---

## テスト手順

1. BO で会員を新規作成し、取得 API で `fullName`/`memberRank`/`addresses` が保持されることを確認する
2. 顧客マイページで `displayName`/`fullName`/`phoneNumber`/`birthDate`/`newsletterOptIn` を更新し、再取得で反映されることを確認する
3. 顧客マイページで住所を複数登録し、`isDefault=true` が同一会員内で1件以下に維持されることを確認する
4. 顧客が `memberRank` や `loyaltyPoints` を更新しようとした場合に拒否されることを確認する
5. BO が `passwordHash`/監査項目/`lastLoginAt`/`termsAgreedAt` を更新しようとした場合に拒否されることを確認する
6. BO 会員新規作成で重複メールを送信した場合に `EMAIL_ALREADY_EXISTS`（409）で拒否されることを確認する
7. 他会員の住所 ID を指定した更新/削除が拒否されることを確認する

---

## Review Packet

### 変更サマリ（10行以内）
- Flyway V9 で `users` 拡張列と `user_addresses` テーブル、制約/インデックスを追加した。
- `User` ドメインに会員拡張項目と `MemberRank` を追加し、`UserAddress` エンティティ/リポジトリを新設した。
- `UserProfileService` を追加し、プロフィール更新と住所 CRUD（`is_default` 正規化、所有者境界チェック）を実装した。
- 顧客向け `/api/auth/me` 系 API を拡張し、許可外フィールドを `INVALID_REQUEST` で拒否するようにした。
- BO 向け会員作成/FULL更新 API を追加し、重複メールおよび更新禁止項目拒否の契約を実装した。
- Customer/BackOffice BFF に会員更新・住所CRUD・会員作成/FULL更新の中継 API を追加した。
- フロントエンドで顧客マイページ新設、管理会員画面の拡張、`entities/customer` 型/APIの更新を実施した。
- `data-model`/`OpenAPI`/`ui`/`requirements` を CHG-020 仕様に更新した。
- `docs/impl-notes/CHG-020.md` に T-1〜T-6 の実装判断を記録した。

### 変更ファイル一覧
- `backend/src/main/resources/db/flyway/V9__extend_user_profile_and_addresses.sql`
- `backend/src/main/java/com/example/aiec/modules/customer/domain/entity/User.java`
- `backend/src/main/java/com/example/aiec/modules/customer/domain/entity/UserAddress.java`
- `backend/src/main/java/com/example/aiec/modules/customer/domain/repository/UserAddressRepository.java`
- `backend/src/main/java/com/example/aiec/modules/customer/domain/service/UserProfileService.java`
- `backend/src/main/java/com/example/aiec/modules/customer/domain/service/UserService.java`
- `backend/src/main/java/com/example/aiec/modules/customer/adapter/dto/UserDto.java`
- `backend/src/main/java/com/example/aiec/modules/customer/adapter/dto/UserAddressDto.java`
- `backend/src/main/java/com/example/aiec/modules/customer/adapter/dto/UpdateMyProfileRequest.java`
- `backend/src/main/java/com/example/aiec/modules/customer/adapter/rest/AuthController.java`
- `backend/src/main/java/com/example/aiec/modules/backoffice/adapter/dto/MemberDetailDto.java`
- `backend/src/main/java/com/example/aiec/modules/backoffice/adapter/dto/CreateMemberRequest.java`
- `backend/src/main/java/com/example/aiec/modules/backoffice/adapter/dto/UpdateMemberRequest.java`
- `backend/src/main/java/com/example/aiec/modules/backoffice/adapter/rest/BoAdminController.java`
- `backend/src/test/java/com/example/aiec/modules/customer/domain/service/UserProfileServiceTest.java`
- `backend/src/test/java/com/example/aiec/modules/customer/adapter/rest/AuthControllerContractTest.java`
- `backend/src/test/java/com/example/aiec/modules/backoffice/adapter/rest/BoAdminControllerContractTest.java`
- `bff/shared/src/dto/user.dto.ts`
- `bff/customer-bff/src/members/members.controller.ts`
- `bff/customer-bff/src/members/members.service.ts`
- `bff/backoffice-bff/src/members/members.controller.ts`
- `bff/backoffice-bff/src/members/members.service.ts`
- `frontend/src/entities/customer/model/types.ts`
- `frontend/src/entities/customer/model/api.ts`
- `frontend/src/entities/customer/index.ts`
- `frontend/src/pages/customer/MyPagePage/index.tsx`
- `frontend/src/app/router/customer.tsx`
- `frontend/src/widgets/CustomerLayout/CustomerLayout.tsx`
- `frontend/src/pages/admin/AdminMembersPage/index.tsx`
- `docs/data-model.md`
- `docs/api/customer-bff-openapi.json`
- `docs/api/backoffice-bff-openapi.json`
- `docs/api/openapi.json`
- `docs/ui/customer-ui.md`
- `docs/ui/admin-ui.md`
- `docs/requirements.md`
- `docs/impl-notes/CHG-020.md`

### リスクと未解決
- Final Gate 記載の `bash ./frontend/e2e/customer-smoke.sh` はリポジトリルート実行だと `../docker-compose.yml` 解決に失敗するため、`cd frontend && bash ./e2e/customer-smoke.sh` で実行した。
- Final Gate 記載の `curl http://localhost:8080/actuator/health` はホスト公開されておらず `000`（compose 上 `backend` は内部 `8080/tcp` のみ）。内部確認 (`docker compose exec backend ...`) では `200`。

### テスト結果（PASS/FAIL、失敗時は30行以内）
- PASS: `cd backend && ./mvnw compile`
- PASS: `cd backend && ./mvnw test`
- PASS: `cd bff/customer-bff && npm run build`
- PASS: `cd bff/backoffice-bff && npm run build`
- PASS: `npm run build --prefix frontend`
- PASS: `cd frontend && bash ./e2e/customer-smoke.sh`
- PASS: `cd frontend && bash ./e2e/admin-smoke.sh`
- PASS: `docker compose up -d`
- PASS: `curl ... http://localhost:3001/health` (`customer-bff=200`)
- PASS: `curl ... http://localhost:3002/health` (`backoffice-bff=200`)
- FAIL: `curl ... http://localhost:8080/actuator/health` (`curl: (7) Failed to connect ...`, `backend=000`)
