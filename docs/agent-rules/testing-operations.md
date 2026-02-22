# テスト実施ガイド

最終更新: 2026-02-20

## 目的
実装タスク時に必要なテスト実施手順を、最小構成で示す。

## テスト区分（必須）

### 回帰テスト
- backend + bff + frontend を対象に、破壊的変更の有無を確認する
- 最小実行セット:
  - backend: `docker run --rm -v "$(pwd)/backend:/build" -w /build maven:3.9.9-eclipse-temurin-21 ./mvnw -DskipTests compile` / `docker run --rm -v "$(pwd)/backend:/build" -w /build maven:3.9.9-eclipse-temurin-21 ./mvnw test`
  - bff: `npm run test --workspace bff/customer-bff` / `npm run test --workspace bff/backoffice-bff`
  - frontend: `npm run lint --workspace frontend` / `npm run build --workspace frontend` / `npm run test:regression --workspace frontend` / `npm run test:smoke --workspace frontend`
- 機能追加時は対象機能の回帰テストを追加する
- テストがNGの場合は、機能改修による失敗か誤修正かを判断し、必要なテスト追加・修正を行う
- テスト追加・修正時は `docs/04_review-note/CHG-XXX.md` に記録する

### taskテスト
- task.md 記載のテスト（Final Gate を含む）を実施する
- 未実施項目は理由・代替案・残項目を提示し、ユーザー合意を得る
- 完了報告は `[PASS]` / `[FAIL] - 理由` 形式で記載する

## タスク実装後の検証フロー
1. task.md の検証コマンドを実施する
2. 回帰テスト（backend + bff + frontend）を実施する
3. NG 時は、機能改修影響か誤修正かを切り分け、必要なテスト追加・修正を行う
4. `docs/04_review-note/CHG-XXX.md` に結果を記録する

## 最小実行コマンド

```bash
# backend（./mvnw test は mvn clean なしで実行可能。CHG-025 で db/archive を classpath 対象外へ移動済み）
docker run --rm -v "$(pwd)/backend:/build" -w /build maven:3.9.9-eclipse-temurin-21 ./mvnw -DskipTests compile
docker run --rm -v "$(pwd)/backend:/build" -w /build maven:3.9.9-eclipse-temurin-21 ./mvnw test

# bff
npm run test --workspace bff/customer-bff
npm run test --workspace bff/backoffice-bff

# frontend
npm run lint --workspace frontend
npm run build --workspace frontend
npm run test:regression --workspace frontend
npm run test:smoke --workspace frontend
```

## 補足ルール
- `npm install` を Docker で実行する場合でも、root で `node_modules` を作成しない

## 関連ドキュメント
- `docs/agent-rules/playwright-runbook.md`
- `docs/agent-rules/implementation-policy.md`
