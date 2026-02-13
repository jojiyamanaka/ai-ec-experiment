# CHG-010: BFF構成への移行 - 技術設計

## 1. アーキテクチャ概要

### 1.1 システム構成

```
┌─────────────────────────────────────────────────────────────┐
│                        ブラウザ                              │
└──────────────┬──────────────────────┬───────────────────────┘
               │                      │
               │ (顧客画面)           │ (管理画面)
               ↓                      ↓
┌──────────────────────────┐  ┌──────────────────────────┐
│   Customer BFF           │  │   BackOffice BFF         │
│   (Nest.js, Port 3001)   │  │   (Nest.js, Port 3002)   │
│   - 顧客向けAPI          │  │   - 管理向けAPI          │
│   - User認証             │  │   - BoUser認証           │
│   - レスポンス最適化     │  │   - レスポンス最適化     │
└──────────────┬───────────┘  └──────────────┬───────────┘
               │                                │
               │ (内部ネットワーク)             │
               └────────────┬───────────────────┘
                            ↓
               ┌────────────────────────┐
               │   Core API             │
               │   (Spring Boot, 8080)  │
               │   - 業務ロジック       │
               │   - トランザクション   │
               │   - DB操作             │
               └────────────┬───────────┘
                            ↓
               ┌────────────────────────┐
               │   PostgreSQL (5432)    │
               └────────────────────────┘
```

### 1.2 レイヤー責務

| レイヤー | 責務 | 技術スタック |
|---------|------|-------------|
| **Customer BFF** | 顧客画面向けAPI公開、User認証、レスポンス整形 | Nest.js + TypeScript |
| **BackOffice BFF** | 管理画面向けAPI公開、BoUser認証、レスポンス整形 | Nest.js + TypeScript |
| **Core API** | 業務ロジック、トランザクション、DB操作 | Spring Boot + Java 21 |
| **DB** | データ永続化 | PostgreSQL 16 |

---

## 2. BFF実装方針（Nest.js）

### 2.1 Nest.js採用理由

1. **型安全性**: TypeScriptネイティブで、フロントエンドと型定義を共有可能
2. **モジュラー設計**: Customer/BackOfficeを明確に分離できる
3. **Spring Boot類似**: DI、デコレータベースで既存開発者の学習コストが低い
4. **BFF向け機能**: HTTPクライアント統合、インターセプター、ガードが標準装備
5. **OpenAPI対応**: `@nestjs/swagger`でドキュメント自動生成

### 2.2 プロジェクト構成（npm workspace）

npm workspaceを使用して、フロントエンド・BFF・共通型定義を一元管理：

```
ai-ec-experiment/
├── package.json            # workspace定義（ルート）
├── frontend/
│   └── package.json        # workspace: frontend
├── backend/                # Spring Boot（workspace対象外）
├── bff/
│   ├── shared/             # 共通型定義
│   │   ├── package.json    # workspace: @app/shared
│   │   ├── tsconfig.json
│   │   └── src/
│   │       ├── index.ts    # 再エクスポート
│   │       ├── dto/
│   │       │   ├── product.dto.ts
│   │       │   ├── cart.dto.ts
│   │       │   ├── order.dto.ts
│   │       │   └── user.dto.ts
│   │       └── types/
│   │           └── api-response.ts
│   ├── customer-bff/       # 顧客向けBFF
│   │   ├── package.json    # dependencies: @app/shared
│   │   ├── tsconfig.json
│   │   ├── Dockerfile
│   │   └── src/
│   │       ├── app.module.ts
│   │       ├── main.ts
│   │       ├── config/
│   │       │   └── configuration.ts  # 環境変数設定
│   │       ├── auth/       # User認証モジュール
│   │       │   ├── auth.guard.ts
│   │       │   └── auth.service.ts
│   │       ├── products/   # 商品APIモジュール
│   │       │   ├── products.controller.ts
│   │       │   └── products.service.ts
│   │       ├── cart/       # カートAPIモジュール
│   │       ├── orders/     # 注文APIモジュール
│   │       ├── members/    # 会員APIモジュール
│   │       ├── core-api/   # Core API連携モジュール
│   │       │   ├── core-api.service.ts
│   │       │   └── core-api.module.ts
│   │       └── common/     # 共通機能（フィルター、インターセプター）
│   │           ├── filters/
│   │           │   └── http-exception.filter.ts
│   │           └── interceptors/
│   │               ├── trace.interceptor.ts
│   │               └── logging.interceptor.ts
│   └── backoffice-bff/     # 管理向けBFF
│       ├── package.json    # dependencies: @app/shared
│       ├── tsconfig.json
│       ├── Dockerfile
│       └── src/
│           ├── app.module.ts
│           ├── main.ts
│           ├── config/
│           │   └── configuration.ts
│           ├── auth/       # BoUser認証モジュール
│           ├── inventory/  # 在庫管理APIモジュール
│           ├── orders/     # 注文管理APIモジュール
│           ├── members/    # 会員管理APIモジュール
│           ├── bo-users/   # BoUser管理APIモジュール
│           ├── core-api/
│           └── common/
```

#### ルート package.json

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

#### bff/shared/package.json

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

#### bff/customer-bff/package.json（抜粋）

```json
{
  "name": "customer-bff",
  "dependencies": {
    "@app/shared": "workspace:*",
    "@nestjs/common": "^10.0.0",
    "@nestjs/core": "^10.0.0",
    "@nestjs/config": "^3.0.0",
    "@nestjs/axios": "^3.0.0"
  }
}
```

### 2.3 共通型定義の共有

```typescript
// bff/shared/src/types/api-response.ts
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
  };
}

// bff/shared/src/dto/product.dto.ts
export interface ProductDto {
  id: number;
  name: string;
  price: number;
  stock: number;
  imageUrl: string;
  description: string;
  isPublic: boolean;
}

// bff/shared/src/index.ts（再エクスポート）
export * from './types/api-response';
export * from './dto/product.dto';
export * from './dto/cart.dto';
export * from './dto/order.dto';
export * from './dto/user.dto';

// フロントエンドで使用
// frontend/src/types/api.ts
export type { ApiResponse, ProductDto } from '@app/shared';

// Customer BFFで使用
// bff/customer-bff/src/products/products.controller.ts
import { ApiResponse, ProductDto } from '@app/shared';
```

### 2.4 設定管理（環境変数 + デフォルト設定）

`@nestjs/config` を使用して、環境変数とデフォルト値を管理：

```typescript
// bff/customer-bff/src/config/configuration.ts
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

// bff/customer-bff/src/app.module.ts
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import configuration from './config/configuration';

@Module({
  imports: [
    ConfigModule.forRoot({
      load: [configuration],
      isGlobal: true,  // 全モジュールで利用可能
    }),
    // 他のモジュール
  ],
})
export class AppModule {}

// 使用例: bff/customer-bff/src/core-api/core-api.service.ts
import { ConfigService } from '@nestjs/config';

@Injectable()
export class CoreApiService {
  private readonly baseUrl: string;
  private readonly timeout: number;

  constructor(private configService: ConfigService) {
    this.baseUrl = this.configService.get<string>('coreApi.url');
    this.timeout = this.configService.get<number>('coreApi.timeout');
  }
}
```

#### 環境変数ファイル

```bash
# bff/customer-bff/.env.development
NODE_ENV=development
PORT=3001
CORE_API_URL=http://localhost:8080
CORE_API_TIMEOUT=5000
CORE_API_RETRY=2
LOG_LEVEL=debug

# bff/customer-bff/.env.production
NODE_ENV=production
PORT=3001
CORE_API_URL=http://backend:8080
CORE_API_TIMEOUT=5000
CORE_API_RETRY=2
LOG_LEVEL=info
```

---

## 3. 認証・認可設計

### 3.1 トークン伝播フロー

#### Customer BFF（User認証）

```
1. ブラウザ → Customer BFF: Authorization: Bearer {userToken}
2. AuthGuard: トークン検証（Core APIの /auth/verify-token 呼び出し）
3. 検証OK → リクエストにユーザー情報を付加
4. Controller → Service → Core API呼び出し（トークンを伝播）
5. Core API応答 → レスポンス整形 → ブラウザへ返却
```

#### BackOffice BFF（BoUser認証）

```
1. ブラウザ → BackOffice BFF: Authorization: Bearer {boUserToken}
2. BoAuthGuard: トークン検証（Core APIの /bo-auth/verify-token 呼び出し）
3. 検証OK → リクエストにBoユーザー情報を付加
4. Controller → Service → Core API呼び出し（トークンを伝播）
5. Core API応答 → レスポンス整形 → ブラウザへ返却
```

### 3.2 AuthGuard実装例

```typescript
// customer-bff/src/auth/auth.guard.ts
@Injectable()
export class AuthGuard implements CanActivate {
  constructor(private coreApiService: CoreApiService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest();
    const token = this.extractToken(request);

    if (!token) {
      throw new UnauthorizedException('認証トークンが必要です');
    }

    try {
      // Core APIでトークン検証
      const user = await this.coreApiService.verifyUserToken(token);
      request.user = user; // リクエストにユーザー情報を付加
      return true;
    } catch (error) {
      throw new UnauthorizedException('無効なトークンです');
    }
  }

  private extractToken(request: Request): string | null {
    const authHeader = request.headers['authorization'];
    if (!authHeader?.startsWith('Bearer ')) return null;
    return authHeader.substring(7);
  }
}
```

### 3.3 誤アクセス防止

- **顧客トークンで管理BFFへアクセス**: BackOffice BFFの `BoAuthGuard` がトークン検証時に401を返す
- **管理トークンで顧客BFFへアクセス**: Customer BFFの `AuthGuard` がトークン検証時に401を返す
- **認証なしアクセス**: 両BFFとも、公開エンドポイント（商品一覧など）以外は401を返す

---

## 4. Core API連携設計

### 4.1 HTTPクライアント設定

```typescript
// customer-bff/src/core-api/core-api.service.ts
import { HttpService } from '@nestjs/axios';
import { Injectable, HttpException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { firstValueFrom, timeout, retry } from 'rxjs';

@Injectable()
export class CoreApiService {
  private readonly baseUrl: string;
  private readonly timeoutMs: number;
  private readonly retryCount: number;

  constructor(
    private httpService: HttpService,
    private configService: ConfigService,
  ) {
    this.baseUrl = this.configService.get('CORE_API_URL', 'http://localhost:8080');
    this.timeoutMs = this.configService.get('CORE_API_TIMEOUT', 5000);
    this.retryCount = this.configService.get('CORE_API_RETRY', 2);
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
            delay: 1000,  // 1秒待機してリトライ
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
          // POSTは冪等でないためリトライしない
          // 冪等性が保証されている特定のエンドポイントのみ個別にリトライ設定
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
          // PUTも冪等でない場合があるためリトライしない
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
          // DELETEは通常冪等だがリトライは慎重に
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
    // ログ出力（トレースID含む）
    this.logger.error(`Core API error: ${method} ${path}`, error);

    if (error.response) {
      // Core APIがエラーレスポンスを返した場合
      const statusCode = error.response.status;
      const responseData = error.response.data;

      // Core APIの ApiResponse<T> 形式をそのまま伝播
      if (responseData?.success === false && responseData?.error) {
        throw new HttpException(responseData, statusCode);
      }

      // 想定外のレスポンス形式の場合はBFFでラップ
      throw new HttpException({
        success: false,
        error: {
          code: 'BFF_CORE_API_ERROR',
          message: responseData?.message || 'Core APIエラー',
        },
      }, statusCode);
    } else if (error.code === 'ECONNREFUSED') {
      // 接続拒否（Core APIが起動していない）
      throw new HttpException({
        success: false,
        error: {
          code: 'BFF_CORE_API_UNAVAILABLE',
          message: 'サービスが一時的に利用できません',
        },
      }, 503);
    } else if (error.name === 'TimeoutError') {
      // タイムアウト
      throw new HttpException({
        success: false,
        error: {
          code: 'BFF_CORE_API_TIMEOUT',
          message: 'リクエストがタイムアウトしました',
        },
      }, 504);
    } else {
      // 予期しないエラー
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

### 4.2 タイムアウト・リトライ設定

| 設定項目 | 環境変数 | デフォルト値 | 説明 |
|---------|---------|------------|------|
| Core API URL | `CORE_API_URL` | `http://localhost:8080` | Core APIのベースURL |
| タイムアウト | `CORE_API_TIMEOUT` | `5000` (5秒) | Core API呼び出しのタイムアウト（ms） |
| リトライ回数 | `CORE_API_RETRY` | `2` | 失敗時のリトライ回数（GETのみ） |

#### リトライポリシー

| HTTPメソッド | リトライ | 理由 |
|------------|---------|------|
| **GET** | ○（2回、1秒間隔） | 冪等性が保証されているため安全 |
| **POST** | × | 冪等でない可能性（注文確定の重複など）を避けるため |
| **PUT** | × | 冪等でない場合があるため |
| **DELETE** | × | 慎重に扱うため（個別に検討） |

**例外**: 特定のエンドポイントで冪等性が保証されている場合（例: トークン検証）は、個別にリトライを有効化できる。

```typescript
// 冪等性が保証されているPOSTの例（トークン検証）
async verifyUserToken(token: string): Promise<UserDto> {
  const response = await firstValueFrom(
    this.httpService.post(`${this.baseUrl}/auth/verify-token`, { token }).pipe(
      timeout(this.timeoutMs),
      retry({ count: 2, delay: 1000 }),  // トークン検証は冪等なのでリトライOK
    ),
  );
  return response.data;
}
```

### 4.3 サーキットブレーカー（将来対応）

初期実装では未導入。将来的に `opossum` ライブラリで実装を検討：

```typescript
// 将来実装例
import CircuitBreaker from 'opossum';

const breaker = new CircuitBreaker(asyncFunction, {
  timeout: 5000,
  errorThresholdPercentage: 50,
  resetTimeout: 30000,
});
```

---

## 5. API設計

### 5.1 設計方針

#### Core API（粗粒度・ドメイン指向）
- **責務**: ドメインロジック、トランザクション管理、DB操作
- **粒度**: ビジネスルール単位の粗粒度API
- **レスポンス**: 正規化されたエンティティ構造
- **変更頻度**: 低（業務ルール変更時のみ）

#### BFF API（細粒度・画面最適化）
- **責務**: 画面要件に最適化、複数Core API集約、レスポンス整形
- **粒度**: 画面・コンポーネント単位の細粒度API
- **レスポンス**: UI要件に合わせた非正規化構造
- **変更頻度**: 高（画面要件変更時）

---

### 5.2 Core API エンドポイント一覧（内部API）

| エンドポイント | メソッド | 説明 | レスポンス構造 |
|--------------|---------|------|--------------|
| `/products` | GET | 商品一覧取得（全件） | `ApiResponse<ProductDto[]>` |
| `/products/{id}` | GET | 商品詳細取得 | `ApiResponse<ProductDto>` |
| `/cart` | GET | カート取得 | `ApiResponse<CartDto>` |
| `/cart/items` | POST | カート追加 | `ApiResponse<CartItemDto>` |
| `/cart/items/{id}` | PUT | カート更新 | `ApiResponse<CartItemDto>` |
| `/cart/items/{id}` | DELETE | カート削除 | `ApiResponse<void>` |
| `/orders` | POST | 注文確定 | `ApiResponse<OrderDto>` |
| `/orders` | GET | 注文一覧取得 | `ApiResponse<OrderDto[]>` |
| `/orders/{id}` | GET | 注文詳細取得 | `ApiResponse<OrderDto>` |
| `/auth/register` | POST | 会員登録 | `ApiResponse<UserDto>` |
| `/auth/login` | POST | ログイン | `ApiResponse<{ token: string, user: UserDto }>` |
| `/auth/logout` | POST | ログアウト | `ApiResponse<void>` |
| `/auth/verify-token` | POST | User トークン検証 | `ApiResponse<UserDto>` |
| `/members/{id}` | GET | 会員情報取得 | `ApiResponse<UserDto>` |
| `/admin/inventory` | GET | 在庫一覧取得 | `ApiResponse<InventoryDto[]>` |
| `/admin/inventory/{id}` | PUT | 在庫更新 | `ApiResponse<InventoryDto>` |
| `/admin/orders` | GET | 管理：注文一覧 | `ApiResponse<OrderDto[]>` |
| `/admin/orders/{id}` | GET | 管理：注文詳細 | `ApiResponse<OrderDto>` |
| `/admin/orders/{id}` | PUT | 管理：注文更新 | `ApiResponse<OrderDto>` |
| `/admin/members` | GET | 管理：会員一覧 | `ApiResponse<UserDto[]>` |
| `/admin/members/{id}` | GET | 管理：会員詳細 | `ApiResponse<UserDto>` |
| `/admin/bo-users` | GET | BoUser一覧 | `ApiResponse<BoUserDto[]>` |
| `/admin/bo-users` | POST | BoUser作成 | `ApiResponse<BoUserDto>` |
| `/bo-auth/login` | POST | BoUserログイン | `ApiResponse<{ token: string, boUser: BoUserDto }>` |
| `/bo-auth/logout` | POST | BoUserログアウト | `ApiResponse<void>` |
| `/bo-auth/verify-token` | POST | BoUser トークン検証 | `ApiResponse<BoUserDto>` |

**特徴**:
- エンティティをそのまま返す（正規化された構造）
- 画面要件とは独立
- 1エンドポイント = 1ドメイン操作

---

### 5.3 Customer BFF エンドポイント詳細

| エンドポイント | メソッド | 説明 | 認証 | Core API呼び出し | BFFの整形内容 |
|--------------|---------|------|------|-----------------|--------------|
| `/api/products` | GET | 商品一覧取得（公開中のみ） | 不要 | `GET /products` | `isPublic=true` フィルタ、管理用フィールド削除 |
| `/api/products/:id` | GET | 商品詳細取得 | 不要 | `GET /products/{id}` | 在庫状況を「在庫あり/残りわずか/在庫なし」に変換 |
| `/api/cart` | GET | カート取得 | User | `GET /cart` | 小計・合計計算、商品情報を埋め込み |
| `/api/cart/items` | POST | カート追加 | User | `POST /cart/items` | エラーメッセージをUI用に変換 |
| `/api/cart/items/:id` | PUT | カート更新 | User | `PUT /cart/items/{id}` | 同上 |
| `/api/cart/items/:id` | DELETE | カート削除 | User | `DELETE /cart/items/{id}` | 同上 |
| `/api/orders` | POST | 注文確定 | User | `POST /orders` + `GET /orders/{id}` | 注文後に詳細を自動取得して返す |
| `/api/orders` | GET | 注文履歴取得 | User | `GET /orders` | ステータスを日本語化、商品情報を埋め込み |
| `/api/orders/:id` | GET | 注文詳細取得 | User | `GET /orders/{id}` | 商品情報・配送状況を埋め込み |
| `/api/auth/register` | POST | 会員登録 | 不要 | `POST /auth/register` | パススルー |
| `/api/auth/login` | POST | ログイン | 不要 | `POST /auth/login` | パススルー |
| `/api/auth/logout` | POST | ログアウト | User | `POST /auth/logout` | パススルー |
| `/api/members/me` | GET | 会員情報取得 | User | `GET /members/{id}` | 機微情報のマスキング（例: 電話番号下4桁） |

#### レスポンス例（画面最適化）

##### `/api/products` のレスポンス（BFF）
```typescript
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "商品A",
      "price": 1000,
      "imageUrl": "https://...",
      "stockStatus": "在庫あり",  // BFFで追加（Core APIのstock数値から変換）
      "description": "商品説明"
      // isPublic, stock などの管理用フィールドは削除
    }
  ]
}
```

##### `/api/cart` のレスポンス（BFF）
```typescript
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "productId": 1,
        "quantity": 2,
        "product": {  // Core APIの商品情報を埋め込み
          "name": "商品A",
          "price": 1000,
          "imageUrl": "https://..."
        },
        "subtotal": 2000  // BFFで計算
      }
    ],
    "totalAmount": 2000,  // BFFで計算
    "totalItems": 2       // BFFで計算
  }
}
```

---

### 5.4 BackOffice BFF エンドポイント詳細

| エンドポイント | メソッド | 説明 | 認証 | Core API呼び出し | BFFの整形内容 |
|--------------|---------|------|------|-----------------|--------------|
| `/api/bo-auth/login` | POST | BoUserログイン | 不要 | `POST /bo-auth/login` | パススルー |
| `/api/bo-auth/logout` | POST | BoUserログアウト | BoUser | `POST /bo-auth/logout` | パススルー |
| `/api/inventory` | GET | 在庫一覧取得 | BoUser | `GET /admin/inventory` | ページング、ソート、検索（将来）|
| `/api/inventory/:id` | PUT | 在庫更新 | BoUser | `PUT /admin/inventory/{id}` | パススルー |
| `/api/admin/orders` | GET | 注文一覧取得 | BoUser | `GET /admin/orders` | ステータスフィルタ、会員名埋め込み |
| `/api/admin/orders/:id` | GET | 注文詳細取得 | BoUser | `GET /admin/orders/{id}` + `GET /admin/members/{userId}` | 注文+会員情報を1レスポンスに集約 |
| `/api/admin/orders/:id` | PUT | 注文ステータス更新 | BoUser | `PUT /admin/orders/{id}` | パススルー |
| `/api/admin/members` | GET | 会員一覧取得 | BoUser | `GET /admin/members` | ページング、最終注文日の埋め込み |
| `/api/admin/members/:id` | GET | 会員詳細取得 | BoUser | `GET /admin/members/{id}` + `GET /orders?userId={id}` | 会員+注文履歴を1レスポンスに集約 |
| `/api/admin/bo-users` | GET | BoUser一覧取得 | BoUser | `GET /admin/bo-users` | ページング |
| `/api/admin/bo-users` | POST | BoUser作成 | BoUser | `POST /admin/bo-users` | パススルー |

#### レスポンス例（画面最適化）

##### `/api/admin/orders` のレスポンス（BFF）
```typescript
{
  "success": true,
  "data": {
    "orders": [
      {
        "id": 1,
        "orderNumber": "ORD-20250115-0001",
        "status": "発送済み",  // BFFで日本語化
        "totalAmount": 5000,
        "createdAt": "2025-01-15T10:00:00Z",
        "memberName": "山田太郎",  // Core APIの会員情報を埋め込み
        "itemCount": 3
      }
    ],
    "pagination": {  // BFFで追加（将来）
      "page": 1,
      "pageSize": 20,
      "totalCount": 100
    }
  }
}
```

##### `/api/admin/members/:id` のレスポンス（BFF）
```typescript
{
  "success": true,
  "data": {
    "member": {
      "id": 1,
      "username": "yamada",
      "email": "yamada@example.com",
      "fullName": "山田太郎",
      "phoneNumber": "090-1234-5678",
      "createdAt": "2025-01-01T00:00:00Z"
    },
    "orderHistory": [  // Core APIの注文履歴を埋め込み
      {
        "id": 1,
        "orderNumber": "ORD-20250115-0001",
        "status": "発送済み",
        "totalAmount": 5000,
        "createdAt": "2025-01-15T10:00:00Z"
      }
    ],
    "stats": {  // BFFで集計
      "totalOrders": 5,
      "totalSpent": 25000,
      "lastOrderDate": "2025-01-15T10:00:00Z"
    }
  }
}
```

---

### 5.5 レスポンス形式

BFFの外部APIは既存の `ApiResponse<T>` 形式を維持：

```typescript
// 成功レスポンス
{
  "success": true,
  "data": {
    // 実際のデータ
  }
}

// エラーレスポンス
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "エラーメッセージ"
  }
}
```

### 5.6 エラーマッピング戦略

#### 基本方針

1. **Core APIエラーの伝播**: Core APIが `ApiResponse<T>` 形式で返すエラーはそのまま伝播
2. **BFF固有エラー**: ネットワーク・タイムアウト・認証など、BFF層で発生するエラーはBFFで変換

#### エラーコード一覧

##### BFF固有のエラーコード

| コード | HTTPステータス | 発生タイミング | メッセージ例 |
|-------|--------------|--------------|-------------|
| `BFF_CORE_API_UNAVAILABLE` | 503 | Core APIに接続できない（ECONNREFUSED） | サービスが一時的に利用できません |
| `BFF_CORE_API_TIMEOUT` | 504 | Core APIがタイムアウト | リクエストがタイムアウトしました |
| `BFF_CORE_API_ERROR` | 500 | Core APIが想定外の形式で応答 | Core APIエラー |
| `BFF_INVALID_TOKEN` | 401 | トークン検証失敗（AuthGuard） | 無効なトークンです |
| `BFF_UNAUTHORIZED` | 401 | トークンなし（AuthGuard） | 認証トークンが必要です |
| `BFF_FORBIDDEN` | 403 | 権限不足 | アクセス権限がありません |
| `BFF_INTERNAL_ERROR` | 500 | BFF内部の予期しないエラー | 予期しないエラーが発生しました |

##### Core APIエラー（伝播）

Core APIが返す `ApiResponse<T>` のエラーはそのまま伝播：

| Core APIエラーコード | HTTPステータス | 説明 |
|------------------|--------------|------|
| `PRODUCT_NOT_FOUND` | 404 | 商品が見つからない |
| `INSUFFICIENT_STOCK` | 400 | 在庫不足 |
| `INVALID_QUANTITY` | 400 | 数量が不正 |
| `ORDER_NOT_FOUND` | 404 | 注文が見つからない |
| `DUPLICATE_USERNAME` | 400 | ユーザー名が重複 |
| `INVALID_CREDENTIALS` | 401 | 認証情報が不正 |

#### エラーマッピング実装例

```typescript
// bff/customer-bff/src/common/filters/http-exception.filter.ts
import { ExceptionFilter, Catch, ArgumentsHost, HttpException } from '@nestjs/common';

@Catch(HttpException)
export class HttpExceptionFilter implements ExceptionFilter {
  catch(exception: HttpException, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse();
    const status = exception.getStatus();
    const exceptionResponse = exception.getResponse();

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

#### エラーレスポンス例

##### BFF固有エラー（タイムアウト）
```json
{
  "success": false,
  "error": {
    "code": "BFF_CORE_API_TIMEOUT",
    "message": "リクエストがタイムアウトしました"
  }
}
```

##### Core APIエラー（在庫不足）を伝播
```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "message": "在庫が不足しています"
  }
}
```

##### AuthGuardエラー（認証なし）
```json
{
  "success": false,
  "error": {
    "code": "BFF_UNAUTHORIZED",
    "message": "認証トークンが必要です"
  }
}
```

---

## 6. トレーシング・ロギング

### 6.1 トレースID伝播

```
1. ブラウザ → BFF: X-Trace-Id ヘッダー（なければBFFで生成）
2. BFF → Core API: X-Trace-Id ヘッダーを伝播
3. 全ログにトレースIDを記録
```

```typescript
// customer-bff/src/common/interceptors/trace.interceptor.ts
import { Injectable, NestInterceptor, ExecutionContext, CallHandler } from '@nestjs/common';
import { Observable } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';

@Injectable()
export class TraceInterceptor implements NestInterceptor {
  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    const request = context.switchToHttp().getRequest();

    // トレースIDを取得または生成
    const traceId = request.headers['x-trace-id'] || uuidv4();
    request.traceId = traceId;

    // レスポンスヘッダーにもトレースIDを付加
    const response = context.switchToHttp().getResponse();
    response.setHeader('X-Trace-Id', traceId);

    return next.handle();
  }
}
```

### 6.2 ログ形式

JSON形式でログを出力（`winston` 使用）：

```json
{
  "timestamp": "2025-01-15T12:34:56.789Z",
  "level": "info",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "service": "customer-bff",
  "method": "GET",
  "path": "/api/products",
  "statusCode": 200,
  "duration": 123,
  "userId": "user123",
  "message": "Request completed"
}
```

### 6.3 メトリクス収集

将来的に Prometheus メトリクスを公開（`@willsoto/nestjs-prometheus` 使用）：

- リクエスト数（エンドポイント別、ステータスコード別）
- レスポンスタイム（p50, p95, p99）
- Core API呼び出し数・レイテンシ
- エラー率

---

## 7. デプロイ構成

### 7.1 Docker構成

#### Customer BFF Dockerfile

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:20-alpine
WORKDIR /app
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY package*.json ./
EXPOSE 3001
CMD ["node", "dist/main.js"]
```

#### BackOffice BFF Dockerfile

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:20-alpine
WORKDIR /app
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY package*.json ./
EXPOSE 3002
CMD ["node", "dist/main.js"]
```

### 7.2 docker-compose.yml 更新

```yaml
version: '3.8'

services:
  # 既存サービス
  postgres:
    image: postgres:16
    # ... (既存設定)

  backend:
    build: ./backend
    ports:
      - "8080:8080"  # 開発環境のみ公開、本番は非公開
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ec_db
      - SPRING_DATASOURCE_USERNAME=ec_user
      - SPRING_DATASOURCE_PASSWORD=ec_password
    depends_on:
      - postgres
    networks:
      - internal  # 内部ネットワークのみ

  # 新規サービス
  customer-bff:
    build: ./bff/customer-bff
    ports:
      - "3001:3001"
    environment:
      - NODE_ENV=production
      - CORE_API_URL=http://backend:8080
      - CORE_API_TIMEOUT=5000
      - CORE_API_RETRY=2
    depends_on:
      - backend
    networks:
      - public
      - internal

  backoffice-bff:
    build: ./bff/backoffice-bff
    ports:
      - "3002:3002"
    environment:
      - NODE_ENV=production
      - CORE_API_URL=http://backend:8080
      - CORE_API_TIMEOUT=5000
      - CORE_API_RETRY=2
    depends_on:
      - backend
    networks:
      - public
      - internal

  frontend:
    build: ./frontend
    ports:
      - "5173:80"
    environment:
      - VITE_CUSTOMER_BFF_URL=http://localhost:3001
      - VITE_BACKOFFICE_BFF_URL=http://localhost:3002
    depends_on:
      - customer-bff
      - backoffice-bff
    networks:
      - public

networks:
  public:
    driver: bridge
  internal:
    driver: bridge
    internal: true  # 外部からアクセス不可
```

### 7.3 環境変数一覧

#### Customer BFF / BackOffice BFF 共通

| 環境変数 | 説明 | デフォルト値 | 必須 |
|---------|------|------------|------|
| `NODE_ENV` | 実行環境 | `development` | ○ |
| `PORT` | ポート番号 | `3001` / `3002` | × |
| `CORE_API_URL` | Core API URL | `http://localhost:8080` | ○ |
| `CORE_API_TIMEOUT` | Core APIタイムアウト（ms） | `5000` | × |
| `CORE_API_RETRY` | Core APIリトライ回数 | `2` | × |
| `LOG_LEVEL` | ログレベル | `info` | × |

#### フロントエンド

| 環境変数 | 説明 | デフォルト値 | 必須 |
|---------|------|------------|------|
| `VITE_CUSTOMER_BFF_URL` | Customer BFF URL | `http://localhost:3001` | ○ |
| `VITE_BACKOFFICE_BFF_URL` | BackOffice BFF URL | `http://localhost:3002` | ○ |

---

## 8. 段階的移行計画

### Phase 1: Customer BFF導入（2週間）

**Week 1: 基盤実装**
1. Nest.jsプロジェクト作成
2. Core API連携サービス実装
3. User認証ガード実装
4. トレースインターセプター実装
5. Docker構成追加

**Week 2: API実装とテスト**
1. 商品API実装（GET /api/products, /api/products/:id）
2. カートAPI実装（GET/POST/PUT/DELETE）
3. 注文API実装（POST/GET）
4. 会員API実装（GET /api/members/me）
5. E2Eテスト実施（顧客画面回帰）

**受け入れ条件**
- [ ] 顧客画面の全機能がCustomer BFF経由で動作
- [ ] 顧客トークンで管理BFFにアクセスすると401
- [ ] トレースIDが伝播している

### Phase 2: BackOffice BFF導入（2週間）

**Week 3: 基盤実装**
1. BackOffice BFF プロジェクト作成
2. BoUser認証ガード実装
3. Docker構成追加

**Week 4: API実装とテスト**
1. 在庫管理API実装
2. 注文管理API実装
3. 会員管理API実装
4. BoUser管理API実装
5. E2Eテスト実施（管理画面回帰）

**受け入れ条件**
- [ ] 管理画面の全機能がBackOffice BFF経由で動作
- [ ] 管理トークンで顧客BFFにアクセスすると401
- [ ] 監査ログが正常に記録

### Phase 3: Core API内部化（1週間）

**Week 5: 公開経路廃止**
1. docker-compose.yml でbackendのポート公開を削除
2. ネットワーク設定（internal network）を適用
3. ブラウザからCore API直アクセスが遮断されることを確認
4. 障害系テスト実施（タイムアウト、エラーハンドリング）
5. 性能テスト実施

**受け入れ条件**
- [ ] ブラウザからCore APIへ直接アクセス不可
- [ ] BFFからCore APIへは通信可能
- [ ] 主要画面の性能が移行前同等以上

---

## 9. テスト方針

### 9.1 ユニットテスト

- **対象**: Service層、Guard、Interceptor
- **ツール**: Jest
- **カバレッジ目標**: 80%以上

```typescript
// 例: AuthGuard のテスト
describe('AuthGuard', () => {
  let guard: AuthGuard;
  let coreApiService: CoreApiService;

  beforeEach(() => {
    const module = Test.createTestingModule({
      providers: [
        AuthGuard,
        { provide: CoreApiService, useValue: mockCoreApiService },
      ],
    }).compile();
    guard = module.get<AuthGuard>(AuthGuard);
    coreApiService = module.get<CoreApiService>(CoreApiService);
  });

  it('有効なトークンでアクセス許可', async () => {
    jest.spyOn(coreApiService, 'verifyUserToken').mockResolvedValue({ id: 1, username: 'test' });
    const context = mockExecutionContext({ headers: { authorization: 'Bearer valid-token' } });
    expect(await guard.canActivate(context)).toBe(true);
  });

  it('無効なトークンでUnauthorizedException', async () => {
    jest.spyOn(coreApiService, 'verifyUserToken').mockRejectedValue(new Error('Invalid'));
    const context = mockExecutionContext({ headers: { authorization: 'Bearer invalid-token' } });
    await expect(guard.canActivate(context)).rejects.toThrow(UnauthorizedException);
  });
});
```

### 9.2 統合テスト

- **対象**: Controller → Service → Core API Mock
- **ツール**: Jest + supertest
- **検証内容**: エラーハンドリング、タイムアウト、レスポンス整形

### 9.3 E2Eテスト

- **対象**: ブラウザ → BFF → Core API → DB
- **ツール**: Playwright（既存と同じ）
- **シナリオ**:
  - 顧客: 商品閲覧 → カート追加 → 注文確定 → 注文履歴確認
  - 管理: BoUserログイン → 在庫更新 → 注文ステータス更新 → 会員詳細確認

### 9.4 障害系テスト

- Core APIタイムアウト擬似（`nock` でディレイ）
- Core API 500エラー擬似
- 認証失敗（無効トークン）

### 9.5 性能テスト

- **ツール**: Apache JMeter / k6
- **シナリオ**: 同時100ユーザーで主要フロー実行
- **検証項目**:
  - p95レスポンスタイムが移行前+20%以内
  - エラー率が1%未満
  - 在庫整合性が維持される

---

## 10. ロールバック手順

### 緊急時の切戻し

**Phase 1/2 でのロールバック**（BFF障害時）:
1. フロントエンドの環境変数を旧Core API URLに戻す
   ```bash
   # .env.production
   VITE_API_URL=http://localhost:8080  # BFF URLから戻す
   ```
2. フロントエンド再ビルド・デプロイ
3. BFFコンテナを停止

**Phase 3 でのロールバック**（Core API内部化後）:
1. docker-compose.yml でbackendのポート公開を復活
   ```yaml
   backend:
     ports:
       - "8080:8080"  # 再公開
   ```
2. フロントエンドの環境変数を旧Core API URLに戻す
3. backend再起動

---

## 11. 将来対応

### 11.1 キャッシュ層

- **対象**: 商品一覧、商品詳細（読み取り中心API）
- **ツール**: Redis + `cache-manager`
- **TTL**: 商品一覧 60秒、商品詳細 300秒

### 11.2 API Gateway導入

- **ツール**: Kong / AWS API Gateway / Traefik
- **機能**: 認証、レート制限、ルーティングをBFF外に集約

### 11.3 GraphQL層

- **ツール**: `@nestjs/graphql`
- **用途**: フロントエンドが必要なフィールドのみクエリできるように最適化

### 11.4 OpenAPI自動生成

```typescript
// main.ts
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';

const config = new DocumentBuilder()
  .setTitle('Customer BFF API')
  .setVersion('1.0')
  .addBearerAuth()
  .build();
const document = SwaggerModule.createDocument(app, config);
SwaggerModule.setup('api-docs', app, document);
```

---

## 12. 備考

### 既存仕様との整合性

- **API契約**: `ApiResponse<T>` 形式を維持
- **エラーコード**: 既存のCore APIエラーコードを継承（BFF固有コードは `BFF_` プレフィックス）
- **認証方式**: CHG-008のUser/BoUser分離を継承
- **データベース**: CHG-009のPostgreSQLを継続利用

### 非機能要件

- **セキュリティ**: Core APIは内部ネットワークのみ、BFFがインターネット公開
- **性能**: 主要画面のp95レスポンスタイム +20%以内
- **可用性**: BFF障害時もCore API直アクセスでロールバック可能（Phase 3前）
- **保守性**: モジュール単位で責務分離、画面要件変更を局所修正で対応

### 重要な設計判断

#### 1. リトライポリシー

**判断**: POSTは原則リトライしない

**理由**:
- 注文確定などの非冪等操作でリトライすると、重複注文が発生する
- GETは冪等なのでリトライ可能
- トークン検証など冪等性が明確な一部のPOSTのみ、個別にリトライを有効化

**例外**:
- `/auth/verify-token`: トークン検証は冪等なのでリトライ可
- 将来的にIdempotency Keyを導入した場合、対象エンドポイントはリトライ可

#### 2. エラーマッピング

**判断**: Core APIエラーは原則そのまま伝播、BFF固有エラーのみ変換

**理由**:
- Core APIが返すビジネスエラー（在庫不足など）はフロントエンドが理解すべき
- ネットワークエラー、タイムアウトなどインフラエラーのみBFFで変換
- エラーコードの二重管理を避ける

**マッピング戦略**:
| エラー種別 | 処理 |
|-----------|------|
| Core API `ApiResponse<T>` エラー | そのまま伝播 |
| 接続エラー（ECONNREFUSED） | `BFF_CORE_API_UNAVAILABLE` (503) |
| タイムアウト | `BFF_CORE_API_TIMEOUT` (504) |
| 認証エラー（AuthGuard） | `BFF_UNAUTHORIZED` / `BFF_INVALID_TOKEN` (401) |
| 予期しないエラー | `BFF_INTERNAL_ERROR` (500) |

### 参考ドキュメント

- [Nest.js公式ドキュメント](https://docs.nestjs.com/)
- [CHG-008: ドメイン分離とBoUser管理](../archive/01_requirements/CHG-008_ドメイン分離とBoUser管理.md)
- [CHG-009: PostgreSQL移行](./CHG-009_PostgreSQL移行.md)
