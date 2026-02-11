# AI EC Experiment - 技術仕様書

**最終更新日**: 2025-02-11

---

## プロジェクト概要

AIがおすすめする商品を販売するECサイトのプロトタイプ。

**目的**: AI推薦機能を備えたECサイトの技術検証とプロトタイピング

**技術スタック**:
- **フロントエンド**: React 19 + TypeScript + Vite
- **スタイリング**: Tailwind CSS 4
- **ルーティング**: React Router v7
- **バックエンド**: Spring Boot 3.4.2 + Java 21
- **データベース**: SQLite + Hibernate
- **コンテナ**: Docker + Docker Compose

---

## 関連ドキュメント

このファイルは技術方針のみを記載します。詳細な仕様は以下のドキュメントを参照してください。

### 📋 基本仕様ドキュメント
- **[業務要件・ビジネスルール](./requirements.md)** - 主要機能、在庫状態のルール、注文の状態遷移
- **[データモデル](./data-model.md)** - エンティティ定義、データベーススキーマ、型定義

### 🖥️ UI層の仕様
- **[顧客向け画面](./ui/customer-ui.md)** - 画面一覧、画面遷移図、UI/UX設計思想
- **[管理画面](./ui/admin-ui.md)** - 商品管理・注文管理のUI仕様
- **[API仕様](./ui/api-spec.md)** - APIエンドポイント、リクエスト/レスポンス仕様

### 🔧 ドメイン層の仕様
- **[商品ドメイン](./specs/product.md)** - 商品マスタ、価格管理、公開制御
- **[在庫ドメイン](./specs/inventory.md)** - 在庫引当（仮引当・本引当）の詳細仕様
- **[注文ドメイン](./specs/order.md)** - 注文フロー、状態遷移の詳細仕様

### 📊 その他
- **[仕様と実装のギャップ一覧](./spec-implementation-gaps.md)** - 未実装機能、制約事項の一覧

---

## 技術アーキテクチャ

### 全体構成

```
┌──────────────────┐
│   Frontend       │
│  (React + Vite)  │  ← Port 5173
└────────┬─────────┘
         │ HTTP
         │ (CORS)
         ▼
┌──────────────────┐
│   Backend        │
│  (Spring Boot)   │  ← Port 8080
└────────┬─────────┘
         │ JDBC
         ▼
┌──────────────────┐
│   Database       │
│    (SQLite)      │  ← /app/data/ec.db
└──────────────────┘
```

---

## フロントエンド技術仕様

### フレームワーク・ライブラリ

- **React 19**: UIライブラリ
- **TypeScript**: 型安全性の確保
- **Vite**: 高速なビルドツール
- **React Router v7**: クライアントサイドルーティング
- **Tailwind CSS 4**: ユーティリティファーストCSS
- **Lucide React**: アイコンライブラリ

### 状態管理

**Context API** を使用したグローバル状態管理:

#### ProductContext（商品データ）
- **役割**: 商品マスタの取得・キャッシュ
- **プロバイダー**: `ProductProvider`
- **提供する値**:
  ```typescript
  {
    products: Product[]
    loading: boolean
    error: string | null
    refreshProducts: () => Promise<void>
  }
  ```

#### CartContext（カート状態）
- **役割**: カート操作とバックエンドAPIとの同期
- **プロバイダー**: `CartProvider`
- **提供する値**:
  ```typescript
  {
    items: CartItem[]
    totalQuantity: number
    totalPrice: number
    addToCart: (product: Product) => Promise<void>
    updateQuantity: (productId: number, quantity: number) => Promise<void>
    removeFromCart: (productId: number) => Promise<void>
    clearCart: () => Promise<void>
  }
  ```

**注意**: カート状態はバックエンドAPIと同期。フロントエンドはUIのみ担当。

### ルーティング

**React Router v7** によるクライアントサイドルーティング:

| パス | コンポーネント | 説明 |
|------|---------------|------|
| `/` | `Home` | TOP画面 |
| `/item` | `ItemList` | 商品一覧 |
| `/item/:id` | `ItemDetail` | 商品詳細 |
| `/order/cart` | `Cart` | カート |
| `/order/reg` | `OrderRegistration` | 注文確認 |
| `/order/complete` | `OrderComplete` | 注文完了 |
| `/order/:id` | `OrderDetail` | 注文詳細 |
| `/bo/item` | `AdminItemManagement` | 管理画面 - 商品管理 |
| `/bo/order` | `AdminOrderManagement` | 管理画面 - 注文管理 |

### API通信

**すべてのAPI呼び出しは `src/lib/api.ts` 経由で実行**:

```typescript
// ❌ 直接fetchを使わない
fetch('/api/item')

// ✅ api.tsの関数を使う
import { getProducts } from '@/lib/api'
const products = await getProducts()
```

**api.ts の役割**:
- `X-Session-Id` ヘッダーの自動付与
- エラーレスポンスの正規化
- 型安全なAPI呼び出し

### セッション管理

- **セッションID**: フロントエンドが初回アクセス時に自動生成（UUID v4）
- **保存先**: `localStorage`（キー: `sessionId`）
- **用途**: カート・注文データのスコープ識別
- **ヘッダー**: `X-Session-Id` として送信（`/order` エンドポイント）

### 型定義

**APIのリクエスト・レスポンス型は `src/types/api.ts` に集約**:

```typescript
// 共通レスポンス型
interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: {
    code: string
    message: string
  }
}

// エンティティ型
interface Product { /* ... */ }
interface CartItem { /* ... */ }
interface Order { /* ... */ }
```

**参照**: [data-model.md](./data-model.md) - 型定義の詳細

---

## バックエンド技術仕様

### フレームワーク・ライブラリ

- **Spring Boot 3.4.2**: Javaアプリケーションフレームワーク
- **Java 21**: プログラミング言語
- **Spring Data JPA**: データアクセス層
- **Hibernate**: ORM
- **SQLite**: 組み込みデータベース
- **Lombok**: ボイラープレートコード削減

### アーキテクチャパターン

**レイヤードアーキテクチャ** を採用:

```
Controller (REST API)
    ↓
Service (ビジネスロジック)
    ↓
Repository (データアクセス)
    ↓
Entity (データモデル)
```

#### Controller層
- `@RestController` による REST API の提供
- リクエストのバリデーション
- レスポンスの構築（`ApiResponse<T>`）

#### Service層
- ビジネスロジックの実装
- トランザクション管理（`@Transactional`）
- 例外のスロー（`ResourceNotFoundException`, `BusinessException`, `ConflictException`）

#### Repository層
- `JpaRepository` の継承
- カスタムクエリメソッド（`@Query`）
- データアクセスロジック

#### Entity層
- JPA エンティティ（`@Entity`）
- データベーステーブルとのマッピング
- Lombok による簡潔な実装

### データベース

**SQLite + Hibernate** による永続化:

- **方言**: `org.hibernate.community.dialect.SQLiteDialect`
- **DBファイルパス**: `/app/data/ec.db`（Docker内）、`./data/ec.db`（ローカル）
- **スキーマ管理**: Hibernate による自動生成（`spring.jpa.hibernate.ddl-auto=update`）

**参照**: [data-model.md](./data-model.md) - テーブル定義の詳細

### 例外処理

**GlobalExceptionHandler**（`@RestControllerAdvice`）が例外を一元管理:

| 例外クラス | HTTPステータス | 用途 |
|-----------|---------------|------|
| `ResourceNotFoundException` | 404 | リソースが見つからない |
| `BusinessException` | 400 | ビジネスルール違反 |
| `ConflictException` | 409 | データ競合・在庫不足 |

**使用例**:
```java
// 商品が見つからない場合
throw new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません");

// 在庫不足の場合
throw new ConflictException("OUT_OF_STOCK", "在庫が不足しています");
```

### APIレスポンス形式

**すべてのエンドポイントが `ApiResponse<T>` を返す**:

```java
// 成功レスポンス
return ApiResponse.success(data);

// エラーレスポンス
return ApiResponse.error("ERROR_CODE", "エラーメッセージ");
```

**型定義**:
```java
@Data
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorDetail error;
}
```

### CORS設定

**WebConfig.java** で CORS を設定:

- **許可オリジン**: `http://localhost:5173`（`application.yml` で設定）
- **許可メソッド**: GET, POST, PUT, DELETE, OPTIONS
- **許可ヘッダー**: `Content-Type`, `X-Session-Id`
- **クレデンシャル**: 許可

---

## 主要機能の実装方針

### 在庫引当システム（2段階方式）

**仮引当・本引当** による在庫管理:

1. **仮引当（TENTATIVE）**: カート追加時に作成。30分で自動失効。
2. **本引当（COMMITTED）**: 注文確定時に仮引当から変換。`products.stock` を減少。
3. **本引当解除**: 注文キャンセル時に削除。`products.stock` を戻す。

**中核サービス**: `InventoryService`
- 引当のライフサイクル管理
- 有効在庫の計算
- 定期クリーンアップ（5分ごと、`@Scheduled`）

**参照**: [specs/inventory.md](./specs/inventory.md) - 在庫引当の詳細

### 注文状態遷移

**状態遷移**: PENDING → CONFIRMED → SHIPPED → DELIVERED
- **中核サービス**: `OrderService`
- **状態管理**: Enumによる型安全な状態定義
- **キャンセル**: PENDING/CONFIRMED のみ可能

**参照**: [specs/order.md](./specs/order.md) - 注文管理の詳細

### セッション管理

- **フロントエンド**: UUID v4 を生成し `localStorage` に保存
- **バックエンド**: `X-Session-Id` ヘッダーでセッションを識別
- **スコープ**: カート・注文データをセッションIDでフィルタ

**制約**:
- Phase 1: セッションIDに有効期限なし
- Phase 2以降: セッション有効期限を実装予定

---

## 開発環境

### 必要なツール

- **Node.js**: v20 以上（フロントエンド）
- **Java**: 21（バックエンド）
- **Maven**: 3.9 以上（バックエンド）
- **Docker**: 20 以上
- **Docker Compose**: v2 以上

### 起動方法

#### Docker Compose（推奨）

```bash
# 全コンテナ起動
docker compose up -d

# ログ確認
docker compose logs -f

# コンテナ停止
docker compose down
```

#### ローカル開発

**フロントエンド**:
```bash
cd frontend
npm install
npm run dev  # http://localhost:5173
```

**バックエンド**:
```bash
cd backend
./mvnw spring-boot:run  # http://localhost:8080
```

### ポート番号

| サービス | ポート | URL |
|---------|--------|-----|
| フロントエンド | 5173 | http://localhost:5173 |
| バックエンド | 8080 | http://localhost:8080 |

---

## コーディング規約

### フロントエンド

- **言語**: TypeScript のみ（JavaScript は使わない）
- **API呼び出し**: 必ず `src/lib/api.ts` の関数を使用（`fetch` を直接使わない）
- **型定義**: `src/types/api.ts` に集約
- **コンポーネント**: 関数コンポーネントのみ
- **フック**: カスタムフックの活用
- **スタイリング**: Tailwind CSS のユーティリティクラスを使用

### バックエンド

- **言語**: Java 21
- **DTO**: エンティティとは明確に分離。DTOには `fromEntity()` 静的メソッドで変換。
- **例外**: `ResourceNotFoundException`, `BusinessException`, `ConflictException` をスローする。
- **Lombok**: `@Data`, `@RequiredArgsConstructor` などを活用してボイラープレートを削減。
- **トランザクション**: サービス層のメソッドに `@Transactional` を付与。

### 共通

- **コミットメッセージ**: 日本語で記述
- **命名規則**: キャメルケース（Java）、キャメルケース（TypeScript）
- **コメント**: 複雑なロジックのみコメントを記述。自明なコードにはコメント不要。

---

## 制約事項

### Phase 1（現在）の制約

1. **データの永続化**: カート・注文はバックエンドDBで永続化。商品データも永続化。
2. **認証・認可なし**: ユーザー登録・ログイン機能なし。管理画面へのアクセス制限なし。
3. **決済機能なし**: 決済処理は未実装。注文確定のみ。
4. **配送料・手数料**: ¥0 固定。
5. **商品画像**: プレースホルダー画像を使用（placehold.co）。

### Phase 2 以降の実装予定

1. **認証・認可**: ユーザー登録・ログイン、管理画面のアクセス制限、JWT認証
2. **決済機能**: 決済サービス連携、クレジットカード決済、決済履歴管理
3. **配送料計算**: 配送料のロジック実装（送料無料ライン、地域別送料）
4. **セッション管理**: セッション有効期限、更新機能
5. **検索・フィルタリング**: 商品名検索、価格帯フィルタ、カテゴリ分類
6. **AIレコメンデーション**: ユーザーの購買履歴・閲覧履歴に基づくおすすめ

**参照**: [spec-implementation-gaps.md](./spec-implementation-gaps.md) - 詳細なギャップ一覧

---

## セキュリティ

### 現在の実装

- **CORS**: 特定オリジン（`http://localhost:5173`）のみ許可
- **SQLインジェクション対策**: JPA による Prepared Statement の使用
- **XSS対策**: React による自動エスケープ

### Phase 2 以降で対応予定

- **認証**: JWT による認証
- **認可**: ロールベースアクセス制御（RBAC）
- **CSRF対策**: CSRF トークンの実装
- **HTTPS**: 本番環境では HTTPS 必須

---

## テスト方針

### バックエンド

- **単体テスト**: JUnit 5 + Mockito
- **統合テスト**: Spring Boot Test（`@SpringBootTest`）
- **実行**: `./mvnw test`

### フロントエンド

- **Phase 1**: テストなし（プロトタイプのため）
- **Phase 2以降**: Vitest + React Testing Library

---

## デプロイ

### Phase 1（現在）

- **環境**: ローカル開発環境のみ
- **方法**: Docker Compose

### Phase 2 以降

- **環境**: AWS / GCP / Azure
- **フロントエンド**: Vercel / Netlify
- **バックエンド**: EC2 / Cloud Run / App Service
- **データベース**: RDS / Cloud SQL / PostgreSQL

---

## 参考資料

### 公式ドキュメント

- [React 公式ドキュメント](https://react.dev/)
- [Spring Boot 公式ドキュメント](https://spring.io/projects/spring-boot)
- [Tailwind CSS 公式ドキュメント](https://tailwindcss.com/)

### プロジェクト内ドキュメント

- **[業務要件](./requirements.md)** - ビジネスルール、主要機能
- **[データモデル](./data-model.md)** - エンティティ定義、DB スキーマ
- **[顧客向け画面](./ui/customer-ui.md)** - 画面一覧、遷移図
- **[管理画面](./ui/admin-ui.md)** - 商品管理・注文管理のUI仕様
- **[API仕様](./ui/api-spec.md)** - エンドポイント、リクエスト/レスポンス
- **[商品ドメイン](./specs/product.md)** - 商品マスタ、価格管理、公開制御
- **[在庫ドメイン](./specs/inventory.md)** - 在庫引当の詳細
- **[注文ドメイン](./specs/order.md)** - 注文フローの詳細
- **[ギャップ一覧](./spec-implementation-gaps.md)** - 未実装機能、制約事項

---

**最終更新**: 2025-02-11
