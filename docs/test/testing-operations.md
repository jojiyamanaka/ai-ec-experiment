# テスト実施ガイド（Docker / WSL bash）

最終更新: 2026-02-13

## 目的
`AGENTS.md` にあったテスト実施手順と操作ルールをまとめる。

## 基本方針
1. テスト・検証は Docker 経由で実行する
2. 機能開発時は `docs/03_tasks/CHG-XXX_*.md` の検証コマンドを優先する
3. 受け入れ条件は `docs/01_requirements/CHG-XXX_*.md` を基準に判定する

## 共通検証コマンド
ローカルに JDK / Node がない前提で実行する。

```bash
docker compose up -d
docker compose ps
docker compose logs backend --since 5m
docker compose exec backend ./mvnw compile
docker compose exec frontend npm run build
```

## タスク実装後の検証フロー
1. `task.md` 冒頭の検証コマンドを実行する
2. ログ確認は `docker compose logs <service> --since 5m` を使い、過去ログ混在を避ける
3. エラー時のみ修正して再検証する
4. 受け入れ条件ごとに `[PASS]` / `[FAIL] - 理由` を記録する

記録例:

```text
[PASS] 会員登録 API: 200 を確認
[FAIL] 注文作成 API: 500 - Flyway 検証エラーで backend 未起動
```

## WSL (bash) での API 手動テスト
1. `curl` を使う
2. JSON は `-d` 直書きせず、一時ファイル + `--data-binary "@<tempfile>"` を使う
3. JSON 保存はヒアドキュメントで一時ファイルへ書き込む
4. カート/注文 API は `X-Session-Id` ヘッダーを付与する
5. `email` / `X-Session-Id` は時刻付きで一意値を使う（再実行時の重複回避）
6. `localhost:8080` へ直接到達できない場合は `docker compose exec backend` 内から `curl` を実行する

例:

```bash
session="manual-test-001"
tmp="$(mktemp)"
cat >"$tmp" <<'JSON'
{"productId":4,"quantity":5}
JSON
curl -s -X POST \
  -H "X-Session-Id: ${session}" \
  -H "Content-Type: application/json" \
  --data-binary "@${tmp}" \
  "http://localhost:8080/api/order/cart/items"
rm -f "$tmp"
```

`localhost` 直叩きが失敗する場合の例:

```bash
docker compose exec backend curl -sS "http://localhost:8080/api/item"
```

## SQLスクリプト実行（Docker）
`psql -f` が環境依存で失敗する場合、標準入力経由で実行する。

```bash
cat scripts/adjust_sequences.sql | docker compose exec -T postgres psql -U ec_app_user -d ec_app
```

## トラブルシュート
### Flyway checksum mismatch
マイグレーション適用済み SQL を変更した場合に発生する。`repair` 実行時はマイグレーション位置を明示する。

```bash
docker compose run --rm backend ./mvnw \
  -Dflyway.url=jdbc:postgresql://postgres:5432/ec_app \
  -Dflyway.user=ec_app_user \
  -Dflyway.password=changeme \
  -Dflyway.locations=filesystem:src/main/resources/db/flyway \
  flyway:repair
```

## 関連ドキュメント
- Playwright 利用方法: `docs/test/playwright-runbook.md`
