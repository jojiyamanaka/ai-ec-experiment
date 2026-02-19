# CHG-019: BFF OpenAPI 導入（@nestjs/swagger）

作成日: 2026-02-19

---

## 1. 背景

- Core API（Spring Boot）は CHG-OpenAPI 対応で `docs/api/openapi.json` が SSOT として整備された。
- BFF（Customer BFF / BackOffice BFF）には対応する OpenAPI が存在せず、フロントエンドが参照できる API 契約の SSOT がない。
- 設計書に書いた API 仕様とコードが乖離した場合、どちらが正しいか判断できない。

---

## 2. 要件

- Customer BFF・BackOffice BFF それぞれの全エンドポイントについて OpenAPI spec を生成できること。
- 生成された spec がフロントエンド開発者・設計者の API 契約 SSOT として機能すること。
- Core API と同様に CI（GitHub Actions）で自動生成・コミットされること。
- ローカル開発時も簡単なコマンドで spec を更新できること。

---

## 3. 受け入れ条件

- `docs/api/customer-bff-openapi.json` と `docs/api/backoffice-bff-openapi.json` が生成・git 管理されること。
- BFF 起動中に Swagger UI（`/api-docs`）でエンドポイント一覧が確認できること。
- GitHub Actions で BFF ソースコードの変更時に spec が自動更新されること。

---

## 4. スコープ

- **対象**: `bff/customer-bff`, `bff/backoffice-bff`
- **対象外**: Core API（対応済み）、フロントエンドの型定義自動生成（将来対応）

---

## 5. 非目標

- フロントエンドコードの自動生成（openapi-generator 等）は今回のスコープ外。
- 認証フロー・リクエスト検証の強化は対象外。
