# CHG-015: OpenTelemetry 導入 - 実装タスク

要件: `docs/01_requirements/CHG-015_OpenTelemetry 導入.md`
設計: `docs/02_designs/CHG-015_OpenTelemetry導入.md`
作成日: 2026-02-18

検証コマンド:
```bash
# インフラ起動（OTel全スタック）
docker compose up -d

# BFF ビルド確認
docker compose build customer-bff backoffice-bff

# バックエンドコンパイル確認
cd backend && ./mvnw compile

# フロントエンドビルド確認
cd frontend && npm run build

# 動作確認URL
# Jaeger:     http://localhost:16686
# Prometheus: http://localhost:9090
# Grafana:    http://localhost:3000  (admin/admin)
```

---

## タスク一覧

### インフラ（OTel設定ファイル）

---

- [x] **T-1**: otel/otel-collector-config.yaml を新規作成

  パス: `otel/otel-collector-config.yaml`（プロジェクトルートに `otel/` ディレクトリを作成）

  ファイル内容:

  ```yaml
  receivers:
    otlp:
      protocols:
        grpc:
          endpoint: 0.0.0.0:4317
        http:
          endpoint: 0.0.0.0:4318
          cors:
            allowed_origins:
              - "http://localhost:5173"
              - "http://localhost:5174"
            allowed_headers:
              - "*"

  processors:
    batch:
      timeout: 5s
      send_batch_size: 512

  exporters:
    otlp/jaeger:
      endpoint: jaeger:4317
      tls:
        insecure: true

    prometheus:
      endpoint: "0.0.0.0:8889"
      namespace: "ec"

    debug:
      verbosity: basic

  service:
    telemetry:
      metrics:
        address: 0.0.0.0:8888

    pipelines:
      traces:
        receivers: [otlp]
        processors: [batch]
        exporters: [otlp/jaeger, debug]
      metrics:
        receivers: [otlp]
        processors: [batch]
        exporters: [prometheus, debug]
      logs:
        receivers: [otlp]
        processors: [batch]
        exporters: [debug]
  ```

---

- [x] **T-2**: otel/prometheus.yml を新規作成

  パス: `otel/prometheus.yml`

  ファイル内容:

  ```yaml
  global:
    scrape_interval: 15s

  scrape_configs:
    - job_name: 'otel-collector-app-metrics'
      static_configs:
        - targets: ['otel-collector:8889']

    - job_name: 'spring-actuator'
      metrics_path: '/actuator/prometheus'
      static_configs:
        - targets: ['backend:8080']
  ```

---

- [x] **T-3**: Grafana プロビジョニングファイル群を新規作成

  パス（3ファイル）:
  - `otel/grafana/provisioning/datasources/datasource.yml`
  - `otel/grafana/provisioning/dashboards/dashboard.yml`
  - `otel/grafana/provisioning/dashboards/ec-business.json`

  **datasource.yml**:

  ```yaml
  apiVersion: 1

  datasources:
    - name: Prometheus
      type: prometheus
      access: proxy
      url: http://prometheus:9090
      isDefault: true

    - name: Jaeger
      type: jaeger
      access: proxy
      url: http://jaeger:16686
  ```

  **dashboard.yml**:

  ```yaml
  apiVersion: 1

  providers:
    - name: default
      orgId: 1
      folder: ''
      type: file
      disableDeletion: false
      updateIntervalSeconds: 10
      options:
        path: /etc/grafana/provisioning/dashboards
  ```

  **ec-business.json**（ビジネスメトリクスダッシュボード）:

  ```json
  {
    "title": "EC ビジネスメトリクス",
    "uid": "ec-business",
    "version": 1,
    "schemaVersion": 37,
    "refresh": "30s",
    "time": { "from": "now-1h", "to": "now" },
    "panels": [
      {
        "id": 1,
        "title": "注文成功率",
        "type": "stat",
        "gridPos": { "x": 0, "y": 0, "w": 6, "h": 4 },
        "targets": [
          {
            "datasource": "Prometheus",
            "expr": "rate(ec_order_created_total[5m]) / (rate(ec_order_created_total[5m]) + rate(ec_order_creation_failed_total[5m]))",
            "refId": "A"
          }
        ],
        "options": { "reduceOptions": { "calcs": ["lastNotNull"] } },
        "fieldConfig": {
          "defaults": { "unit": "percentunit", "thresholds": { "steps": [{"color": "red", "value": 0}, {"color": "green", "value": 0.9}] } }
        }
      },
      {
        "id": 2,
        "title": "在庫引当失敗率",
        "type": "timeseries",
        "gridPos": { "x": 6, "y": 0, "w": 9, "h": 4 },
        "targets": [
          {
            "datasource": "Prometheus",
            "expr": "rate(ec_inventory_reservation_failed_total[5m])",
            "legendFormat": "在庫引当失敗",
            "refId": "A"
          }
        ]
      },
      {
        "id": 3,
        "title": "認証失敗数",
        "type": "timeseries",
        "gridPos": { "x": 15, "y": 0, "w": 9, "h": 4 },
        "targets": [
          {
            "datasource": "Prometheus",
            "expr": "rate(ec_auth_login_failed_total[5m])",
            "legendFormat": "認証失敗",
            "refId": "A"
          }
        ]
      },
      {
        "id": 4,
        "title": "注文処理レイテンシ (p95 / p99)",
        "type": "timeseries",
        "gridPos": { "x": 0, "y": 4, "w": 12, "h": 6 },
        "targets": [
          {
            "datasource": "Prometheus",
            "expr": "histogram_quantile(0.95, rate(ec_order_creation_duration_seconds_bucket[5m]))",
            "legendFormat": "p95",
            "refId": "A"
          },
          {
            "datasource": "Prometheus",
            "expr": "histogram_quantile(0.99, rate(ec_order_creation_duration_seconds_bucket[5m]))",
            "legendFormat": "p99",
            "refId": "B"
          }
        ],
        "fieldConfig": { "defaults": { "unit": "s" } }
      },
      {
        "id": 5,
        "title": "HTTP p95 レイテンシ (BFF)",
        "type": "timeseries",
        "gridPos": { "x": 12, "y": 4, "w": 12, "h": 6 },
        "targets": [
          {
            "datasource": "Prometheus",
            "expr": "histogram_quantile(0.95, rate(http_server_request_duration_seconds_bucket{job=\"customer-bff\"}[5m]))",
            "legendFormat": "customer-bff p95",
            "refId": "A"
          },
          {
            "datasource": "Prometheus",
            "expr": "histogram_quantile(0.95, rate(http_server_request_duration_seconds_bucket{job=\"backoffice-bff\"}[5m]))",
            "legendFormat": "backoffice-bff p95",
            "refId": "B"
          }
        ],
        "fieldConfig": { "defaults": { "unit": "s" } }
      }
    ]
  }
  ```

---

- [x] **T-4**: docker-compose.yml にOTelスタック追加と既存サービスの環境変数更新

  パス: `docker-compose.yml`

  **変更1**: `services:` ブロックの末尾（`redis:` サービスの後）に追記:

  ```yaml
    otel-collector:
      image: otel/opentelemetry-collector-contrib:0.116.0
      container_name: ec-otel-collector
      command: ["--config=/etc/otel/config.yaml"]
      volumes:
        - ./otel/otel-collector-config.yaml:/etc/otel/config.yaml
      ports:
        - "4317:4317"
        - "4318:4318"
      networks:
        - public
        - internal

    jaeger:
      image: jaegertracing/all-in-one:1.63
      container_name: ec-jaeger
      environment:
        - COLLECTOR_OTLP_ENABLED=true
      ports:
        - "16686:16686"
      networks:
        - internal

    prometheus:
      image: prom/prometheus:v3.1.0
      container_name: ec-prometheus
      volumes:
        - ./otel/prometheus.yml:/etc/prometheus/prometheus.yml
      ports:
        - "9090:9090"
      networks:
        - internal

    grafana:
      image: grafana/grafana:11.4.0
      container_name: ec-grafana
      environment:
        - GF_SECURITY_ADMIN_PASSWORD=admin
        - GF_AUTH_ANONYMOUS_ENABLED=true
        - GF_AUTH_ANONYMOUS_ORG_ROLE=Viewer
      ports:
        - "3000:3000"
      volumes:
        - grafana-data:/var/lib/grafana
        - ./otel/grafana/provisioning:/etc/grafana/provisioning
      depends_on:
        - prometheus
        - jaeger
      networks:
        - internal
        - public
  ```

  **変更2**: `backend:` サービスの `environment:` に追記（行35〜38の `SPRING_PROFILES_ACTIVE` 行の後）:

  変更前（行27〜31）:
  ```yaml
      environment:
        - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ec_db
        - SPRING_DATASOURCE_USERNAME=ec_user
        - SPRING_DATASOURCE_PASSWORD=ec_password
        - SPRING_PROFILES_ACTIVE=production-internal
  ```

  変更後:
  ```yaml
      environment:
        - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ec_db
        - SPRING_DATASOURCE_USERNAME=ec_user
        - SPRING_DATASOURCE_PASSWORD=ec_password
        - SPRING_PROFILES_ACTIVE=production-internal
        - MANAGEMENT_OTLP_TRACING_ENDPOINT=http://otel-collector:4317
        - MANAGEMENT_OTLP_TRACING_TRANSPORT=grpc
  ```

  **変更3**: `customer-bff:` サービスの `environment:` に追記（行56 `LOG_LEVEL=info` 行の後）:

  変更前（行49〜56）:
  ```yaml
      environment:
        - NODE_ENV=production
        - PORT=3001
        - CORE_API_URL=http://backend:8080
        - CORE_API_TIMEOUT=5000
        - CORE_API_RETRY=2
        - LOG_LEVEL=info
        - REDIS_HOST=redis
  ```

  変更後:
  ```yaml
      environment:
        - NODE_ENV=production
        - PORT=3001
        - CORE_API_URL=http://backend:8080
        - CORE_API_TIMEOUT=5000
        - CORE_API_RETRY=2
        - LOG_LEVEL=info
        - REDIS_HOST=redis
        - REDIS_PORT=6379
        - REDIS_TIMEOUT=5000
        - OTEL_EXPORTER_OTLP_GRPC_ENDPOINT=http://otel-collector:4317
        - OTEL_SERVICE_NAME=customer-bff
  ```

  ※ 重複を避けるため、既存の `REDIS_PORT` と `REDIS_TIMEOUT` は削除して上記に統合する。

  **変更4**: `backoffice-bff:` サービスの `environment:` も同様に追記:

  変更前（行79〜88）:
  ```yaml
      environment:
        - NODE_ENV=production
        - PORT=3002
        - CORE_API_URL=http://backend:8080
        - CORE_API_TIMEOUT=5000
        - CORE_API_RETRY=2
        - LOG_LEVEL=info
        - REDIS_HOST=redis
        - REDIS_PORT=6379
        - REDIS_TIMEOUT=5000
  ```

  変更後:
  ```yaml
      environment:
        - NODE_ENV=production
        - PORT=3002
        - CORE_API_URL=http://backend:8080
        - CORE_API_TIMEOUT=5000
        - CORE_API_RETRY=2
        - LOG_LEVEL=info
        - REDIS_HOST=redis
        - REDIS_PORT=6379
        - REDIS_TIMEOUT=5000
        - OTEL_EXPORTER_OTLP_GRPC_ENDPOINT=http://otel-collector:4317
        - OTEL_SERVICE_NAME=backoffice-bff
  ```

  **変更5**: `frontend-customer:` と `frontend-admin:` の `environment:` に追記:

  `frontend-customer:` (行109〜111):
  ```yaml
      environment:
        - VITE_APP_MODE=customer
        - VITE_API_URL=http://localhost:3001
        - VITE_OTEL_ENDPOINT=http://localhost:4318
  ```

  `frontend-admin:` (行126〜128):
  ```yaml
      environment:
        - VITE_APP_MODE=admin
        - VITE_API_URL=http://localhost:3002
        - VITE_OTEL_ENDPOINT=http://localhost:4318
  ```

  **変更6**: `volumes:` セクション（末尾）に `grafana-data:` を追加:

  変更前（行150〜152）:
  ```yaml
  volumes:
    postgres-data:
    redis-data:
  ```

  変更後:
  ```yaml
  volumes:
    postgres-data:
    redis-data:
    grafana-data:
  ```

  **変更7**: `backend:` の `depends_on:` に `otel-collector` を追加し、`customer-bff:` / `backoffice-bff:` の `depends_on:` にも追加:

  ```yaml
  # backend の depends_on
  depends_on:
    - postgres
    - otel-collector

  # customer-bff の depends_on
  depends_on:
    - backend
    - redis
    - otel-collector

  # backoffice-bff の depends_on
  depends_on:
    - backend
    - redis
    - otel-collector
  ```

---

### BFF - Customer BFF

---

- [x] **T-5**: bff/customer-bff/package.json に OTel gRPC 依存を追加

  パス: `bff/customer-bff/package.json`

  変更前（行22〜35 `"dependencies":` ブロック）:
  ```json
  "dependencies": {
    "@app/shared": "^1.0.0",
    "@nestjs/axios": "^3.0.0",
    "@nestjs/common": "^10.0.0",
    "@nestjs/config": "^3.0.0",
    "@nestjs/core": "^10.0.0",
    "@nestjs/platform-express": "^10.0.0",
    "axios": "^1.6.0",
    "reflect-metadata": "^0.2.2",
    "rxjs": "^7.8.0",
    "uuid": "^9.0.0",
    "winston": "^3.11.0",
    "ioredis": "^5.3.0"
  },
  ```

  変更後:
  ```json
  "dependencies": {
    "@app/shared": "^1.0.0",
    "@nestjs/axios": "^3.0.0",
    "@nestjs/common": "^10.0.0",
    "@nestjs/config": "^3.0.0",
    "@nestjs/core": "^10.0.0",
    "@nestjs/platform-express": "^10.0.0",
    "@opentelemetry/api": "^1.9.0",
    "@opentelemetry/auto-instrumentations-node": "^0.54.0",
    "@opentelemetry/exporter-metrics-otlp-grpc": "^0.57.0",
    "@opentelemetry/exporter-trace-otlp-grpc": "^0.57.0",
    "@opentelemetry/resources": "^1.29.0",
    "@opentelemetry/sdk-metrics": "^1.29.0",
    "@opentelemetry/sdk-node": "^0.57.0",
    "@opentelemetry/semantic-conventions": "^1.29.0",
    "axios": "^1.6.0",
    "ioredis": "^5.3.0",
    "reflect-metadata": "^0.2.2",
    "rxjs": "^7.8.0",
    "uuid": "^9.0.0",
    "winston": "^3.11.0"
  },
  ```

  追加後に `npm install` を実行（Dockerビルド時に自動実行される）:
  ```bash
  cd bff/customer-bff && npm install
  ```

---

- [x] **T-6**: bff/customer-bff/src/tracing.ts を新規作成

  パス: `bff/customer-bff/src/tracing.ts`

  ファイル内容:

  ```typescript
  import { NodeSDK } from '@opentelemetry/sdk-node';
  import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node';
  import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-grpc';
  import { OTLPMetricExporter } from '@opentelemetry/exporter-metrics-otlp-grpc';
  import { PeriodicExportingMetricReader } from '@opentelemetry/sdk-metrics';
  import { Resource } from '@opentelemetry/resources';
  import { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION } from '@opentelemetry/semantic-conventions';

  const grpcEndpoint = process.env.OTEL_EXPORTER_OTLP_GRPC_ENDPOINT
    || 'http://localhost:4317';

  const sdk = new NodeSDK({
    resource: new Resource({
      [ATTR_SERVICE_NAME]: process.env.OTEL_SERVICE_NAME || 'customer-bff',
      [ATTR_SERVICE_VERSION]: '1.0.0',
    }),
    traceExporter: new OTLPTraceExporter({
      url: grpcEndpoint,
    }),
    metricReader: new PeriodicExportingMetricReader({
      exporter: new OTLPMetricExporter({
        url: grpcEndpoint,
      }),
      exportIntervalMillis: 30000,
    }),
    instrumentations: [
      getNodeAutoInstrumentations({
        '@opentelemetry/instrumentation-fs': { enabled: false },
      }),
    ],
  });

  sdk.start();

  process.on('SIGTERM', () => {
    sdk.shutdown().finally(() => process.exit(0));
  });
  ```

---

- [x] **T-7**: bff/customer-bff/Dockerfile の CMD を変更

  パス: `bff/customer-bff/Dockerfile`

  変更前（行37）:
  ```dockerfile
  CMD ["node", "dist/main.js"]
  ```

  変更後:
  ```dockerfile
  CMD ["node", "--require", "./dist/tracing", "dist/main"]
  ```

---

- [x] **T-8**: bff/customer-bff/src/main.ts の CORS 設定に traceparent/tracestate を追加

  パス: `bff/customer-bff/src/main.ts`

  変更前（行20〜23）:
  ```typescript
    app.enableCors({
      origin: 'http://localhost:5173',
      credentials: true,
    });
  ```

  変更後:
  ```typescript
    app.enableCors({
      origin: 'http://localhost:5173',
      credentials: true,
      allowedHeaders: [
        'Content-Type', 'Authorization', 'X-Session-Id',
        'traceparent', 'tracestate',
      ],
    });
  ```

---

- [x] **T-9**: bff/customer-bff/src/common/interceptors/trace.interceptor.ts を OTel 対応に更新

  パス: `bff/customer-bff/src/common/interceptors/trace.interceptor.ts`

  変更後（ファイル全体を置き換え）:
  ```typescript
  import { Injectable, NestInterceptor, ExecutionContext, CallHandler } from '@nestjs/common';
  import { Observable } from 'rxjs';
  import { trace } from '@opentelemetry/api';

  @Injectable()
  export class TraceInterceptor implements NestInterceptor {
    intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
      const request = context.switchToHttp().getRequest();
      const response = context.switchToHttp().getResponse();

      // OTel SDK が traceparent を自動処理済み。ここでは転写のみ
      const traceId = trace.getActiveSpan()?.spanContext()?.traceId;
      request.traceId = traceId ?? '';
      if (traceId) {
        response.setHeader('X-Trace-Id', traceId);
      }

      return next.handle();
    }
  }
  ```

---

- [x] **T-10**: bff/customer-bff/src/common/interceptors/logging.interceptor.ts を OTel 対応に更新

  パス: `bff/customer-bff/src/common/interceptors/logging.interceptor.ts`

  変更後（ファイル全体を置き換え）:
  ```typescript
  import { Injectable, NestInterceptor, ExecutionContext, CallHandler, Logger } from '@nestjs/common';
  import { Observable } from 'rxjs';
  import { tap } from 'rxjs/operators';
  import { trace } from '@opentelemetry/api';

  @Injectable()
  export class LoggingInterceptor implements NestInterceptor {
    private readonly logger = new Logger('HTTP');

    intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
      const request = context.switchToHttp().getRequest();
      const { method, url } = request;
      const startTime = Date.now();

      return next.handle().pipe(
        tap({
          next: () => {
            const response = context.switchToHttp().getResponse();
            const { statusCode } = response;
            const duration = Date.now() - startTime;

            const span = trace.getActiveSpan();
            const spanCtx = span?.spanContext();
            const traceId = spanCtx?.traceId ?? '';
            const spanId = spanCtx?.spanId ?? '';

            this.logger.log(JSON.stringify({
              traceId,
              spanId,
              method,
              url,
              statusCode,
              duration,
            }));
          },
          error: (error) => {
            const duration = Date.now() - startTime;

            const span = trace.getActiveSpan();
            const spanCtx = span?.spanContext();
            const traceId = spanCtx?.traceId ?? '';
            const spanId = spanCtx?.spanId ?? '';

            this.logger.error(JSON.stringify({
              traceId,
              spanId,
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

---

### BFF - BackOffice BFF

---

- [x] **T-11**: bff/backoffice-bff/package.json に OTel gRPC 依存を追加

  パス: `bff/backoffice-bff/package.json`

  変更前（行8〜20 `"dependencies":` ブロック）:
  ```json
  "dependencies": {
    "@app/shared": "^1.0.0",
    "@nestjs/common": "^10.0.0",
    "@nestjs/core": "^10.0.0",
    "@nestjs/platform-express": "^10.0.0",
    "@nestjs/config": "^3.0.0",
    "@nestjs/axios": "^3.0.0",
    "axios": "^1.6.0",
    "rxjs": "^7.8.0",
    "winston": "^3.11.0",
    "uuid": "^9.0.0",
    "ioredis": "^5.3.0"
  },
  ```

  変更後:
  ```json
  "dependencies": {
    "@app/shared": "^1.0.0",
    "@nestjs/axios": "^3.0.0",
    "@nestjs/common": "^10.0.0",
    "@nestjs/config": "^3.0.0",
    "@nestjs/core": "^10.0.0",
    "@nestjs/platform-express": "^10.0.0",
    "@opentelemetry/api": "^1.9.0",
    "@opentelemetry/auto-instrumentations-node": "^0.54.0",
    "@opentelemetry/exporter-metrics-otlp-grpc": "^0.57.0",
    "@opentelemetry/exporter-trace-otlp-grpc": "^0.57.0",
    "@opentelemetry/resources": "^1.29.0",
    "@opentelemetry/sdk-metrics": "^1.29.0",
    "@opentelemetry/sdk-node": "^0.57.0",
    "@opentelemetry/semantic-conventions": "^1.29.0",
    "axios": "^1.6.0",
    "ioredis": "^5.3.0",
    "rxjs": "^7.8.0",
    "uuid": "^9.0.0",
    "winston": "^3.11.0"
  },
  ```

  追加後に `npm install` を実行:
  ```bash
  cd bff/backoffice-bff && npm install
  ```

---

- [x] **T-12**: bff/backoffice-bff/src/tracing.ts を新規作成

  パス: `bff/backoffice-bff/src/tracing.ts`

  ファイル内容（T-6 の customer-bff と同じ構造、サービス名のみ異なる）:

  ```typescript
  import { NodeSDK } from '@opentelemetry/sdk-node';
  import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node';
  import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-grpc';
  import { OTLPMetricExporter } from '@opentelemetry/exporter-metrics-otlp-grpc';
  import { PeriodicExportingMetricReader } from '@opentelemetry/sdk-metrics';
  import { Resource } from '@opentelemetry/resources';
  import { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION } from '@opentelemetry/semantic-conventions';

  const grpcEndpoint = process.env.OTEL_EXPORTER_OTLP_GRPC_ENDPOINT
    || 'http://localhost:4317';

  const sdk = new NodeSDK({
    resource: new Resource({
      [ATTR_SERVICE_NAME]: process.env.OTEL_SERVICE_NAME || 'backoffice-bff',
      [ATTR_SERVICE_VERSION]: '1.0.0',
    }),
    traceExporter: new OTLPTraceExporter({
      url: grpcEndpoint,
    }),
    metricReader: new PeriodicExportingMetricReader({
      exporter: new OTLPMetricExporter({
        url: grpcEndpoint,
      }),
      exportIntervalMillis: 30000,
    }),
    instrumentations: [
      getNodeAutoInstrumentations({
        '@opentelemetry/instrumentation-fs': { enabled: false },
      }),
    ],
  });

  sdk.start();

  process.on('SIGTERM', () => {
    sdk.shutdown().finally(() => process.exit(0));
  });
  ```

---

- [x] **T-13**: bff/backoffice-bff/Dockerfile の CMD を変更

  パス: `bff/backoffice-bff/Dockerfile`

  変更前（行34）:
  ```dockerfile
  CMD ["node", "dist/main.js"]
  ```

  変更後:
  ```dockerfile
  CMD ["node", "--require", "./dist/tracing", "dist/main"]
  ```

---

- [x] **T-14**: bff/backoffice-bff/src/main.ts の CORS 設定に traceparent/tracestate を追加

  パス: `bff/backoffice-bff/src/main.ts`

  変更前（行16〜19）:
  ```typescript
    app.enableCors({
      origin: 'http://localhost:5174',
      credentials: true,
    });
  ```

  変更後:
  ```typescript
    app.enableCors({
      origin: 'http://localhost:5174',
      credentials: true,
      allowedHeaders: [
        'Content-Type', 'Authorization', 'X-Session-Id',
        'traceparent', 'tracestate',
      ],
    });
  ```

---

- [x] **T-15**: bff/backoffice-bff/src/common/interceptors/trace.interceptor.ts を OTel 対応に更新

  パス: `bff/backoffice-bff/src/common/interceptors/trace.interceptor.ts`

  変更後（T-9 と同一内容）:
  ```typescript
  import { Injectable, NestInterceptor, ExecutionContext, CallHandler } from '@nestjs/common';
  import { Observable } from 'rxjs';
  import { trace } from '@opentelemetry/api';

  @Injectable()
  export class TraceInterceptor implements NestInterceptor {
    intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
      const request = context.switchToHttp().getRequest();
      const response = context.switchToHttp().getResponse();

      const traceId = trace.getActiveSpan()?.spanContext()?.traceId;
      request.traceId = traceId ?? '';
      if (traceId) {
        response.setHeader('X-Trace-Id', traceId);
      }

      return next.handle();
    }
  }
  ```

---

- [x] **T-16**: bff/backoffice-bff/src/common/interceptors/logging.interceptor.ts を OTel 対応に更新

  パス: `bff/backoffice-bff/src/common/interceptors/logging.interceptor.ts`

  変更後（T-10 と同一内容）:
  ```typescript
  import { Injectable, NestInterceptor, ExecutionContext, CallHandler, Logger } from '@nestjs/common';
  import { Observable } from 'rxjs';
  import { tap } from 'rxjs/operators';
  import { trace } from '@opentelemetry/api';

  @Injectable()
  export class LoggingInterceptor implements NestInterceptor {
    private readonly logger = new Logger('HTTP');

    intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
      const request = context.switchToHttp().getRequest();
      const { method, url } = request;
      const startTime = Date.now();

      return next.handle().pipe(
        tap({
          next: () => {
            const response = context.switchToHttp().getResponse();
            const { statusCode } = response;
            const duration = Date.now() - startTime;

            const span = trace.getActiveSpan();
            const spanCtx = span?.spanContext();
            const traceId = spanCtx?.traceId ?? '';
            const spanId = spanCtx?.spanId ?? '';

            this.logger.log(JSON.stringify({
              traceId,
              spanId,
              method,
              url,
              statusCode,
              duration,
            }));
          },
          error: (error) => {
            const duration = Date.now() - startTime;

            const span = trace.getActiveSpan();
            const spanCtx = span?.spanContext();
            const traceId = spanCtx?.traceId ?? '';
            const spanId = spanCtx?.spanId ?? '';

            this.logger.error(JSON.stringify({
              traceId,
              spanId,
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

---

### バックエンド（Spring Boot）

---

- [x] **T-17**: backend/pom.xml に OTel 関連依存を追加

  パス: `backend/pom.xml`

  挿入位置: 行109（`</dependencies>` の直前）に以下を追加:

  ```xml
          <!-- Micrometer Tracing → OTel Bridge -->
          <dependency>
              <groupId>io.micrometer</groupId>
              <artifactId>micrometer-tracing-bridge-otel</artifactId>
          </dependency>

          <!-- OTel OTLP Exporter（gRPC版） -->
          <dependency>
              <groupId>io.opentelemetry</groupId>
              <artifactId>opentelemetry-exporter-otlp</artifactId>
          </dependency>

          <!-- Prometheus メトリクス（Actuator 経由のスクレイプ） -->
          <dependency>
              <groupId>io.micrometer</groupId>
              <artifactId>micrometer-registry-prometheus</artifactId>
          </dependency>
  ```

---

- [x] **T-18**: backend/src/main/resources/application.yml にトレース・メトリクス設定を追加

  パス: `backend/src/main/resources/application.yml`

  **変更1**: デフォルトプロファイルに `management:` セクションを追加（行79 `# OpenAPI / Swagger UI 設定` の直前）:

  ```yaml
  management:
    tracing:
      sampling:
        probability: 1.0
    endpoints:
      web:
        exposure:
          include: health,prometheus,info
    prometheus:
      metrics:
        export:
          enabled: true
  ```

  **変更2**: `production-internal` プロファイルのログパターンを更新（行119）:

  変更前:
  ```yaml
    pattern:
      console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [traceId=%X{traceId}] - %msg%n"
  ```

  変更後:
  ```yaml
    pattern:
      console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [traceId=%X{traceId},spanId=%X{spanId}] - %msg%n"
  ```

  **変更3**: `production-internal` プロファイルに OTel トレース設定を追加（ログパターン変更の後）:

  ```yaml
  management:
    tracing:
      sampling:
        probability: 0.1
    otlp:
      tracing:
        endpoint: http://otel-collector:4317
        transport: grpc
    endpoints:
      web:
        exposure:
          include: health,prometheus,info
    prometheus:
      metrics:
        export:
          enabled: true
  ```

---

- [x] **T-19**: backend/src/main/java/com/example/aiec/config/TraceIdFilter.java を削除

  パス: `backend/src/main/java/com/example/aiec/config/TraceIdFilter.java`

  Micrometer Tracing が `traceparent` ヘッダを自動処理し MDC に `traceId` / `spanId` を注入するため、このファイルは不要になる。

  ```bash
  rm backend/src/main/java/com/example/aiec/config/TraceIdFilter.java
  ```

  削除後、このクラスを参照している箇所がないか確認:
  ```bash
  grep -r "TraceIdFilter" backend/src/main/java/
  ```
  参照がなければ削除完了。

---

- [x] **T-20**: OrderUseCase.java に Counter と Timer を追加

  パス: `backend/src/main/java/com/example/aiec/modules/purchase/application/usecase/OrderUseCase.java`

  **変更1**: import を追加（行22 `import java.math.BigDecimal;` の直前）:

  ```java
  import io.micrometer.core.instrument.Counter;
  import io.micrometer.core.instrument.MeterRegistry;
  import io.micrometer.core.instrument.Timer;
  import jakarta.annotation.PostConstruct;
  ```

  **変更2**: フィールド宣言を追加（行37 `private final UserRepository userRepository;` の後）:

  変更前（行33〜37）:
  ```java
      private final OrderRepository orderRepository;
      private final CartRepository cartRepository;
      private final CartService cartService;
      private final InventoryCommandPort inventoryCommand;
      private final UserRepository userRepository;
  ```

  変更後:
  ```java
      private final OrderRepository orderRepository;
      private final CartRepository cartRepository;
      private final CartService cartService;
      private final InventoryCommandPort inventoryCommand;
      private final UserRepository userRepository;
      private final MeterRegistry meterRegistry;

      private Counter orderCreatedCounter;
      private Counter orderCreationFailedCounter;
      private Counter inventoryReservationFailedCounter;
      private Timer orderCreationTimer;

      @PostConstruct
      private void initMetrics() {
          this.orderCreatedCounter = Counter.builder("order.created")
                  .description("注文成功数").register(meterRegistry);
          this.orderCreationFailedCounter = Counter.builder("order.creation.failed")
                  .description("注文失敗数").register(meterRegistry);
          this.inventoryReservationFailedCounter = Counter.builder("inventory.reservation.failed")
                  .description("在庫引当失敗数").register(meterRegistry);
          this.orderCreationTimer = Timer.builder("order.creation.duration")
                  .description("注文作成処理時間")
                  .publishPercentileHistogram(true)
                  .register(meterRegistry);
      }
  ```

  **変更3**: `createOrder` メソッド本体をタイマー計測で囲む（行43〜105）:

  変更前（行43〜45）:
  ```java
      @Override
      @Transactional(rollbackFor = Exception.class)
      public OrderDto createOrder(String sessionId, String cartId, Long userId) {
  ```

  変更後（メソッドシグネチャ直後に `Timer.Sample sample = Timer.start(meterRegistry);` を追加し、末尾を変更）:

  ```java
      @Override
      @Transactional(rollbackFor = Exception.class)
      public OrderDto createOrder(String sessionId, String cartId, Long userId) {
          Timer.Sample sample = Timer.start(meterRegistry);
          try {
              if (!sessionId.equals(cartId)) {
                  throw new BusinessException("INVALID_REQUEST", "無効なリクエストです");
              }

              Cart cart = cartRepository.findBySessionId(sessionId)
                      .orElseThrow(() -> new ResourceNotFoundException("CART_NOT_FOUND", "カートが見つかりません"));

              if (cart.getItems().isEmpty()) {
                  throw new BusinessException("CART_EMPTY", "カートが空です");
              }

              List<UnavailableProductDetail> unavailableProducts = cart.getItems().stream()
                      .filter(item -> !item.getProduct().getIsPublished())
                      .map(item -> new UnavailableProductDetail(
                              item.getProduct().getId(),
                              item.getProduct().getName()))
                      .toList();

              if (!unavailableProducts.isEmpty()) {
                  throw new ItemNotAvailableException(
                          "ITEM_NOT_AVAILABLE",
                          "購入できない商品がカートに含まれています",
                          unavailableProducts);
              }

              Order order = new Order();
              order.setOrderNumber(generateOrderNumber());
              order.setSessionId(sessionId);
              order.setTotalPrice(cart.getTotalPrice());
              order.setStatus(Order.OrderStatus.PENDING);

              if (userId != null) {
                  User user = userRepository.findById(userId)
                          .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "ユーザーが見つかりません"));
                  order.setUser(user);
              }

              cart.getItems().forEach(cartItem -> {
                  OrderItem orderItem = new OrderItem();
                  orderItem.setProduct(cartItem.getProduct());
                  orderItem.setProductName(cartItem.getProduct().getName());
                  orderItem.setProductPrice(cartItem.getProduct().getPrice());
                  orderItem.setQuantity(cartItem.getQuantity());
                  orderItem.setSubtotal(
                          cartItem.getProduct().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity().longValue()))
                  );
                  order.addItem(orderItem);
              });

              Order savedOrder = orderRepository.save(order);

              inventoryCommand.commitReservations(sessionId, savedOrder);

              cartRepository.findBySessionId(sessionId)
                      .ifPresent(c -> {
                          c.getItems().clear();
                          cartRepository.save(c);
                      });

              orderCreatedCounter.increment();
              return OrderDto.fromEntity(savedOrder);

          } catch (Exception e) {
              if (e.getClass().getSimpleName().equals("InsufficientStockException")
                      || e instanceof ItemNotAvailableException) {
                  inventoryReservationFailedCounter.increment();
              }
              orderCreationFailedCounter.increment();
              throw e;
          } finally {
              sample.stop(orderCreationTimer);
          }
      }
  ```

  > **注意**: catch ブロックの `InsufficientStockException` チェックは、該当例外クラスのパッケージを確認し、直接 `catch (InsufficientStockException e)` に変更することが望ましい。

---

- [x] **T-21**: AuthService.java に認証失敗 Counter を追加

  パス: `backend/src/main/java/com/example/aiec/modules/customer/domain/service/AuthService.java`

  **変更1**: import を追加（行10 `import lombok.RequiredArgsConstructor;` の後）:

  ```java
  import io.micrometer.core.instrument.Counter;
  import io.micrometer.core.instrument.MeterRegistry;
  import jakarta.annotation.PostConstruct;
  ```

  **変更2**: フィールド宣言を追加（行26〜27 既存フィールドの後）:

  変更前（行26〜27）:
  ```java
      private final AuthTokenRepository authTokenRepository;
      private static final int TOKEN_EXPIRATION_DAYS = 7;
  ```

  変更後:
  ```java
      private final AuthTokenRepository authTokenRepository;
      private final MeterRegistry meterRegistry;
      private static final int TOKEN_EXPIRATION_DAYS = 7;

      private Counter loginFailedCounter;

      @PostConstruct
      private void initMetrics() {
          this.loginFailedCounter = Counter.builder("auth.login.failed")
                  .tag("type", "customer")
                  .register(meterRegistry);
      }
  ```

  **変更3**: `verifyToken` メソッドで認証失敗時にカウンタをインクリメント（行60〜70）:

  変更前:
  ```java
          AuthToken authToken = authTokenRepository.findByTokenHash(tokenHash)
                  .orElseThrow(() -> new BusinessException("UNAUTHORIZED", "認証が必要です"));

          // 3. 有効性チェック（期限切れ・失効）
          if (!authToken.isValid()) {
              throw new BusinessException("UNAUTHORIZED",
                      authToken.getIsRevoked() ? "トークンが失効しています" : "トークンの有効期限が切れています");
          }
  ```

  変更後:
  ```java
          AuthToken authToken = authTokenRepository.findByTokenHash(tokenHash)
                  .orElseThrow(() -> {
                      loginFailedCounter.increment();
                      return new BusinessException("UNAUTHORIZED", "認証が必要です");
                  });

          // 3. 有効性チェック（期限切れ・失効）
          if (!authToken.isValid()) {
              loginFailedCounter.increment();
              throw new BusinessException("UNAUTHORIZED",
                      authToken.getIsRevoked() ? "トークンが失効しています" : "トークンの有効期限が切れています");
          }
  ```

---

### フロントエンド

---

- [x] **T-22**: frontend/package.json に OTel HTTP 依存を追加

  パス: `frontend/package.json`

  変更前（行21〜27 `"dependencies":` ブロック）:
  ```json
  "dependencies": {
    "@app/shared": "^1.0.0",
    "@tailwindcss/vite": "^4.1.18",
    "react": "^19.2.0",
    "react-dom": "^19.2.0",
    "react-router": "^7.13.0",
    "tailwindcss": "^4.1.18"
  },
  ```

  変更後:
  ```json
  "dependencies": {
    "@app/shared": "^1.0.0",
    "@opentelemetry/core": "^1.29.0",
    "@opentelemetry/exporter-trace-otlp-http": "^0.57.0",
    "@opentelemetry/instrumentation": "^0.57.0",
    "@opentelemetry/instrumentation-document-load": "^0.44.0",
    "@opentelemetry/instrumentation-fetch": "^0.57.0",
    "@opentelemetry/resources": "^1.29.0",
    "@opentelemetry/sdk-trace-web": "^1.29.0",
    "@opentelemetry/sdk-web": "^1.29.0",
    "@opentelemetry/semantic-conventions": "^1.29.0",
    "@tailwindcss/vite": "^4.1.18",
    "react": "^19.2.0",
    "react-dom": "^19.2.0",
    "react-router": "^7.13.0",
    "tailwindcss": "^4.1.18"
  },
  ```

  追加後に `npm install` を実行:
  ```bash
  cd frontend && npm install
  ```

---

- [x] **T-23**: frontend/src/lib/tracing.ts を新規作成

  パス: `frontend/src/lib/tracing.ts`

  ファイル内容:

  ```typescript
  import { WebTracerProvider } from '@opentelemetry/sdk-web';
  import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
  import { BatchSpanProcessor } from '@opentelemetry/sdk-trace-web';
  import { FetchInstrumentation } from '@opentelemetry/instrumentation-fetch';
  import { DocumentLoadInstrumentation } from '@opentelemetry/instrumentation-document-load';
  import { registerInstrumentations } from '@opentelemetry/instrumentation';
  import { Resource } from '@opentelemetry/resources';
  import { ATTR_SERVICE_NAME } from '@opentelemetry/semantic-conventions';
  import { W3CTraceContextPropagator } from '@opentelemetry/core';

  const APP_MODE = import.meta.env.VITE_APP_MODE === 'admin' ? 'admin' : 'customer';

  // ブラウザは gRPC 非対応のため OTLP/HTTP 固定（4318）
  const otlpHttpEndpoint = import.meta.env.VITE_OTEL_ENDPOINT || 'http://localhost:4318';

  const provider = new WebTracerProvider({
    resource: new Resource({
      [ATTR_SERVICE_NAME]: `frontend-${APP_MODE}`,
    }),
    spanProcessors: [
      new BatchSpanProcessor(
        new OTLPTraceExporter({
          url: `${otlpHttpEndpoint}/v1/traces`,
        })
      ),
    ],
  });

  provider.register({
    propagator: new W3CTraceContextPropagator(),
  });

  registerInstrumentations({
    instrumentations: [
      new FetchInstrumentation({
        propagateTraceHeaderCorsUrls: [
          /localhost:300[12]/,
        ],
      }),
      new DocumentLoadInstrumentation(),
    ],
  });
  ```

---

- [x] **T-24**: frontend/src/main.tsx に OTel 初期化 import を追加

  パス: `frontend/src/main.tsx`

  変更前（行1〜5）:
  ```typescript
  import { StrictMode } from 'react'
  import { createRoot } from 'react-dom/client'
  import './index.css'
  import App from './App.tsx'
  ```

  変更後:
  ```typescript
  import './lib/tracing'
  import { StrictMode } from 'react'
  import { createRoot } from 'react-dom/client'
  import './index.css'
  import App from './App.tsx'
  ```

  > `import './lib/tracing'` は他のすべての import より前に置くこと（OTel SDK の初期化を最初に完了させるため）。

---

## 実装順序

```
T-1, T-2, T-3（OTel設定ファイル新規作成: 並行可能）
  → T-4（docker-compose.yml 更新: T-1〜T-3 が必要）
    ├── T-5 → T-6 → T-7 → T-8 → T-9 → T-10（customer-bff: 順次実行）
    ├── T-11 → T-12 → T-13 → T-14 → T-15 → T-16（backoffice-bff: 順次実行）
    └── T-17 → T-18 → T-19 → T-20 → T-21（Spring Boot: 順次実行）
T-22 → T-23 → T-24（フロントエンド: 順次実行。他グループと並行可能）

全タスク完了後: docker compose up -d で全スタック起動
```

---

## テスト手順

実装後に以下を手動確認:

1. **コンテナ起動確認**
   ```bash
   docker compose up -d
   docker compose ps  # 全サービスが healthy であること
   ```

2. **gRPC 接続確認**
   ```bash
   docker logs ec-otel-collector
   # BFF / Core API からの span 受信ログが出力されること
   ```

3. **分散トレースの連結確認**
   - ブラウザで `http://localhost:5173` にアクセスし、商品を注文する
   - Jaeger UI（`http://localhost:16686`）で `customer-bff` または `frontend-customer` を検索
   - 1つのトレースにフロント・BFF・Core API の span が含まれること

4. **ログとトレースの相関確認**
   ```bash
   docker logs ec-customer-bff | grep traceId
   docker logs ec-backend | grep traceId
   ```
   同一リクエストの `traceId` が両サービスで一致すること

5. **ビジネスメトリクス確認**
   - 注文を作成し、Prometheus（`http://localhost:9090`）で以下を確認:
     - `ec_order_created_total` のカウントが増加すること
     - `ec_order_creation_duration_seconds_bucket` が存在すること
   - Grafana（`http://localhost:3000`）で「EC ビジネスメトリクス」ダッシュボードを開き、パネルにデータが表示されること

6. **フロントエンドトレース確認**
   - ブラウザの開発者ツール → ネットワークタブで BFF へのリクエストに `traceparent` ヘッダが付与されること
   ```bash
   docker logs ec-otel-collector | grep frontend
   # ブラウザ発の span が受信されていること
   ```

7. **OTel Collector 障害耐性確認**
   ```bash
   docker compose stop otel-collector
   # アプリの主機能（商品表示・注文）が継続して動作すること
   docker compose start otel-collector
   ```

## Review Packet
### 変更サマリ（10行以内）
- OTel Collector / Prometheus / Grafana の設定ファイル群を `otel/` 配下に追加。
- `docker-compose.yml` に OTel スタック（collector/jaeger/prometheus/grafana）を追加し、backend/BFF/frontend の OTel 関連環境変数と依存関係を更新。
- customer-bff / backoffice-bff に OTel 依存、`src/tracing.ts`、Docker起動CMD（`--require ./dist/tracing`）を追加。
- 両BFFの CORS 許可ヘッダに `traceparent`/`tracestate` を追加。
- 両BFFの `TraceInterceptor` / `LoggingInterceptor` を OTel の active span 参照実装に更新。
- backend に OTel/Micrometer 依存を追加し、`application.yml` の tracing/metrics 設定を更新。
- backend の `TraceIdFilter` を削除し、`OrderUseCase`/`AuthService` にビジネスメトリクス（Counter/Timer）を追加。
- frontend に OTel 依存、`src/lib/tracing.ts`、`src/main.tsx` 先頭 import を追加。
- `order.created` の命名衝突（Prometheus 予約接尾辞 `_created`）を回避するため、`order.created.count` を採用。
- Grafana ダッシュボードの PromQL を実測メトリクス名（`order_created_count_total` 等）へ補正。

### 変更ファイル一覧
- docker-compose.yml
- otel/otel-collector-config.yaml
- otel/prometheus.yml
- otel/grafana/provisioning/datasources/datasource.yml
- otel/grafana/provisioning/dashboards/dashboard.yml
- otel/grafana/provisioning/dashboards/ec-business.json
- bff/customer-bff/package.json
- bff/customer-bff/src/tracing.ts
- bff/customer-bff/Dockerfile
- bff/customer-bff/src/main.ts
- bff/customer-bff/src/common/interceptors/trace.interceptor.ts
- bff/customer-bff/src/common/interceptors/logging.interceptor.ts
- bff/backoffice-bff/package.json
- bff/backoffice-bff/src/tracing.ts
- bff/backoffice-bff/Dockerfile
- bff/backoffice-bff/src/main.ts
- bff/backoffice-bff/src/common/interceptors/trace.interceptor.ts
- bff/backoffice-bff/src/common/interceptors/logging.interceptor.ts
- backend/pom.xml
- backend/src/main/resources/application.yml
- backend/src/main/java/com/example/aiec/config/TraceIdFilter.java
- backend/src/main/java/com/example/aiec/modules/purchase/application/usecase/OrderUseCase.java
- backend/src/main/java/com/example/aiec/modules/customer/domain/service/AuthService.java
- frontend/package.json
- frontend/src/lib/tracing.ts
- frontend/src/main.tsx

### リスクと未解決
- `[逸脱] T-20`: `order.created` は Prometheus で `order_total` に正規化されるため、`order.created.count` に変更。
- `[逸脱] T-3`: ダッシュボード式を `ec_*` から実メトリクス名へ変更（`order_created_count_total` / `order_creation_duration_seconds_bucket` など）。
- ブラウザ操作前提の手動確認（Jaeger UI でのトレース画面確認、フロント発 span の明示確認）は CLI では未実施。

### テスト結果
- [PASS] `docker compose up -d`
- [PASS] `docker compose build customer-bff backoffice-bff`
- [PASS] `cd backend && ./mvnw compile`
- [PASS] `cd frontend && npm run build`
- [PASS] `bash ./e2e/customer-smoke.sh`
- [PASS] `bash ./e2e/admin-smoke.sh`
- [PASS] OTel Collector 受信確認
  - `docker logs ec-otel-collector --since 5m` で traces/metrics export ログを確認
- [PASS] ログ相関確認
  - `traceparent=4bf92f3577b34da6a3ce929d0e0e4736...` を付与した `/api/products` で、`ec-customer-bff` と `ec-backend` の双方に同一 `traceId` を確認
- [PASS] ビジネスメトリクス確認
  - 注文作成後に `order_created_count_total=1` と `order_creation_duration_seconds_count=1` を `/actuator/prometheus` と Prometheus API で確認
- [PASS] Jaeger/Prometheus/Grafana の疎通確認
  - Jaeger `/api/services` で `backoffice-bff`, `customer-bff`, `ai-ec-backend` を確認
  - Prometheus `/-/healthy` が healthy
  - Grafana `/api/health` が `database: ok`
- [PASS] OTel Collector 障害耐性確認
  - `docker compose stop otel-collector` 中も `customer-bff /api/products` は `200` を維持
