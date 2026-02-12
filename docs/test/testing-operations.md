# テスト実施ガイド（Docker / WSL bash）

最終更新: 2026-02-12

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
docker compose exec backend ./mvnw compile
docker compose exec frontend npm run build
```

## タスク実装後の検証フロー
1. `task.md` 冒頭の検証コマンドを実行する
2. エラー時のみ修正して再検証する
3. 受け入れ条件ごとに `[PASS]` / `[FAIL] - 理由` を記録する

## WSL (bash) での API 手動テスト
1. `curl` を使う
2. JSON は `-d` 直書きせず、一時ファイル + `--data-binary "@<tempfile>"` を使う
3. JSON 保存はヒアドキュメントで一時ファイルへ書き込む
4. カート/注文 API は `X-Session-Id` ヘッダーを付与する

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

## 関連ドキュメント
- Playwright 利用方法: `docs/test/playwright-runbook.md`
