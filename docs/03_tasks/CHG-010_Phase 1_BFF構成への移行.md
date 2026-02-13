# CHG-010: BFF構成への移行 - 実装タスク

## 前提条件

- CHG-008（ドメイン分離とBoUser管理）完了済み
- CHG-009（PostgreSQL移行）完了済み
- 技術設計ドキュメント（`docs/02_designs/CHG-010_BFF構成への移行.md`）確定済み

---

## Phase 1: Customer BFF導入

### 検証コマンド

```bash
# BFF起動確認
cd bff/customer-bff
npm run start:dev

# フロントエンドから接続確認
curl http://localhost:3001/api/products

# E2Eテスト
cd frontend
npm run test:e2e
```

---

## Task 1: プロジェクト基盤セットアップ

### 目的
npm workspaceを設定し、BFF・共通型定義のプロジェクト構造を作成する。

### 実装内容

#### 1.1 ルート package.json 作成

**ファイル**: `/package.json`

```json
{
  "name": "ai-ec-experiment",
  "version": "1.0.0",
  "private": true,
  "workspaces": [
    "frontend",
    "bff/shared",
    "bff/customer-bff",
    "bff/backoffice-bff"
  ],
  "scripts": {
    "dev:frontend": "npm run dev --workspace=frontend",
    "dev:customer-bff": "npm run start:dev --workspace=bff/customer-bff",
    "dev:backoffice-bff": "npm run start:dev --workspace=bff/backoffice-bff",
    "build:all": "npm run build --workspaces --if-present",
    "test:all": "npm run test --workspaces --if-present"
  },
  "devDependencies": {
    "@types/node": "^20.0.0",
    "typescript": "^5.3.0"
  }
}
```

#### 1.2 bff/shared プロジェクト作成

**ディレクトリ作成**:
```bash
mkdir -p bff/shared/src/{dto,types}
```

**ファイル**: `bff/shared/package.json`

```json
{
  "name": "@app/shared",
  "version": "1.0.0",
  "main": "dist/index.js",
  "types": "dist/index.d.ts",
  "scripts": {
    "build": "tsc",
    "watch": "tsc --watch"
  },
  "devDependencies": {
    "typescript": "^5.3.0"
  }
}
```

**ファイル**: `bff/shared/tsconfig.json`

```json
{
  "compilerOptions": {
    "target": "ES2021",
    "module": "commonjs",
    "declaration": true,
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
```

#### 1.3 customer-bff プロジェクト初期化

**ディレクトリ作成**:
```bash
mkdir -p bff/customer-bff
cd bff/customer-bff
```

**Nest.js CLI でプロジェクト初期化**:
```bash
npm i -g @nestjs/cli
nest new . --skip-git --package-manager npm
```

**ファイル**: `bff/customer-bff/package.json` に依存関係を追加

```json
{
  "dependencies": {
    "@app/shared": "workspace:*",
    "@nestjs/common": "^10.0.0",
    "@nestjs/core": "^10.0.0",
    "@nestjs/platform-express": "^10.0.0",
    "@nestjs/config": "^3.0.0",
    "@nestjs/axios": "^3.0.0",
    "axios": "^1.6.0",
    "rxjs": "^7.8.0",
    "winston": "^3.11.0",
    "uuid": "^9.0.0"
  },
  "devDependencies": {
    "@nestjs/cli": "^10.0.0",
    "@nestjs/schematics": "^10.0.0",
    "@nestjs/testing": "^10.0.0",
    "@types/express": "^4.17.17",
    "@types/jest": "^29.5.2",
    "@types/node": "^20.3.1",
    "@types/uuid": "^9.0.0",
    "jest": "^29.5.0",
    "ts-jest": "^29.1.0",
    "ts-node": "^10.9.1",
    "typescript": "^5.1.3"
  }
}
```

#### 1.4 依存関係のインストール

```bash
# ルートでインストール
cd /home/joji/work/ai-ec-experiment
npm install
```

### 検証

```bash
# workspace構成確認
npm run build:all

# customer-bff が起動するか確認
npm run dev:customer-bff
# → http://localhost:3000 でNest.jsデフォルトページが表示されればOK
```

---

## Task 2: 共通型定義とCore API連携基盤

### 目的
フロントエンド・BFF間で共有する型定義を実装し、Core APIとの通信基盤を構築する。

### 実装内容

#### 2.1 共通型定義（bff/shared）

**ファイル**: `bff/shared/src/types/api-response.ts`

```typescript
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
  };
}
```

**ファイル**: `bff/shared/src/dto/product.dto.ts`

```typescript
export interface ProductDto {
  id: number;
  name: string;
  price: number;
  stock: number;
  imageUrl: string;
  description: string;
  isPublic: boolean;
}
```

**ファイル**: `bff/shared/src/dto/cart.dto.ts`

```typescript
export interface CartDto {
  items: CartItemDto[];
  totalAmount: number;
  totalItems: number;
}

export interface CartItemDto {
  id: number;
  productId: number;
  quantity: number;
  product?: ProductDto;
  subtotal?: number;
}
```

**ファイル**: `bff/shared/src/dto/order.dto.ts`

```typescript
export interface OrderDto {
  id: number;
  orderNumber: string;
  userId?: number;
  status: string;
  totalAmount: number;
  createdAt: string;
  items: OrderItemDto[];
}

export interface OrderItemDto {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  price: number;
}
```

**ファイル**: `bff/shared/src/dto/user.dto.ts`

```typescript
export interface UserDto {
  id: number;
  username: string;
  email: string;
  fullName: string;
  phoneNumber?: string;
  createdAt: string;
}
```

**ファイル**: `bff/shared/src/index.ts`

```typescript
// 型定義
export * from './types/api-response';

// DTO
export * from './dto/product.dto';
export * from './dto/cart.dto';
export * from './dto/order.dto';
export * from './dto/user.dto';
```

**ビルド**:
```bash
cd bff/shared
npm run build
```

#### 2.2 設定管理（customer-bff）

**ファイル**: `bff/customer-bff/src/config/configuration.ts`

```typescript
export default () => ({
  server: {
    port: parseInt(process.env.PORT, 10) || 3001,
  },
  coreApi: {
    url: process.env.CORE_API_URL || 'http://localhost:8080',
    timeout: parseInt(process.env.CORE_API_TIMEOUT, 10) || 5000,
    retry: parseInt(process.env.CORE_API_RETRY, 10) || 2,
  },
  logging: {
    level: process.env.LOG_LEVEL || 'info',
  },
});
```

**ファイル**: `bff/customer-bff/.env.development`

```bash
NODE_ENV=development
PORT=3001
CORE_API_URL=http://localhost:8080
CORE_API_TIMEOUT=5000
CORE_API_RETRY=2
LOG_LEVEL=debug
```

#### 2.3 Core API連携サービス

**ファイル**: `bff/customer-bff/src/core-api/core-api.module.ts`

```typescript
import { Module } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { CoreApiService } from './core-api.service';

@Module({
  imports: [HttpModule],
  providers: [CoreApiService],
  exports: [CoreApiService],
})
export class CoreApiModule {}
```

**ファイル**: `bff/customer-bff/src/core-api/core-api.service.ts`

```typescript
import { Injectable, HttpException, Logger } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { firstValueFrom, timeout, retry } from 'rxjs';

@Injectable()
export class CoreApiService {
  private readonly logger = new Logger(CoreApiService.name);
  private readonly baseUrl: string;
  private readonly timeoutMs: number;
  private readonly retryCount: number;

  constructor(
    private httpService: HttpService,
    private configService: ConfigService,
  ) {
    this.baseUrl = this.configService.get<string>('coreApi.url');
    this.timeoutMs = this.configService.get<number>('coreApi.timeout');
    this.retryCount = this.configService.get<number>('coreApi.retry');
  }

  async get<T>(path: string, token?: string, traceId?: string): Promise<T> {
    try {
      const response = await firstValueFrom(
        this.httpService.get(`${this.baseUrl}${path}`, {
          headers: this.buildHeaders(token, traceId),
        }).pipe(
          timeout(this.timeoutMs),
          retry({
            count: this.retryCount,
            delay: 1000,
          }),
        ),
      );
      return response.data;
    } catch (error) {
      this.handleError(error, path, 'GET');
    }
  }

  async post<T>(path: string, data: any, token?: string, traceId?: string): Promise<T> {
    try {
      const response = await firstValueFrom(
        this.httpService.post(`${this.baseUrl}${path}`, data, {
          headers: this.buildHeaders(token, traceId),
        }).pipe(
          timeout(this.timeoutMs),
        ),
      );
      return response.data;
    } catch (error) {
      this.handleError(error, path, 'POST');
    }
  }

  async put<T>(path: string, data: any, token?: string, traceId?: string): Promise<T> {
    try {
      const response = await firstValueFrom(
        this.httpService.put(`${this.baseUrl}${path}`, data, {
          headers: this.buildHeaders(token, traceId),
        }).pipe(
          timeout(this.timeoutMs),
        ),
      );
      return response.data;
    } catch (error) {
      this.handleError(error, path, 'PUT');
    }
  }

  async delete<T>(path: string, token?: string, traceId?: string): Promise<T> {
    try {
      const response = await firstValueFrom(
        this.httpService.delete(`${this.baseUrl}${path}`, {
          headers: this.buildHeaders(token, traceId),
        }).pipe(
          timeout(this.timeoutMs),
        ),
      );
      return response.data;
    } catch (error) {
      this.handleError(error, path, 'DELETE');
    }
  }

  private buildHeaders(token?: string, traceId?: string): Record<string, string> {
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    if (traceId) headers['X-Trace-Id'] = traceId;
    return headers;
  }

  private handleError(error: any, path: string, method: string): never {
    this.logger.error(`Core API error: ${method} ${path}`, error);

    if (error.response) {
      const statusCode = error.response.status;
      const responseData = error.response.data;

      if (responseData?.success === false && responseData?.error) {
        throw new HttpException(responseData, statusCode);
      }

      throw new HttpException({
        success: false,
        error: {
          code: 'BFF_CORE_API_ERROR',
          message: responseData?.message || 'Core APIエラー',
        },
      }, statusCode);
    } else if (error.code === 'ECONNREFUSED') {
      throw new HttpException({
        success: false,
        error: {
          code: 'BFF_CORE_API_UNAVAILABLE',
          message: 'サービスが一時的に利用できません',
        },
      }, 503);
    } else if (error.name === 'TimeoutError') {
      throw new HttpException({
        success: false,
        error: {
          code: 'BFF_CORE_API_TIMEOUT',
          message: 'リクエストがタイムアウトしました',
        },
      }, 504);
    } else {
      throw new HttpException({
        success: false,
        error: {
          code: 'BFF_INTERNAL_ERROR',
          message: '予期しないエラーが発生しました',
        },
      }, 500);
    }
  }
}
```

#### 2.4 app.module.ts 更新

**ファイル**: `bff/customer-bff/src/app.module.ts`

```typescript
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import configuration from './config/configuration';
import { CoreApiModule } from './core-api/core-api.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      load: [configuration],
      isGlobal: true,
    }),
    CoreApiModule,
  ],
})
export class AppModule {}
```

### 検証

```bash
# ビルド確認
cd bff/customer-bff
npm run build

# 起動確認
npm run start:dev

# Core API疎通確認（別ターミナル）
curl http://localhost:3001/health  # ヘルスチェック（実装後）
```

---

## Task 3: 認証・インターセプター実装

### 目的
User認証、トレース伝播、ログ出力、エラーハンドリングの共通機能を実装する。

### 実装内容

#### 3.1 TraceInterceptor

**ファイル**: `bff/customer-bff/src/common/interceptors/trace.interceptor.ts`

```typescript
import { Injectable, NestInterceptor, ExecutionContext, CallHandler } from '@nestjs/common';
import { Observable } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';

@Injectable()
export class TraceInterceptor implements NestInterceptor {
  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    const request = context.switchToHttp().getRequest();
    const response = context.switchToHttp().getResponse();

    const traceId = request.headers['x-trace-id'] || uuidv4();
    request.traceId = traceId;

    response.setHeader('X-Trace-Id', traceId);

    return next.handle();
  }
}
```

#### 3.2 LoggingInterceptor

**ファイル**: `bff/customer-bff/src/common/interceptors/logging.interceptor.ts`

```typescript
import { Injectable, NestInterceptor, ExecutionContext, CallHandler, Logger } from '@nestjs/common';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable()
export class LoggingInterceptor implements NestInterceptor {
  private readonly logger = new Logger('HTTP');

  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    const request = context.switchToHttp().getRequest();
    const { method, url, traceId } = request;
    const startTime = Date.now();

    return next.handle().pipe(
      tap({
        next: () => {
          const response = context.switchToHttp().getResponse();
          const { statusCode } = response;
          const duration = Date.now() - startTime;

          this.logger.log(JSON.stringify({
            traceId,
            method,
            url,
            statusCode,
            duration,
          }));
        },
        error: (error) => {
          const duration = Date.now() - startTime;

          this.logger.error(JSON.stringify({
            traceId,
            method,
            url,
            statusCode: error.status || 500,
            duration,
            error: error.message,
          }));
        },
      }),
    );
  }
}
```

#### 3.3 HttpExceptionFilter

**ファイル**: `bff/customer-bff/src/common/filters/http-exception.filter.ts`

```typescript
import { ExceptionFilter, Catch, ArgumentsHost, HttpException, Logger } from '@nestjs/common';

@Catch(HttpException)
export class HttpExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(HttpExceptionFilter.name);

  catch(exception: HttpException, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse();
    const request = ctx.getRequest();
    const status = exception.getStatus();
    const exceptionResponse = exception.getResponse();

    this.logger.error(`Exception: ${request.method} ${request.url}`, exceptionResponse);

    // すでに ApiResponse<T> 形式の場合はそのまま返す
    if (typeof exceptionResponse === 'object' && 'success' in exceptionResponse) {
      return response.status(status).json(exceptionResponse);
    }

    // それ以外はBFF形式にラップ
    response.status(status).json({
      success: false,
      error: {
        code: 'BFF_INTERNAL_ERROR',
        message: typeof exceptionResponse === 'string'
          ? exceptionResponse
          : exceptionResponse['message'] || '予期しないエラー',
      },
    });
  }
}
```

#### 3.4 AuthGuard

**ファイル**: `bff/customer-bff/src/auth/auth.guard.ts`

```typescript
import { Injectable, CanActivate, ExecutionContext, UnauthorizedException } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';

@Injectable()
export class AuthGuard implements CanActivate {
  constructor(private coreApiService: CoreApiService) {}

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
      // Core APIでトークン検証（冪等なのでリトライ有効）
      const user = await this.verifyToken(token, request.traceId);
      request.user = user;
      request.token = token;
      return true;
    } catch (error) {
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
    // トークン検証は冪等なのでリトライ可能
    return this.coreApiService.post('/auth/verify-token', { token }, token, traceId);
  }
}
```

#### 3.5 main.ts でグローバル設定

**ファイル**: `bff/customer-bff/src/main.ts`

```typescript
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { ConfigService } from '@nestjs/config';
import { TraceInterceptor } from './common/interceptors/trace.interceptor';
import { LoggingInterceptor } from './common/interceptors/logging.interceptor';
import { HttpExceptionFilter } from './common/filters/http-exception.filter';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  const configService = app.get(ConfigService);

  // グローバルインターセプター
  app.useGlobalInterceptors(new TraceInterceptor());
  app.useGlobalInterceptors(new LoggingInterceptor());

  // グローバルフィルター
  app.useGlobalFilters(new HttpExceptionFilter());

  // CORS設定
  app.enableCors({
    origin: 'http://localhost:5173',
    credentials: true,
  });

  const port = configService.get<number>('server.port');
  await app.listen(port);
  console.log(`Customer BFF is running on: http://localhost:${port}`);
}
bootstrap();
```

### 検証

```bash
# 起動
npm run start:dev

# トレースID確認
curl -v http://localhost:3001/
# → レスポンスヘッダーに X-Trace-Id が含まれればOK

# ログ出力確認
# → コンソールにJSON形式のログが出力されればOK
```

---

## Task 4: 商品API実装

### 目的
顧客向け商品API（一覧・詳細）を実装し、BFFでレスポンスを整形する。

### 実装内容

#### 4.1 ProductsModule

**ファイル**: `bff/customer-bff/src/products/products.module.ts`

```typescript
import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { ProductsController } from './products.controller';
import { ProductsService } from './products.service';

@Module({
  imports: [CoreApiModule],
  controllers: [ProductsController],
  providers: [ProductsService],
})
export class ProductsModule {}
```

#### 4.2 ProductsService

**ファイル**: `bff/customer-bff/src/products/products.service.ts`

```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse, ProductDto } from '@app/shared';

@Injectable()
export class ProductsService {
  constructor(private coreApiService: CoreApiService) {}

  async getProducts(traceId?: string): Promise<ApiResponse<ProductDto[]>> {
    const response = await this.coreApiService.get<ApiResponse<ProductDto[]>>(
      '/products',
      undefined,
      traceId,
    );

    // BFFで整形: 公開商品のみフィルタ、管理用フィールド削除
    const publicProducts = response.data
      ?.filter(p => p.isPublic)
      .map(p => this.transformProduct(p));

    return {
      success: true,
      data: publicProducts,
    };
  }

  async getProductById(id: number, traceId?: string): Promise<ApiResponse<ProductDto>> {
    const response = await this.coreApiService.get<ApiResponse<ProductDto>>(
      `/products/${id}`,
      undefined,
      traceId,
    );

    if (!response.data?.isPublic) {
      return {
        success: false,
        error: {
          code: 'PRODUCT_NOT_FOUND',
          message: '商品が見つかりません',
        },
      };
    }

    return {
      success: true,
      data: this.transformProduct(response.data),
    };
  }

  private transformProduct(product: ProductDto): any {
    return {
      id: product.id,
      name: product.name,
      price: product.price,
      imageUrl: product.imageUrl,
      description: product.description,
      stockStatus: this.getStockStatus(product.stock),
    };
  }

  private getStockStatus(stock: number): string {
    if (stock === 0) return '在庫なし';
    if (stock <= 5) return '残りわずか';
    return '在庫あり';
  }
}
```

#### 4.3 ProductsController

**ファイル**: `bff/customer-bff/src/products/products.controller.ts`

```typescript
import { Controller, Get, Param, Req } from '@nestjs/common';
import { ProductsService } from './products.service';
import { ApiResponse, ProductDto } from '@app/shared';

@Controller('api/products')
export class ProductsController {
  constructor(private productsService: ProductsService) {}

  @Get()
  async getProducts(@Req() req: any): Promise<ApiResponse<ProductDto[]>> {
    return this.productsService.getProducts(req.traceId);
  }

  @Get(':id')
  async getProductById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<ProductDto>> {
    return this.productsService.getProductById(parseInt(id, 10), req.traceId);
  }
}
```

#### 4.4 app.module.ts に追加

**ファイル**: `bff/customer-bff/src/app.module.ts`

```typescript
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import configuration from './config/configuration';
import { CoreApiModule } from './core-api/core-api.module';
import { ProductsModule } from './products/products.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      load: [configuration],
      isGlobal: true,
    }),
    CoreApiModule,
    ProductsModule,
  ],
})
export class AppModule {}
```

### 検証

```bash
# Core API起動
cd backend
./mvnw spring-boot:run

# Customer BFF起動
cd bff/customer-bff
npm run start:dev

# 商品一覧取得
curl http://localhost:3001/api/products
# → 公開商品のみ、stockStatus フィールド付きで返却されればOK

# 商品詳細取得
curl http://localhost:3001/api/products/1
# → 公開商品であれば詳細が返却されればOK
```

---

## Task 5: カートAPI実装

### 目的
カートAPI（取得・追加・更新・削除）を実装し、小計・合計を計算する。

### 実装内容

#### 5.1 CartModule

**ファイル**: `bff/customer-bff/src/cart/cart.module.ts`

```typescript
import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { CartController } from './cart.controller';
import { CartService } from './cart.service';

@Module({
  imports: [CoreApiModule],
  controllers: [CartController],
  providers: [CartService],
})
export class CartModule {}
```

#### 5.2 CartService

**ファイル**: `bff/customer-bff/src/cart/cart.service.ts`

```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse, CartDto, CartItemDto, ProductDto } from '@app/shared';

@Injectable()
export class CartService {
  constructor(private coreApiService: CoreApiService) {}

  async getCart(token: string, traceId?: string): Promise<ApiResponse<CartDto>> {
    const response = await this.coreApiService.get<ApiResponse<any>>(
      '/cart',
      token,
      traceId,
    );

    if (!response.success || !response.data) {
      return response;
    }

    // BFFで整形: 小計・合計計算、商品情報埋め込み
    const items = response.data.items.map((item: any) => ({
      ...item,
      subtotal: item.quantity * (item.product?.price || 0),
    }));

    const totalAmount = items.reduce((sum, item) => sum + item.subtotal, 0);
    const totalItems = items.reduce((sum, item) => sum + item.quantity, 0);

    return {
      success: true,
      data: {
        items,
        totalAmount,
        totalItems,
      },
    };
  }

  async addCartItem(
    productId: number,
    quantity: number,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<CartItemDto>> {
    return this.coreApiService.post(
      '/cart/items',
      { productId, quantity },
      token,
      traceId,
    );
  }

  async updateCartItem(
    id: number,
    quantity: number,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<CartItemDto>> {
    return this.coreApiService.put(
      `/cart/items/${id}`,
      { quantity },
      token,
      traceId,
    );
  }

  async deleteCartItem(
    id: number,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<void>> {
    return this.coreApiService.delete(
      `/cart/items/${id}`,
      token,
      traceId,
    );
  }
}
```

#### 5.3 CartController

**ファイル**: `bff/customer-bff/src/cart/cart.controller.ts`

```typescript
import { Controller, Get, Post, Put, Delete, Body, Param, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from '../auth/auth.guard';
import { CartService } from './cart.service';
import { ApiResponse, CartDto, CartItemDto } from '@app/shared';

@Controller('api/cart')
@UseGuards(AuthGuard)
export class CartController {
  constructor(private cartService: CartService) {}

  @Get()
  async getCart(@Req() req: any): Promise<ApiResponse<CartDto>> {
    return this.cartService.getCart(req.token, req.traceId);
  }

  @Post('items')
  async addCartItem(
    @Body() body: { productId: number; quantity: number },
    @Req() req: any,
  ): Promise<ApiResponse<CartItemDto>> {
    return this.cartService.addCartItem(
      body.productId,
      body.quantity,
      req.token,
      req.traceId,
    );
  }

  @Put('items/:id')
  async updateCartItem(
    @Param('id') id: string,
    @Body() body: { quantity: number },
    @Req() req: any,
  ): Promise<ApiResponse<CartItemDto>> {
    return this.cartService.updateCartItem(
      parseInt(id, 10),
      body.quantity,
      req.token,
      req.traceId,
    );
  }

  @Delete('items/:id')
  async deleteCartItem(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<void>> {
    return this.cartService.deleteCartItem(
      parseInt(id, 10),
      req.token,
      req.traceId,
    );
  }
}
```

#### 5.4 app.module.ts に追加

```typescript
import { CartModule } from './cart/cart.module';

@Module({
  imports: [
    // ...
    CartModule,
  ],
})
```

### 検証

```bash
# ログイン（トークン取得）
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"password"}'
# → {"success":true,"data":{"token":"...", "user":{...}}}

# カート取得（BFF経由）
curl http://localhost:3001/api/cart \
  -H "Authorization: Bearer {token}"
# → totalAmount, totalItems が含まれていればOK

# カート追加
curl -X POST http://localhost:3001/api/cart/items \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'
```

---

## Task 6: 注文・会員API実装

### 目的
注文API（確定・履歴・詳細）と会員API（情報取得）を実装する。

### 実装内容

#### 6.1 OrdersModule

**ファイル**: `bff/customer-bff/src/orders/orders.module.ts`

```typescript
import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { OrdersController } from './orders.controller';
import { OrdersService } from './orders.service';

@Module({
  imports: [CoreApiModule],
  controllers: [OrdersController],
  providers: [OrdersService],
})
export class OrdersModule {}
```

#### 6.2 OrdersService

**ファイル**: `bff/customer-bff/src/orders/orders.service.ts`

```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse, OrderDto } from '@app/shared';

@Injectable()
export class OrdersService {
  constructor(private coreApiService: CoreApiService) {}

  async createOrder(token: string, traceId?: string): Promise<ApiResponse<OrderDto>> {
    // 注文確定後、詳細を自動取得
    const createResponse = await this.coreApiService.post<ApiResponse<OrderDto>>(
      '/orders',
      {},
      token,
      traceId,
    );

    if (!createResponse.success || !createResponse.data) {
      return createResponse;
    }

    const orderId = createResponse.data.id;
    return this.getOrderById(orderId, token, traceId);
  }

  async getOrders(token: string, traceId?: string): Promise<ApiResponse<OrderDto[]>> {
    const response = await this.coreApiService.get<ApiResponse<OrderDto[]>>(
      '/orders',
      token,
      traceId,
    );

    if (!response.success || !response.data) {
      return response;
    }

    // BFFで整形: ステータスを日本語化
    const orders = response.data.map(order => ({
      ...order,
      status: this.translateStatus(order.status),
    }));

    return {
      success: true,
      data: orders,
    };
  }

  async getOrderById(id: number, token: string, traceId?: string): Promise<ApiResponse<OrderDto>> {
    const response = await this.coreApiService.get<ApiResponse<OrderDto>>(
      `/orders/${id}`,
      token,
      traceId,
    );

    if (!response.success || !response.data) {
      return response;
    }

    return {
      success: true,
      data: {
        ...response.data,
        status: this.translateStatus(response.data.status),
      },
    };
  }

  private translateStatus(status: string): string {
    const statusMap: Record<string, string> = {
      'PENDING': '処理中',
      'CONFIRMED': '確定',
      'SHIPPED': '発送済み',
      'DELIVERED': '配達完了',
      'CANCELLED': 'キャンセル',
    };
    return statusMap[status] || status;
  }
}
```

#### 6.3 OrdersController

**ファイル**: `bff/customer-bff/src/orders/orders.controller.ts`

```typescript
import { Controller, Get, Post, Param, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from '../auth/auth.guard';
import { OrdersService } from './orders.service';
import { ApiResponse, OrderDto } from '@app/shared';

@Controller('api/orders')
@UseGuards(AuthGuard)
export class OrdersController {
  constructor(private ordersService: OrdersService) {}

  @Post()
  async createOrder(@Req() req: any): Promise<ApiResponse<OrderDto>> {
    return this.ordersService.createOrder(req.token, req.traceId);
  }

  @Get()
  async getOrders(@Req() req: any): Promise<ApiResponse<OrderDto[]>> {
    return this.ordersService.getOrders(req.token, req.traceId);
  }

  @Get(':id')
  async getOrderById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<OrderDto>> {
    return this.ordersService.getOrderById(parseInt(id, 10), req.token, req.traceId);
  }
}
```

#### 6.4 MembersModule

**ファイル**: `bff/customer-bff/src/members/members.module.ts`

```typescript
import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { MembersController } from './members.controller';
import { MembersService } from './members.service';

@Module({
  imports: [CoreApiModule],
  controllers: [MembersController],
  providers: [MembersService],
})
export class MembersModule {}
```

#### 6.5 MembersService

**ファイル**: `bff/customer-bff/src/members/members.service.ts`

```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse, UserDto } from '@app/shared';

@Injectable()
export class MembersService {
  constructor(private coreApiService: CoreApiService) {}

  async getMe(userId: number, token: string, traceId?: string): Promise<ApiResponse<UserDto>> {
    const response = await this.coreApiService.get<ApiResponse<UserDto>>(
      `/members/${userId}`,
      token,
      traceId,
    );

    if (!response.success || !response.data) {
      return response;
    }

    // BFFで整形: 機微情報のマスキング（電話番号下4桁のみ表示）
    const user = response.data;
    if (user.phoneNumber) {
      user.phoneNumber = user.phoneNumber.slice(-4).padStart(user.phoneNumber.length, '*');
    }

    return {
      success: true,
      data: user,
    };
  }
}
```

#### 6.6 MembersController

**ファイル**: `bff/customer-bff/src/members/members.controller.ts`

```typescript
import { Controller, Get, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from '../auth/auth.guard';
import { MembersService } from './members.service';
import { ApiResponse, UserDto } from '@app/shared';

@Controller('api/members')
@UseGuards(AuthGuard)
export class MembersController {
  constructor(private membersService: MembersService) {}

  @Get('me')
  async getMe(@Req() req: any): Promise<ApiResponse<UserDto>> {
    return this.membersService.getMe(req.user.id, req.token, req.traceId);
  }
}
```

#### 6.7 AuthModule（ログイン・登録・ログアウト）

**ファイル**: `bff/customer-bff/src/auth/auth.module.ts`

```typescript
import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { AuthGuard } from './auth.guard';

@Module({
  imports: [CoreApiModule],
  controllers: [AuthController],
  providers: [AuthService, AuthGuard],
  exports: [AuthGuard],
})
export class AuthModule {}
```

**ファイル**: `bff/customer-bff/src/auth/auth.service.ts`

```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class AuthService {
  constructor(private coreApiService: CoreApiService) {}

  async register(username: string, password: string, email: string, fullName: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.post(
      '/auth/register',
      { username, password, email, fullName },
      undefined,
      traceId,
    );
  }

  async login(username: string, password: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.post(
      '/auth/login',
      { username, password },
      undefined,
      traceId,
    );
  }

  async logout(token: string, traceId?: string): Promise<ApiResponse<void>> {
    return this.coreApiService.post(
      '/auth/logout',
      {},
      token,
      traceId,
    );
  }
}
```

**ファイル**: `bff/customer-bff/src/auth/auth.controller.ts`

```typescript
import { Controller, Post, Body, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from './auth.guard';
import { AuthService } from './auth.service';
import { ApiResponse } from '@app/shared';

@Controller('api/auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  @Post('register')
  async register(
    @Body() body: { username: string; password: string; email: string; fullName: string },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.authService.register(body.username, body.password, body.email, body.fullName, req.traceId);
  }

  @Post('login')
  async login(
    @Body() body: { username: string; password: string },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.authService.login(body.username, body.password, req.traceId);
  }

  @Post('logout')
  @UseGuards(AuthGuard)
  async logout(@Req() req: any): Promise<ApiResponse<void>> {
    return this.authService.logout(req.token, req.traceId);
  }
}
```

#### 6.8 app.module.ts に追加

```typescript
import { OrdersModule } from './orders/orders.module';
import { MembersModule } from './members/members.module';
import { AuthModule } from './auth/auth.module';

@Module({
  imports: [
    // ...
    AuthModule,
    OrdersModule,
    MembersModule,
  ],
})
```

### 検証

```bash
# 注文確定
curl -X POST http://localhost:3001/api/orders \
  -H "Authorization: Bearer {token}"
# → 注文詳細が返却されればOK

# 注文履歴
curl http://localhost:3001/api/orders \
  -H "Authorization: Bearer {token}"
# → ステータスが日本語化されていればOK

# 会員情報取得
curl http://localhost:3001/api/members/me \
  -H "Authorization: Bearer {token}"
# → 電話番号がマスキングされていればOK
```

---

## Task 7: Docker構成

### 目的
Customer BFFをDocker化し、docker-compose.ymlに追加する。

### 実装内容

#### 7.1 Dockerfile

**ファイル**: `bff/customer-bff/Dockerfile`

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app

# shared の依存関係も含めてインストール
COPY package*.json ./
COPY bff/shared/package*.json ./bff/shared/
COPY bff/customer-bff/package*.json ./bff/customer-bff/

RUN npm ci

# ソースコピー＆ビルド
COPY bff/shared ./bff/shared
COPY bff/customer-bff ./bff/customer-bff

WORKDIR /app/bff/shared
RUN npm run build

WORKDIR /app/bff/customer-bff
RUN npm run build

# 本番イメージ
FROM node:20-alpine
WORKDIR /app

COPY --from=builder /app/bff/customer-bff/dist ./dist
COPY --from=builder /app/bff/customer-bff/node_modules ./node_modules
COPY --from=builder /app/bff/shared/dist ./node_modules/@app/shared/dist
COPY --from=builder /app/bff/customer-bff/package*.json ./

EXPOSE 3001

CMD ["node", "dist/main.js"]
```

#### 7.2 .dockerignore

**ファイル**: `bff/customer-bff/.dockerignore`

```
node_modules
dist
npm-debug.log
.env*
.git
```

#### 7.3 docker-compose.yml 更新

**ファイル**: `/docker-compose.yml` に追加

```yaml
services:
  # 既存サービス（postgres, backend, frontend）

  customer-bff:
    build:
      context: .
      dockerfile: bff/customer-bff/Dockerfile
    ports:
      - "3001:3001"
    environment:
      - NODE_ENV=production
      - PORT=3001
      - CORE_API_URL=http://backend:8080
      - CORE_API_TIMEOUT=5000
      - CORE_API_RETRY=2
      - LOG_LEVEL=info
    depends_on:
      - backend
    networks:
      - app-network

networks:
  app-network:
    driver: bridge
```

### 検証

```bash
# ビルド
docker compose build customer-bff

# 起動
docker compose up -d customer-bff

# ログ確認
docker compose logs -f customer-bff

# 疎通確認
curl http://localhost:3001/api/products
```

---

## Task 8: フロントエンド接続切替

### 目的
フロントエンドのAPI呼び出し先をBFFに切り替え、型定義を共有する。

### 実装内容

#### 8.1 frontend/package.json に依存関係追加

**ファイル**: `frontend/package.json`

```json
{
  "dependencies": {
    "@app/shared": "workspace:*",
    // 既存の依存関係
  }
}
```

#### 8.2 frontend/src/types/api.ts を更新

**ファイル**: `frontend/src/types/api.ts`

```typescript
// 共通型定義を再エクスポート
export type {
  ApiResponse,
  ProductDto,
  CartDto,
  CartItemDto,
  OrderDto,
  OrderItemDto,
  UserDto,
} from '@app/shared';

// フロントエンド固有の型定義はそのまま
```

#### 8.3 .env.development 更新

**ファイル**: `frontend/.env.development`

```bash
# BFF URL に変更
VITE_API_URL=http://localhost:3001
```

#### 8.4 frontend/src/lib/api.ts の確認

既存の `api.ts` はそのまま使用可能（`VITE_API_URL` 環境変数を参照しているため）。

### 検証

```bash
# フロントエンド起動
cd frontend
npm run dev

# ブラウザで http://localhost:5173 を開く
# → 商品一覧が表示されればOK
# → DevToolsのNetworkタブで http://localhost:3001/api/* へのリクエストを確認
```

---

## Task 9: E2Eテスト・検証

### 目的
顧客向け主要フローの回帰テストを実施し、BFF経由で問題なく動作することを確認する。

### 検証シナリオ

#### 9.1 商品閲覧フロー

```bash
# 商品一覧取得
curl http://localhost:3001/api/products

# 検証ポイント:
# - 公開商品のみ返却される
# - stockStatus フィールドが含まれる
# - isPublic フィールドは含まれない（管理用フィールド削除）

# 商品詳細取得
curl http://localhost:3001/api/products/1

# 検証ポイント:
# - 在庫状況が「在庫あり」「残りわずか」「在庫なし」に変換される
```

#### 9.2 会員登録・ログインフロー

```bash
# 会員登録
curl -X POST http://localhost:3001/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com",
    "fullName": "テストユーザー"
  }'

# 検証ポイント:
# - success: true で登録成功

# ログイン
curl -X POST http://localhost:3001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'

# 検証ポイント:
# - トークンが返却される
# - ユーザー情報が含まれる
```

#### 9.3 カート追加・注文フロー

```bash
# トークン取得（上記ログインで取得）
TOKEN="..."

# カート追加
curl -X POST http://localhost:3001/api/cart/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId": 1, "quantity": 2}'

# カート取得
curl http://localhost:3001/api/cart \
  -H "Authorization: Bearer $TOKEN"

# 検証ポイント:
# - totalAmount, totalItems が計算されている
# - 各アイテムに subtotal が含まれる

# 注文確定
curl -X POST http://localhost:3001/api/orders \
  -H "Authorization: Bearer $TOKEN"

# 検証ポイント:
# - 注文詳細が返却される
# - ステータスが日本語化されている

# 注文履歴取得
curl http://localhost:3001/api/orders \
  -H "Authorization: Bearer $TOKEN"

# 検証ポイント:
# - 注文一覧が返却される
```

#### 9.4 認証テスト

```bash
# 認証なしでカート取得（エラー期待）
curl http://localhost:3001/api/cart

# 検証ポイント:
# - 401 Unauthorized
# - エラーコード: BFF_UNAUTHORIZED
# - メッセージ: "認証トークンが必要です"

# 無効なトークンでカート取得（エラー期待）
curl http://localhost:3001/api/cart \
  -H "Authorization: Bearer invalid-token"

# 検証ポイント:
# - 401 Unauthorized
# - エラーコード: BFF_INVALID_TOKEN
# - メッセージ: "無効なトークンです"
```

#### 9.5 トレースID伝播確認

```bash
# トレースIDを指定してリクエスト
curl -v http://localhost:3001/api/products \
  -H "X-Trace-Id: test-trace-123"

# 検証ポイント:
# - レスポンスヘッダーに X-Trace-Id: test-trace-123 が含まれる
# - Customer BFFのログにトレースIDが記録される
# - Core APIのログにも同じトレースIDが記録される

# トレースID未指定の場合
curl -v http://localhost:3001/api/products

# 検証ポイント:
# - レスポンスヘッダーに X-Trace-Id が自動生成される（UUID形式）
```

#### 9.6 エラーハンドリング確認

```bash
# Core APIが停止している状態でリクエスト
docker compose stop backend

curl http://localhost:3001/api/products

# 検証ポイント:
# - 503 Service Unavailable
# - エラーコード: BFF_CORE_API_UNAVAILABLE
# - メッセージ: "サービスが一時的に利用できません"

# Core API再起動
docker compose start backend
```

### Playwright E2Eテスト（オプション）

既存のE2Eテストが動作することを確認：

```bash
cd frontend
npm run test:e2e

# 検証ポイント:
# - 全E2Eテストがパスする
# - BFF経由でのAPI呼び出しが成功する
```

---

## Phase 1 完了条件

以下をすべて満たすこと：

- [ ] Customer BFF が起動し、`http://localhost:3001` でアクセス可能
- [ ] 商品一覧・詳細が BFF 経由で取得できる
- [ ] 会員登録・ログインが BFF 経由で動作する
- [ ] カート追加・取得・更新・削除が BFF 経由で動作する
- [ ] 注文確定・履歴取得が BFF 経由で動作する
- [ ] トレースIDがブラウザ→BFF→Core API で伝播する
- [ ] 認証なしアクセス時に401エラーが返る
- [ ] 無効なトークンで401エラーが返る
- [ ] Core API停止時に503エラーが返る
- [ ] フロントエンドが BFF 経由で正常動作する
- [ ] 既存E2Eテストがパスする

---

## Phase 2・Phase 3について

Phase 1完了後、以下を実施：

- **Phase 2: BackOffice BFF導入**（別タスクで定義）
  - 管理画面向けAPI実装
  - BoUser認証
  - 在庫・注文・会員・BoUser管理API

- **Phase 3: Core API内部化**（別タスクで定義）
  - docker-compose.yml でbackendポート非公開化
  - ネットワーク分離（internal network）
  - 直接アクセス遮断の確認

---

## トラブルシューティング

### npm workspace エラー

```bash
# ルートで再インストール
rm -rf node_modules package-lock.json
rm -rf bff/shared/node_modules bff/customer-bff/node_modules
npm install
```

### Core API接続エラー

```bash
# Core APIが起動しているか確認
docker compose ps

# Core APIログ確認
docker compose logs -f backend

# ネットワーク確認
docker compose exec customer-bff ping backend
```

### トークン検証エラー

```bash
# Core APIの認証エンドポイント確認
curl http://localhost:8080/auth/verify-token \
  -H "Content-Type: application/json" \
  -d '{"token":"..."}'

# BFFのログ確認
docker compose logs -f customer-bff
```

---

## 参考資料

- [技術設計ドキュメント](../02_designs/CHG-010_BFF構成への移行.md)
- [Nest.js公式ドキュメント](https://docs.nestjs.com/)
- [npm workspaces](https://docs.npmjs.com/cli/v7/using-npm/workspaces)
