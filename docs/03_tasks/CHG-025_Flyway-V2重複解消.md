# CHG-025: Flyway V2 マイグレーション重複解消 - 実装タスク

要件: `docs/01_requirements/CHG-025_Flyway-V2重複解消.md`
設計: `docs/02_designs/CHG-025_Flyway-V2重複解消.md`
作成日: 2026-02-22

---

## タスク一覧

### バックエンド

- [ ] **T-1** `[SAFE]`: `db/archive/` を Maven リソーススキャン対象外へ移動し、stale ファイルを除去する

  触る範囲: `backend/src/main/resources/db/archive/` → `backend/db-archive/`

  作業手順:
  1. `backend/src/main/resources/db/archive/` を `backend/db-archive/` へ移動（`git mv`）
  2. `backend/db-archive/flyway-v2-to-v12/README.md` のパス参照を更新
  3. `./mvnw clean` を一度実行して `target/` の stale ファイルを除去

  Done: `cd backend && ./mvnw test` が `BUILD SUCCESS`（Flyway V2 重複エラーなし）

---

- [ ] **T-2** `[SAFE]`: `docs/agent-rules/testing-operations.md` を更新する

  触る範囲: `docs/agent-rules/testing-operations.md`

  Done: `./mvnw test`（`clean` なし）が Final Gate コマンドとして使用可能と明記されていること

---

## 実装順序

```
T-1 → T-2
```

---

## 検証

### Final Gate（全タスク完了後に必ず実行し、結果を `docs/04_review-note/CHG-025.md` に記録すること）

```bash
cd backend && ./mvnw test
cd backend && ./mvnw clean test
```

**Final Gate 記録先:** `docs/04_review-note/CHG-025.md` の `## Final Gate 結果`

---

## テスト手順

1. `cd backend && ./mvnw test` → `BUILD SUCCESS`、Flyway V2 重複エラーが出ないこと
2. `cd backend && ./mvnw clean test` → 同様に `BUILD SUCCESS`
3. `target/classes/` 配下に `db-archive/` 以下の SQL が含まれていないこと（`ls target/classes/db/` で `archive/` ディレクトリが存在しないことを確認）
