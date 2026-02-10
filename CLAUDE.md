# AI EC Experiment

AIを活用したECサイト実験プロジェクト

## 技術スタック

### フロントエンド
- React 19 + TypeScript
- Vite（ビルドツール）
- Tailwind CSS（スタイリング）
- React Router（ルーティング）

### バックエンド
- Spring Boot 3.4.2
- SQLite（データベース）
- Hibernate（ORM）

### 開発環境
- Docker Compose
- Maven（バックエンドビルド）

## 現在のフェーズ

✅ **Phase 1 完了: フロントエンド + バックエンド統合**
- フロントエンド画面実装（全6画面）
- バックエンドAPI実装（全9エンドポイント）
- Docker環境構築
- API統合・動作確認完了

## 実装済み機能

### 画面（フロントエンド）
1. ✅ 商品一覧画面（/item）
2. ✅ 商品詳細画面（/item/:id）
3. ✅ カート画面（/order/cart）
4. ✅ 注文確認画面（/order/reg）
5. ✅ 注文完了画面（/order/complete）
6. ✅ 管理画面（/bo/item）

### API（バックエンド）
1. ✅ GET /api/item - 商品一覧取得
2. ✅ GET /api/item/:id - 商品詳細取得
3. ✅ PUT /api/item/:id - 商品更新（管理用）
4. ✅ GET /api/order/cart - カート取得
5. ✅ POST /api/order/cart/items - カート追加
6. ✅ PUT /api/order/cart/items/:id - カート数量変更
7. ✅ DELETE /api/order/cart/items/:id - カート削除
8. ✅ POST /api/order - 注文作成
9. ✅ GET /api/order/:id - 注文詳細取得

### その他
- ✅ CORS設定（フロントエンド↔バックエンド通信）
- ✅ セッション管理（localStorage + X-Session-Id）
- ✅ DB永続化（SQLite）
- ✅ エラーハンドリング
- ✅ ローディング状態

## 開発環境のセットアップ

### 起動方法
```bash
# すべてのコンテナを起動
docker compose up -d

# ログ確認
docker compose logs -f

# 停止
docker compose down
```

### アクセスURL
- フロントエンド: http://localhost:5173
- バックエンドAPI: http://localhost:8080

### DB確認
```bash
# DBファイルの場所
docker exec ai-ec-backend ls -lh /app/data/
```

## コーディング規約

### 共通
- TypeScript を使用（JavaScript は使わない）
- 日本語コメントOK
- コミットメッセージは日本語で明確に

### フロントエンド
- コンポーネントは関数コンポーネントで書く
- Hooks（useState, useEffect等）を活用
- Context APIでグローバル状態管理
- API呼び出しは `src/lib/api.ts` を使用
- 型定義は `src/types/api.ts` に配置
- 非同期処理は async/await を使用
- エラーハンドリングを必ず実装

### バックエンド
- Springのアノテーション駆動開発
- REST APIの設計原則に従う
- DTOとEntityを明確に分離
- Serviceレイヤーでビジネスロジックを実装
- 例外処理は GlobalExceptionHandler で一元管理

## ディレクトリ構成

```
.
├── frontend/              # React アプリ
│   ├── src/
│   │   ├── components/   # 再利用可能コンポーネント
│   │   ├── contexts/     # Context API（状態管理）
│   │   ├── pages/        # ページコンポーネント
│   │   ├── lib/          # ユーティリティ（API等）
│   │   └── types/        # 型定義
│   ├── Dockerfile.dev    # フロントエンド用Docker
│   └── .env              # 環境変数（Git管理外）
│
├── backend/              # Spring Boot アプリ
│   ├── src/main/java/com/example/aiec/
│   │   ├── controller/  # REST APIコントローラ
│   │   ├── service/     # ビジネスロジック
│   │   ├── repository/  # データアクセス層
│   │   ├── entity/      # エンティティ（DB）
│   │   ├── dto/         # データ転送オブジェクト
│   │   ├── config/      # 設定クラス
│   │   └── exception/   # 例外処理
│   ├── Dockerfile.dev   # バックエンド用Docker
│   └── data/            # SQLite DBファイル（永続化）
│
├── docs/                 # 仕様書
│   ├── api-spec.md      # API仕様書
│   └── SPEC.md          # 機能仕様書
│
├── docker-compose.yml    # Docker構成
└── CLAUDE.md            # このファイル（開発ガイド）
```

## 重要な注意事項

### セッション管理
- フロントエンドはlocalStorageでセッションIDを自動生成
- カート・注文APIには `X-Session-Id` ヘッダーが必須
- セッションIDは初回アクセス時に自動生成される

### API呼び出し
- 必ず `src/lib/api.ts` のAPIクライアントを使用
- 直接fetchを使わない（セッション管理が含まれているため）
- エラーハンドリングを忘れずに実装

### DB永続化
- SQLiteのDBファイルは `/app/data/ec.db` に保存
- Dockerボリュームマウントで永続化
- コンテナ再起動後もデータ保持

### CORS
- バックエンドで `http://localhost:5173` からのアクセスを許可
- 設定は `application.yml` に記載

### 環境変数
- フロントエンド: `frontend/.env`
  - `VITE_API_URL=http://localhost:8080`
- バックエンド: `docker-compose.yml` で設定

## 参考資料

- [API仕様書](./docs/api-spec.md)
- [機能仕様書](./docs/SPEC.md)
- [README](./README.md)