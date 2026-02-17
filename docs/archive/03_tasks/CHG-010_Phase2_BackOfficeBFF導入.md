# CHG-010 Phase 2: BackOffice BFF導入 - 実装タスク

## 前提条件

- Phase 1（Customer BFF導入）が完了済み
- npm workspace が設定済み
- bff/shared に共通型定義が実装済み

---

## Phase 2: BackOffice BFF導入

### 検証コマンド

```bash
# BackOffice BFF起動確認
cd bff/backoffice-bff
npm run start:dev

# BackOffice BFF API 接続確認
curl http://localhost:3002/api/inventory \
  -H "Authorization: Bearer {boUserToken}"

# E2Eテスト
cd frontend
npm run test:e2e:admin
```

---

### アーキテクチャ（Phase 2）

```text
ブラウザ
  ├─> Customer Front（localhost:5173, 静的配信）
  │      └─(fetch)─> Customer BFF（localhost:3001）
  └─> Admin Front（localhost:5174, 静的配信）
         └─(fetch)─> BackOffice BFF（localhost:3002）
                           └─> Core API（localhost:8080）
```

---

## Task 1: BackOffice BFF プロジェクト作成

### 目的
BackOffice BFF（管理向けBFF）のプロジェクト構造を作成する。

### 実装内容

#### 1.1 backoffice-bff プロジェクト初期化

**ディレクトリ作成**:
```bash
mkdir -p bff/backoffice-bff
cd bff/backoffice-bff
```

**Nest.js CLI でプロジェクト初期化**:
```bash
nest new . --skip-git --package-manager npm
```

**ファイル**: `bff/backoffice-bff/package.json` に依存関係を追加

```json
{
  "name": "backoffice-bff",
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
  },
  "scripts": {
    "start:dev": "nest start --watch",
    "start:prod": "node dist/main.js",
    "build": "nest build",
    "test": "jest"
  }
}
```

#### 1.2 ルート package.json に追加

**ファイル**: `/package.json` の workspaces に追加

```json
{
  "workspaces": [
    "frontend",
    "bff/shared",
    "bff/customer-bff",
    "bff/backoffice-bff"
  ]
}
```

#### 1.3 設定ファイル

**ファイル**: `bff/backoffice-bff/src/config/configuration.ts`

```typescript
export default () => ({
  server: {
    port: parseInt(process.env.PORT, 10) || 3002,
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

**ファイル**: `bff/backoffice-bff/.env.development`

```bash
NODE_ENV=development
PORT=3002
CORE_API_URL=http://localhost:8080
CORE_API_TIMEOUT=5000
CORE_API_RETRY=2
LOG_LEVEL=debug
```

#### 1.4 Core API連携モジュール（Customer BFFと同じ）

**ディレクトリ作成**:
```bash
mkdir -p src/core-api src/common/{interceptors,filters}
```

**ファイル**: `bff/backoffice-bff/src/core-api/core-api.module.ts`
（Customer BFFと同一内容）

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

**ファイル**: `bff/backoffice-bff/src/core-api/core-api.service.ts`
（Customer BFFの `customer-bff/src/core-api/core-api.service.ts` と同一内容をコピー）

#### 1.5 共通インターセプター・フィルター

以下のファイルをCustomer BFFからコピー：

- `src/common/interceptors/trace.interceptor.ts`
- `src/common/interceptors/logging.interceptor.ts`
- `src/common/filters/http-exception.filter.ts`

#### 1.6 main.ts

**ファイル**: `bff/backoffice-bff/src/main.ts`

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

  app.useGlobalInterceptors(new TraceInterceptor());
  app.useGlobalInterceptors(new LoggingInterceptor());
  app.useGlobalFilters(new HttpExceptionFilter());

  app.enableCors({
    origin: 'http://localhost:5174',
    credentials: true,
  });

  const port = configService.get<number>('server.port');
  await app.listen(port);
  console.log(`BackOffice BFF is running on: http://localhost:${port}`);
}
bootstrap();
```

#### 1.7 app.module.ts

**ファイル**: `bff/backoffice-bff/src/app.module.ts`

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

#### 1.8 依存関係のインストール

```bash
cd /home/joji/work/ai-ec-experiment
npm install
```

### 検証

```bash
# ビルド確認
cd bff/backoffice-bff
npm run build

# 起動確認
npm run start:dev
# → http://localhost:3002 でNest.jsデフォルトページが表示されればOK
```

---

## Task 2: BoUser認証ガード実装

### 目的
BackOffice専用のBoUser認証ガードを実装する。

### 実装内容

#### 2.1 bff/shared に BoUserDto 追加

**ファイル**: `bff/shared/src/dto/bo-user.dto.ts`

```typescript
export interface BoUserDto {
  id: number;
  username: string;
  email: string;
  createdAt: string;
}
```

**ファイル**: `bff/shared/src/index.ts` に追加

```typescript
export * from './dto/bo-user.dto';
```

**ビルド**:
```bash
cd bff/shared
npm run build
```

#### 2.2 BoAuthGuard実装

**ファイル**: `bff/backoffice-bff/src/auth/bo-auth.guard.ts`

```typescript
import { Injectable, CanActivate, ExecutionContext, UnauthorizedException } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';

@Injectable()
export class BoAuthGuard implements CanActivate {
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
      // Core APIでBoUserトークン検証
      const boUser = await this.verifyBoToken(token, request.traceId);
      request.boUser = boUser;
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

  private async verifyBoToken(token: string, traceId?: string): Promise<any> {
    // BoUserトークン検証（冪等なのでリトライ可能）
    return this.coreApiService.post('/bo-auth/verify-token', { token }, token, traceId);
  }
}
```

#### 2.3 BoAuthModule

**ファイル**: `bff/backoffice-bff/src/auth/bo-auth.module.ts`

```typescript
import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { BoAuthController } from './bo-auth.controller';
import { BoAuthService } from './bo-auth.service';
import { BoAuthGuard } from './bo-auth.guard';

@Module({
  imports: [CoreApiModule],
  controllers: [BoAuthController],
  providers: [BoAuthService, BoAuthGuard],
  exports: [BoAuthGuard],
})
export class BoAuthModule {}
```

#### 2.4 BoAuthService

**ファイル**: `bff/backoffice-bff/src/auth/bo-auth.service.ts`

```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class BoAuthService {
  constructor(private coreApiService: CoreApiService) {}

  async login(username: string, password: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.post(
      '/bo-auth/login',
      { username, password },
      undefined,
      traceId,
    );
  }

  async logout(token: string, traceId?: string): Promise<ApiResponse<void>> {
    return this.coreApiService.post(
      '/bo-auth/logout',
      {},
      token,
      traceId,
    );
  }
}
```

#### 2.5 BoAuthController

**ファイル**: `bff/backoffice-bff/src/auth/bo-auth.controller.ts`

```typescript
import { Controller, Post, Body, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from './bo-auth.guard';
import { BoAuthService } from './bo-auth.service';
import { ApiResponse } from '@app/shared';

@Controller('api/bo-auth')
export class BoAuthController {
  constructor(private boAuthService: BoAuthService) {}

  @Post('login')
  async login(
    @Body() body: { username: string; password: string },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.boAuthService.login(body.username, body.password, req.traceId);
  }

  @Post('logout')
  @UseGuards(BoAuthGuard)
  async logout(@Req() req: any): Promise<ApiResponse<void>> {
    return this.boAuthService.logout(req.token, req.traceId);
  }
}
```

#### 2.6 app.module.ts に追加

```typescript
import { BoAuthModule } from './auth/bo-auth.module';

@Module({
  imports: [
    // ...
    BoAuthModule,
  ],
})
```

### 検証

```bash
# 起動
npm run start:dev

# BoUserログイン
curl -X POST http://localhost:3002/api/bo-auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# → トークンが返却されればOK

# ログアウト
curl -X POST http://localhost:3002/api/bo-auth/logout \
  -H "Authorization: Bearer {token}"
# → success: true であればOK
```

---

## Task 3: 在庫管理API実装

### 目的
在庫一覧取得・在庫更新APIを実装する。

### 実装内容

#### 3.1 bff/shared に InventoryDto 追加

**ファイル**: `bff/shared/src/dto/inventory.dto.ts`

```typescript
export interface InventoryDto {
  id: number;
  productName: string;
  stock: number;
  reservedStock: number;
  availableStock: number;
  lastUpdated: string;
}
```

**ファイル**: `bff/shared/src/index.ts` に追加

```typescript
export * from './dto/inventory.dto';
```

**ビルド**:
```bash
cd bff/shared
npm run build
```

#### 3.2 InventoryModule

**ファイル**: `bff/backoffice-bff/src/inventory/inventory.module.ts`

```typescript
import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { InventoryController } from './inventory.controller';
import { InventoryService } from './inventory.service';

@Module({
  imports: [CoreApiModule],
  controllers: [InventoryController],
  providers: [InventoryService],
})
export class InventoryModule {}
```

#### 3.3 InventoryService

**ファイル**: `bff/backoffice-bff/src/inventory/inventory.service.ts`

```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse, InventoryDto } from '@app/shared';

@Injectable()
export class InventoryService {
  constructor(private coreApiService: CoreApiService) {}

  async getInventory(token: string, traceId?: string): Promise<ApiResponse<InventoryDto[]>> {
    return this.coreApiService.get<ApiResponse<InventoryDto[]>>(
      '/admin/inventory',
      token,
      traceId,
    );
  }

  async updateInventory(
    id: number,
    stock: number,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<InventoryDto>> {
    return this.coreApiService.put<ApiResponse<InventoryDto>>(
      `/admin/inventory/${id}`,
      { stock },
      token,
      traceId,
    );
  }
}
```

#### 3.4 InventoryController

**ファイル**: `bff/backoffice-bff/src/inventory/inventory.controller.ts`

```typescript
import { Controller, Get, Put, Param, Body, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { InventoryService } from './inventory.service';
import { ApiResponse, InventoryDto } from '@app/shared';

@Controller('api/inventory')
@UseGuards(BoAuthGuard)
export class InventoryController {
  constructor(private inventoryService: InventoryService) {}

  @Get()
  async getInventory(@Req() req: any): Promise<ApiResponse<InventoryDto[]>> {
    return this.inventoryService.getInventory(req.token, req.traceId);
  }

  @Put(':id')
  async updateInventory(
    @Param('id') id: string,
    @Body() body: { stock: number },
    @Req() req: any,
  ): Promise<ApiResponse<InventoryDto>> {
    return this.inventoryService.updateInventory(
      parseInt(id, 10),
      body.stock,
      req.token,
      req.traceId,
    );
  }
}
```

#### 3.5 app.module.ts に追加

```typescript
import { InventoryModule } from './inventory/inventory.module';

@Module({
  imports: [
    // ...
    InventoryModule,
  ],
})
```

### 検証

```bash
# BoUserトークン取得（Task 2で取得）
TOKEN="..."

# 在庫一覧取得
curl http://localhost:3002/api/inventory \
  -H "Authorization: Bearer $TOKEN"
# → 在庫一覧が返却されればOK

# 在庫更新
curl -X PUT http://localhost:3002/api/inventory/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"stock":100}'
# → 更新後の在庫情報が返却されればOK
```

---

## Task 4: 注文管理API実装

### 目的
管理画面向けの注文一覧・詳細・ステータス更新APIを実装する。

### 実装内容

#### 4.1 OrdersModule

**ファイル**: `bff/backoffice-bff/src/orders/orders.module.ts`

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

#### 4.2 OrdersService

**ファイル**: `bff/backoffice-bff/src/orders/orders.service.ts`

```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse, OrderDto } from '@app/shared';

@Injectable()
export class OrdersService {
  constructor(private coreApiService: CoreApiService) {}

  async getOrders(token: string, traceId?: string): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.get<ApiResponse<OrderDto[]>>(
      '/admin/orders',
      token,
      traceId,
    );

    if (!response.success || !response.data) {
      return response;
    }

    // BFFで整形: ステータス日本語化、会員名埋め込み（将来）
    const orders = response.data.map(order => ({
      ...order,
      status: this.translateStatus(order.status),
    }));

    return {
      success: true,
      data: {
        orders,
        pagination: {
          page: 1,
          pageSize: 20,
          totalCount: orders.length,
        },
      },
    };
  }

  async getOrderById(id: number, token: string, traceId?: string): Promise<ApiResponse<any>> {
    const orderResponse = await this.coreApiService.get<ApiResponse<OrderDto>>(
      `/admin/orders/${id}`,
      token,
      traceId,
    );

    if (!orderResponse.success || !orderResponse.data) {
      return orderResponse;
    }

    const order = orderResponse.data;

    // 将来: 会員情報も取得して埋め込み
    // const memberResponse = await this.coreApiService.get(`/admin/members/${order.userId}`, token, traceId);

    return {
      success: true,
      data: {
        ...order,
        status: this.translateStatus(order.status),
      },
    };
  }

  async updateOrderStatus(
    id: number,
    status: string,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<OrderDto>> {
    return this.coreApiService.put<ApiResponse<OrderDto>>(
      `/admin/orders/${id}`,
      { status },
      token,
      traceId,
    );
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

#### 4.3 OrdersController

**ファイル**: `bff/backoffice-bff/src/orders/orders.controller.ts`

```typescript
import { Controller, Get, Put, Param, Body, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { OrdersService } from './orders.service';
import { ApiResponse, OrderDto } from '@app/shared';

@Controller('api/admin/orders')
@UseGuards(BoAuthGuard)
export class OrdersController {
  constructor(private ordersService: OrdersService) {}

  @Get()
  async getOrders(@Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.getOrders(req.token, req.traceId);
  }

  @Get(':id')
  async getOrderById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.ordersService.getOrderById(parseInt(id, 10), req.token, req.traceId);
  }

  @Put(':id')
  async updateOrderStatus(
    @Param('id') id: string,
    @Body() body: { status: string },
    @Req() req: any,
  ): Promise<ApiResponse<OrderDto>> {
    return this.ordersService.updateOrderStatus(
      parseInt(id, 10),
      body.status,
      req.token,
      req.traceId,
    );
  }
}
```

#### 4.4 app.module.ts に追加

```typescript
import { OrdersModule } from './orders/orders.module';

@Module({
  imports: [
    // ...
    OrdersModule,
  ],
})
```

### 検証

```bash
# 注文一覧取得
curl http://localhost:3002/api/admin/orders \
  -H "Authorization: Bearer $TOKEN"
# → ステータスが日本語化されていればOK

# 注文詳細取得
curl http://localhost:3002/api/admin/orders/1 \
  -H "Authorization: Bearer $TOKEN"

# 注文ステータス更新
curl -X PUT http://localhost:3002/api/admin/orders/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"SHIPPED"}'
```

---

## Task 5: 会員管理API実装

### 目的
管理画面向けの会員一覧・詳細APIを実装する。

### 実装内容

#### 5.1 MembersModule

**ファイル**: `bff/backoffice-bff/src/members/members.module.ts`

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

#### 5.2 MembersService

**ファイル**: `bff/backoffice-bff/src/members/members.service.ts`

```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse, UserDto } from '@app/shared';

@Injectable()
export class MembersService {
  constructor(private coreApiService: CoreApiService) {}

  async getMembers(token: string, traceId?: string): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.get<ApiResponse<UserDto[]>>(
      '/admin/members',
      token,
      traceId,
    );

    if (!response.success || !response.data) {
      return response;
    }

    return {
      success: true,
      data: {
        members: response.data,
        pagination: {
          page: 1,
          pageSize: 20,
          totalCount: response.data.length,
        },
      },
    };
  }

  async getMemberById(id: number, token: string, traceId?: string): Promise<ApiResponse<any>> {
    const memberResponse = await this.coreApiService.get<ApiResponse<UserDto>>(
      `/admin/members/${id}`,
      token,
      traceId,
    );

    if (!memberResponse.success || !memberResponse.data) {
      return memberResponse;
    }

    // 将来: 注文履歴も取得して埋め込み
    // const ordersResponse = await this.coreApiService.get(`/orders?userId=${id}`, token, traceId);

    return {
      success: true,
      data: {
        member: memberResponse.data,
        orderHistory: [],  // 将来実装
        stats: {
          totalOrders: 0,
          totalSpent: 0,
          lastOrderDate: null,
        },
      },
    };
  }
}
```

#### 5.3 MembersController

**ファイル**: `bff/backoffice-bff/src/members/members.controller.ts`

```typescript
import { Controller, Get, Param, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { MembersService } from './members.service';
import { ApiResponse } from '@app/shared';

@Controller('api/admin/members')
@UseGuards(BoAuthGuard)
export class MembersController {
  constructor(private membersService: MembersService) {}

  @Get()
  async getMembers(@Req() req: any): Promise<ApiResponse<any>> {
    return this.membersService.getMembers(req.token, req.traceId);
  }

  @Get(':id')
  async getMemberById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.membersService.getMemberById(parseInt(id, 10), req.token, req.traceId);
  }
}
```

#### 5.4 app.module.ts に追加

```typescript
import { MembersModule } from './members/members.module';

@Module({
  imports: [
    // ...
    MembersModule,
  ],
})
```

### 検証

```bash
# 会員一覧取得
curl http://localhost:3002/api/admin/members \
  -H "Authorization: Bearer $TOKEN"

# 会員詳細取得
curl http://localhost:3002/api/admin/members/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

## Task 6: BoUser管理API実装

### 目的
BoUser（管理ユーザー）一覧・作成APIを実装する。

### 実装内容

#### 6.1 BoUsersModule

**ファイル**: `bff/backoffice-bff/src/bo-users/bo-users.module.ts`

```typescript
import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { BoUsersController } from './bo-users.controller';
import { BoUsersService } from './bo-users.service';

@Module({
  imports: [CoreApiModule],
  controllers: [BoUsersController],
  providers: [BoUsersService],
})
export class BoUsersModule {}
```

#### 6.2 BoUsersService

**ファイル**: `bff/backoffice-bff/src/bo-users/bo-users.service.ts`

```typescript
import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse, BoUserDto } from '@app/shared';

@Injectable()
export class BoUsersService {
  constructor(private coreApiService: CoreApiService) {}

  async getBoUsers(token: string, traceId?: string): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.get<ApiResponse<BoUserDto[]>>(
      '/admin/bo-users',
      token,
      traceId,
    );

    if (!response.success || !response.data) {
      return response;
    }

    return {
      success: true,
      data: {
        boUsers: response.data,
        pagination: {
          page: 1,
          pageSize: 20,
          totalCount: response.data.length,
        },
      },
    };
  }

  async createBoUser(
    username: string,
    password: string,
    email: string,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<BoUserDto>> {
    return this.coreApiService.post<ApiResponse<BoUserDto>>(
      '/admin/bo-users',
      { username, password, email },
      token,
      traceId,
    );
  }
}
```

#### 6.3 BoUsersController

**ファイル**: `bff/backoffice-bff/src/bo-users/bo-users.controller.ts`

```typescript
import { Controller, Get, Post, Body, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { BoUsersService } from './bo-users.service';
import { ApiResponse, BoUserDto } from '@app/shared';

@Controller('api/admin/bo-users')
@UseGuards(BoAuthGuard)
export class BoUsersController {
  constructor(private boUsersService: BoUsersService) {}

  @Get()
  async getBoUsers(@Req() req: any): Promise<ApiResponse<any>> {
    return this.boUsersService.getBoUsers(req.token, req.traceId);
  }

  @Post()
  async createBoUser(
    @Body() body: { username: string; password: string; email: string },
    @Req() req: any,
  ): Promise<ApiResponse<BoUserDto>> {
    return this.boUsersService.createBoUser(
      body.username,
      body.password,
      body.email,
      req.token,
      req.traceId,
    );
  }
}
```

#### 6.4 app.module.ts に追加

```typescript
import { BoUsersModule } from './bo-users/bo-users.module';

@Module({
  imports: [
    // ...
    BoUsersModule,
  ],
})
```

### 検証

```bash
# BoUser一覧取得
curl http://localhost:3002/api/admin/bo-users \
  -H "Authorization: Bearer $TOKEN"

# BoUser作成
curl -X POST http://localhost:3002/api/admin/bo-users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username":"newadmin",
    "password":"password123",
    "email":"newadmin@example.com"
  }'
```

---

## Task 7: Docker構成

### 目的
BackOffice BFFをDocker化し、docker-compose.ymlに追加する。

### 実装内容

#### 7.1 Dockerfile

**ファイル**: `bff/backoffice-bff/Dockerfile`

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app

COPY package*.json ./
COPY bff/shared/package*.json ./bff/shared/
COPY bff/backoffice-bff/package*.json ./bff/backoffice-bff/

RUN npm ci

COPY bff/shared ./bff/shared
COPY bff/backoffice-bff ./bff/backoffice-bff

WORKDIR /app/bff/shared
RUN npm run build

WORKDIR /app/bff/backoffice-bff
RUN npm run build

FROM node:20-alpine
WORKDIR /app

COPY --from=builder /app/bff/backoffice-bff/dist ./dist
COPY --from=builder /app/bff/backoffice-bff/node_modules ./node_modules
COPY --from=builder /app/bff/shared/dist ./node_modules/@app/shared/dist
COPY --from=builder /app/bff/backoffice-bff/package*.json ./

EXPOSE 3002

CMD ["node", "dist/main.js"]
```

#### 7.2 .dockerignore

**ファイル**: `bff/backoffice-bff/.dockerignore`

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
  # 既存サービス（postgres, backend, customer-bff, frontend-customer, frontend-admin）

  backoffice-bff:
    build:
      context: .
      dockerfile: bff/backoffice-bff/Dockerfile
    ports:
      - "3002:3002"
    environment:
      - NODE_ENV=production
      - PORT=3002
      - CORE_API_URL=http://backend:8080
      - CORE_API_TIMEOUT=5000
      - CORE_API_RETRY=2
      - LOG_LEVEL=info
    depends_on:
      - backend
    networks:
      - app-network
```

### 検証

```bash
# ビルド
docker compose build backoffice-bff

# 起動
docker compose up -d backoffice-bff

# ログ確認
docker compose logs -f backoffice-bff

# 疎通確認
curl -X POST http://localhost:3002/api/bo-auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

---

## Task 8: フロントエンド接続切替

### 目的
管理画面のAPI呼び出し先をBackOffice BFFに切り替える。

### 前提
- Front は静的配信のみを担当する
- APIリクエストはブラウザからBFFへ直接送信する
- 管理画面は `localhost:5174` から `localhost:3002`（BackOffice BFF）を利用する

### 実装内容

#### 8.1 .env.admin 更新

**ファイル**: `frontend/.env.admin`

```bash
# admin モードで BackOffice BFF に接続
VITE_APP_MODE=admin
VITE_API_URL=http://localhost:3002
```

#### 8.2 frontend/src/lib/api.ts の確認（変更不要）

既存の `api.ts` をそのまま使用する。

理由:
- `VITE_API_URL` により接続先BFFを切り替え可能
- 管理系API呼び出し時は `bo_token` を使う既存ロジックを維持できる
- `fetch` 直接実装の追加は不要

#### 8.3 管理画面コンポーネントの確認

管理画面の各ページは既存の `api.ts` 経由呼び出しを維持する。

```typescript
// 例: AdminInventoryPage.tsx
import * as api from '../lib/api';

// 在庫一覧取得
const response = await api.get('/bo/admin/inventory');
```

### 検証

```bash
# フロントエンド起動
cd frontend
npm run dev:admin

# ブラウザで http://localhost:5174/bo/item を開く
# → 管理画面が表示され、BackOffice BFF経由でAPIが呼ばれればOK
# → DevToolsのNetworkタブで http://localhost:3002/api/* へのリクエストを確認
```

---

## Task 9: E2Eテスト・検証

### 目的
管理画面向け主要フローの回帰テストを実施し、BackOffice BFF経由で問題なく動作することを確認する。

### 検証シナリオ

#### 9.1 BoUserログインフロー

```bash
# BoUserログイン
curl -X POST http://localhost:3002/api/bo-auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username":"admin",
    "password":"admin123"
  }'

# 検証ポイント:
# - success: true
# - token が返却される

BO_TOKEN="..."
```

#### 9.2 在庫管理フロー

```bash
# 在庫一覧取得
curl http://localhost:3002/api/inventory \
  -H "Authorization: Bearer $BO_TOKEN"

# 検証ポイント:
# - 在庫一覧が返却される
# - availableStock が計算されている

# 在庫更新
curl -X PUT http://localhost:3002/api/inventory/1 \
  -H "Authorization: Bearer $BO_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"stock":100}'

# 検証ポイント:
# - 更新成功
# - 監査ログが記録される（Core API側）
```

#### 9.3 注文管理フロー

```bash
# 注文一覧取得
curl http://localhost:3002/api/admin/orders \
  -H "Authorization: Bearer $BO_TOKEN"

# 検証ポイント:
# - ステータスが日本語化されている
# - pagination が含まれている

# 注文詳細取得
curl http://localhost:3002/api/admin/orders/1 \
  -H "Authorization: Bearer $BO_TOKEN"

# 注文ステータス更新
curl -X PUT http://localhost:3002/api/admin/orders/1 \
  -H "Authorization: Bearer $BO_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"SHIPPED"}'
```

#### 9.4 会員管理フロー

```bash
# 会員一覧取得
curl http://localhost:3002/api/admin/members \
  -H "Authorization: Bearer $BO_TOKEN"

# 会員詳細取得
curl http://localhost:3002/api/admin/members/1 \
  -H "Authorization: Bearer $BO_TOKEN"

# 検証ポイント:
# - 会員情報が含まれる
# - stats が含まれる（将来実装）
```

#### 9.5 BoUser管理フロー

```bash
# BoUser一覧取得
curl http://localhost:3002/api/admin/bo-users \
  -H "Authorization: Bearer $BO_TOKEN"

# BoUser作成
curl -X POST http://localhost:3002/api/admin/bo-users \
  -H "Authorization: Bearer $BO_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username":"newadmin",
    "password":"password123",
    "email":"newadmin@example.com"
  }'

# 検証ポイント:
# - 作成成功
# - 監査ログが記録される
```

#### 9.6 誤アクセス防止テスト

```bash
# 顧客トークンで管理APIへアクセス（エラー期待）
USER_TOKEN="..."  # Customer BFFで取得したトークン

curl http://localhost:3002/api/inventory \
  -H "Authorization: Bearer $USER_TOKEN"

# 検証ポイント:
# - 401 Unauthorized
# - エラーコード: BFF_INVALID_TOKEN

# 管理トークンで顧客APIへアクセス（エラー期待）
curl http://localhost:3001/api/cart \
  -H "Authorization: Bearer $BO_TOKEN"

# 検証ポイント:
# - 401 Unauthorized
# - エラーコード: BFF_INVALID_TOKEN
```

#### 9.7 監査ログ確認

```bash
# Core APIの監査ログを確認
docker compose exec backend cat /app/logs/audit.log

# 検証ポイント:
# - 在庫更新、注文ステータス更新、BoUser作成のログが記録されている
# - actorType: BO_USER が記録されている
# - トレースIDが記録されている
```

---

## Phase 2 完了条件

以下をすべて満たすこと：

- [ ] BackOffice BFF が起動し、`http://localhost:3002` でアクセス可能
- [ ] BoUserログイン・ログアウトが動作する
- [ ] 在庫一覧・更新が BackOffice BFF 経由で動作する
- [ ] 注文一覧・詳細・ステータス更新が BackOffice BFF 経由で動作する
- [ ] 会員一覧・詳細が BackOffice BFF 経由で動作する
- [ ] BoUser一覧・作成が BackOffice BFF 経由で動作する
- [ ] 顧客トークンで管理APIにアクセスすると401エラーが返る
- [ ] 管理トークンで顧客APIにアクセスすると401エラーが返る
- [ ] 監査ログが正常に記録される
- [ ] トレースIDがブラウザ→BFF→Core APIで伝播する
- [ ] 管理画面が BackOffice BFF 経由で正常動作する
- [ ] 管理画面の通信経路が `localhost:5174 -> localhost:3002 -> Core API` である

---

## トラブルシューティング

### BoUser認証エラー

```bash
# Core APIのBoUser認証エンドポイント確認
curl http://localhost:8080/bo-auth/verify-token \
  -H "Content-Type: application/json" \
  -d '{"token":"..."}'

# BackOffice BFFのログ確認
docker compose logs -f backoffice-bff
```

### 誤アクセス防止が機能しない

```bash
# トークンのactorTypeを確認
# Core APIのログで、どちらのトークンか確認

# BoAuthGuardが正しくUserトークンを拒否しているか確認
docker compose logs -f backoffice-bff | grep "INVALID_TOKEN"
```

---

## 参考資料

- [技術設計ドキュメント](../02_designs/CHG-010_BFF構成への移行.md)
- [Phase 1実装タスク](./CHG-010_BFF構成への移行.md)
