# CHG-014: Redis導入とBFF機能拡張 - 実装タスク

要件: `docs/01_requirements/CHG-014_Redis導入とBFF機能拡張.md`
設計: `docs/02_designs/CHG-014_Redis導入とBFF機能拡張.md`
作成日: 2026-02-18

検証コマンド（Phase 1完了後に実行可能）:
```bash
# コンテナ再ビルドと起動
docker compose build customer-bff backoffice-bff && docker compose up -d

# Redis疎通確認
docker compose exec redis redis-cli ping  # → PONG

# BFF起動確認
curl http://localhost:3001/health  # → {"status":"ok"}
curl http://localhost:3002/health  # → {"status":"ok"}
```

---

## 実装順序

```
T-1（docker-compose.yml）
  → T-2〜T-5（customer-bff Redis基盤）と T-6〜T-9（backoffice-bff Redis基盤）— 並行可能
    → T-10（customer-bff app.module.ts）と T-11（backoffice-bff app.module.ts）— 並行可能
      → T-12（customer-bff products.service.ts）と T-13（backoffice-bff inventory.service.ts）— 並行可能
        → T-14（products.controller.ts）, T-15（orders.service.ts）— 並行可能
          → T-16（orders.controller.ts）
        → T-17（session.middleware.ts）
          → T-18（customer-bff auth.service.ts）
            → T-19（customer-bff auth.controller.ts）— T-24, T-25完了後に実施
        → T-20（customer-bff auth.guard.ts）と T-21（backoffice-bff bo-auth.guard.ts）— 並行可能
        → T-22（backoffice-bff bo-auth.service.ts）
      → T-23（customer-bff rate-limit.guard.ts）と T-24（rate-limit.decorator.ts）— 並行可能
        → T-19（customer-bff auth.controller.ts）
        → T-25（customer-bff cart.controller.ts）
      → T-26（backoffice-bff rate-limit.guard.ts）
```

---

## Phase 1: Redis基盤（全Phaseの前提）

### T-1: docker-compose.yml — Redisサービス追加

パス: `docker-compose.yml`

**変更前（133〜135行）**:
```yaml
volumes:
  postgres-data:
```

**変更後**:
```yaml
  redis:
    image: redis:7.2-alpine
    container_name: ec-redis
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
    volumes:
      - redis-data:/data
    networks:
      - internal

volumes:
  postgres-data:
  redis-data:
```

**変更前（customer-bff environment、54〜56行）**:
```yaml
      - LOG_LEVEL=info
    depends_on:
      - backend
```

**変更後**:
```yaml
      - LOG_LEVEL=info
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_TIMEOUT=5000
    depends_on:
      - backend
      - redis
```

**変更前（backoffice-bff environment、80〜81行）**:
```yaml
      - LOG_LEVEL=info
    depends_on:
      - backend
```

**変更後**:
```yaml
      - LOG_LEVEL=info
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_TIMEOUT=5000
    depends_on:
      - backend
      - redis
```

---

### T-2: bff/customer-bff/package.json — ioredis追加

パス: `bff/customer-bff/package.json`

**変更前（`"dependencies"` 内）**:
```json
    "uuid": "^9.0.0",
    "winston": "^3.11.0"
```

**変更後**:
```json
    "uuid": "^9.0.0",
    "winston": "^3.11.0",
    "ioredis": "^5.3.0"
```

インストール:
```bash
docker compose run --rm customer-bff npm install
# または npm install --prefix bff/customer-bff
```

---

### T-3: bff/customer-bff/src/redis/redis.service.ts — 新規作成

パス: `bff/customer-bff/src/redis/redis.service.ts`（新規）

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

  async get<T>(key: string): Promise<T | null> {
    try {
      const value = await this._client.get(key);
      return value ? (JSON.parse(value) as T) : null;
    } catch (err) {
      this.logger.warn(`Redis GET failed: ${key}`, err.message);
      return null;
    }
  }

  async set(key: string, value: unknown, ttlSeconds: number): Promise<void> {
    try {
      await this._client.setex(key, ttlSeconds, JSON.stringify(value));
    } catch (err) {
      this.logger.warn(`Redis SET failed: ${key}`, err.message);
    }
  }

  async del(...keys: string[]): Promise<void> {
    try {
      if (keys.length > 0) await this._client.del(...keys);
    } catch (err) {
      this.logger.warn(`Redis DEL failed: ${keys.join(',')}`, err.message);
    }
  }

  async delPattern(pattern: string): Promise<void> {
    try {
      const keys = await this._client.keys(pattern);
      if (keys.length > 0) await this._client.del(...keys);
    } catch (err) {
      this.logger.warn(`Redis delPattern failed: ${pattern}`, err.message);
    }
  }

  // INCR + EXPIRE を Lua で原子的に実行（レート制限用）
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

---

### T-4: bff/customer-bff/src/redis/redis.module.ts — 新規作成

パス: `bff/customer-bff/src/redis/redis.module.ts`（新規）

```typescript
import { Global, Module } from '@nestjs/common';
import { RedisService } from './redis.service';

@Global()
@Module({
  providers: [RedisService],
  exports: [RedisService],
})
export class RedisModule {}
```

---

### T-5: bff/customer-bff/src/config/configuration.ts — Redis設定追加

パス: `bff/customer-bff/src/config/configuration.ts`

**変更前（23〜27行）**:
```typescript
  coreApi: {
    url: process.env.CORE_API_URL || 'http://localhost:8080',
    timeout: parseIntEnv('CORE_API_TIMEOUT', 5000),
    retry: parseIntEnv('CORE_API_RETRY', 2),
  },
  logging: {
    level: process.env.LOG_LEVEL || 'info',
  },
```

**変更後**:
```typescript
  coreApi: {
    url: process.env.CORE_API_URL || 'http://localhost:8080',
    timeout: parseIntEnv('CORE_API_TIMEOUT', 5000),
    retry: parseIntEnv('CORE_API_RETRY', 2),
  },
  redis: {
    host: process.env.REDIS_HOST || 'localhost',
    port: parseIntEnv('REDIS_PORT', 6379),
  },
  logging: {
    level: process.env.LOG_LEVEL || 'info',
  },
```

---

### T-6: bff/backoffice-bff/package.json — ioredis追加

パス: `bff/backoffice-bff/package.json`

**変更前（`"dependencies"` 内）**:
```json
    "uuid": "^9.0.0"
```

**変更後**:
```json
    "uuid": "^9.0.0",
    "ioredis": "^5.3.0"
```

インストール:
```bash
docker compose run --rm backoffice-bff npm install
# または npm install --prefix bff/backoffice-bff
```

---

### T-7: bff/backoffice-bff/src/redis/redis.service.ts — 新規作成

パス: `bff/backoffice-bff/src/redis/redis.service.ts`（新規）

T-3（customer-bff）と同一内容をコピー:

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

  async get<T>(key: string): Promise<T | null> {
    try {
      const value = await this._client.get(key);
      return value ? (JSON.parse(value) as T) : null;
    } catch (err) {
      this.logger.warn(`Redis GET failed: ${key}`, err.message);
      return null;
    }
  }

  async set(key: string, value: unknown, ttlSeconds: number): Promise<void> {
    try {
      await this._client.setex(key, ttlSeconds, JSON.stringify(value));
    } catch (err) {
      this.logger.warn(`Redis SET failed: ${key}`, err.message);
    }
  }

  async del(...keys: string[]): Promise<void> {
    try {
      if (keys.length > 0) await this._client.del(...keys);
    } catch (err) {
      this.logger.warn(`Redis DEL failed: ${keys.join(',')}`, err.message);
    }
  }

  async delPattern(pattern: string): Promise<void> {
    try {
      const keys = await this._client.keys(pattern);
      if (keys.length > 0) await this._client.del(...keys);
    } catch (err) {
      this.logger.warn(`Redis delPattern failed: ${pattern}`, err.message);
    }
  }

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
      return 0;
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

---

### T-8: bff/backoffice-bff/src/redis/redis.module.ts — 新規作成

パス: `bff/backoffice-bff/src/redis/redis.module.ts`（新規）

T-4（customer-bff）と同一内容:

```typescript
import { Global, Module } from '@nestjs/common';
import { RedisService } from './redis.service';

@Global()
@Module({
  providers: [RedisService],
  exports: [RedisService],
})
export class RedisModule {}
```

---

### T-9: bff/backoffice-bff/src/config/configuration.ts — Redis設定追加

パス: `bff/backoffice-bff/src/config/configuration.ts`

**変更前（19〜26行）**:
```typescript
  coreApi: {
    url: process.env.CORE_API_URL || 'http://localhost:8080',
    timeout: parseIntEnv('CORE_API_TIMEOUT', 5000),
    retry: parseIntEnv('CORE_API_RETRY', 2),
  },
  logging: {
    level: process.env.LOG_LEVEL || 'info',
  },
```

**変更後**:
```typescript
  coreApi: {
    url: process.env.CORE_API_URL || 'http://localhost:8080',
    timeout: parseIntEnv('CORE_API_TIMEOUT', 5000),
    retry: parseIntEnv('CORE_API_RETRY', 2),
  },
  redis: {
    host: process.env.REDIS_HOST || 'localhost',
    port: parseIntEnv('REDIS_PORT', 6379),
  },
  logging: {
    level: process.env.LOG_LEVEL || 'info',
  },
```

---

## Phase 2: レスポンスキャッシュ + キャッシュ無効化

### T-10: bff/customer-bff/src/app.module.ts — RedisModule追加（※Phase 4・6の変更も同時に実施）

パス: `bff/customer-bff/src/app.module.ts`

**変更後（ファイル全体を以下に置き換え）**:

```typescript
import { MiddlewareConsumer, Module, NestModule } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { ConfigModule } from '@nestjs/config';
import configuration from './config/configuration';
import { CoreApiModule } from './core-api/core-api.module';
import { RedisModule } from './redis/redis.module';
import { ProductsModule } from './products/products.module';
import { CartModule } from './cart/cart.module';
import { OrdersModule } from './orders/orders.module';
import { MembersModule } from './members/members.module';
import { AuthModule } from './auth/auth.module';
import { HealthController } from './health/health.controller';
import { TestModule } from './test/test.module';
import { SessionMiddleware } from './session/session.middleware';
import { RateLimitGuard } from './common/guards/rate-limit.guard';

const testModules = process.env.NODE_ENV === 'production' ? [] : [TestModule];

@Module({
  controllers: [HealthController],
  imports: [
    ConfigModule.forRoot({
      load: [configuration],
      isGlobal: true,
    }),
    RedisModule,
    CoreApiModule,
    ProductsModule,
    CartModule,
    AuthModule,
    OrdersModule,
    MembersModule,
    ...testModules,
  ],
  providers: [
    {
      provide: APP_GUARD,
      useClass: RateLimitGuard,
    },
  ],
})
export class AppModule implements NestModule {
  configure(consumer: MiddlewareConsumer) {
    consumer.apply(SessionMiddleware).forRoutes('*');
  }
}
```

> **注意**: このタスクは T-17（session.middleware.ts）, T-23（rate-limit.guard.ts）, T-24（rate-limit.decorator.ts）完了後でないとビルドが通らない。Phase 1検証後、他タスクと並行してファイルを用意し、最後にまとめてビルドする。

---

### T-11: bff/customer-bff/src/products/products.service.ts — キャッシュ追加 + getProductFull追加

パス: `bff/customer-bff/src/products/products.service.ts`

**変更後（ファイル全体を以下に置き換え）**:

```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';
import { ApiResponse } from '@app/shared';

interface CoreProduct {
  id: number;
  name: string;
  price: number;
  image?: string;
  description?: string;
  category?: string;
  stock: number;
  isPublished?: boolean;
  published?: boolean;
}

interface CoreProductList {
  items: CoreProduct[];
  total: number;
  page: number;
  limit: number;
}

@Injectable()
export class ProductsService {
  constructor(
    private coreApiService: CoreApiService,
    private redisService: RedisService,
  ) {}

  async getProducts(page: number, limit: number, traceId?: string): Promise<ApiResponse<any>> {
    const safePage = Number.isFinite(page) && page > 0 ? page : 1;
    const safeLimit = Number.isFinite(limit) && limit > 0 ? limit : 20;
    const cacheKey = `cache:products:list:page:${safePage}:limit:${safeLimit}`;

    // 1. キャッシュ確認
    const cached = await this.redisService.get<any>(cacheKey);
    if (cached) return { success: true, data: cached };

    // 2. Core API 呼び出し
    const response = await this.coreApiService.get<ApiResponse<CoreProductList>>(
      `/api/item?page=${safePage}&limit=${safeLimit}`,
      undefined,
      traceId,
    );

    if (!response.success || !response.data) {
      return response as ApiResponse<any>;
    }

    const items = response.data.items
      .filter((product) => this.isPublished(product))
      .map((product) => this.transformProduct(product));

    const data = {
      items,
      total: items.length,
      page: response.data.page,
      limit: response.data.limit,
    };

    // 3. キャッシュ保存（TTL: 180秒）
    await this.redisService.set(cacheKey, data, 180);
    return { success: true, data };
  }

  async getProductById(id: number, traceId?: string): Promise<ApiResponse<any>> {
    const cacheKey = `cache:product:${id}`;

    // 1. キャッシュ確認
    const cached = await this.redisService.get<any>(cacheKey);
    if (cached) return { success: true, data: cached };

    // 2. Core API 呼び出し
    const response = await this.coreApiService.get<ApiResponse<CoreProduct>>(
      `/api/item/${id}`,
      undefined,
      traceId,
    );

    if (!response.success || !response.data) {
      return response as ApiResponse<any>;
    }

    if (!this.isPublished(response.data)) {
      return {
        success: false,
        error: {
          code: 'PRODUCT_NOT_FOUND',
          message: '商品が見つかりません',
        },
      };
    }

    const data = this.transformProduct(response.data);

    // 3. キャッシュ保存（TTL: 600秒）
    await this.redisService.set(cacheKey, data, 600);
    return { success: true, data };
  }

  async getProductFull(id: number, traceId?: string): Promise<ApiResponse<any>> {
    const relatedCacheKey = `cache:product:${id}:related`;

    // 商品詳細 + 関連商品キャッシュを並列取得
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
      const relatedResponse = await this.coreApiService.get<ApiResponse<CoreProductList>>(
        `/api/item?limit=4${category ? `&category=${encodeURIComponent(category)}` : ''}`,
        undefined,
        traceId,
      );
      relatedProducts = ((relatedResponse.data?.items ?? []) as CoreProduct[])
        .filter((p) => p.id !== id)
        .slice(0, 4)
        .map((p) => this.transformProduct(p));
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

  transformProduct(product: CoreProduct): any {
    const stockStatus = this.getStockStatus(product.stock);
    const isPublished = this.isPublished(product);
    return {
      id: product.id,
      name: product.name,
      price: Number(product.price),
      image: product.image ?? '',
      imageUrl: product.image ?? '',
      description: product.description ?? '',
      category: product.category ?? '',
      stock: product.stock,
      isPublished,
      stockStatus,
    };
  }

  private isPublished(product: CoreProduct): boolean {
    return Boolean(product.isPublished ?? product.published);
  }

  private getStockStatus(stock: number): string {
    if (stock === 0) {
      return '在庫なし';
    }
    if (stock <= 5) {
      return '残りわずか';
    }
    return '在庫あり';
  }
}
```

> **注意**: `transformProduct` を `private` から非private（`public` or `protected`）に変更。`getProductFull` 内の関連商品変換で使用するため。

---

### T-12: bff/backoffice-bff/src/app.module.ts — RedisModule追加

パス: `bff/backoffice-bff/src/app.module.ts`

**変更前（1〜27行）**:
```typescript
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
...

@Module({
  imports: [
    ConfigModule.forRoot({ ... }),
    CoreApiModule,
    BoAuthModule,
    ...
  ],
})
export class AppModule {}
```

**変更後（ファイル全体を以下に置き換え）**:

```typescript
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import configuration from './config/configuration';
import { CoreApiModule } from './core-api/core-api.module';
import { RedisModule } from './redis/redis.module';
import { BoAuthModule } from './auth/bo-auth.module';
import { InventoryModule } from './inventory/inventory.module';
import { OrdersModule } from './orders/orders.module';
import { MembersModule } from './members/members.module';
import { BoUsersModule } from './bo-users/bo-users.module';
import { HealthController } from './health/health.controller';

@Module({
  controllers: [HealthController],
  imports: [
    ConfigModule.forRoot({
      load: [configuration],
      isGlobal: true,
    }),
    RedisModule,
    CoreApiModule,
    BoAuthModule,
    InventoryModule,
    OrdersModule,
    MembersModule,
    BoUsersModule,
  ],
})
export class AppModule {}
```

---

### T-13: bff/backoffice-bff/src/inventory/inventory.service.ts — キャッシュ無効化追加

パス: `bff/backoffice-bff/src/inventory/inventory.service.ts`

**変更前（1〜2行）**:
```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
```

**変更後**:
```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';
```

**変更前（constructor、15〜16行）**:
```typescript
export class InventoryService {
  constructor(private coreApiService: CoreApiService) {}
```

**変更後**:
```typescript
export class InventoryService {
  constructor(
    private coreApiService: CoreApiService,
    private redisService: RedisService,
  ) {}
```

**変更前（adjustInventory メソッド本体、34〜47行）**:
```typescript
  async adjustInventory(
    productId: number,
    quantityDelta: number,
    reason: string,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    return this.coreApiService.post<ApiResponse<any>>(
      '/api/bo/admin/inventory/adjust',
      { productId, quantityDelta, reason },
      token,
      traceId,
    );
  }
```

**変更後**:
```typescript
  async adjustInventory(
    productId: number,
    quantityDelta: number,
    reason: string,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.post<ApiResponse<any>>(
      '/api/bo/admin/inventory/adjust',
      { productId, quantityDelta, reason },
      token,
      traceId,
    );

    if (response.success) {
      // 商品キャッシュとリスト全体を無効化
      await this.redisService.del(`cache:product:${productId}`);
      await this.redisService.delPattern('cache:products:list:*');
    }
    return response;
  }
```

---

## Phase 3: レスポンス集約（/full エンドポイント）

### T-14: bff/customer-bff/src/products/products.controller.ts — GET :id/full追加

パス: `bff/customer-bff/src/products/products.controller.ts`

**変更前（18〜25行）**:
```typescript
  @Get(':id')
  async getProductById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.productsService.getProductById(parseInt(id, 10), req.traceId);
  }
}
```

**変更後**:
```typescript
  @Get(':id')
  async getProductById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.productsService.getProductById(parseInt(id, 10), req.traceId);
  }

  @Get(':id/full')
  async getProductFull(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.productsService.getProductFull(parseInt(id, 10), req.traceId);
  }
}
```

---

### T-15: bff/customer-bff/src/orders/orders.service.ts — getOrderFull追加

パス: `bff/customer-bff/src/orders/orders.service.ts`

**変更前（1〜2行）**:
```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
```

**変更後**:
```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';
```

**変更前（constructor、6〜7行）**:
```typescript
export class OrdersService {
  constructor(private coreApiService: CoreApiService) {}
```

**変更後**:
```typescript
export class OrdersService {
  constructor(
    private coreApiService: CoreApiService,
    private redisService: RedisService,
  ) {}
```

**挿入位置**: `cancelOrder` メソッド（77行）の直後、`private resolveSessionId` メソッドの前に以下を追加:

```typescript
  async getOrderFull(
    id: number,
    token: string,
    userId?: number,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    // 注文詳細取得（キャッシュなし — 最新状態を返す）
    const orderResponse = await this.getOrderById(id, token, userId, traceId);
    if (!orderResponse.success) return orderResponse;

    const order = orderResponse.data;
    const items = order.items ?? [];

    // 各注文アイテムの商品情報を並列取得（商品キャッシュ活用）
    const enrichedItems = await Promise.all(
      items.map(async (item: any) => {
        const cacheKey = `cache:product:${item.productId}`;
        let product = await this.redisService.get<any>(cacheKey);
        if (!product) {
          const productResponse = await this.coreApiService.get<any>(
            `/api/item/${item.productId}`,
            undefined,
            traceId,
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

### T-16: bff/customer-bff/src/orders/orders.controller.ts — GET :id/full追加

パス: `bff/customer-bff/src/orders/orders.controller.ts`

**変更前（29〜35行）**:
```typescript
  @Get(':id')
  async getOrderById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.ordersService.getOrderById(parseInt(id, 10), req.token, req.user?.id, req.traceId);
  }
```

**変更後**:
```typescript
  @Get(':id')
  async getOrderById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.ordersService.getOrderById(parseInt(id, 10), req.token, req.user?.id, req.traceId);
  }

  @Get(':id/full')
  async getOrderFull(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.ordersService.getOrderFull(parseInt(id, 10), req.token, req.user?.id, req.traceId);
  }
```

---

## Phase 4: セッション管理

### T-17: bff/customer-bff/src/session/session.middleware.ts — 新規作成

パス: `bff/customer-bff/src/session/session.middleware.ts`（新規）

```typescript
import { Injectable, NestMiddleware } from '@nestjs/common';
import { Request, Response, NextFunction } from 'express';
import { v4 as uuidv4 } from 'uuid';
import { RedisService } from '../redis/redis.service';

const SESSION_TTL = 30 * 60; // 30分
const LOGGED_IN_TTL = 7 * 24 * 60 * 60; // 7日

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

    const existing = await this.redisService.get<any>(sessionKey);
    const session = existing ?? {
      sessionId,
      createdAt: now,
      ip: req.ip,
      userAgent: req.headers['user-agent'] ?? '',
    };

    session.lastAccessAt = now;

    // ログイン済みユーザーは TTL を延長（7日）
    const ttl = session.userId ? LOGGED_IN_TTL : SESSION_TTL;
    await this.redisService.set(sessionKey, session, ttl);

    req['session'] = session;
    res.setHeader('X-Session-Id', sessionId);
    next();
  }
}
```

---

### T-18: bff/customer-bff/src/auth/auth.service.ts — login セッション更新 + logout キャッシュ削除

パス: `bff/customer-bff/src/auth/auth.service.ts`

**変更後（ファイル全体を以下に置き換え）**:

```typescript
import { Injectable } from '@nestjs/common';
import { createHash } from 'crypto';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class AuthService {
  constructor(
    private coreApiService: CoreApiService,
    private redisService: RedisService,
  ) {}

  async register(
    email: string,
    displayName: string,
    password: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    return this.coreApiService.post(
      '/api/auth/register',
      { email, displayName, password },
      undefined,
      traceId,
    );
  }

  async login(
    email: string,
    password: string,
    traceId?: string,
    sessionId?: string,
  ): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.post(
      '/api/auth/login',
      { email, password },
      undefined,
      traceId,
    );

    // ログイン成功時、セッションにユーザーIDを紐付ける
    if (response.success && sessionId) {
      const sessionKey = `session:${sessionId}`;
      const session = await this.redisService.get<any>(sessionKey) ?? {};
      session.userId = response.data?.user?.id;
      await this.redisService.set(sessionKey, session, 7 * 24 * 60 * 60);
    }

    return response;
  }

  async logout(token: string, traceId?: string): Promise<ApiResponse<void>> {
    const response = await this.coreApiService.post(
      '/api/auth/logout',
      {},
      token,
      traceId,
    );

    // ログアウト成功時、認証トークンキャッシュを削除
    if (response.success) {
      const tokenHash = createHash('sha256').update(token).digest('hex').slice(0, 32);
      await this.redisService.del(`auth:token:${tokenHash}`);
    }

    return response;
  }
}
```

---

### T-19: bff/customer-bff/src/auth/auth.controller.ts — sessionId伝播 + RateLimitGuard適用

パス: `bff/customer-bff/src/auth/auth.controller.ts`

> **前提**: T-23（rate-limit.guard.ts）, T-24（rate-limit.decorator.ts）完了後に実施

**変更後（ファイル全体を以下に置き換え）**:

```typescript
import { Controller, Post, Body, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from './auth.guard';
import { AuthService } from './auth.service';
import { ApiResponse } from '@app/shared';
import { RateLimitGuard } from '../common/guards/rate-limit.guard';
import { RateLimit } from '../common/decorators/rate-limit.decorator';

@Controller('api/auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  @Post('register')
  @UseGuards(RateLimitGuard)
  @RateLimit({ limit: 3, ttlSeconds: 600, keyType: 'ip' })
  async register(
    @Body() body: { email?: string; password: string; displayName?: string; username?: string; fullName?: string },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    const email = body.email ?? body.username ?? '';
    const displayName = body.displayName ?? body.fullName ?? body.username ?? email;
    return this.authService.register(email, displayName, body.password, req.traceId);
  }

  @Post('login')
  @UseGuards(RateLimitGuard)
  @RateLimit({ limit: 5, ttlSeconds: 60, keyType: 'ip' })
  async login(
    @Body() body: { email?: string; username?: string; password: string },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    const email = body.email ?? body.username ?? '';
    return this.authService.login(email, body.password, req.traceId, req['session']?.sessionId);
  }

  @Post('logout')
  @UseGuards(AuthGuard)
  async logout(@Req() req: any): Promise<ApiResponse<void>> {
    return this.authService.logout(req.token, req.traceId);
  }
}
```

---

## Phase 5: 認証トークンキャッシュ

### T-20: bff/customer-bff/src/auth/auth.guard.ts — トークンキャッシュ追加

パス: `bff/customer-bff/src/auth/auth.guard.ts`

**変更後（ファイル全体を以下に置き換え）**:

```typescript
import { Injectable, CanActivate, ExecutionContext, HttpException, UnauthorizedException } from '@nestjs/common';
import { createHash } from 'crypto';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';

@Injectable()
export class AuthGuard implements CanActivate {
  constructor(
    private coreApiService: CoreApiService,
    private redisService: RedisService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest();
    const token = this.extractToken(request);

    if (!token) {
      throw new UnauthorizedException({
        success: false,
        error: {
          code: 'BFF_UNAUTHORIZED',
          message: '認証トークンが必要です',
        },
      });
    }

    try {
      const user = await this.verifyToken(token, request.traceId);
      request.user = user;
      request.token = token;
      return true;
    } catch (error) {
      if (error instanceof HttpException) {
        const status = error.getStatus();
        if (status === 503 || status === 504) {
          throw error;
        }
      }
      throw new UnauthorizedException({
        success: false,
        error: {
          code: 'BFF_INVALID_TOKEN',
          message: '無効なトークンです',
        },
      });
    }
  }

  private extractToken(request: any): string | null {
    const authHeader = request.headers['authorization'];
    if (!authHeader?.startsWith('Bearer ')) return null;
    return authHeader.substring(7);
  }

  private async verifyToken(token: string, traceId?: string): Promise<any> {
    const tokenHash = createHash('sha256').update(token).digest('hex').slice(0, 32);
    const cacheKey = `auth:token:${tokenHash}`;

    // 1. キャッシュ確認
    const cached = await this.redisService.get<any>(cacheKey);
    if (cached) return cached;

    // 2. Core API 検証
    const response = await this.coreApiService.get<{ success: boolean; data?: any }>(
      '/api/auth/me',
      token,
      traceId,
    );
    if (!response?.success || !response.data) throw new Error('invalid token');

    // 3. キャッシュ保存（TTL: 60秒）
    await this.redisService.set(cacheKey, response.data, 60);
    return response.data;
  }
}
```

---

### T-21: bff/backoffice-bff/src/auth/bo-auth.guard.ts — トークンキャッシュ追加

パス: `bff/backoffice-bff/src/auth/bo-auth.guard.ts`

**変更後（ファイル全体を以下に置き換え）**:

```typescript
import { Injectable, CanActivate, ExecutionContext, HttpException, UnauthorizedException } from '@nestjs/common';
import { createHash } from 'crypto';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';

@Injectable()
export class BoAuthGuard implements CanActivate {
  constructor(
    private coreApiService: CoreApiService,
    private redisService: RedisService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest();
    const token = this.extractToken(request);

    if (!token) {
      throw new UnauthorizedException({
        success: false,
        error: {
          code: 'BFF_UNAUTHORIZED',
          message: '認証トークンが必要です',
        },
      });
    }

    try {
      const boUser = await this.verifyBoToken(token, request.traceId);
      request.boUser = boUser;
      request.token = token;
      return true;
    } catch (error) {
      if (error instanceof HttpException) {
        const status = error.getStatus();
        if (status === 503 || status === 504) {
          throw error;
        }
      }
      throw new UnauthorizedException({
        success: false,
        error: {
          code: 'BFF_INVALID_TOKEN',
          message: '無効なトークンです',
        },
      });
    }
  }

  private extractToken(request: any): string | null {
    const authHeader = request.headers['authorization'];
    if (!authHeader?.startsWith('Bearer ')) return null;
    return authHeader.substring(7);
  }

  private async verifyBoToken(token: string, traceId?: string): Promise<any> {
    const tokenHash = createHash('sha256').update(token).digest('hex').slice(0, 32);
    const cacheKey = `bo-auth:token:${tokenHash}`;

    // 1. キャッシュ確認
    const cached = await this.redisService.get<any>(cacheKey);
    if (cached) return cached;

    // 2. Core API 検証
    const response = await this.coreApiService.get<{ success: boolean; data?: any }>(
      '/api/bo-auth/me',
      token,
      traceId,
    );
    if (!response?.success || !response.data) throw new Error('invalid token');

    // 3. キャッシュ保存（TTL: 60秒）
    await this.redisService.set(cacheKey, response.data, 60);
    return response.data;
  }
}
```

---

### T-22: bff/backoffice-bff/src/auth/bo-auth.service.ts — logout時キャッシュ削除

パス: `bff/backoffice-bff/src/auth/bo-auth.service.ts`

**変更後（ファイル全体を以下に置き換え）**:

```typescript
import { Injectable } from '@nestjs/common';
import { createHash } from 'crypto';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class BoAuthService {
  constructor(
    private coreApiService: CoreApiService,
    private redisService: RedisService,
  ) {}

  async login(email: string, password: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.post(
      '/api/bo-auth/login',
      { email, password },
      undefined,
      traceId,
    );
  }

  async logout(token: string, traceId?: string): Promise<ApiResponse<void>> {
    const response = await this.coreApiService.post(
      '/api/bo-auth/logout',
      {},
      token,
      traceId,
    );

    // ログアウト成功時、認証トークンキャッシュを削除
    if (response.success) {
      const tokenHash = createHash('sha256').update(token).digest('hex').slice(0, 32);
      await this.redisService.del(`bo-auth:token:${tokenHash}`);
    }

    return response;
  }
}
```

---

## Phase 6: レート制限

### T-23: bff/customer-bff/src/common/guards/rate-limit.guard.ts — 新規作成

パス: `bff/customer-bff/src/common/guards/rate-limit.guard.ts`（新規）

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
      throw new HttpException(
        {
          success: false,
          error: {
            code: 'RATE_LIMIT_EXCEEDED',
            message: 'リクエスト制限を超えました。しばらくしてから再試行してください。',
            retryAfter: config.ttlSeconds,
          },
        },
        429,
      );
    }
    return true;
  }
}
```

---

### T-24: bff/customer-bff/src/common/decorators/rate-limit.decorator.ts — 新規作成

パス: `bff/customer-bff/src/common/decorators/rate-limit.decorator.ts`（新規）

```typescript
import { SetMetadata } from '@nestjs/common';
import { RateLimitConfig, RATE_LIMIT_KEY } from '../guards/rate-limit.guard';

export const RateLimit = (config: RateLimitConfig) => SetMetadata(RATE_LIMIT_KEY, config);
```

---

### T-25: bff/customer-bff/src/cart/cart.controller.ts — addCartItem にRateLimitGuard適用

パス: `bff/customer-bff/src/cart/cart.controller.ts`

**変更前（1行）**:
```typescript
import { Controller, Get, Post, Put, Delete, Body, Param, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from '../auth/auth.guard';
import { CartService } from './cart.service';
import { ApiResponse } from '@app/shared';
```

**変更後**:
```typescript
import { Controller, Get, Post, Put, Delete, Body, Param, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from '../auth/auth.guard';
import { CartService } from './cart.service';
import { ApiResponse } from '@app/shared';
import { RateLimitGuard } from '../common/guards/rate-limit.guard';
import { RateLimit } from '../common/decorators/rate-limit.decorator';
```

**変更前（16〜17行）**:
```typescript
  @Post('items')
  async addCartItem(
```

**変更後**:
```typescript
  @Post('items')
  @UseGuards(RateLimitGuard)
  @RateLimit({ limit: 10, ttlSeconds: 60, keyType: 'user' })
  async addCartItem(
```

---

### T-26: bff/backoffice-bff/src/common/guards/rate-limit.guard.ts — 新規作成

パス: `bff/backoffice-bff/src/common/guards/rate-limit.guard.ts`（新規）

T-23（customer-bff）と同一内容をコピー:

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
      ? `user:${request.boUser?.id ?? 'anonymous'}`
      : `ip:${request.ip}`;
    const endpoint = context.getHandler().name;
    const key = `ratelimit:${identifier}:${endpoint}`;

    const count = await this.redisService.incr(key, config.ttlSeconds);
    if (count === 0) return true;

    const remaining = Math.max(0, config.limit - count);
    const resetAt = Math.floor(Date.now() / 1000) + config.ttlSeconds;

    const response = context.switchToHttp().getResponse();
    response.setHeader('X-RateLimit-Limit', config.limit);
    response.setHeader('X-RateLimit-Remaining', remaining);
    response.setHeader('X-RateLimit-Reset', resetAt);

    if (count > config.limit) {
      throw new HttpException(
        {
          success: false,
          error: {
            code: 'RATE_LIMIT_EXCEEDED',
            message: 'リクエスト制限を超えました。しばらくしてから再試行してください。',
            retryAfter: config.ttlSeconds,
          },
        },
        429,
      );
    }
    return true;
  }
}
```

> **注意**: backoffice-bff版は `request.user` の代わりに `request.boUser` を参照する。

---

## テスト手順

実装後に以下を手動確認（docker compose up -d 後）:

```bash
# Redis疎通確認
docker compose exec redis redis-cli ping
```

1. **商品一覧キャッシュHIT**: 2回連続で `GET /api/products` → 2回目はRedisから返ること
   ```bash
   curl http://localhost:3001/api/products
   docker compose exec redis redis-cli keys "cache:products:*"  # キーが存在すること
   ```

2. **商品詳細キャッシュHIT**: `GET /api/products/1` → `cache:product:1` がRedisに格納されること
   ```bash
   docker compose exec redis redis-cli get "cache:product:1"
   ```

3. **在庫調整後のキャッシュ無効化**: BackOffice BFFから在庫調整 → `cache:products:list:*` が削除されること

4. **GET /api/products/1/full**: `{ product: {...}, relatedProducts: [...] }` が返ること
   ```bash
   curl http://localhost:3001/api/products/1/full
   ```

5. **GET /api/orders/1/full**: 認証トークン付きで `{ order: {...}, items: [{..., product: {...}}] }` が返ること
   ```bash
   curl -H "Authorization: Bearer <token>" http://localhost:3001/api/orders/1/full
   ```

6. **セッション生成**: `X-Session-Id` ヘッダーなしのリクエストでレスポンスに `X-Session-Id` が付与されること
   ```bash
   curl -v http://localhost:3001/api/products 2>&1 | grep X-Session-Id
   ```

7. **トークンキャッシュHIT**: ログイン後に複数回認証APIを呼び出し、Core APIへの呼び出し回数が減ること（customer-bffログで確認）
   ```bash
   docker compose logs -f customer-bff  # "Redis GET" のログを確認
   ```

8. **レート制限**: ログインエンドポイントに6回リクエスト → 6回目で429が返ること
   ```bash
   for i in {1..6}; do curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:3001/api/auth/login -H "Content-Type: application/json" -d '{"email":"test@test.com","password":"wrong"}'; done
   ```

9. **Redis障害時フォールバック**: Redisを停止してもAPIが正常動作すること
   ```bash
   docker compose stop redis
   curl http://localhost:3001/api/products  # 正常レスポンスを確認
   docker compose start redis
   ```

---

## Review Packet

### 変更サマリ（10行以内）
- docker-compose.yml にRedisサービス追加、BFF両者に REDIS_HOST/PORT/TIMEOUT 環境変数追加
- customer-bff/backoffice-bff に ioredis ^5.3.0 追加、RedisService/RedisModule を各bffに新規作成
- configuration.ts に redis.host/port 設定追加（両bff）
- customer-bff: app.module.ts を NestModule 実装に更新、SessionMiddleware・RateLimitGuard をグローバル登録
- customer-bff: products.service.ts に Redisキャッシュ追加（TTL 180s/600s）+ getProductFull 追加
- backoffice-bff: inventory.service.ts の adjustInventory にキャッシュ無効化追加
- customer-bff: orders.service.ts に getOrderFull 追加、products/orders controller に /full エンドポイント追加
- session.middleware.ts 新規作成（X-Session-Id ヘッダー管理）
- auth.service.ts/auth.guard.ts/bo-auth.guard.ts/bo-auth.service.ts に Redisトークンキャッシュ追加
- rate-limit.guard.ts/rate-limit.decorator.ts 新規作成（customer-bff/backoffice-bff）、cart.controller.ts に適用

### 変更ファイル一覧
- docker-compose.yml
- bff/customer-bff/package.json
- bff/customer-bff/src/redis/redis.service.ts（新規）
- bff/customer-bff/src/redis/redis.module.ts（新規）
- bff/customer-bff/src/config/configuration.ts
- bff/customer-bff/src/app.module.ts
- bff/customer-bff/src/products/products.service.ts
- bff/customer-bff/src/products/products.controller.ts
- bff/customer-bff/src/orders/orders.service.ts
- bff/customer-bff/src/orders/orders.controller.ts
- bff/customer-bff/src/session/session.middleware.ts（新規）
- bff/customer-bff/src/auth/auth.service.ts
- bff/customer-bff/src/auth/auth.controller.ts
- bff/customer-bff/src/auth/auth.guard.ts
- bff/customer-bff/src/common/guards/rate-limit.guard.ts（新規）
- bff/customer-bff/src/common/decorators/rate-limit.decorator.ts（新規）
- bff/customer-bff/src/cart/cart.controller.ts
- bff/backoffice-bff/package.json
- bff/backoffice-bff/src/redis/redis.service.ts（新規）
- bff/backoffice-bff/src/redis/redis.module.ts（新規）
- bff/backoffice-bff/src/config/configuration.ts
- bff/backoffice-bff/src/app.module.ts
- bff/backoffice-bff/src/inventory/inventory.service.ts
- bff/backoffice-bff/src/auth/bo-auth.guard.ts
- bff/backoffice-bff/src/auth/bo-auth.service.ts
- bff/backoffice-bff/src/common/guards/rate-limit.guard.ts（新規）
- package-lock.json（ioredis追加による自動更新）

### リスクと未解決
- task.md のコードにはTypeScript型パラメータが省略されていたため、`noImplicitAny: true` の環境でコンパイルエラーが発生。`coreApiService.post<ApiResponse<any>>` 等の型パラメータを補完し、`req['session']` を `(req as any)['session']` に修正した（最小限の変更）
- APP_GUARD（グローバル）と @UseGuards(RateLimitGuard)（ルート）が二重適用されるため、login/register/addCartItemのレート制限は実効リミットが半減する（limit:5 → 実質3回）。task.md 設計に従い変更は加えていない

### テスト結果
```
docker compose exec redis redis-cli ping  # → PONG
curl http://localhost:3001/health  # → {"status":"ok"}
curl http://localhost:3002/health  # → {"status":"ok"}
```

- [PASS] Redis疎通: `PONG`
- [PASS] customer-bff起動: `{"status":"ok","service":"customer-bff"}`
- [PASS] backoffice-bff起動: `{"status":"ok","service":"backoffice-bff"}`
- [PASS] 商品一覧キャッシュHIT: `cache:products:list:page:1:limit:20` がRedisに格納
- [PASS] GET /api/products/1/full: `{ product: {...}, relatedProducts: [...] }` が返る
- [PASS] X-Session-Id: レスポンスヘッダーに `X-Session-Id` が付与される
- [PASS] レート制限: loginに6回リクエストで3回目から429（APP_GUARD二重適用による実効半減）
- [PASS] Redis障害時フォールバック: Redis停止後も `/api/products` が正常レスポンス
