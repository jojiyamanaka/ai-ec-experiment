# CHG-020: User データモデル項目追加と画面反映 - 技術設計

要件: `docs/01_requirements/CHG-020_User データモデル項目追加と画面反映.md`  
作成日: 2026-02-19

**SSOT（唯一の真実）**:
- API契約: このドキュメントの「API契約」セクション
- DBスキーマ: `backend/src/main/resources/db/flyway/V9__extend_user_profile_and_addresses.sql`（Flywayが正）
- モジュール境界: `backend/AGENTS.md` のモジュール依存ルール
- 監査・認証ルール: `docs/specs/authentication.md`

---

## 1. 設計方針

### 1.1 スコープ境界

- 本 CHG は `User` ドメインのみを対象とし、Product/Cart/Order 系の契約は変更しない。
- `users` を会員情報の主集約とし、複数住所は新設 `user_addresses` で 1:N 管理する。
- 顧客向けと BO 向けで DTO を分離し、内部運用情報の露出境界を固定する。
- BO会員管理に「会員新規登録」を追加し、運用者が顧客会員を作成できるようにする。

### 1.2 `users` 追加項目（カテゴリ確定）

| カテゴリ | 追加カラム | 用途 | 顧客更新 | BO更新 |
|---|---|---|---|---|
| 識別/認証連携 | `last_login_at`, `terms_agreed_at` | 認証連携・利用状況把握 | 不可（参照のみ） | 不可（システム管理） |
| 連絡/プロフィール | `full_name`, `phone_number`, `birth_date`, `newsletter_opt_in` | 顧客連絡・本人情報 | 可 | 可 |
| 会員運用 | `member_rank`, `loyalty_points`, `deactivation_reason` | BO運用・会員施策 | 不可（参照のみ） | 可 |

補足:
- 既存の `email`, `display_name`, `is_active` は継続利用する。
- システム管理項目（`password_hash`、トークン、監査カラム等）は更新禁止とする。

### 1.3 `user_addresses` 方針

- 住所は `user_addresses` で管理し、1会員に複数レコードを許可する。
- 住所の同時更新整合を保つため、`is_default=true` は会員ごとに最大1件。
- `user_addresses` は `users` と同様に監査カラム + 論理削除を持つ。

`user_addresses` の主要カラム:
- `id`, `user_id`, `label`, `recipient_name`, `recipient_phone_number`
- `postal_code`, `prefecture`, `city`, `address_line1`, `address_line2`
- `is_default`, `address_order`
- 監査カラム（`created_at` 等）

---

## 2. API契約

### 2.1 DTO境界

`UserAddressDto`（顧客/BO共通）:
- `id`, `label`, `recipientName`, `recipientPhoneNumber`
- `postalCode`, `prefecture`, `city`, `addressLine1`, `addressLine2`
- `isDefault`, `addressOrder`

`CustomerProfileDto`（顧客向け）:
- 公開: `id`, `email`, `displayName`, `fullName`, `phoneNumber`, `birthDate`, `newsletterOptIn`
- 公開: `memberRank`, `loyaltyPoints`, `isActive`, `createdAt`, `updatedAt`, `addresses: UserAddressDto[]`
- 非公開: `deactivationReason`, システム管理項目

`AdminMemberDto`（BO向け）:
- `CustomerProfileDto` の全項目
- 追加公開: `deactivationReason`, `lastLoginAt`, `termsAgreedAt`

### 2.2 顧客向け契約（Customer BFF）

| エンドポイント | メソッド | 認証 | 契約 |
|---|---|---|---|
| `/api/members/me` | GET | User | `CustomerProfileDto` を返す |
| `/api/members/me` | PUT | User | 顧客更新可能項目のみ更新。レスポンスは `CustomerProfileDto` |
| `/api/members/me/addresses` | POST | User | 住所追加。レスポンスは追加後 `UserAddressDto` |
| `/api/members/me/addresses/{addressId}` | PUT | User | 自分の住所のみ更新。レスポンスは更新後 `UserAddressDto` |
| `/api/members/me/addresses/{addressId}` | DELETE | User | 自分の住所を論理削除。レスポンスは `success=true` |

顧客向け `PUT /api/members/me` で受け付ける項目:
- `displayName`, `fullName`, `phoneNumber`, `birthDate`, `newsletterOptIn`

受け付けない項目（送信時は拒否）:
- `memberRank`, `loyaltyPoints`, `deactivationReason`, `isActive`
- `passwordHash`, トークン関連、監査カラム

### 2.3 管理向け契約（BackOffice BFF）

既存互換:
- 既存の `/api/admin/members/*` と `/api/bo/admin/members/*` の両方を維持する。

| エンドポイント | メソッド | 認証 | 契約 |
|---|---|---|---|
| `/api/admin/members` | POST | BoUser(ADMIN以上) | 会員新規登録 `AdminMemberDto` |
| `/api/admin/members` | GET | BoUser(ADMIN以上) | 一覧 `AdminMemberDto[]` |
| `/api/admin/members/{id}` | GET | BoUser(ADMIN以上) | 詳細 `AdminMemberDto` |
| `/api/admin/members/{id}` | PUT | BoUser(ADMIN以上) | 会員情報の FULL 更新（許可項目のみ） |
| `/api/admin/members/{id}/status` | PUT | BoUser(ADMIN以上) | 既存の有効/無効更新を維持 |

`POST /api/admin/members` で受け付ける項目:
- 必須: `email`, `displayName`, `password`
- 任意: `fullName`, `phoneNumber`, `birthDate`, `newsletterOptIn`
- 任意: `memberRank`, `loyaltyPoints`, `isActive`, `deactivationReason`
- 任意: `addresses: UserAddressUpsert[]`

作成時の補完ルール:
- `memberRank` 未指定は `STANDARD`
- `loyaltyPoints` 未指定は `0`
- `isActive` 未指定は `true`

`PUT /api/admin/members/{id}` で受け付ける項目:
- `displayName`, `fullName`, `phoneNumber`, `birthDate`, `newsletterOptIn`
- `memberRank`, `loyaltyPoints`, `deactivationReason`, `isActive`
- `addresses: UserAddressUpsert[]`（追加・更新・削除対象を含む）

BOでも更新禁止（送信時は拒否）:
- `passwordHash`, トークン関連、監査カラム、`lastLoginAt`, `termsAgreedAt`

### 2.4 Core API契約（BFFのマッピング先）

| Core API | 用途 |
|---|---|
| `GET /api/auth/me` | 顧客プロフィール取得（拡張後 `CustomerProfileDto` 相当） |
| `PUT /api/auth/me` | 顧客プロフィール更新 |
| `POST /api/auth/me/addresses` | 顧客住所追加 |
| `PUT /api/auth/me/addresses/{addressId}` | 顧客住所更新 |
| `DELETE /api/auth/me/addresses/{addressId}` | 顧客住所削除 |
| `POST /api/bo/admin/members` | BO会員新規登録 |
| `GET /api/bo/admin/members` | BO会員一覧 |
| `GET /api/bo/admin/members/{id}` | BO会員詳細 |
| `PUT /api/bo/admin/members/{id}` | BO会員 FULL 更新 |
| `PUT /api/bo/admin/members/{id}/status` | 既存の状態更新 |

エラー契約:
- 更新禁止項目を含むリクエストは `INVALID_REQUEST` で拒否。
- 他会員の住所更新/削除試行は `FORBIDDEN`（または既存の認可エラー契約）で拒否。
- 重複メールの会員作成は `EMAIL_ALREADY_EXISTS`（409）で拒否。

---

## 3. モジュール・レイヤ構成

### 3.1 Backend

```
customer/
  domain/entity/
    User
    UserAddress              (新規)
  domain/repository/
    UserRepository
    UserAddressRepository    (新規)
  domain/service/
    UserService              (拡張)
    UserProfileService       (新規: profile/address操作)
  adapter/dto/
    UserDto                  (拡張: 顧客公開項目)
    UserAddressDto           (新規)
    UpdateMyProfileRequest   (新規)
  adapter/rest/
    AuthController           (拡張: PUT /me, addresses API)

backoffice/
  adapter/dto/
    MemberDetailDto          (拡張: User拡張 + addresses)
    CreateMemberRequest      (新規)
    UpdateMemberRequest      (新規)
  adapter/rest/
    BoAdminController        (拡張: POST/PUT /members)
```

### 3.2 BFF

```
customer-bff/
  members/
    members.controller.ts    (拡張: PUT /me + addresses API)
    members.service.ts       (拡張)

backoffice-bff/
  members/
    members.controller.ts    (拡張: POST / + PUT /:id)
    members.service.ts       (拡張)
```

### 3.3 Frontend

```
entities/customer/
  model/types.ts             (User + Address型拡張)
  model/api.ts               (profile/address API追加)

pages/customer/
  MyPagePage                 (新規)

pages/admin/
  AdminMembersPage           (拡張: 新規登録 + FULL更新 + 住所管理UI)

app/router/customer.tsx      (/mypage 追加)
widgets/CustomerLayout       (マイページ導線追加)
```

---

## 4. 主要クラス/IFの責務

| クラス/IF | 責務 | レイヤ |
|---|---|---|
| `UserAddress` | 会員住所の永続モデル。デフォルト住所制約の対象 | customer/domain |
| `UserAddressRepository` | `userId` 単位の住所取得・更新・論理削除 | customer/domain |
| `UserProfileService` | 顧客自身のプロフィール更新/住所CRUD、公開可能フィールド制御 | customer/domain |
| `AuthController` | `/api/auth/me` 系 API 契約の入口。認証済み会員ID解決 | customer/adapter |
| `BoAdminController` | BO会員作成/FULL更新/status更新の認可境界 | backoffice/adapter |
| `MembersService` (customer-bff) | Customer BFF から Core API の me/address API に中継 | bff/customer |
| `MembersService` (backoffice-bff) | BackOffice BFF から会員更新 API に中継 | bff/backoffice |
| `AdminMembersPage` | BO会員新規作成と会員情報・住所の編集UI（許可項目のみ） | frontend/pages |
| `MyPagePage` | 顧客の自己情報参照/更新、住所管理UI | frontend/pages |

---

## 5. トランザクション・非同期方針

- 顧客のプロフィール更新と住所更新は、API単位で `@Transactional` を適用する。
- BO会員新規登録は `users` + `user_addresses` + パスワードハッシュ化を単一トランザクションで処理する。
- BOの `PUT /members/{id}` は user本体と address群を同一トランザクションで更新し、部分更新を防ぐ。
- `is_default` 更新時は同一トランザクション内で同会員の他住所を `false` に正規化する。
- 監査ログ（`ADMIN_ACTION`）は既存の Outbox 発行方針を継続する。
- 非同期処理・リトライ戦略の新規追加は行わない（既存の BFF/Core API リトライ設定を踏襲）。

---

## 6. 処理フロー

### 6.1 顧客マイページ更新

```
MyPage (Frontend)
  → Customer BFF /api/members/me (PUT)
    → Core API /api/auth/me (PUT)
      → UserProfileService
        → users 更新
        → 必要時 user_addresses 更新
      → CustomerProfileDto 返却
  → 画面再描画
```

### 6.2 BO会員 FULL 更新

```
AdminMembersPage
  → BackOffice BFF /api/admin/members/{id} (PUT)
    → Core API /api/bo/admin/members/{id} (PUT)
      → BoAdminController (ADMIN権限確認)
      → UserProfileService (BOモード)
        → 許可項目のみ更新
        → 更新禁止項目が含まれる場合は拒否
      → AdminMemberDto 返却
```

### 6.3 BO会員新規登録

```
AdminMembersPage (新規登録フォーム)
  → BackOffice BFF /api/admin/members (POST)
    → Core API /api/bo/admin/members (POST)
      → BoAdminController (ADMIN権限確認)
      → UserService / UserProfileService
        → メール重複チェック
        → password をハッシュ化して users 作成
        → 初期 addresses 作成（指定時）
      → AdminMemberDto 返却
```

---

## 7. 影響範囲

| 区分 | 対象（クラス/ファイル） | 変更概要 |
|---|---|---|
| 新規作成 | `backend/src/main/resources/db/flyway/V9__extend_user_profile_and_addresses.sql` | `users` 拡張 + `user_addresses` 新設 |
| 新規作成 | `backend/src/main/java/com/example/aiec/modules/customer/domain/entity/UserAddress.java` | 住所エンティティ |
| 新規作成 | `backend/src/main/java/com/example/aiec/modules/customer/domain/repository/UserAddressRepository.java` | 住所リポジトリ |
| 新規作成 | `backend/src/main/java/com/example/aiec/modules/customer/domain/service/UserProfileService.java` | プロフィール/住所更新ユースケース |
| 既存変更 | `backend/src/main/java/com/example/aiec/modules/customer/domain/entity/User.java` | 追加カラム対応 |
| 既存変更 | `backend/src/main/java/com/example/aiec/modules/customer/adapter/dto/UserDto.java` | 顧客向けDTO拡張 |
| 既存変更 | `backend/src/main/java/com/example/aiec/modules/customer/adapter/rest/AuthController.java` | `/api/auth/me` 系拡張 |
| 既存変更 | `backend/src/main/java/com/example/aiec/modules/backoffice/adapter/dto/MemberDetailDto.java` | BO向け項目/住所反映 |
| 既存変更 | `backend/src/main/java/com/example/aiec/modules/backoffice/adapter/rest/BoAdminController.java` | 新規登録 + FULL更新エンドポイント追加 |
| 既存変更 | `backend/src/main/java/com/example/aiec/modules/customer/domain/service/UserService.java` | BO経由会員作成の入口追加 |
| 既存変更 | `bff/customer-bff/src/members/members.controller.ts` | me/address API追加 |
| 既存変更 | `bff/customer-bff/src/members/members.service.ts` | Core APIマッピング追加 |
| 既存変更 | `bff/backoffice-bff/src/members/members.controller.ts` | `POST /` + `PUT /:id` 追加 |
| 既存変更 | `bff/backoffice-bff/src/members/members.service.ts` | 新規登録 + FULL更新呼び出し追加 |
| 既存変更 | `frontend/src/entities/customer/model/types.ts` | User/Address 型拡張 |
| 既存変更 | `frontend/src/entities/customer/model/api.ts` | profile/address API追加 |
| 新規作成 | `frontend/src/pages/customer/MyPagePage/index.tsx` | マイページ UI |
| 既存変更 | `frontend/src/app/router/customer.tsx` | `/mypage` 追加 |
| 既存変更 | `frontend/src/widgets/CustomerLayout/CustomerLayout.tsx` | マイページ導線 |
| 既存変更 | `frontend/src/pages/admin/AdminMembersPage/index.tsx` | 新規登録 + FULL更新UI化 |
| 既存変更 | `docs/data-model.md` | `users`/`user_addresses` 仕様反映 |
| 既存変更 | `docs/api/customer-bff-openapi.json` | 顧客向け契約反映 |
| 既存変更 | `docs/api/backoffice-bff-openapi.json` | 管理向け契約反映 |
| 既存変更 | `docs/api/openapi.json` | Core API契約反映 |
| 既存変更 | `docs/ui/customer-ui.md` | マイページ仕様反映 |
| 既存変更 | `docs/ui/admin-ui.md` | 会員 FULL更新仕様反映 |
| 既存変更 | `docs/requirements.md` | 会員/住所管理ルール追記 |

---

## 8. テスト観点

- 正常系:
  - BOが `POST /api/admin/members` で会員を新規作成できる。
  - 顧客が `PUT /api/members/me` でプロフィール更新できる。
  - 顧客が住所を複数登録・更新・削除できる。
  - BOが会員情報と住所を一括更新できる。
  - `is_default` が常に1件以下に保たれる。
- 異常系:
  - BO会員作成で重複メールを送ると `EMAIL_ALREADY_EXISTS` で拒否される。
  - 顧客が `memberRank` 等の非許可項目を更新すると拒否される。
  - BOが `passwordHash`/監査項目更新を試みると拒否される。
  - 他会員の住所IDを指定した操作は拒否される。
- 境界値:
  - 住所0件、1件、多件の各ケースで取得順序が安定する。
  - `loyaltyPoints=0`、`deactivationReason` 空/設定ありで DTO が整合する。
  - 既存会員データ（追加カラム NULL/DEFAULT）でも API 取得が成功する。
