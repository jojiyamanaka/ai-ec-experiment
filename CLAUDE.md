# CLAUDE.md

このファイルは、Claude Code (claude.ai/code) がこのリポジトリで作業する際のガイドです。

## プロジェクト概要

AIを活用したECサイト実験プロジェクト。React + TypeScript のフロントエンドと Spring Boot + SQLite のバックエンドを Docker コンテナで動作させる。

## 開発コマンド

### Docker（メインの開発フロー）
```bash
docker compose up -d          # 全コンテナ起動（frontend + backend）
docker compose down            # 全コンテナ停止
docker compose logs -f         # ログ確認
docker compose logs -f backend # バックエンドのログのみ確認
```

### フロントエンド（コンテナ内またはローカル）
```bash
cd frontend
npm run dev      # 開発サーバー起動（Vite、ポート5173）
npm run build    # 型チェック + ビルド（tsc -b && vite build）
npm run lint     # ESLint
```

### バックエンド（コンテナ内またはMavenラッパー経由）
```bash
cd backend
./mvnw spring-boot:run                    # アプリ起動（ポート8080）
./mvnw test                               # 全テスト実行
./mvnw test -Dtest=ClassName              # 特定テストクラスのみ実行
./mvnw test -Dtest=ClassName#methodName   # 特定テストメソッドのみ実行
./mvnw compile                            # コンパイルのみ
```

### アクセスURL
- フロントエンド: http://localhost:5173
- バックエンドAPI: http://localhost:8080

## アーキテクチャ

### フロントエンド（React 19 + Vite + Tailwind CSS 4）
- **ルーティング**: React Router v7、`App.tsx` で `BrowserRouter` を使用
- **状態管理**: アプリ全体を2つのContextプロバイダーでラップ:
  - `ProductProvider` — 商品データの取得・キャッシュ
  - `CartProvider` — バックエンドAPIと同期したカート状態
- **API層**: API呼び出しはすべて `src/lib/api.ts` 経由で行う。`fetch` を直接使わないこと。このモジュールが `X-Session-Id` ヘッダーの付与とエラー正規化を担当する。
- **型定義**: APIのリクエスト・レスポンス型はすべて `src/types/api.ts` に定義。共通レスポンスラッパーは `ApiResponse<T>`（`success`, `data?`, `error?` フィールド）。

### バックエンド（Spring Boot 3.4.2、Java 21）
- **レイヤードアーキテクチャ**: Controller → Service → Repository → Entity
- **DB**: SQLite + Hibernate（方言: `hibernate-community-dialects` の `SQLiteDialect`）。DBファイルはコンテナ内 `/app/data/ec.db`。
- **Lombok**: エンティティやサービス全体で使用（`@RequiredArgsConstructor`, `@Data` 等）
- **例外処理**: `GlobalExceptionHandler`（`@RestControllerAdvice`）がカスタム例外をHTTPレスポンスにマッピング:
  - `ResourceNotFoundException` → 404
  - `BusinessException` → 400
  - `ConflictException` → 409
- **APIレスポンス形式**: 全エンドポイントが `ApiResponse<T>`（`success` ブール値）を返す。`ApiResponse.success(data)` / `ApiResponse.error(code, message)` を使用。

### 在庫引当システム（2段階方式）
仮引当・本引当モデルを採用:
1. **仮引当（TENTATIVE）**: カート追加時に作成。30分で有効期限切れ。`@Scheduled` で5分ごとに自動クリーンアップ。
2. **本引当（COMMITTED）**: 注文確定時に仮引当から変換。`products.stock` を減少させる。注文キャンセル時に元に戻す。

中核サービス: `InventoryService` — 引当のライフサイクル全体を管理。`CartService` と `OrderService` がこれを呼び出す。

### セッション管理
- フロントエンドが初回アクセス時に `localStorage` でセッションIDを自動生成
- `api.ts` が `/order` エンドポイントに `X-Session-Id` ヘッダーを自動付与
- カート・注文データはこのセッションIDにスコープされる

## コーディング規約

- **言語**: フロントエンドはTypeScriptのみ（JSは使わない）。バックエンドはJava 21。
- **コミットメッセージ**: 日本語で記述
- **フロントエンドAPI呼び出し**: 必ず `src/lib/api.ts` の関数を使用
- **フロントエンド型定義**: `src/types/api.ts` に定義
- **バックエンドDTO**: エンティティとは明確に分離。DTOには `fromEntity()` 静的メソッドで変換。
- **バックエンド例外**: `ResourceNotFoundException`、`BusinessException`、`ConflictException` をスローする（グローバルハンドラーで処理される）。

## 主要な設定

- **CORS**: `WebConfig.java` で設定。許可オリジンは `application.yml`（`app.cors.*`）から読み込み。現在は `http://localhost:5173` を許可。
- **DBパス**: 環境変数 `DB_PATH` または Springプロパティで設定可能。デフォルト: `./data/ec.db`。Docker内: `/app/data/ec.db`。
- **フロントエンドAPI URL**: 環境変数 `VITE_API_URL` で設定（デフォルト: `http://localhost:8080`）。

## ドキュメント

- `docs/SPEC.md` — 機能仕様書
- `docs/api-spec.md` — API仕様書
- `docs/specs/` — 詳細仕様（在庫管理、注文管理、価格管理、管理画面）
- `docs/spec-implementation-gaps.md` — 仕様と実装のギャップ一覧
