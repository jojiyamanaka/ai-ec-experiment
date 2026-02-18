# CHG-014: Redis導入とBFF機能拡張 - 技術設計

要件: `docs/01_requirements/CHG-014_Redis導入とBFF機能拡張.md`
作成日: 2026-02-18

---

## 1. 設計方針

### 基本原則

1. **既存パターン維持**: 現行の `CoreApiService` / `ProductsService` のパターンを踏襲し、Redisレイヤーをオーバーレイとして追加する
2. **フォールバック優先**: Redis障害時は機能を維持しパフォーマンス劣化のみを許容する（`try/catch` + `WARN` ログ）
3. **単一Redisインスタンス**: 両BFFが同一Redisに接続し、BFF間のキャッシュ共有と無効化を実現する
4. **スコープ分離**: Customer BFF（キャッシュ・セッション・トークン・レート制限）vs BackOffice BFF（キャッシュ無効化・レート制限）でRedisの責務を分ける

### アーキテクチャ概要

```
┌──────────────────┐     ┌──────────────────┐
│ Customer BFF     │     │ BackOffice BFF   │
│ :3001            │     │ :3002            │
│                  │     │                  │
│ - RedisService   │     │ - RedisService   │
│ - CacheService   │     │ - CacheService   │
│ - SessionMiddle  │     │   (invalidation) │
│ - RateLimitGuard │     │ - RateLimitGuard │
│ - AuthGuard*     │     │ - BoAuthGuard*   │
└────────┬─────────┘     └────────┬─────────┘
         │                        │
         └──────────┬─────────────┘
                    ▼
           ┌──────────────┐
           │ Redis 7.2    │
           │ :6379        │
           │ (internal)   │
           └──────────────┘
                    ▲
           ┌──────────────┐
           │ Core API     │
           │ :8080        │
           └──────────────┘

* AuthGuard / BoAuthGuard は Redis token cache を使用
```

---

## 2. インフラ変更（Docker Compose）

### 変更ファイル: `docker-compose.yml`

**追加サービス**:
```yaml
  redis:
    image: redis:7.2-alpine
    container_name: ec-redis
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
    volumes:
      - redis-data:/data
    networks:
      - internal  # 外部公開なし

volumes:
  redis-data:     # 追加
```

**customer-bff / backoffice-bff への環境変数追加**:
```yaml
  customer-bff:
    environment:
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_TIMEOUT=5000
    depends_on:
      - backend
      - redis      # 追加
```

---

## 3. RedisService（両BFF共通パターン）

### 新規ファイル: `src/redis/redis.service.ts`（各BFFに配置）

```typescript
import { Injectable, Logger, OnModuleDestroy, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import Redis from 'ioredis';

@Injectable()
export class RedisService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(RedisService.name);
  private _client: Redis;

  constructor(private configService: ConfigService) {}

  onModuleInit() {
    const host = this.configService.get<string>('redis.host', 'localhost');
    const port = this.configService.get<number>('redis.port', 6379);
    this._client = new Redis({
      host,
      port,
      lazyConnect: true,
      connectTimeout: 5000,
      maxRetriesPerRequest: 1,
    });
    this._client.on('error', (err) =>
      this.logger.warn('Redis connection error', err.message),
    );
    this._client.connect().catch((err) =>
      this.logger.warn('Redis connect failed, running without cache', err.message),
    );
  }

  async onModuleDestroy() {
    await this._client.quit();
  }

  get client(): Redis {
    return this._client;
  }

  // キャッシュ取得（Redis障害時はnullを返す）
  async get<T>(key: string): Promise<T | null> {
    try {
      const value = await this._client.get(key);
      return value ? (JSON.parse(value) as T) : null;
    } catch (err) {
      this.logger.warn(`Redis GET failed: ${key}`, err.message);
      return null;
    }
  }

  // キャッシュ保存
  async set(key: string, value: unknown, ttlSeconds: number): Promise<void> {
    try {
      await this._client.setex(key, ttlSeconds, JSON.stringify(value));
    } catch (err) {
      this.logger.warn(`Redis SET failed: ${key}`, err.message);
    }
  }

  // キャッシュ削除
  async del(...keys: string[]): Promise<void> {
    try {
      if (keys.length > 0) await this._client.del(...keys);
    } catch (err) {
      this.logger.warn(`Redis DEL failed: ${keys.join(',')}`, err.message);
    }
  }

  // パターン削除（SCAN ベース）
  async delPattern(pattern: string): Promise<void> {
    try {
      const keys = await this._client.keys(pattern);
      if (keys.length > 0) await this._client.del(...keys);
    } catch (err) {
      this.logger.warn(`Redis delPattern failed: ${pattern}`, err.message);
    }
  }

  // INCR + EXPIRE（レート制限用）— Luaで原子性を担保
  // INCR と初回 EXPIRE を1トランザクションで実行し、
  // INCR 成功後にプロセスクラッシュしてもキーが残り続けるリスクを排除する
  private static readonly INCR_SCRIPT = `
    local count = redis.call('INCR', KEYS[1])
    if count == 1 then
      redis.call('EXPIRE', KEYS[1], ARGV[1])
    end
    return count
  `;

  async incr(key: string, ttlSeconds: number): Promise<number> {
    try {
      const count = await this._client.eval(
        RedisService.INCR_SCRIPT, 1, key, String(ttlSeconds),
      ) as number;
      return count;
    } catch (err) {
      this.logger.warn(`Redis INCR failed: ${key}`, err.message);
      return 0; // Redis障害時はレート制限スキップ
    }
  }

  async ttl(key: string): Promise<number> {
    try {
      return await this._client.ttl(key);
    } catch {
      return -1;
    }
  }
}
```

### 新規ファイル: `src/redis/redis.module.ts`

```typescript
import { Module, Global } from '@nestjs/common';
import { RedisService } from './redis.service';

@Global()
@Module({
  providers: [RedisService],
  exports: [RedisService],
})
export class RedisModule {}
```

### 設定追加: `src/config/configuration.ts`

```typescript
// 追加項目
redis: {
  host: process.env.REDIS_HOST || 'localhost',
  port: parseIntEnv('REDIS_PORT', 6379),
},
```

---

## 4. レスポンスキャッシュ（Customer BFF）

### 変更ファイル: `bff/customer-bff/src/products/products.service.ts`

**変更方針**: `CoreApiService` 呼び出しの前後にキャッシュ読み書きを追加。キャッシュキーは要件定義書の命名規則に従う。

```typescript
// 変更前: CoreApiService を直接呼び出す
async getProducts(page: number, limit: number, traceId?: string) {
  const response = await this.coreApiService.get<...>(`/api/item?...`);
  ...
}

// 変更後: Redis cache wrapping
async getProducts(page: number, limit: number, traceId?: string) {
  const cacheKey = `cache:products:list:page:${safePage}:limit:${safeLimit}`;

  // 1. キャッシュ確認
  const cached = await this.redisService.get<any>(cacheKey);
  if (cached) return { success: true, data: cached };

  // 2. Core API 呼び出し
  const response = await this.coreApiService.get<...>(`/api/item?...`);
  if (!response.success) return response;

  // 3. 変換 → キャッシュ保存（TTL: 180秒）
  const data = { items, total: items.length, page, limit };
  await this.redisService.set(cacheKey, data, 180);
  return { success: true, data };
}
```

同様に `getProductById` も `cache:product:{id}` (TTL: 600秒) でキャッシュ。

### キャッシュ無効化（BackOffice BFF）

管理画面から商品を更新した際、`BoAdminInventoryController` が Core API へ書き込みに成功した後、Redisキャッシュを削除する。

**変更ファイル**: `bff/backoffice-bff/src/inventory/inventory.service.ts`

```typescript
async adjustInventory(data: any, token: string, traceId?: string) {
  const response = await this.coreApiService.post('/api/bo/admin/inventory/adjust', data, token, traceId);

  if (response.success) {
    // キャッシュ無効化
    await this.redisService.del(`cache:product:${data.productId}:stock`);
    await this.redisService.delPattern('cache:products:list:*');
  }
  return response;
}
```

---

## 5. レスポンス集約（Customer BFF）

### 新規エンドポイント: `GET /api/products/:id/full`

**新規ファイル**: `bff/customer-bff/src/products/products.service.ts`（メソッド追加）

```typescript
async getProductFull(id: number, traceId?: string): Promise<ApiResponse<any>> {
  // 1. 商品詳細キャッシュ確認
  const productCacheKey = `cache:product:${id}`;
  const relatedCacheKey = `cache:product:${id}:related`;

  // 2. 並列取得（商品詳細 + 関連商品）
  const [product, relatedCached] = await Promise.all([
    this.getProductById(id, traceId),
    this.redisService.get<any>(relatedCacheKey),
  ]);

  if (!product.success) return product;

  let relatedProducts: any[];
  if (relatedCached) {
    relatedProducts = relatedCached;
  } else {
    const category = product.data.category ?? '';
    const relatedResponse = await this.coreApiService.get<any>(
      `/api/item?limit=4${category ? `&category=${category}` : ''}`,
      undefined,
      traceId,
    );
    relatedProducts = (relatedResponse.data?.items ?? [])
      .filter((p: any) => p.id !== id)
      .slice(0, 4)
      .map((p: any) => this.transformProduct(p));
    await this.redisService.set(relatedCacheKey, relatedProducts, 180);
  }

  return {
    success: true,
    data: {
      product: product.data,
      relatedProducts,
    },
  };
}
```

**コントローラ追加**: `products.controller.ts`

```typescript
@Get(':id/full')
async getProductFull(
  @Param('id') id: string,
  @Req() req: any,
): Promise<ApiResponse<any>> {
  return this.productsService.getProductFull(parseInt(id, 10), req.traceId);
}
```

### 新規エンドポイント: `GET /api/orders/:id/full`

**変更ファイル**: `bff/customer-bff/src/orders/orders.service.ts`（メソッド追加）

```typescript
async getOrderFull(id: number, token: string, userId?: number, traceId?: string): Promise<ApiResponse<any>> {
  // 1. 注文詳細取得（キャッシュなし）
  const orderResponse = await this.getOrderById(id, token, userId, traceId);
  if (!orderResponse.success) return orderResponse;

  const order = orderResponse.data;
  const items = order.items ?? [];

  // 2. 各注文アイテムの商品情報を並列取得（商品キャッシュ活用）
  const enrichedItems = await Promise.all(
    items.map(async (item: any) => {
      const cacheKey = `cache:product:${item.productId}`;
      let product = await this.redisService.get<any>(cacheKey);
      if (!product) {
        const productResponse = await this.coreApiService.get<any>(
          `/api/item/${item.productId}`, undefined, traceId,
        );
        product = productResponse.success ? productResponse.data : null;
        if (product) await this.redisService.set(cacheKey, product, 600);
      }
      return { ...item, product };
    }),
  );

  return {
    success: true,
    data: { order, items: enrichedItems },
  };
}
```

---

## 6. セッション管理（Customer BFF）

### 新規ファイル: `bff/customer-bff/src/session/session.middleware.ts`

```typescript
import { Injectable, NestMiddleware } from '@nestjs/common';
import { Request, Response, NextFunction } from 'express';
import { v4 as uuidv4 } from 'uuid';
import { RedisService } from '../redis/redis.service';

const SESSION_TTL = 30 * 60; // 30分

@Injectable()
export class SessionMiddleware implements NestMiddleware {
  constructor(private redisService: RedisService) {}

  async use(req: Request, res: Response, next: NextFunction) {
    let sessionId = req.headers['x-session-id'] as string;

    if (!sessionId) {
      sessionId = uuidv4();
    }

    const sessionKey = `session:${sessionId}`;
    const now = Date.now();

    // セッション取得 or 新規作成
    const existing = await this.redisService.get<any>(sessionKey);
    const session = existing ?? {
      sessionId,
      createdAt: now,
      ip: req.ip,
      userAgent: req.headers['user-agent'] ?? '',
    };

    // 最終アクセス更新
    session.lastAccessAt = now;
    await this.redisService.set(sessionKey, session, SESSION_TTL);

    // ログイン済みユーザーは TTL を延長（7日）
    if (session.userId) {
      await this.redisService.set(sessionKey, session, 7 * 24 * 60 * 60);
    }

    req['session'] = session;
    res.setHeader('X-Session-Id', sessionId);
    next();
  }
}
```

### `app.module.ts` への追加

```typescript
export class AppModule implements NestModule {
  configure(consumer: MiddlewareConsumer) {
    consumer.apply(SessionMiddleware).forRoutes('*');
  }
}
```

### ログイン時のセッション更新（`auth.service.ts`）

```typescript
async login(email: string, password: string, traceId?: string, sessionId?: string) {
  const response = await this.coreApiService.post('/api/auth/login', { email, password }, undefined, traceId);
  if (response.success && sessionId) {
    // セッションにユーザーIDを紐付け
    const sessionKey = `session:${sessionId}`;
    const session = await this.redisService.get<any>(sessionKey) ?? {};
    session.userId = response.data.user.id;
    await this.redisService.set(sessionKey, session, 7 * 24 * 60 * 60);
  }
  return response;
}
```

---

## 7. 認証トークンキャッシュ

### 変更ファイル: `bff/customer-bff/src/auth/auth.guard.ts`

**変更前**: 毎回 Core API `/api/auth/me` を呼び出す

**変更後**: Redis Read-Through Cache

```typescript
private async verifyToken(token: string, traceId?: string): Promise<any> {
  // トークンのSHA256ハッシュをキーに使用（トークン自体を保存しない）
  const tokenHash = createHash('sha256').update(token).digest('hex').slice(0, 32);
  const cacheKey = `auth:token:${tokenHash}`;

  // 1. キャッシュ確認
  const cached = await this.redisService.get<any>(cacheKey);
  if (cached) return cached;

  // 2. Core API 検証
  const response = await this.coreApiService.get<{ success: boolean; data?: any }>(
    '/api/auth/me', token, traceId,
  );
  if (!response?.success || !response.data) throw new Error('invalid token');

  // 3. キャッシュ保存（TTL: 60秒）
  await this.redisService.set(cacheKey, response.data, 60);
  return response.data;
}
```

### ログアウト時のキャッシュ削除（`auth.service.ts`）

```typescript
async logout(token: string, traceId?: string) {
  const response = await this.coreApiService.post('/api/auth/logout', {}, token, traceId);
  if (response.success) {
    const tokenHash = createHash('sha256').update(token).digest('hex').slice(0, 32);
    await this.redisService.del(`auth:token:${tokenHash}`);
  }
  return response;
}
```

同様のパターンを `bff/backoffice-bff/src/auth/bo-auth.guard.ts` にも適用（キー: `bo-auth:token:{tokenHash}`, TTL: 60秒）。

---

## 8. レート制限

### 新規ファイル: `bff/customer-bff/src/common/guards/rate-limit.guard.ts`

```typescript
import { CanActivate, ExecutionContext, HttpException, Injectable } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { RedisService } from '../../redis/redis.service';

export interface RateLimitConfig {
  limit: number;
  ttlSeconds: number;
  keyType: 'ip' | 'user';
}

export const RATE_LIMIT_KEY = 'rateLimit';

@Injectable()
export class RateLimitGuard implements CanActivate {
  constructor(
    private reflector: Reflector,
    private redisService: RedisService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const config = this.reflector.get<RateLimitConfig>(RATE_LIMIT_KEY, context.getHandler())
      ?? { limit: 100, ttlSeconds: 60, keyType: 'ip' };

    const request = context.switchToHttp().getRequest();
    const identifier = config.keyType === 'user'
      ? `user:${request.user?.id ?? 'anonymous'}`
      : `ip:${request.ip}`;
    const endpoint = context.getHandler().name;
    const key = `ratelimit:${identifier}:${endpoint}`;

    const count = await this.redisService.incr(key, config.ttlSeconds);
    if (count === 0) return true; // Redis障害時はスキップ

    const remaining = Math.max(0, config.limit - count);
    const resetAt = Math.floor(Date.now() / 1000) + config.ttlSeconds;

    const response = context.switchToHttp().getResponse();
    response.setHeader('X-RateLimit-Limit', config.limit);
    response.setHeader('X-RateLimit-Remaining', remaining);
    response.setHeader('X-RateLimit-Reset', resetAt);

    if (count > config.limit) {
      throw new HttpException({
        success: false,
        error: {
          code: 'RATE_LIMIT_EXCEEDED',
          message: 'リクエスト制限を超えました。しばらくしてから再試行してください。',
          retryAfter: config.ttlSeconds,
        },
      }, 429);
    }
    return true;
  }
}
```

### デコレータ定義: `src/common/decorators/rate-limit.decorator.ts`

```typescript
import { SetMetadata } from '@nestjs/common';
import { RateLimitConfig, RATE_LIMIT_KEY } from '../guards/rate-limit.guard';

export const RateLimit = (config: RateLimitConfig) => SetMetadata(RATE_LIMIT_KEY, config);
```

### コントローラへの適用例

```typescript
// auth.controller.ts
@Post('login')
@UseGuards(RateLimitGuard)
@RateLimit({ limit: 5, ttlSeconds: 60, keyType: 'ip' })
async login(...) {}

@Post('register')
@UseGuards(RateLimitGuard)
@RateLimit({ limit: 3, ttlSeconds: 600, keyType: 'ip' })
async register(...) {}

// cart.controller.ts
@Post('items')
@UseGuards(AuthGuard, RateLimitGuard)
@RateLimit({ limit: 10, ttlSeconds: 60, keyType: 'user' })
async addItem(...) {}
```

グローバルレート制限（100req/min/IP）は `APP_GUARD` として `app.module.ts` に登録する。

---

## 9. 処理フロー

### キャッシュヒット時（商品一覧）

```
Browser → Customer BFF
  → SessionMiddleware（セッション更新）
  → RateLimitGuard（100req/min チェック）
  → ProductsController.getProducts()
    → ProductsService.getProducts()
      → RedisService.get("cache:products:list:page:1:limit:20")
      → HIT: 即座にレスポンス返却（~5ms）
```

### キャッシュミス時（商品一覧）

```
Browser → Customer BFF
  → SessionMiddleware
  → RateLimitGuard
  → ProductsController.getProducts()
    → ProductsService.getProducts()
      → RedisService.get(...) → MISS
      → CoreApiService.get("/api/item?...") → Core API → PostgreSQL
      → RedisService.set(..., TTL=180)
      → レスポンス返却（~80ms）
```

### 認証フロー（トークンキャッシュHIT）

```
Browser → Customer BFF
  → AuthGuard.canActivate()
    → SHA256(token) → "abc123..."
    → RedisService.get("auth:token:abc123...") → HIT
    → Core API 呼び出しスキップ（~2ms）
```

### 商品更新 → キャッシュ無効化

```
Admin Browser → BackOffice BFF
  → BoAuthGuard（認証）
  → InventoryService.adjustInventory()
    → CoreApiService.post("/api/bo/admin/inventory/adjust")
    → response.success → true
    → RedisService.del("cache:product:42:stock")
    → RedisService.delPattern("cache:products:list:*")
    → レスポンス返却
```

---

## 10. API仕様更新

### 新規エンドポイント（Customer BFF）

| Method | Path | 認証 | 説明 |
|--------|------|------|------|
| GET | `/api/products/:id/full` | 不要 | 商品詳細 + 関連商品 |
| GET | `/api/orders/:id/full` | User | 注文詳細 + 商品情報 |

#### GET /api/products/:id/full レスポンス

```json
{
  "success": true,
  "data": {
    "product": { "id": 1, "name": "...", "price": 1000, ... },
    "relatedProducts": [{ "id": 2, "name": "...", ... }]
  }
}
```

#### GET /api/orders/:id/full レスポンス

```json
{
  "success": true,
  "data": {
    "order": { "orderId": 1, "status": "PENDING", ... },
    "items": [
      { "productId": 1, "quantity": 2, "product": { "id": 1, "name": "...", ... } }
    ]
  }
}
```

### 新規エラーコード

| コード | HTTP | 説明 |
|--------|------|------|
| `RATE_LIMIT_EXCEEDED` | 429 | レート制限超過 |

---

## 11. パッケージ追加

### customer-bff / backoffice-bff 共通

```json
{
  "dependencies": {
    "ioredis": "^5.3.0",
    "@types/ioredis": "^4.28.10"
  }
}
```

`ioredis` は型定義を内包しているため `@types/ioredis` は不要（v5+）。実際は `ioredis` のみでよい。

---

## 12. 影響範囲

### 変更ファイル一覧

#### docker-compose.yml
- Redisサービス追加
- customer-bff / backoffice-bff の環境変数追加
- `depends_on: redis` 追加

#### bff/customer-bff/
| ファイル | 変更種別 | 内容 |
|---------|---------|------|
| `package.json` | 変更 | `ioredis` 追加 |
| `src/config/configuration.ts` | 変更 | Redis設定追加 |
| `src/app.module.ts` | 変更 | RedisModule・SessionMiddleware追加 |
| `src/redis/redis.module.ts` | 新規 | Redisモジュール |
| `src/redis/redis.service.ts` | 新規 | Redis操作サービス |
| `src/session/session.middleware.ts` | 新規 | セッション管理ミドルウェア |
| `src/common/guards/rate-limit.guard.ts` | 新規 | レート制限ガード |
| `src/common/decorators/rate-limit.decorator.ts` | 新規 | デコレータ |
| `src/products/products.service.ts` | 変更 | キャッシュ追加・`getProductFull`追加 |
| `src/products/products.controller.ts` | 変更 | `/full` エンドポイント追加 |
| `src/orders/orders.service.ts` | 変更 | `getOrderFull`追加 |
| `src/orders/orders.controller.ts` | 変更 | `/full` エンドポイント追加 |
| `src/auth/auth.guard.ts` | 変更 | トークンキャッシュ追加 |
| `src/auth/auth.service.ts` | 変更 | ログアウト時キャッシュ削除・セッション更新 |
| `src/auth/auth.controller.ts` | 変更 | RateLimitGuard追加 |
| `src/cart/cart.controller.ts` | 変更 | RateLimitGuard追加 |

#### bff/backoffice-bff/
| ファイル | 変更種別 | 内容 |
|---------|---------|------|
| `package.json` | 変更 | `ioredis` 追加 |
| `src/config/configuration.ts` | 変更 | Redis設定追加 |
| `src/app.module.ts` | 変更 | RedisModule追加 |
| `src/redis/redis.module.ts` | 新規 | Redisモジュール（customer-bffと同内容） |
| `src/redis/redis.service.ts` | 新規 | Redis操作サービス（同上） |
| `src/common/guards/rate-limit.guard.ts` | 新規 | レート制限ガード（同上） |
| `src/inventory/inventory.service.ts` | 変更 | キャッシュ無効化追加 |
| `src/auth/bo-auth.guard.ts` | 変更 | トークンキャッシュ追加 |
| `src/auth/bo-auth.service.ts` | 変更 | ログアウト時キャッシュ削除 |

### 影響なし（変更不要）

- `frontend/` 全体（APIパスは変わらず）
- `backend/` 全体（Core APIは変更なし）
- `bff/shared/` 全体（共通DTOは変更なし）
- Core APIのDBスキーマ

---

## 13. 実装フェーズ計画

| Phase | 内容 | 優先度 |
|-------|------|--------|
| Phase 1 | Redis導入（Docker）+ RedisService | 必須 |
| Phase 2 | レスポンスキャッシュ（商品一覧・詳細） + キャッシュ無効化 | 高 |
| Phase 3 | レスポンス集約（/full エンドポイント） | 中 |
| Phase 4 | セッション管理 | 中 |
| Phase 5 | 認証トークンキャッシュ（Auth Guard更新） | 高 |
| Phase 6 | レート制限 | 高 |

各Phaseは独立して実装・検証可能。Phase 1が他の全Phaseの前提条件。

---

## 14. テスト観点

| テスト | 観点 |
|--------|------|
| 商品一覧 キャッシュHIT | 2回目呼び出しでRedisから返ることを確認 |
| 商品一覧 キャッシュMISS | Core API呼び出しが発生することを確認 |
| 在庫調整後のキャッシュ無効化 | 調整後に商品一覧キャッシュが削除されることを確認 |
| /api/products/:id/full | 商品詳細 + 関連商品が1リクエストで返ることを確認 |
| /api/orders/:id/full | 注文 + 商品情報が1リクエストで返ることを確認 |
| セッション生成 | X-Session-Id なしのリクエストでUUIDが生成されることを確認 |
| セッション継続 | X-Session-Id 付きのリクエストでTTLが延長されることを確認 |
| トークンキャッシュHIT | 2回目認証でCore API呼び出しがスキップされることを確認 |
| ログアウト後のキャッシュ削除 | ログアウト後に認証トークンキャッシュが削除されることを確認 |
| レート制限 | ログインエンドポイントで5回超過後に429が返ることを確認 |
| Redis障害時フォールバック | Redisを停止してもAPIが正常に動作することを確認 |
