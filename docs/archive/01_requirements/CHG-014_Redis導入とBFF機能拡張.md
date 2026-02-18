# CHG-014: Redis導入とBFF機能拡張

**作成日**: 2026-02-18
**ステータス**: 要件定義
**優先度**: 高

---

## 1. 背景と目的

### 現状の課題

**BFFが「薄すぎる」プロキシ**:
- 単なるリクエスト転送に留まっている
- Core APIへの負荷が高い（毎回DB問い合わせ）
- フロントエンドが複数APIを個別呼び出し（ネットワーク往復多い）
- キャッシュ機構がない
- セッション管理が不十分
- レート制限がない（DoS攻撃に脆弱）

**パフォーマンス課題**:
- 商品一覧・詳細の表示が遅い（毎回DB問い合わせ）
- 認証チェックのオーバーヘッド（毎回Core API呼び出し）
- 複数API呼び出しによるネットワーク遅延

### 目的

BFF本来の役割を実装し、パフォーマンスと信頼性を向上させる。

1. **Redis導入**: キャッシュ・セッション管理の基盤構築
2. **レスポンスキャッシュ**: 頻繁アクセスデータの高速化
3. **レスポンス集約**: 複数API呼び出しを1リクエストにまとめる
4. **セッション管理強化**: Redisでのセッション管理
5. **将来拡張性**: シャローログイン/ディープログインに対応できる設計

---

## 2. スコープ

### 実装する機能

#### 2.1 Redis導入
- Docker ComposeでRedis追加
- BFFからのRedis接続設定
- Connection Pool設定

#### 2.2 レスポンスキャッシュ
- 商品一覧のキャッシュ（TTL: 5分）
- 商品詳細のキャッシュ（TTL: 10分）
- 在庫状態のキャッシュ（TTL: 1分）
- キャッシュ無効化機構（商品更新時）

#### 2.3 レスポンス集約（Aggregation）
- 商品詳細フルページ取得API
  - 商品詳細 + 関連商品を1リクエストで返す
- 注文詳細フルページ取得API
  - 注文詳細 + 注文アイテム + 商品情報を1リクエストで返す

#### 2.4 セッション管理
- ゲストセッションのRedis管理
- 会員セッションのRedis管理
- セッションメタデータ（最終アクセス時刻、IP、User-Agent）
- セッションタイムアウト（30分アイドルで期限切れ）

#### 2.5 認証トークンキャッシュ
- 認証トークンのRedisキャッシュ（TTL: 5分、Read-Through Cache）
- トークン検証の高速化（Core API呼び出し削減）
- PostgreSQLが真実のソース、Redisはキャッシュ層のみ

#### 2.6 レート制限
- IP単位のレート制限（10req/秒）
- ユーザー単位のレート制限（カート操作: 5req/秒）
- ログインエンドポイントの制限（3req/10秒）

### 実装しない機能（将来対応）

- シャローログイン/ディープログイン機能
  - 現時点では通常ログインのみ
  - 認証レベルの区別は設計のみ対応
- サーキットブレーカー
- A/Bテスト・フィーチャーフラグ
- 高度なメトリクス・モニタリング

---

## 3. 要件詳細

### 3.1 Redis導入

**技術要件**:
- Redis 7.2（Docker）
- NestJS: `ioredis` ライブラリ使用
- Connection Pool: 最小5、最大20接続
- タイムアウト: 5秒

**非機能要件**:
- Redis障害時のフォールバック: Core APIから直接取得（性能劣化は許容）
- Redis接続失敗時: アプリケーション起動を停止しない（警告ログのみ）

---

### 3.2 レスポンスキャッシュ

#### キャッシュ対象

| データ | キー形式 | TTL | 無効化タイミング | 備考 |
|--------|---------|-----|----------------|------|
| 商品一覧 | `cache:products:list:page:{page}:limit:{limit}` | 3分 | 商品更新時に該当ページ削除 | デフォルトページング結果のみ。検索・フィルタ付きはキャッシュしない |
| 商品詳細 | `cache:product:{id}` | 10分 | 商品更新時に該当ID削除 | 個別商品を積極的にキャッシュ |
| 在庫状態 | `cache:product:{id}:stock` | 1分 | 在庫調整時に該当ID削除 | カート追加/注文時はCore APIで再確認必須 |
| ユーザー情報 | `cache:user:{id}` | 5分 | ユーザー更新時に該当ID削除 | |

#### キャッシュ戦略

**Lazy Loading（遅延ロード）**:
```
1. Redisから取得試行
2. キャッシュHIT → そのまま返す
3. キャッシュMISS → Core APIから取得 → Redisに保存 → 返す
```

**キャッシュ無効化**:
- **責任**: BFFが書き込みAPIプロキシ時にレスポンス受領後に削除
- 商品更新API: 該当商品のキャッシュ削除
- 在庫調整API: 該当商品の在庫キャッシュ削除
- 全削除: `cache:product:*` パターン削除（管理画面から手動実行可能）

**エラーハンドリング**:
- Redisエラー時: Core APIから取得（キャッシュスキップ）
- ログレベル: WARN

---

### 3.3 レスポンス集約（Aggregation）

#### 新規エンドポイント

##### GET /api/products/:id/full

**概要**: 商品詳細ページに必要な全データを1リクエストで返す

**内部処理**:
```
並列実行:
1. GET /api/item/:id（商品詳細）
2. GET /api/item?category={category}&limit=4（関連商品）

レスポンス:
{
  success: true,
  data: {
    product: { ... },
    relatedProducts: [ ... ]
  }
}
```

**キャッシュ**: 別々にキャッシュ
- 商品詳細: `cache:product:{id}` (TTL 10分)
- 関連商品: `cache:product:{id}:related` (TTL 3分)

##### GET /api/orders/:id/full

**概要**: 注文詳細ページに必要な全データを1リクエストで返す

**内部処理**:
```
並列実行:
1. GET /api/order/:id（注文詳細 + 注文アイテム）
2. 各注文アイテムの商品情報取得（並列）

レスポンス:
{
  success: true,
  data: {
    order: { ... },
    items: [
      { ...orderItem, product: { ... } }
    ]
  }
}
```

**キャッシュ**: なし（注文は変動データ）

---

### 3.4 セッション管理

#### セッションデータ構造

```typescript
// Redis Key: session:{sessionId}
{
  sessionId: string;        // UUID v4
  userId?: number;          // 会員の場合のみ
  createdAt: number;        // タイムスタンプ
  lastAccessAt: number;     // 最終アクセス時刻
  ip: string;               // IPアドレス
  userAgent: string;        // User-Agent
  // 将来拡張
  authLevel?: 'shallow' | 'deep';  // シャロー/ディープログイン
}
```

**TTL管理**:
- アイドルタイムアウト: 30分（アクセス毎に更新）
- 会員ログイン後: 7日間（トークン有効期限と同期）

**セッションID生成**:
- UUID v4（BFFが生成）
- HTTPヘッダー: `X-Session-Id`
- 初回アクセス時: BFFがUUID生成 → レスポンスヘッダーで返す
- フロントエンドはlocalStorageに保存 → 以降のリクエストでヘッダー付与

**アクセス毎の処理**:
```
1. X-Session-Id ヘッダーを取得
2. Redisからセッション情報取得
3. lastAccessAtを更新
4. TTLを30分に延長（EXPIRE）
```

---

### 3.5 認証トークンキャッシュ

#### キャッシュ戦略

**PostgreSQL + Redisキャッシュ**:
- **PostgreSQL**: 唯一の真実のソース、監査証跡（完全な履歴）
- **Redis**: 読み取りキャッシュのみ（書き込みはしない）

**設計方針**:
- 書き込みはPostgreSQLのみ（Core API経由）
- RedisはRead-Through Cache（読み取り時にキャッシュ）
- 整合性はシンプル（PostgreSQL更新時にRedis削除するのみ）

**データフロー**:
```
ログイン:
1. Core API: トークン生成 → PostgreSQL保存
2. BFF: 何もしない（次回認証時にキャッシュされる）

認証チェック:
1. BFF: Redisキャッシュ確認
2. HIT → キャッシュデータで認証（Core API呼び出しスキップ）
3. MISS → Core API `/api/auth/me` 呼び出し → レスポンスをRedisにキャッシュ（TTL: 1分）

ログアウト:
1. Core API: PostgreSQL更新（is_revoked = true）
2. BFF: Redisキャッシュ削除
```

**キャッシュTTL**: 1分
- ログアウト反映の遅延は最大1分
- セキュリティインシデント時の即座ログアウトに対応

**Redis Key**: `auth:token:{tokenHash}`

**キャッシュデータ形式**:
```json
{
  "userId": 42,
  "email": "user@example.com",
  "displayName": "山田太郎",
  "expiresAt": 1740000000
}
```

**整合性の保証**:
- PostgreSQLが常に正しい状態
- Redisキャッシュは削除するだけ（同期不要）
- Redis障害時はCore APIから直接取得（性能劣化のみ）

---

### 3.6 レート制限

#### 制限ルール

| エンドポイント | 制限単位 | 制限値 | 期間 |
|--------------|---------|-------|------|
| 全API | IP | 100req | 1分 |
| GET /api/products | IP | 20req | 1分 |
| POST /api/cart/* | User | 10req | 1分 |
| POST /api/auth/login | IP | 5req | 1分 |
| POST /api/auth/register | IP | 3req | 10分 |

#### 実装方式

**Redis INCR + EXPIRE**:
```typescript
const key = `ratelimit:${ip}:${endpoint}`;
const count = await redis.incr(key);
if (count === 1) {
  await redis.expire(key, ttl);
}
if (count > limit) {
  throw new TooManyRequestsException();
}
```

**レスポンスヘッダー**:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1740000000
```

**エラーレスポンス** (429):
```json
{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "リクエスト制限を超えました。しばらくしてから再試行してください。",
    "retryAfter": 60
  }
}
```

---

## 4. データ設計

### 4.1 データ配置戦略

#### PostgreSQL（真実のソース）

**永続化が必要なデータ**:
- 商品マスタ（products）
- ユーザー情報（users）
- 認証トークン（auth_tokens, bo_auth_tokens）
- カート（carts, cart_items）
- 注文（orders, order_items）
- 在庫引当（stock_reservations）— 仮引当・本引当ともに
- 操作履歴（operation_histories）
- 在庫調整履歴（inventory_adjustments）

**理由**: 監査証跡、法的記録、トランザクション整合性が必要

#### Redis（キャッシュ・セッション層）

**Read-Through Cache**:
- 商品一覧（TTL: 3分、デフォルトページング結果のみ）※将来的に検索エンジン導入時は廃止
- 商品詳細（TTL: 10分）※個別商品を積極的にキャッシュ
- ユーザー情報（TTL: 5分）
- 認証トークン（TTL: 1分）
- 在庫状態（TTL: 1分）

**セッション管理**:
- ゲスト/会員セッション（TTL: 30分アイドル）

**一時データ**:
- レート制限カウンター（TTL: 1-10分）

**理由**: 高速アクセス、TTL自動期限切れ、揮発可能

#### データフロー原則

```
書き込み: PostgreSQLのみ（Core API経由）
読み取り: Redis優先 → MISS時PostgreSQL → Redisキャッシュ
更新:    PostgreSQL更新 → Redisキャッシュ削除
削除:    PostgreSQL削除 → Redisキャッシュ削除
```

### 4.2 Redis Key命名規則

```
{namespace}:{entity}:{id}:{attribute}

例:
- cache:products:list:page:1:limit:20
- cache:product:42
- cache:product:42:stock
- cache:product:42:full
- cache:user:123
- session:uuid-1234-5678
- auth:token:abc123def
- ratelimit:ip:192.168.1.1:api_products
```

### 4.3 PostgreSQLテーブル（変更なし）

在庫引当（仮引当・本引当）はPostgreSQLに残す。
- 会計監査が必要
- トランザクション整合性が重要
- Redis化は不適切（データロストリスク）

---

## 5. 非機能要件

### 5.1 パフォーマンス

**目標**:
- 商品一覧表示:
  - キャッシュHIT: **20ms以下**
  - キャッシュMISS: 100ms以下
- 商品詳細表示:
  - キャッシュHIT: **15ms以下**
  - キャッシュMISS: 80ms以下
- 認証チェック: 50ms → **2ms以下**（Redisキャッシュ使用）
- レスポンス集約: 複数リクエスト（200ms） → **単一リクエスト（50ms）**
- **キャッシュヒット率**: 90%以上

### 5.2 可用性

**Redis障害時の影響範囲**:
- **キャッシュ**: Core APIから直接取得（性能劣化のみ）
- **セッション管理**:
  - ゲストセッション消失 → ゲストカート消失（一時的データのため許容）
  - 会員セッション消失 → 認証トークンで再構築可能（許容）
- **レート制限**: 無効化（DoS攻撃リスク増加）
- **復旧**: Redis再起動後、自動的にキャッシュ・セッション再構築

### 5.3 セキュリティ

**レート制限**:
- DoS攻撃防止
- ブルートフォース攻撃防止（ログイン）

**セッション管理**:
- セッションハイジャック対策: IP変更検知（将来実装）
- セッション固定攻撃対策: ログイン時にセッションID再生成（将来実装）

---

## 6. 将来拡張対応

### シャローログイン/ディープログインの設計考慮

**認証レベルの定義**（将来実装）:

| 認証レベル | 説明 | できること | 有効期限 |
|-----------|------|-----------|---------|
| **Anonymous** | 未認証ゲスト | 商品閲覧、カート追加 | セッション30分 |
| **Shallow** | 軽い認証（メアドのみ、SNS連携等） | カート保存、注文履歴閲覧 | 7日間 |
| **Deep** | 完全認証（パスワード設定済み） | 注文確定、会員情報変更 | 7日間 |

**設計上の考慮点**:
- `session` データに `authLevel` フィールド追加可能
- AuthGuardで認証レベルチェック可能
- エンドポイント毎に必要認証レベルを定義

**現時点の実装**:
- Anonymous / Deep のみ（シャローは未実装）
- 将来の拡張に備えてデータ構造を拡張可能にする

---

## 7. 技術スタック

**新規導入**:
- Redis 7.2（Docker）
- NestJS: `ioredis`（Redis クライアント）
- NestJS: `@nestjs/throttler`（レート制限）

**既存**:
- NestJS 10
- PostgreSQL 16
- Docker Compose

---

## 8. 成功基準

1. ✅ Redisが正常に起動し、BFFから接続できる
2. ✅ 商品一覧・詳細のレスポンスタイムが目標値以下
3. ✅ レスポンス集約APIが正常に動作
4. ✅ セッション管理がRedisで動作
5. ✅ 認証トークンキャッシュが動作（Core API呼び出し削減）
6. ✅ レート制限が正常に動作（制限超過時に429エラー）
7. ✅ Redis障害時にフォールバックが動作

---

## 9. 制約事項

- Redis障害時は性能劣化を許容（機能は維持）
- キャッシュ無効化の遅延（最大TTL分）を許容
- シャローログイン/ディープログインは将来実装

---

## 10. 関連ドキュメント

- **技術仕様**: [SPEC.md](../SPEC.md)
- **BFFアーキテクチャ**: [bff-architecture.md](../specs/bff-architecture.md)
- **API仕様**: [api-spec.md](../ui/api-spec.md)

---

## 11. 次のステップ

1. 技術設計書作成（CHG-014）
2. 実装タスク分割（CHG-014）
3. Phase 1実装: Redis導入・レスポンスキャッシュ
4. Phase 2実装: レスポンス集約・セッション管理
5. Phase 3実装: レート制限・認証トークンキャッシュ
