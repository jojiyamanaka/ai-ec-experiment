# CHG-025: Flyway V2 マイグレーション重複解消（要件定義）

作成日: 2026-02-22
CHG番号: CHG-025
ステータス: 起票済み
優先度: 高

> **方針**: このドキュメントには **Why（なぜ）** と **What（何を）** のみを書く。
> 技術的な How（クラス設計・SQL・アーキテクチャ選択）は設計書（`02_designs/`）に委ねる。

---

## 1. 背景

`cd backend && ./mvnw test` を実行すると、Flyway が `Found more than one migration with version 2` エラーを出して ApplicationContext の生成に失敗する。結果としてすべての Spring Boot 統合テスト（例: `BoAuthSecurityTest`）が起動すらできない状態にある。

原因は `src/main/resources/db/` 以下に V2 という同一バージョン番号を持つ SQL ファイルが2本存在することにある。

- `db/flyway/V2__baseline_current_schema.sql` — 現行の flyway スキャン対象ディレクトリに置かれた現役ファイル
- `db/archive/flyway-v2-to-v12/V2__insert_sample_data.sql` — 歴史的経緯で archive に移されたはずのファイル

`src/main/resources/` 配下はすべて Maven によって `target/classes/` にコピーされる。`application.yml` の Flyway 設定は `classpath:db/flyway` を指しているが、`target/classes/db/flyway/` には過去のビルド成果物として `V2__insert_sample_data.sql` が残留している（`mvn clean` なしでビルドした際に stale ファイルが蓄積したもの）。

この状態は `./mvnw clean test` では回避できるが、`./mvnw test` 単体では必ず失敗する。Final Gate・CI 双方に影響するため、クリーンビルドに依存しない恒久的な解消が必要。

---

## 2. 要件

### 2-1. アーカイブ SQL の classpath 除外

`db/archive/` 配下に格納された旧マイグレーション SQL は、実行時・テスト時の classpath に含まれないよう管理すること。過去の移行経緯を記録する目的のドキュメントとして保持は可であるが、Flyway がスキャンできる場所に置かない。

### 2-2. `./mvnw test` の全テスト PASS

クリーンビルド（`mvn clean`）を前提とせず、`./mvnw test` 単体で全 Spring Boot テストが ApplicationContext を正常に起動し、PASS すること。

---

## 3. 受け入れ条件

- [ ] `cd backend && ./mvnw test` が `BUILD SUCCESS` で終了する（`BoAuthSecurityTest` を含む）
- [ ] Flyway が V2 重複エラーを出さない
- [ ] `cd backend && ./mvnw clean test` でも同様に PASS する
- [ ] `db/archive/` 以下の SQL ファイルが Flyway のスキャン対象外であることが設定または配置から明らかである

---

## 4. スコープ外

- `db/flyway/V1__create_schema.sql`・`V2__baseline_current_schema.sql` の内容変更
- アーカイブ SQL の内容修正・削除
- Flyway バージョンのアップグレード
- CI パイプラインの設定変更

---

## 5. 仕様への反映

- `docs/agent-rules/testing-operations.md` — Final Gate コマンドに `./mvnw test`（`clean` なし）が使用可能であることを明記

---

## 6. 関連資料

- `docs/archive/04_review-note/CHG-024.md` — リスクと未解決セクション（Flyway V2 重複の発見経緯）
- `backend/src/main/resources/application.yml` — `spring.flyway.locations` 設定
- `backend/src/test/resources/application-test.yml` — テスト用 Flyway 設定
- `backend/src/main/resources/db/flyway/` — 現行マイグレーションディレクトリ
- `backend/src/main/resources/db/archive/flyway-v2-to-v12/` — アーカイブ SQL 格納場所
