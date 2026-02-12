# Playwright 利用ガイド（Docker / WSL bash）

最終更新: 2026-02-12

## 目的
このドキュメントは、このリポジトリで Playwright を使って UI 検証を行うための「実行方法」と「注意点」をまとめたものです。

## 基本方針
1. Playwright は `frontend` コンテナ内では実行しない
2. `mcr.microsoft.com/playwright:v1.58.2-noble` を一時起動して実行する
3. 接続は `--network host` と `http://localhost:<port>` を使う

## 事前確認
リポジトリルートで実行する。

```bash
docker compose up -d
docker compose ps
curl -s "http://localhost:5173" >/dev/null
curl -s "http://localhost:8080/api/item" >/dev/null
```

## スクリプト配置ルール
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

## 実行コマンド
`tmp/<script>.mjs` を作成したあと、以下で実行する。

```bash
docker run --rm --network host \
  -v "$(pwd):/work" \
  -w /tmp \
  mcr.microsoft.com/playwright:v1.58.2-noble \
  bash -lc "npm init -y >/dev/null 2>&1 && npm i playwright >/dev/null 2>&1 && cp /work/tmp/<script>.mjs /tmp/<script>.mjs && node /tmp/<script>.mjs"
```

## 注意点
1. `frontend` コンテナ（Alpine 系）では Chromium 実行で失敗する場合がある
2. `http://frontend:5173` や `http://host.docker.internal:5173` は `403` になることがある
3. `playwright` モジュールは毎回コンテナ内でインストールする前提
4. SPA 遷移の待機は `waitForURL` だけでなく、要素表示待機も併用する
5. 検証スクリプトがデータを作成・更新する場合は、DB への影響を考慮する

## 片付け
```bash
rm -rf tmp
```
