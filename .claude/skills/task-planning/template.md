# CHG-XXX: <機能名> - 実装タスク

要件: `docs/01_requirements/CHG-XXX_<機能名>.md`
設計: `docs/02_designs/CHG-XXX_<機能名>.md`
作成日: YYYY-MM-DD

---

## タスク一覧

### バックエンド

- [ ] **T-1** `[SAFE]`: （作業内容を1行で）

  触る範囲: `モジュール名 / クラス名`

  Done: （観測可能な完了条件 — コンパイル通過 / テスト通過 / 画面表示など）

---

- [ ] **T-2** `[CONTRACT]`: （API契約またはDBスキーマを変更するタスク）

  触る範囲: `モジュール名 / クラス名`

  Done: （観測可能な完了条件）

  > 📝 ゲート高。Codex は impl-notes（変更の概要・判断の根拠）を `docs/impl-notes/CHG-XXX.md` の `## T-N` セクションに追記し、追加検証コマンドを実行すること。

---

- [ ] **T-3** `[ARCH]`: （モジュール境界・トランザクション境界を変更するタスク）

  触る範囲: `モジュール名 / クラス名`

  Done: （観測可能な完了条件）

  > 📝 ゲート高。Codex は impl-notes（変更の概要・判断の根拠）を `docs/impl-notes/CHG-XXX.md` の `## T-N` セクションに追記し、追加検証コマンドを実行すること。

---

### フロントエンド（該当する場合）

- [ ] **T-N** `[SAFE]`: ...

  触る範囲: `src/features/xxx / ComponentName`

  Done: ...

---

### BFF（該当する場合）

- [ ] **T-N** `[SAFE]`: ...

  触る範囲: `bff/customer-bff / ServiceName`

  Done: ...

---

## 実装順序

```
T-1 → T-2 → T-3 → T-4 → T-5
```

---

## 検証

### Per-task 検証コマンド

各タスク完了時に実行する最小検証:

```bash
# バックエンドタスク完了時
cd backend && ./mvnw compile

# バックエンドテストを含むタスク完了時
cd backend && ./mvnw test -Dtest=TargetTestClass
```

### Final Gate（全タスク完了後に必ず実行し、結果をこのファイルに貼り付けること）

```bash
# バックエンド全体
cd backend && ./mvnw compile
cd backend && ./mvnw test

# フロントエンド
npm run build --prefix frontend

# コンテナ起動が必要な場合
docker compose up -d
```

**Final Gate 結果:** （例: 全テスト成功・コンパイル通過）

---

## テスト手順

1. （シナリオ → 期待結果）
2. ...
