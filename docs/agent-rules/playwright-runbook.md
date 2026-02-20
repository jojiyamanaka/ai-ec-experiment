# Playwright 利用ガイド（MCP推奨 / Dockerフォールバック）

最終更新: 2026-02-20

## 目的
このドキュメントは、このリポジトリで Playwright を使って UI 検証を行うための「実行方法」と「注意点」をまとめたものです。

## 基本方針
1. UI 手動検証は MCP Playwright を推奨する（`browser_navigate` / `browser_snapshot` / `browser_click` など）
2. 再実行可能な検証スクリプト化や CI 連携が必要な場合は Docker 実行を使う
3. Playwright を `frontend` コンテナ内で直接実行しない

## 使い分けの判断基準
1. 画面を見ながらのデバッグ・単発確認は MCP を使う
2. 再現性が必要な検証（共有・CI転用・定期実行）は Docker を使う
3. 迷った場合は MCP から開始し、必要に応じて Docker に切り替える

## 事前確認
リポジトリルートで実行する。

```bash
docker compose up -d
docker compose ps
curl -s "http://localhost:5173" >/dev/null
curl -s "http://localhost:5174" >/dev/null
curl -s "http://localhost:3001/health" >/dev/null
curl -s "http://localhost:3002/health" >/dev/null
```

## MCP 実行手順（標準）
1. 事前確認コマンドがすべて `200` であることを確認する
2. `browser_navigate` で対象 URL を開く
3. `browser_snapshot` で要素参照を取得してから操作する
4. 遷移系は URL だけでなく表示要素でも待機する
5. 検証結果は必要に応じて `browser_take_screenshot` で記録する
6. 終了時は `browser_close` でブラウザを閉じる

## スクリプト配置ルール（Docker フォールバック時）
1. 検証スクリプトは一時的に `tmp/*.mjs` へ作成する
2. 実行後は `tmp` を削除する

## 最小テンプレート
```javascript
import { chromium } from 'playwright';

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext();
const page = await context.newPage();

try {
  await page.goto('http://localhost:5173', { waitUntil: 'domcontentloaded' });
  console.log(await page.title());
} finally {
  await browser.close();
}
```

## Docker 実行コマンド（フォールバック）
`tmp/<script>.mjs` を作成したあと、以下で実行する。

```bash
docker run --rm --network host \
  -v "$(pwd):/work" \
  -w /tmp \
  mcr.microsoft.com/playwright:v1.58.2-noble \
  bash -lc "npm init -y >/dev/null 2>&1 && npm i playwright >/dev/null 2>&1 && cp /work/tmp/<script>.mjs /tmp/<script>.mjs && node /tmp/<script>.mjs"
```

## 注意点
1. SPA 遷移の待機は `waitForURL` だけでなく、要素表示待機も併用する
2. 画面ルートが変わりやすいので、検証前に実装側ルーター定義を確認する
3. 検証スクリプトがデータを作成・更新する場合は、DB への影響を考慮する
4. Core API（`localhost:8080`）は内部化されているため、ブラウザ疎通確認には使わない
5. Docker フォールバック時は `http://frontend:5173` や `http://host.docker.internal:5173` ではなく `http://localhost:<port>` を使う
6. Docker フォールバック時は `playwright` モジュールをコンテナ内でインストールする前提

## 片付け
```bash
rm -rf tmp
```
