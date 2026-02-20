# CHG-010 Phase 3: Core API内部化 - 実装タスク

## 前提条件

- Phase 1（Customer BFF導入）が完了済み
- Phase 2（BackOffice BFF導入）が完了済み
- 全てのBFF経由でのAPI呼び出しが正常動作していること

---

## Phase 3: Core API内部化

### 検証コマンド

```bash
# Core APIへの直接アクセスが遮断されることを確認
curl http://localhost:8080/products
# → Connection refused または timeout

# BFF経由では正常にアクセスできることを確認
curl http://localhost:3001/api/products
# → 正常に応答

# 性能テスト
cd performance-test
k6 run load-test.js
```

---

## Task 1: ネットワーク分離設定

### 目的
Docker Composeでネットワークを分離し、Core APIを内部ネットワークのみに配置する。

### 実装内容

#### 1.1 docker-compose.yml 更新

**ファイル**: `/docker-compose.yml`

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
    container_name: ec-postgres
    environment:
      POSTGRES_USER: ec_user
      POSTGRES_PASSWORD: ec_password
      POSTGRES_DB: ec_db
    ports:
      - "5432:5432"  # 開発環境のみ公開
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./backend/src/main/resources/db/init:/docker-entrypoint-initdb.d
    networks:
      - internal

  backend:
    build: ./backend
    container_name: ec-backend
    # ポート公開を削除（内部ネットワークのみ）
    # ports:
    #   - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ec_db
      - SPRING_DATASOURCE_USERNAME=ec_user
      - SPRING_DATASOURCE_PASSWORD=ec_password
      - SPRING_PROFILES_ACTIVE=production
    depends_on:
      - postgres
    networks:
      - internal  # 内部ネットワークのみ

  customer-bff:
    build:
      context: .
      dockerfile: bff/customer-bff/Dockerfile
    container_name: ec-customer-bff
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
      - public    # 外部公開
      - internal  # Core API接続

  backoffice-bff:
    build:
      context: .
      dockerfile: bff/backoffice-bff/Dockerfile
    container_name: ec-backoffice-bff
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
      - public
      - internal

  frontend-customer:
    build: ./frontend
    container_name: ec-frontend-customer
    ports:
      - "5173:5173"
    environment:
      - VITE_APP_MODE=customer
      - VITE_API_URL=http://localhost:3001
    command: npm run dev:customer
    depends_on:
      - customer-bff
    networks:
      - public

  frontend-admin:
    build: ./frontend
    container_name: ec-frontend-admin
    ports:
      - "5174:5174"
    environment:
      - VITE_APP_MODE=admin
      - VITE_API_URL=http://localhost:3002
    command: npm run dev:admin
    depends_on:
      - backoffice-bff
    networks:
      - public

networks:
  public:
    driver: bridge
  internal:
    driver: bridge
    internal: true  # 外部からのアクセスを遮断

volumes:
  postgres-data:
```

#### 1.2 ネットワーク分離の説明

- **public ネットワーク**: インターネットに公開（frontend-customer, frontend-admin, customer-bff, backoffice-bff）
- **internal ネットワーク**: 内部通信のみ（backend, postgres）
  - `internal: true` により外部からのアクセスを完全遮断
  - BFFからのみアクセス可能

### 検証

```bash
# コンテナ再起動
docker compose down
docker compose up -d

# ネットワーク確認
docker network ls
# → ai-ec-experiment_public と ai-ec-experiment_internal が存在すること

# internal ネットワークの設定確認
docker network inspect ai-ec-experiment_internal
# → "Internal": true であること

# Core APIへの直接アクセスが拒否されることを確認
curl http://localhost:8080/products
# → Connection refused

# BFF経由では正常にアクセスできることを確認
curl http://localhost:3001/api/products
# → 正常に応答
```

---

## Task 2: Core API設定更新

### 目的
Core APIを内部専用モードに設定し、外部公開を前提としない設定に変更する。

### 実装内容

#### 2.1 application.yml 更新（内部モード）

**ファイル**: `backend/src/main/resources/application.yml` に追加

```yaml
# 既存設定

---
# 本番環境（内部ネットワーク専用）
spring:
  config:
    activate:
      on-profile: production-internal

  datasource:
    url: jdbc:postgresql://postgres:5432/ec_db
    username: ec_user
    password: ec_password

# サーバー設定（内部専用）
server:
  port: 8080
  # 内部ネットワークのみで動作
  address: 0.0.0.0

# CORS設定（BFFからのアクセスのみ許可）
app:
  cors:
    allowed-origins:
      - http://customer-bff:3001
      - http://backoffice-bff:3002
    allowed-methods:
      - GET
      - POST
      - PUT
      - DELETE
    allowed-headers:
      - "*"
    allow-credentials: true

# ログ設定
logging:
  level:
    com.example.aiec: INFO
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [traceId=%X{traceId}] - %msg%n"
```

#### 2.2 WebConfig.java 更新

**ファイル**: `backend/src/main/java/com/example/aiec/config/WebConfig.java`

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${app.cors.allowed-methods}")
    private List<String> allowedMethods;

    @Value("${app.cors.allowed-headers}")
    private List<String> allowedHeaders;

    @Value("${app.cors.allow-credentials}")
    private boolean allowCredentials;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods(allowedMethods.toArray(new String[0]))
                .allowedHeaders(allowedHeaders.toArray(new String[0]))
                .allowCredentials(allowCredentials);
    }
}
```

#### 2.3 docker-compose.yml の環境変数更新

```yaml
backend:
  environment:
    - SPRING_PROFILES_ACTIVE=production-internal
```

### 検証

```bash
# コンテナ再起動
docker compose restart backend

# ログ確認（profile確認）
docker compose logs backend | grep "production-internal"
# → "The following 1 profile is active: "production-internal"" が表示されること

# CORS設定確認（BFF経由）
curl -v http://localhost:3001/api/products
# → Access-Control-Allow-Origin ヘッダーが含まれていればOK
```

---

## Task 3: 直接アクセス遮断確認

### 目的
ブラウザやcurlからCore APIへの直接アクセスが完全に遮断されることを確認する。

### 検証内容

#### 3.1 外部からの直接アクセステスト

```bash
# Core APIへの直接アクセス（すべて失敗するはず）

# 商品一覧
curl http://localhost:8080/products
# → Connection refused

# 認証
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"password"}'
# → Connection refused

# 管理API
curl http://localhost:8080/admin/inventory
# → Connection refused

# ヘルスチェック
curl http://localhost:8080/actuator/health
# → Connection refused
```

#### 3.2 BFF経由でのアクセステスト（成功するはず）

```bash
# Customer BFF経由
curl http://localhost:3001/api/products
# → 正常に応答

# BackOffice BFF経由（要認証）
BO_TOKEN="..."
curl http://localhost:3002/api/inventory \
  -H "Authorization: Bearer $BO_TOKEN"
# → 正常に応答
```

#### 3.3 内部ネットワーク疎通確認

```bash
# BFFコンテナからCore APIへの疎通確認
docker compose exec customer-bff sh -c "curl http://backend:8080/actuator/health"
# → 正常に応答（内部ネットワーク経由）

docker compose exec backoffice-bff sh -c "curl http://backend:8080/actuator/health"
# → 正常に応答
```

#### 3.4 ブラウザからの直接アクセステスト

1. ブラウザで `http://localhost:8080/products` を開く
   - → アクセス不可（ERR_CONNECTION_REFUSED）

2. ブラウザで `http://localhost:3001/api/products` を開く
   - → 正常に商品一覧が表示される

### 検証結果記録

**ファイル**: `docs/verification/phase3-access-control-test.md`

```markdown
# Phase 3 アクセス制御テスト結果

## テスト実施日
2026-XX-XX

## テスト環境
- Docker Compose
- ネットワーク分離設定適用済み

## テスト結果

### 1. 外部からCore APIへの直接アクセス

| エンドポイント | 結果 | 期待値 |
|--------------|------|-------|
| GET /products | Connection refused | ✅ 遮断 |
| POST /auth/login | Connection refused | ✅ 遮断 |
| GET /admin/inventory | Connection refused | ✅ 遮断 |
| GET /actuator/health | Connection refused | ✅ 遮断 |

### 2. BFF経由でのアクセス

| BFF | エンドポイント | 結果 | 期待値 |
|-----|--------------|------|-------|
| Customer | GET /api/products | 200 OK | ✅ 成功 |
| Customer | POST /api/cart/items | 200 OK | ✅ 成功 |
| BackOffice | GET /api/inventory | 200 OK | ✅ 成功 |
| BackOffice | PUT /api/inventory/1 | 200 OK | ✅ 成功 |

### 3. 内部ネットワーク疎通

| コンテナ | Core API疎通 | 結果 |
|---------|-------------|------|
| customer-bff | curl http://backend:8080/actuator/health | ✅ 成功 |
| backoffice-bff | curl http://backend:8080/actuator/health | ✅ 成功 |

## 結論

Core APIの内部化が正常に完了。
```

---

## Task 4: 障害系テスト

### 目的
Core API障害時のBFFの挙動を確認する。

### 検証内容

#### 4.1 Core API停止時のテスト

```bash
# Core API停止
docker compose stop backend

# Customer BFF経由でアクセス
curl http://localhost:3001/api/products
# → 503 Service Unavailable
# → エラーコード: BFF_CORE_API_UNAVAILABLE
# → メッセージ: "サービスが一時的に利用できません"

# レスポンス確認
{
  "success": false,
  "error": {
    "code": "BFF_CORE_API_UNAVAILABLE",
    "message": "サービスが一時的に利用できません"
  }
}

# Core API再起動
docker compose start backend

# 復旧確認
curl http://localhost:3001/api/products
# → 正常に応答
```

#### 4.2 Core APIタイムアウトテスト

Core APIに意図的な遅延を追加してテスト（テスト用エンドポイント作成）:

**ファイル**: `backend/src/main/java/com/example/aiec/controller/TestController.java`

```java
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/slow")
    public ApiResponse<String> slow() throws InterruptedException {
        Thread.sleep(10000);  // 10秒遅延
        return ApiResponse.success("Slow response");
    }
}
```

```bash
# タイムアウトテスト（BFF経由、5秒でタイムアウト設定）
curl http://localhost:3001/api/test/slow
# → 504 Gateway Timeout（5秒後）
# → エラーコード: BFF_CORE_API_TIMEOUT
# → メッセージ: "リクエストがタイムアウトしました"
```

#### 4.3 Core API 500エラーテスト

```bash
# 存在しない商品を取得（404）
curl http://localhost:3001/api/products/99999
# → 404 Not Found
# → Core APIのエラーがそのまま伝播

# サーバーエラー（500）を発生させる（テスト用）
# → Core APIのエラーがそのまま伝播
```

### 検証結果記録

**ファイル**: `docs/verification/phase3-failure-test.md`

```markdown
# Phase 3 障害系テスト結果

## 1. Core API停止時

| テスト | BFFレスポンス | HTTPステータス | エラーコード |
|-------|-------------|--------------|------------|
| 商品一覧取得 | エラー | 503 | BFF_CORE_API_UNAVAILABLE |
| カート取得 | エラー | 503 | BFF_CORE_API_UNAVAILABLE |

## 2. タイムアウト

| テスト | BFFレスポンス | HTTPステータス | エラーコード |
|-------|-------------|--------------|------------|
| /test/slow (10秒) | エラー（5秒後） | 504 | BFF_CORE_API_TIMEOUT |

## 3. Core APIエラー伝播

| Core APIエラー | BFFレスポンス | 挙動 |
|--------------|-------------|------|
| 404 Not Found | 404 | そのまま伝播 ✅ |
| 400 Bad Request | 400 | そのまま伝播 ✅ |
| 500 Internal Error | 500 | そのまま伝播 ✅ |
```

---

## Task 5: 性能テスト

### 目的
BFF導入による性能劣化がないことを確認する。

### 実装内容

#### 5.1 k6負荷テストスクリプト作成

**ファイル**: `performance-test/load-test.js`

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },  // ウォームアップ
    { duration: '1m', target: 50 },   // 50同時ユーザー
    { duration: '2m', target: 100 },  // 100同時ユーザー
    { duration: '30s', target: 0 },   // クールダウン
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // p95が500ms以内
    http_req_failed: ['rate<0.01'],    // エラー率1%未満
  },
};

export default function () {
  // 商品一覧取得
  let res = http.get('http://localhost:3001/api/products');
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);

  // 商品詳細取得
  res = http.get('http://localhost:3001/api/products/1');
  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
```

#### 5.2 認証ありシナリオ

**ファイル**: `performance-test/authenticated-test.js`

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

const TOKEN = __ENV.USER_TOKEN;  // 環境変数から取得

export const options = {
  stages: [
    { duration: '1m', target: 50 },
    { duration: '2m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<800'],  // 認証ありは800ms以内
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const params = {
    headers: {
      'Authorization': `Bearer ${TOKEN}`,
    },
  };

  // カート取得
  let res = http.get('http://localhost:3001/api/cart', params);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(2);

  // 注文履歴
  res = http.get('http://localhost:3001/api/orders', params);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(2);
}
```

#### 5.3 性能テスト実行

```bash
# k6インストール（Ubuntuの場合）
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# テスト実行
cd performance-test

# 基本シナリオ
k6 run load-test.js

# 認証ありシナリオ（トークン取得後）
USER_TOKEN="..." k6 run authenticated-test.js
```

### 検証基準

| メトリクス | 目標値 | 許容値 |
|----------|-------|-------|
| p95レスポンスタイム（公開API） | < 300ms | < 500ms |
| p95レスポンスタイム（認証API） | < 500ms | < 800ms |
| エラー率 | 0% | < 1% |
| スループット | > 100 req/s | > 50 req/s |

### 検証結果記録

**ファイル**: `docs/verification/phase3-performance-test.md`

```markdown
# Phase 3 性能テスト結果

## テスト実施日
2026-XX-XX

## テスト環境
- Docker Compose
- k6 負荷テストツール
- 同時ユーザー数: 最大100

## テスト結果

### 1. 公開API（認証なし）

| メトリクス | 結果 | 目標値 | 判定 |
|----------|------|-------|------|
| p95レスポンスタイム | XXX ms | < 500ms | ✅ / ❌ |
| エラー率 | X.XX% | < 1% | ✅ / ❌ |
| スループット | XXX req/s | > 50 req/s | ✅ / ❌ |

### 2. 認証API

| メトリクス | 結果 | 目標値 | 判定 |
|----------|------|-------|------|
| p95レスポンスタイム | XXX ms | < 800ms | ✅ / ❌ |
| エラー率 | X.XX% | < 1% | ✅ / ❌ |

### 3. BFF追加によるオーバーヘッド

| 項目 | 移行前 | 移行後 | 増加率 |
|-----|-------|-------|-------|
| p95レスポンスタイム | XXX ms | XXX ms | +XX% |

## 結論

性能要件を満たしている / 満たしていない
```

---

## Task 6: ロールバック手順検証

### 目的
緊急時のロールバック手順を検証し、手順書を作成する。

### 実装内容

#### 6.1 ロールバック手順書作成


```markdown
# BFF構成ロールバック手順

## 概要
Phase 3完了後、緊急時にCore APIを直接公開する手順。

## 前提条件
- Phase 1/2/3が完了済み
- Core APIがinternal networkのみに配置されている
- BFF経由でのみAPI利用可能

## ロールバック手順

### Step 1: docker-compose.yml を一時修正

\`\`\`yaml
# backend サービスにポート公開を追加
backend:
  ports:
    - "8080:8080"  # 再公開
  networks:
    - public     # 追加
    - internal
\`\`\`

### Step 2: backendコンテナ再起動

\`\`\`bash
docker compose up -d backend
\`\`\`

### Step 3: 疎通確認

\`\`\`bash
curl http://localhost:8080/products
# → 正常に応答すればOK
\`\`\`

### Step 4: フロントエンド切り戻し（オプション）

BFF障害時のみ実施：

\`\`\`bash
# frontend/.env.customer
VITE_API_URL=http://localhost:8080

# frontend/.env.admin
VITE_API_URL=http://localhost:8080
\`\`\`

\`\`\`bash
# フロントエンド再ビルド・デプロイ
cd frontend
npm run build
docker compose up -d frontend-customer frontend-admin
\`\`\`

### Step 5: 動作確認

- [ ] ブラウザで商品一覧が表示される
- [ ] カート追加・注文が正常動作する
- [ ] 管理画面が正常動作する

### Step 6: 監視強化

ロールバック期間中は以下を監視：
- Core APIのエラー率
- レスポンスタイム
- CPU/メモリ使用率

## ロールバック後の復旧手順

問題解決後、BFF構成に戻す：

1. docker-compose.ymlを元に戻す（backendポート非公開）
2. backend再起動
3. フロントエンド設定を元に戻す（BFF URL）
4. 疎通確認

## 所要時間

- ロールバック: 約5分
- 復旧: 約10分
```

#### 6.2 ロールバック手順の実施テスト

```bash
# 1. 現在の状態確認（Core API非公開）
curl http://localhost:8080/products
# → Connection refused

# 2. docker-compose.yml を一時修正
# （Task 6.1の手順に従って修正）

# 3. backend再起動
docker compose up -d backend

# 4. 疎通確認
curl http://localhost:8080/products
# → 正常に応答

# 5. 元に戻す（docker-compose.ymlを元に戻して再起動）
docker compose up -d backend

# 6. 最終確認
curl http://localhost:8080/products
# → Connection refused（元に戻ったことを確認）
```

### 検証結果記録

**ファイル**: `docs/verification/phase3-rollback-test.md`

```markdown
# Phase 3 ロールバック手順検証結果

## テスト実施日
2026-XX-XX

## テスト内容

### 1. ロールバック手順（Core API再公開）

| ステップ | 実施内容 | 所要時間 | 結果 |
|---------|---------|---------|------|
| 1 | docker-compose.yml修正 | 1分 | ✅ |
| 2 | backend再起動 | 30秒 | ✅ |
| 3 | 疎通確認 | 30秒 | ✅ |
| **合計** | | **2分** | **✅** |

### 2. 復旧手順（BFF構成に戻す）

| ステップ | 実施内容 | 所要時間 | 結果 |
|---------|---------|---------|------|
| 1 | docker-compose.yml修正（元に戻す） | 1分 | ✅ |
| 2 | backend再起動 | 30秒 | ✅ |
| 3 | 疎通確認（直アクセス遮断） | 30秒 | ✅ |
| 4 | BFF経由アクセス確認 | 1分 | ✅ |
| **合計** | | **3分** | **✅** |

## 結論

ロールバック手順は正常に動作。緊急時に迅速に切り戻し可能。
```

---

## Task 7: 監視・ヘルスチェック設定

### 目的
BFF・Core APIの監視とヘルスチェックを設定する。

### 実装内容

#### 7.1 ヘルスチェックエンドポイント追加

**Customer BFF**: `bff/customer-bff/src/health/health.controller.ts`

```typescript
import { Controller, Get } from '@nestjs/common';

@Controller('health')
export class HealthController {
  @Get()
  health(): { status: string; service: string } {
    return {
      status: 'ok',
      service: 'customer-bff',
    };
  }
}
```

**BackOffice BFF**: `bff/backoffice-bff/src/health/health.controller.ts`

```typescript
import { Controller, Get } from '@nestjs/common';

@Controller('health')
export class HealthController {
  @Get()
  health(): { status: string; service: string } {
    return {
      status: 'ok',
      service: 'backoffice-bff',
    };
  }
}
```

#### 7.2 docker-compose.yml にヘルスチェック追加

```yaml
services:
  backend:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  customer-bff:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3001/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  backoffice-bff:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3002/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

#### 7.3 ヘルスチェック確認

```bash
# コンテナ再起動
docker compose up -d

# ヘルスステータス確認
docker compose ps

# 個別確認
curl http://localhost:3001/health
# → {"status":"ok","service":"customer-bff"}

curl http://localhost:3002/health
# → {"status":"ok","service":"backoffice-bff"}
```

#### 7.4 監視項目一覧


```markdown
# BFF構成 監視項目

## 1. Customer BFF

| 項目 | エンドポイント | 閾値 | アラート |
|-----|--------------|------|---------|
| ヘルスチェック | GET /health | status != "ok" | Critical |
| エラー率 | ログ集計 | > 5% | Warning |
| p95レスポンスタイム | ログ集計 | > 500ms | Warning |
| CPU使用率 | Docker Stats | > 80% | Warning |
| メモリ使用率 | Docker Stats | > 80% | Warning |

## 2. BackOffice BFF

| 項目 | エンドポイント | 閾値 | アラート |
|-----|--------------|------|---------|
| ヘルスチェック | GET /health | status != "ok" | Critical |
| エラー率 | ログ集計 | > 5% | Warning |
| p95レスポンスタイム | ログ集計 | > 800ms | Warning |

## 3. Core API

| 項目 | エンドポイント | 閾値 | アラート |
|-----|--------------|------|---------|
| ヘルスチェック | GET /actuator/health | status != "UP" | Critical |
| DB接続 | /actuator/health/db | DOWN | Critical |
| エラー率 | ログ集計 | > 3% | Warning |

## 4. ネットワーク

| 項目 | 確認方法 | 閾値 | アラート |
|-----|---------|------|---------|
| BFF→Core API疎通 | 内部ヘルスチェック | 失敗 | Critical |
| 外部→Core API遮断 | 定期確認 | アクセス可能 | Critical |
```

---

## Task 8: ドキュメント更新

### 目的
Phase 3完了に伴い、関連ドキュメントを更新する。

### 実装内容

#### 8.1 README.md 更新

**ファイル**: `/README.md` に追記

```markdown
## アーキテクチャ

### システム構成（Phase 3完了後）

\`\`\`
┌─────────────┐
│  ブラウザ   │
└──────┬──────┘
       │
   ┌───┴───────────┐
   │               │
   ↓               ↓
┌──────────────┐ ┌──────────────┐
│Customer Front│ │ Admin Front  │
│ (Port 5173)  │ │ (Port 5174)  │
└──────┬───────┘ └──────┬───────┘
       ↓                ↓
┌──────────────┐ ┌──────────────┐
│Customer BFF  │ │BackOffice BFF│
│ (Port 3001)  │ │ (Port 3002)  │
└──────┬───────┘ └──────┬───────┘
       └───────┬────────┘
               ↓
                  ┌────────────────┐
                  │   Core API     │
                  │  (内部のみ)    │
                  │  (Port 8080)   │
                  └────────┬───────┘
                           ↓
                  ┌────────────────┐
                  │  PostgreSQL    │
                  └────────────────┘
\`\`\`

### セキュリティ

- **Core APIは内部ネットワークのみ**: 外部から直接アクセス不可
- **BFF経由のみアクセス可**: Customer BFF / BackOffice BFFを経由
- **認証トークン分離**: User（顧客）とBoUser（管理者）を厳密に分離

## 開発コマンド

### Docker（メインの開発フロー）

\`\`\`bash
docker compose up -d          # 全コンテナ起動
docker compose down            # 全コンテナ停止
docker compose logs -f         # 全ログ確認
docker compose logs -f customer-bff    # Customer BFFログ
docker compose logs -f backoffice-bff  # BackOffice BFFログ
docker compose logs -f backend         # Core APIログ
\`\`\`

### アクセスURL

- 顧客画面: http://localhost:5173
- 管理画面: http://localhost:5174
- Customer BFF: http://localhost:3001
- BackOffice BFF: http://localhost:3002
- Core API: 直接アクセス不可（内部ネットワークのみ）
```

#### 8.2 SPEC.md 更新

**ファイル**: `docs/SPEC.md` に追記

```markdown
## アーキテクチャ変更履歴

### CHG-010: BFF構成への移行（Phase 3完了）

**実施日**: 2026-XX-XX

**変更内容**:
- Customer BFF導入（顧客向けAPI）
- BackOffice BFF導入（管理向けAPI）
- Core APIの内部ネットワーク化

**影響**:
- Core APIへの直接アクセスは不可
- すべてのAPI呼び出しはBFF経由
- 顧客トークンと管理トークンの境界を明確化

```

#### 8.3 api-spec.md 更新

**ファイル**: `docs/ui/api-spec.md` に追記

```markdown
## API構成（Phase 3完了後）

### Customer BFF（顧客向け）

**ベースURL**: `http://localhost:3001`

| エンドポイント | メソッド | 説明 | 認証 |
|--------------|---------|------|------|
| /api/products | GET | 商品一覧取得 | 不要 |
| /api/cart | GET | カート取得 | User |
| /api/orders | POST | 注文確定 | User |

（詳細は技術設計ドキュメント参照）

### BackOffice BFF（管理向け）

**ベースURL**: `http://localhost:3002`

| エンドポイント | メソッド | 説明 | 認証 |
|--------------|---------|------|------|
| /api/inventory | GET | 在庫一覧取得 | BoUser |
| /api/admin/orders | GET | 注文一覧取得 | BoUser |
| /api/admin/members | GET | 会員一覧取得 | BoUser |

（詳細は技術設計ドキュメント参照）

### Core API（内部専用）

**ベースURL**: `http://backend:8080`（内部ネットワークのみ）

- 外部からの直接アクセスは不可
- BFF経由でのみアクセス可能
```

---

## Phase 3 完了条件

以下をすべて満たすこと：

- [ ] docker-compose.ymlでネットワーク分離が設定されている
- [ ] Core APIのポート公開が削除されている
- [ ] 外部からCore APIへの直接アクセスが遮断されている
- [ ] BFF経由でCore APIへアクセスできる
- [ ] 顧客画面がCustomer BFF経由で正常動作する
- [ ] 管理画面がBackOffice BFF経由で正常動作する
- [ ] Core API停止時にBFFが503エラーを返す
- [ ] タイムアウト時にBFFが504エラーを返す
- [ ] p95レスポンスタイムが目標値以内（公開API < 500ms、認証API < 800ms）
- [ ] エラー率が1%未満
- [ ] ロールバック手順が検証済み
- [ ] ヘルスチェックが正常動作する
- [ ] 監視項目が定義されている
- [ ] ドキュメントが更新されている

---

## トラブルシューティング

### ネットワーク分離が機能しない

```bash
# internal networkの設定確認
docker network inspect ai-ec-experiment_internal
# → "Internal": true であることを確認

# コンテナのネットワーク接続確認
docker inspect ec-backend | grep -A 10 "Networks"
# → internal のみに接続されていることを確認
```

### BFFからCore APIへの疎通不可

```bash
# BFFコンテナからCore APIへping
docker compose exec customer-bff ping backend
# → 疎通確認

# Core APIのログ確認
docker compose logs backend
# → エラーログを確認

# ネットワーク再作成
docker compose down
docker network prune
docker compose up -d
```

---

## 参考資料

- [技術設計ドキュメント](../02_designs/CHG-010_BFF構成への移行.md)
- [Phase 1実装タスク](./CHG-010_BFF構成への移行.md)
- [Phase 2実装タスク](./CHG-010_Phase2_BackOfficeBFF導入.md)
