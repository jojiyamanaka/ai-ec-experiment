# CHG-025: Flyway V2 マイグレーション重複解消 - 技術設計

要件: `docs/01_requirements/CHG-025_Flyway-V2重複解消.md`
作成日: 2026-02-22

---

## 1. 設計方針

**根本原因**: `backend/src/main/resources/db/archive/` は Maven のリソースディレクトリ配下にあるため、`target/classes/db/archive/` へコピーされる。過去に `db/flyway/` にあった `V2__insert_sample_data.sql` を archive へ移動した後も、`target/classes/db/flyway/` に stale ファイルが残留し続ける（Maven は target の不要ファイルを削除しない）。

**採用方針: `db/archive/` を `src/main/resources/` 配下から移動する**

- 移動先: `backend/db-archive/`（Maven リソーススキャン対象外）
- 理由: アーカイブ SQL はデプロイ不要な参照用ドキュメント。`src/main/resources/` に置く必然性がない
- 効果: 移動後は `./mvnw test` を何度繰り返しても archive SQL が classpath に混入しない
- stale ファイルの扱い: 本 CHG の実装時に `./mvnw clean` を一度実行し、既存 stale ファイルを除去する。`target/` は gitignore 対象のため、新規クローン環境では発生しない

**不採用: `maven-resources-plugin` による除外設定**

`pom.xml` に exclude を追加する方法も有効だが、設定がビルドファイルに散在して保守コストが上がる。ディレクトリ配置で解決するほうが自明。

---

## 2. 影響範囲

| 区分 | 対象 | 変更概要 |
|------|------|---------|
| ディレクトリ移動 | `backend/src/main/resources/db/archive/` → `backend/db-archive/` | Maven リソーススキャン対象外へ移動 |
| 既存変更 | `backend/db-archive/flyway-v2-to-v12/README.md` | パス参照を更新 |
| ドキュメント更新 | `docs/agent-rules/testing-operations.md` | `./mvnw test`（clean なし）が使用可能と明記 |
| 影響なし | `db/flyway/V1__create_schema.sql`、`db/flyway/V2__baseline_current_schema.sql` | 内容変更なし |
| 影響なし | `application.yml`、`application-test.yml` | `spring.flyway.locations` 変更なし |

---

## 3. テスト観点

- 正常系: `./mvnw test` が `BUILD SUCCESS`（ApplicationContext 正常起動、Flyway V2 重複エラーなし）
- 正常系: `./mvnw clean test` も同様に PASS
- 確認: `db-archive/` 配下の SQL ファイルが classpath に存在しないこと（`target/classes/` に含まれないこと）
