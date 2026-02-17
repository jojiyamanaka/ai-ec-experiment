# CLAUDE.md

このファイルは、Claude Code (claude.ai/code) がこのリポジトリで作業する際のガイドです。

## プロジェクト概要

AIを活用したECサイト実験プロジェクト。React + TypeScript のフロントエンド、NestJS BFF、Spring Boot Core API を Docker コンテナで動作させる。

## 開発コマンド

### Docker（メインの開発フロー）
```bash
docker compose up -d          # 全コンテナ起動（frontend + bff + backend + postgres）
docker compose down            # 全コンテナ停止
docker compose logs -f         # ログ確認
docker compose logs -f backend # バックエンドのログのみ確認
```

### フロントエンド（コンテナ内またはローカル）
```bash
cd frontend
npm run dev:customer  # 顧客画面（ポート5173）
npm run dev:admin     # 管理画面（ポート5174）
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
- 顧客画面: http://localhost:5173
- 管理画面: http://localhost:5174
- Customer BFF: http://localhost:3001
- BackOffice BFF: http://localhost:3002
- Core API: `backend:8080`（内部ネットワーク。ブラウザから直接アクセスしない）

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
- **DB**: PostgreSQL + Hibernate（方言: `org.hibernate.dialect.PostgreSQLDialect`）
- **Lombok**: エンティティやサービス全体で使用（`@RequiredArgsConstructor`, `@Data` 等）
- **例外処理**: `GlobalExceptionHandler`（`@RestControllerAdvice`）がカスタム例外をHTTPレスポンスにマッピング:
  - `ResourceNotFoundException` → 404
  - `BusinessException` → 400
  - `ConflictException` → 409
- **APIレスポンス形式**: 全エンドポイントが `ApiResponse<T>`（`success` ブール値）を返す。`ApiResponse.success(data)` / `ApiResponse.error(code, message)` を使用。

### BFF（NestJS）
- **customer-bff**: 顧客画面用 API (`/api/products`, `/api/cart`, `/api/orders` など)
- **backoffice-bff**: 管理画面用 API (`/api/inventory`, `/api/admin/orders`, `/api/admin/members` など)
- **共通型**: `@app/shared` を workspace で参照

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

- **CORS**: `application.yml` の `app.cors.*` で設定。開発時は `http://localhost:5173` / `http://localhost:5174` を許可し、`production-internal` では BFF 起点のみ許可。
- **DB接続**: `DB_URL` / `DB_USER` / `DB_PASSWORD`（開発デフォルト: `jdbc:postgresql://localhost:5432/ec_app`）。
- **フロントエンドAPI URL**: 環境変数 `VITE_API_URL` で設定（デフォルト: customer=`http://localhost:3001`, admin=`http://localhost:3002`）。

## 機能開発プロセス

新機能の開発は以下の3ステップで進める。各ステップのドキュメントは `docs/` 配下に作成する。

各ステップのファイル名は `CHG-XXX_<機能名>.md` で統一する（XXX は変更案件の通し番号）。同じ案件は全ステップで同じ番号を使う。

1. **要件定義** (`docs/01_requirements/CHG-XXX_*.md`)
   - 背景・課題（なぜ必要か）
   - ビジネス要件（何を実現すべきか）
   - 受け入れ条件
   - 技術的な「How」は書かない

2. **技術設計** (`docs/02_designs/CHG-XXX_*.md`)
   - API設計（エンドポイント、エラーコード、レスポンス形式）
   - 実装方針（新規クラス、既存クラスの変更内容）
   - 既存パターンとの整合性
   - 処理フロー

3. **実装タスク** (`docs/03_tasks/CHG-XXX_*.md`)
   - タスクごとにファイルパス・挿入箇所・コード断片を明記
   - 挿入位置はメソッド名・コメント等のランドマークで指定する（行番号は変動するため避ける）
   - 参考にすべき既存実装のパスを記載
   - このファイル単体で実装可能な粒度にする
   - 検証コマンドを冒頭に記載

### アーカイブルール

実装が完了し、主要ドキュメントに反映されたCHG案件は、アーカイブディレクトリに移動する。

**アーカイブ対象**:
- 実装完了済みの CHG-XXX ドキュメント（要件定義、技術設計、実装タスク）
- 主要ドキュメント（SPEC.md、requirements.md、data-model.md など）に内容が反映済み

**アーカイブ先**:
```
docs/archive/
├── 01_requirements/
│   └── CHG-XXX_*.md
├── 02_designs/
│   └── CHG-XXX_*.md
└── 03_tasks/
    └── CHG-XXX_*.md
```

**アーカイブの目的**:
- 主要ドキュメントを「Single Source of Truth」として維持
- CHG案件は詳細な履歴・背景として参照可能（アーカイブ）
- ドキュメントディレクトリの見通しを良くする

**移動コマンド例**:
```bash
# CHG-007 の実装完了後
mv docs/01_requirements/CHG-007*.md docs/archive/01_requirements/
mv docs/02_designs/CHG-007*.md docs/archive/02_designs/
mv docs/03_tasks/CHG-007*.md docs/archive/03_tasks/
```

**アーカイブ済み案件（参考）**:
- CHG-002: 非公開商品カート整合性
- CHG-003: カート数量上限（1-9個）
- CHG-004: エラーメッセージ改善
- CHG-005: エディトリアルデザイン
- CHG-006: 会員機能追加（認証・認可）

## ドキュメント

- `docs/SPEC.md` — 機能仕様書
- `docs/ui/api-spec.md` — API仕様書
- `docs/specs/` — 詳細仕様（在庫管理、注文管理、価格管理、管理画面）
- `docs/spec-implementation-gaps.md` — 仕様と実装のギャップ一覧
