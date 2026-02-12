# AGENTS.md

## 検証コマンド

ローカルに JDK / Node がないため、Docker 経由で検証する。

```bash
docker compose exec backend ./mvnw compile       # バックエンドのコンパイル確認
docker compose exec frontend npm run build        # フロントエンドの型チェック + ビルド
```

コンテナが起動していない場合は先に `docker compose up -d` を実行する。

## プロジェクト構成

- `backend/` — Spring Boot 3.4.2 / Java 21 / SQLite
- `frontend/` — React 19 / TypeScript / Vite / Tailwind CSS 4

## バックエンドの規約

### レイヤー構成

```
controller/  → リクエスト受付、レスポンス返却
service/     → ビジネスロジック
repository/  → DBアクセス（Spring Data JPA）
entity/      → DBテーブル対応
dto/         → リクエスト・レスポンス用の型。エンティティとは分離する
exception/   → カスタム例外
```

パッケージ: `com.example.aiec`

### Lombok

全クラスで使用。以下が頻出:
- エンティティ・DTO: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- サービス: `@RequiredArgsConstructor`
- 例外の追加フィールド: `@Getter`

### 例外処理

カスタム例外をスローすると `GlobalExceptionHandler` が HTTP レスポンスに変換する:

| 例外クラス | HTTPステータス | 用途 |
|-----------|---------------|------|
| `BusinessException` | 400 | ビジネスルール違反 |
| `ResourceNotFoundException` | 404 | リソース不在 |
| `ConflictException` | 409 | 競合 |
| `InsufficientStockException` (extends `ConflictException`) | 409 | 在庫不足（details付き） |

サブクラス例外を追加する場合は `GlobalExceptionHandler` に専用ハンドラーを追加し、親クラスのハンドラーより上に配置する。

### APIレスポンス形式

全エンドポイントが `ApiResponse<T>` を返す:

```java
ApiResponse.success(data)                          // 成功
ApiResponse.error(code, message)                   // エラー
ApiResponse.errorWithDetails(code, message, details) // 詳細付きエラー
```

### DTO変換

エンティティ → DTO は `fromEntity()` 静的メソッドで行う。

## フロントエンドの規約

### API呼び出し

`src/lib/api.ts` の関数を使う。`fetch` を直接使わない。

### 型定義

`src/types/api.ts` に集約。レスポンス型は `ApiResponse<T>` で統一。

### 状態管理

- `ProductContext` — 商品データ
- `CartContext` — カート状態（バックエンドと同期）

### コーディング

- TypeScript のみ（JS 禁止）
- スタイリングは Tailwind CSS のユーティリティクラス

## コミットメッセージ

日本語で記述。
