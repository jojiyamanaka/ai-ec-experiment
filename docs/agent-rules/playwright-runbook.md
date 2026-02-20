# Playwright 実行ガイド（最小）

最終更新: 2026-02-20

## 目的
MCP Playwright で UI 手動確認を実施するための最小手順を示す。

## 前提
- `docker compose up -d` 済み
- 対象 URL が `200` 応答
  - `http://localhost:5173`
  - `http://localhost:5174`
  - `http://localhost:3001/health`
  - `http://localhost:3002/health`

## 標準フロー（MCP）
1. `browser_navigate` で対象 URL を開く
2. `browser_snapshot` で要素参照を取得する
3. `browser_click` / `browser_fill_form` で操作する
4. 遷移時は URL と表示要素の両方で待機する
5. 必要な場面だけ `browser_take_screenshot` で証跡を残す
6. `browser_close` で終了する

## 失敗時の最小切り分け
1. サービスが起動しているか（frontend / bff の health）
2. 要素参照が更新されていないか（再 `browser_snapshot`）
3. 認証状態がシナリオ前提と一致しているか（ログイン済み/未ログイン）

## 記録ルール
- 結果は `docs/04_review-note/CHG-XXX.md` に記録する
- 形式は `[PASS]` / `[FAIL] - 理由`
