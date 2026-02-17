# CHG-XXX: <機能名> - 実装タスク

要件: `docs/01_requirements/CHG-XXX_<機能名>.md`
設計: `docs/02_designs/CHG-XXX_<機能名>.md`
作成日: YYYY-MM-DD

検証コマンド:
- バックエンド: `docker compose exec backend ./mvnw compile`
- フロントエンド: `docker compose exec frontend npm run build`
- コンテナ未起動の場合: `docker compose up -d` を先に実行

---

## タスク一覧

### バックエンド

- [ ] **T-1**: （変更内容の要約）

  パス: `backend/src/main/java/...`

  （挿入位置のランドマーク）:

  ```java
  // 具体的なコード
  ```

---

- [ ] **T-2**: ...

---

### フロントエンド

- [ ] **T-N**: （変更内容の要約）

  パス: `frontend/src/...`

  （挿入位置のランドマーク）:

  ```typescript
  // 具体的なコード
  ```

---

### BFF（該当する場合）

- [ ] **T-N**: ...

---

### ドキュメント更新（該当する場合）

- [ ] **T-N**: ...

---

## 実装順序

```
T-1, T-2（並行可能）
  → T-3
    → T-4, T-5（並行可能）
      → T-6（ドキュメント）
```

---

## テスト手順

実装後に以下を手動確認:

1. （テストシナリオ → 期待結果）
2. ...
