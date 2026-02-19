# CHG-015: OpenTelemetry 導入 - 技術設計

要件: `docs/01_requirements/CHG-015_OpenTelemetry 導入.md`
作成日: 2026-02-18

---

## 1. 設計方針

### 基本原則

- **計測と保存・表示の分離**: OTel SDK（計測）→ OTel Collector（ルーティング）→ Jaeger/Prometheus/Grafana（保存・表示）の3層構成
- **標準プロトコル採用**: W3C Trace Context（`traceparent` ヘッダ）でレイヤ間を伝播。既存の独自 `X-Trace-Id` ヘッダを置換
- **自動計装優先**: HTTP、DB などの基盤レイヤは各SDKの自動計装に任せ、ビジネス固有メトリクスのみ手動実装
- **開発環境での最小動作**: docker compose up 一発で分散トレーシング体験が得られる構成

### エクスポートプロトコル方針

| 送信元 | 宛先 | プロトコル | ポート | 理由 |
|------|-----|--------|------|------|
| Browser (React) | OTel Collector | OTLP/**HTTP** | **4318** | ブラウザはgRPC非対応 |
| NestJS BFF | OTel Collector | OTLP/**gRPC** | **4317** | サーバ間はgRPC推奨（効率・多重化） |
| Spring Boot | OTel Collector | OTLP/**gRPC** | **4317** | 同上 |
| OTel Collector | Jaeger | OTLP/**gRPC** | 4317(Jaeger内部) | サーバ間はgRPC |

> **NestJS と Spring Boot で環境変数 `OTEL_EXPORTER_OTLP_ENDPOINT` を直接共有しない。**
> ブラウザ向け（HTTP、4318）とサーバ向け（gRPC、4317）でポートとエクスポーターライブラリが異なるため、
> 各サービスに専用の変数を設定する（後述）。

### ログとトレースの相関方針

- **NestJS BFF**: `LoggingInterceptor` で OTel コンテキストから `traceId` / `spanId` を取得してログに付与。OTel SDK が初期化されていなければ空文字（fallback UUID は廃止）
- **Spring Boot**: Micrometer Tracing が MDC に `traceId` / `spanId` を自動注入。既存のログパターンを継続利用

---

## 2. アーキテクチャ概要

```
Browser (React)
  │  traceparent ヘッダ + OTLP/HTTP
  │  ↓ port 4318（ブラウザはgRPC不可のためHTTP固定）
  ▼
┌─────────────────────────────────────┐
│ OTel Collector                       │
│ recv: gRPC(:4317) + HTTP(:4318)      │
│ export: gRPC → Jaeger                │
│          Prometheus pull ← :8889     │
└───────────────┬─────────────────────┘
                │ ↑ OTLP/gRPC port 4317（サーバ間）
  ┌─────────────┴──────────────┐
  ▼                            ▼
Customer BFF (NestJS)    BackOffice BFF (NestJS)
  │ traceparent ヘッダ伝播
  ▼
Core API (Spring Boot)
  │ JDBC span
  ▼
PostgreSQL

Collector
  ├─ OTLP/gRPC → Jaeger(:16686)
  └─ Prometheus pull(:8889) ← Prometheus(:9090) → Grafana(:3000)
```

---

## 3. インフラ実装（docker-compose.yml）

### 追加サービス

```yaml
  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.116.0
    container_name: ec-otel-collector
    command: ["--config=/etc/otel/config.yaml"]
    volumes:
      - ./otel/otel-collector-config.yaml:/etc/otel/config.yaml
    ports:
      - "4317:4317"   # OTLP gRPC 受信（NestJS BFF / Spring Boot → Collector）
      - "4318:4318"   # OTLP HTTP 受信（ブラウザ → Collector）
      # 8888(Collector自己メトリクス) / 8889(Prometheusエクスポーター) は内部ネットワークのみ
    networks:
      - public     # ブラウザ(4318)からのアクセスに必要
      - internal   # バックエンドサービスとの通信

  jaeger:
    image: jaegertracing/all-in-one:1.63
    container_name: ec-jaeger
    environment:
      - COLLECTOR_OTLP_ENABLED=true   # Jaeger の OTLP gRPC 受信を有効化
    ports:
      - "16686:16686"  # Jaeger UI
      # 4317(OTLP gRPC)はCollectorからのみ接続。ホスト非公開
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

`volumes` セクションに `grafana-data:` を追加する。

### 新規ファイル: otel/otel-collector-config.yaml

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317   # NestJS BFF / Spring Boot からの受信
      http:
        endpoint: 0.0.0.0:4318   # ブラウザからの受信
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
  # Collector → Jaeger: OTLP gRPC（サーバ間はgRPC）
  # "otlp/jaeger" はラベル。実態は otlp exporter の別名インスタンス
  otlp/jaeger:
    endpoint: jaeger:4317    # ホスト名のみ。gRPCはパス不要
    tls:
      insecure: true         # 内部ネットワークのためTLS不要

  # Prometheusがpullで収集するエンドポイント
  # 8888(Collector自己メトリクス) とは別ポート
  prometheus:
    endpoint: "0.0.0.0:8889"
    namespace: "ec"

  debug:
    verbosity: basic

service:
  # Collector自身のメトリクスは 8888 で公開（Prometheus の任意スクレイプ対象）
  telemetry:
    metrics:
      address: 0.0.0.0:8888

  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp/jaeger, debug]
    # NestJS BFF / Browser からの OTLP メトリクスのみ受信
    # Spring Boot のメトリクスは /actuator/prometheus を Prometheus が直接スクレイプするため
    # このパイプラインを通らない（prometheus.yml の spring-actuator ジョブを参照）
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [prometheus, debug]
    logs:
      receivers: [otlp]
      processors: [batch]
      exporters: [debug]
```

**ポートまとめ（Collector）**:

| ポート | 用途 | 方向 |
|------|------|------|
| 4317 | OTLP gRPC 受信（BFF トレース / Spring Boot トレース） | IN |
| 4318 | OTLP HTTP 受信（ブラウザ トレース＋メトリクス） | IN |
| 8888 | Collector **自身**のメトリクス（内部のみ） | OUT（Prometheus pull） |
| 8889 | **NestJS/Browser** メトリクスの Prometheus エンドポイント（Spring Boot は通らない） | OUT（Prometheus pull） |

### 新規ファイル: otel/prometheus.yml

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  # OTel CollectorがアプリからOTLPで受け取ったメトリクスを公開するエンドポイント
  - job_name: 'otel-collector-app-metrics'
    static_configs:
      - targets: ['otel-collector:8889']

  # Spring Boot Actuator の Prometheus エンドポイントを直接スクレイプ
  - job_name: 'spring-actuator'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
```

---

## 4. BFF 実装（NestJS）

customer-bff / backoffice-bff で同一パターンを適用する。以下は customer-bff を例示。

### 4-1. パッケージ追加（bff/customer-bff/package.json）

```json
"dependencies": {
  "@opentelemetry/sdk-node": "^0.57.0",
  "@opentelemetry/auto-instrumentations-node": "^0.54.0",
  "@opentelemetry/exporter-trace-otlp-grpc": "^0.57.0",
  "@opentelemetry/exporter-metrics-otlp-grpc": "^0.57.0",
  "@opentelemetry/resources": "^1.29.0",
  "@opentelemetry/semantic-conventions": "^1.29.0"
}
```

> **HTTP エクスポーターではなく gRPC エクスポーターを使用する。**
> `OTEL_EXPORTER_OTLP_ENDPOINT` はサーバ間（gRPC、port 4317）。
> ブラウザ用の HTTP エクスポーター（port 4318）とは別ライブラリ。

### 4-2. 新規ファイル: bff/customer-bff/src/tracing.ts

OTel SDK は **NestFactory より前に初期化** する必要があるため、エントリポイント分離パターンを使用する（Dockerfile の `--require` 経由でロード）。

```typescript
import { NodeSDK } from '@opentelemetry/sdk-node';
import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-grpc';
import { OTLPMetricExporter } from '@opentelemetry/exporter-metrics-otlp-grpc';
import { PeriodicExportingMetricReader } from '@opentelemetry/sdk-metrics';
import { Resource } from '@opentelemetry/resources';
import { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION } from '@opentelemetry/semantic-conventions';

// gRPC exporter の url はパス不要（gRPC はサービス名でルーティング）
// スキームの扱いはライブラリバージョンで挙動が変わることがある:
//   http://otel-collector:4317  → insecure gRPC として解釈（推奨。まずこちらで試す）
//   otel-collector:4317         → スキームなし。接続できない場合はこちらを試す
// 接続失敗時はまず url からスキームを除いた形式に変更する
const grpcEndpoint = process.env.OTEL_EXPORTER_OTLP_GRPC_ENDPOINT
  || 'http://localhost:4317';

const sdk = new NodeSDK({
  resource: new Resource({
    [ATTR_SERVICE_NAME]: process.env.OTEL_SERVICE_NAME || 'customer-bff',
    [ATTR_SERVICE_VERSION]: '1.0.0',
  }),
  traceExporter: new OTLPTraceExporter({
    url: grpcEndpoint,   // gRPC: ホスト:ポートのみ。/v1/traces は不要
  }),
  metricReader: new PeriodicExportingMetricReader({
    exporter: new OTLPMetricExporter({
      url: grpcEndpoint,  // traces と同じ gRPC エンドポイントを使用
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

### 4-3. Dockerfile の CMD 変更

```dockerfile
# 変更前
CMD ["node", "dist/main.js"]

# 変更後（tracing.ts を --require でアプリより先にロード）
CMD ["node", "--require", "./dist/tracing", "dist/main"]
```

### 4-4. LoggingInterceptor の更新

OTel コンテキストから trace_id を取得してログに付与する。
**uuidv4() の fallback は廃止**。OTel SDK が未初期化の場合にランダム UUID を生成しても
他サービスのトレースと相関できず、むしろ障害を隠す原因になる。

```typescript
// 変更前（抜粋）
this.logger.log(JSON.stringify({
  traceId,   // request から取得（UUID fallback あり）
  method, url, statusCode, duration,
}));

// 変更後（抜粋）
import { trace } from '@opentelemetry/api';

const span = trace.getActiveSpan();
const spanCtx = span?.spanContext();
// OTel初期化失敗時は空文字。UUID fallbackは廃止（相関不能な偽IDは有害）
const traceId = spanCtx?.traceId ?? '';
const spanId = spanCtx?.spanId ?? '';

this.logger.log(JSON.stringify({
  traceId,
  spanId,
  method, url, statusCode, duration,
}));
```

### 4-5. TraceInterceptor の変更

OTel SDK が `traceparent` ヘッダを自動処理する。カスタム UUID 生成は廃止し、
OTel の trace_id を `request.traceId` に転写するだけにする。

```typescript
// 変更後
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

### 4-6. docker-compose.yml の環境変数追加

```yaml
customer-bff:
  environment:
    - OTEL_EXPORTER_OTLP_GRPC_ENDPOINT=http://otel-collector:4317
    - OTEL_SERVICE_NAME=customer-bff
    # OTEL_EXPORTER_OTLP_ENDPOINT は使用しない（gRPC/HTTP混在時の混乱回避）

backoffice-bff:
  environment:
    - OTEL_EXPORTER_OTLP_GRPC_ENDPOINT=http://otel-collector:4317
    - OTEL_SERVICE_NAME=backoffice-bff
```

---

## 5. バックエンド実装（Spring Boot）

### 5-1. pom.xml への依存追加

```xml
<!-- Micrometer Tracing → OTel Bridge -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- OTel OTLP Exporter（gRPC版）-->
<!-- spring-boot-starter-parent が opentelemetry.version を管理 -->
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

### 5-2. application.yml への設定追加

> **注意**: `management.otlp.tracing.endpoint` のプロトコルと URL フォーマットはトランスポートで異なる。
> - gRPC: `http://host:4317`（パスなし）
> - HTTP: `http://host:4318/v1/traces`（パスあり）
>
> Spring Boot 3.3+ は `management.otlp.tracing.transport=grpc` をサポート。
> 誤って HTTP ポート（4318）に gRPC 接続、または gRPC ポート（4317）に HTTP パスを付けると
> 接続エラーまたはサイレントな送信失敗が起きる。

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 開発環境は全件サンプリング
  otlp:
    tracing:
      endpoint: http://localhost:4317   # gRPC: パスなし、ポート4317
      transport: grpc                   # Spring Boot 3.3+ でgRPC指定
  endpoints:
    web:
      exposure:
        include: health,prometheus,info
```

production-internal プロファイルに追加:
```yaml
management:
  tracing:
    sampling:
      probability: 0.1   # 本番は10%サンプリング（要件に応じて調整）
  otlp:
    tracing:
      endpoint: http://otel-collector:4317   # gRPC: パスなし
      transport: grpc
```

**メトリクスは Prometheus スクレイプ方式**（OTel Collector 経由の OTLP は不使用）:
```yaml
management:
  prometheus:
    metrics:
      export:
        enabled: true
```
`prometheus.yml` が `backend:8080/actuator/prometheus` を直接スクレイプする。

### 5-3. TraceIdFilter の廃止

Micrometer Tracing が `traceparent` ヘッダを自動処理し MDC に `traceId` / `spanId` を注入するため削除する。

```java
// 削除対象: backend/src/main/java/com/example/aiec/config/TraceIdFilter.java
```

ログパターンは `spanId` を追加するだけで変更最小:

```yaml
# 変更前
console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [traceId=%X{traceId}] - %msg%n"
# 変更後（Micrometer Tracing が traceId/spanId を MDC に注入）
console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [traceId=%X{traceId},spanId=%X{spanId}] - %msg%n"
```

### 5-4. ビジネスメトリクス（Counter + Timer）

**Counter** でイベント発生数を、**Timer**（= Histogram + Counter）で処理時間を計測する。
Timer は Prometheus に `_seconds_bucket`, `_seconds_count`, `_seconds_sum` として展開され、
Grafana で `histogram_quantile(0.95, ...)` による p95/p99 計算が可能になる。

**実装箇所**: `OrderUseCase.java`

```java
// フィールド
private final Counter orderCreatedCounter;
private final Counter orderCreationFailedCounter;
private final Counter inventoryReservationFailedCounter;
private final Timer orderCreationTimer;

// コンストラクタ内
this.orderCreatedCounter = Counter.builder("order.created")
    .description("注文成功数").register(meterRegistry);

this.orderCreationFailedCounter = Counter.builder("order.creation.failed")
    .description("注文失敗数").register(meterRegistry);

this.inventoryReservationFailedCounter = Counter.builder("inventory.reservation.failed")
    .description("在庫引当失敗数").register(meterRegistry);

// Timer = Histogram + Counter。p95/p99 計算に使用
this.orderCreationTimer = Timer.builder("order.creation.duration")
    .description("注文作成処理時間")
    .publishPercentileHistogram(true)   // Prometheus の bucket を有効化
    .register(meterRegistry);

// 注文処理の計測
public OrderDto createOrder(...) {
    return orderCreationTimer.recordCallable(() -> {
        try {
            // ... 注文ロジック ...
            orderCreatedCounter.increment();
            return result;
        } catch (InsufficientStockException e) {
            inventoryReservationFailedCounter.increment();
            orderCreationFailedCounter.increment();
            throw e;
        }
    });
}
```

**実装箇所**: `AuthService.java`

```java
private final Counter loginFailedCounter;

this.loginFailedCounter = Counter.builder("auth.login.failed")
    .tag("type", "customer")
    .register(meterRegistry);
```

### 5-5. docker-compose.yml の環境変数追加

```yaml
backend:
  environment:
    - MANAGEMENT_OTLP_TRACING_ENDPOINT=http://otel-collector:4317
    - MANAGEMENT_OTLP_TRACING_TRANSPORT=grpc
    # OTEL_EXPORTER_OTLP_ENDPOINT は設定しない
    # Spring Boot は management.otlp.* プロパティで制御する
```

---

## 6. フロントエンド実装（React）

### 6-1. ブラウザからCollectorへの直接送信について

> **ローカル開発専用設計**。ブラウザが `http://localhost:4318` へ直接 OTLP を送信する構成は
> 本番環境では非推奨（Collector の公開・CORS 対応が必要）。
> 将来の本番化では「BFF に `/v1/traces` プロキシエンドポイントを追加し、
> BFF が gRPC で Collector へ転送する」構成に移行する。

### 6-2. パッケージ追加（frontend/package.json）

```json
"dependencies": {
  "@opentelemetry/sdk-web": "^1.29.0",
  "@opentelemetry/exporter-trace-otlp-http": "^0.57.0",
  "@opentelemetry/instrumentation-fetch": "^0.57.0",
  "@opentelemetry/instrumentation-document-load": "^0.44.0",
  "@opentelemetry/resources": "^1.29.0",
  "@opentelemetry/semantic-conventions": "^1.29.0"
}
```

### 6-3. 新規ファイル: frontend/src/lib/tracing.ts

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
// VITE_OTEL_ENDPOINT はブラウザから見えるアドレス（localhost:4318）
const otlpHttpEndpoint = import.meta.env.VITE_OTEL_ENDPOINT || 'http://localhost:4318';

const provider = new WebTracerProvider({
  resource: new Resource({
    [ATTR_SERVICE_NAME]: `frontend-${APP_MODE}`,
  }),
  spanProcessors: [
    new BatchSpanProcessor(
      new OTLPTraceExporter({
        // HTTP exporter は url にシグナル別パスを明示する
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
        /localhost:300[12]/,  // Customer/BackOffice BFF への fetch に traceparent を付与
      ],
    }),
    new DocumentLoadInstrumentation(),
  ],
});
```

### 6-4. main.tsx の変更

```typescript
// 変更後（tracing の初期化を先頭に追加）
import './lib/tracing'  // OTel初期化（副作用インポート。他のimportより前に置く）
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
```

### 6-5. BFF の CORS 設定更新（customer-bff/src/main.ts）

フロントエンドからの `traceparent` / `tracestate` ヘッダを許可する。

```typescript
app.enableCors({
  origin: 'http://localhost:5173',
  credentials: true,
  allowedHeaders: [
    'Content-Type', 'Authorization', 'X-Session-Id',
    'traceparent', 'tracestate',  // W3C Trace Context ヘッダを追加
  ],
});
```

### 6-6. 環境変数追加（docker-compose.yml）

```yaml
frontend-customer:
  environment:
    - VITE_OTEL_ENDPOINT=http://localhost:4318  # ブラウザから見えるアドレス（内部ネットワーク名不可）

frontend-admin:
  environment:
    - VITE_OTEL_ENDPOINT=http://localhost:4318
```

---

## 7. 処理フロー

### 注文作成のトレース伝播

```
Browser (React)
  span: "POST /api/orders"  [traceId=abc123, spanId=aaa111]
  traceparent: 00-abc123-aaa111-01 を fetch ヘッダに付与
  OTLP/HTTP → Collector:4318
     │
     ▼
Customer BFF (NestJS)
  span: "POST /api/orders" (子span, 同一traceId=abc123, spanId=bbb222)
  traceparent: 00-abc123-bbb222-01 を Core APIへ転送
  OTLP/gRPC → Collector:4317
     │
     ▼
Core API (Spring Boot)
  span: "POST /api/orders" (子span, traceId=abc123, spanId=ccc333)
  子span: "OrderUseCase.createOrder"
  子span: "InventoryUseCase.reserve" → DB span (JDBC)
  OTLP/gRPC → Collector:4317

OTel Collector → Jaeger
  → Jaeger UI で traceId=abc123 を検索すると全レイヤの span が1画面に表示
```

### ログ相関の例

```
# Customer BFF ログ
{"traceId":"abc123","spanId":"bbb222","method":"POST","url":"/api/orders","statusCode":200,"duration":234}

# Core API ログ
2026-02-18 10:00:00 [http-nio-8080-exec-1] INFO OrderController [traceId=abc123,spanId=ccc333] - 注文作成開始

→ traceId=abc123 でJaeger・BFFログ・CoreAPIログを横断検索可能
```

---

## 8. Grafana ダッシュボード設計

### プロビジョニングファイル構成

```
otel/grafana/provisioning/
  datasources/
    datasource.yml    # Prometheus, Jaeger データソース定義
  dashboards/
    dashboard.yml     # ダッシュボード自動読み込み設定
    ec-business.json  # ビジネスメトリクスダッシュボード
```

### ビジネスメトリクスダッシュボード

| パネル | Prometheus クエリ | 表示形式 |
|------|---------|--------|
| 注文成功率 | `rate(ec_order_created_total[5m]) / (rate(ec_order_created_total[5m]) + rate(ec_order_creation_failed_total[5m]))` | Stat |
| 在庫引当失敗率 | `rate(ec_inventory_reservation_failed_total[5m])` | 時系列 |
| 注文処理 p95 レイテンシ | `histogram_quantile(0.95, rate(ec_order_creation_duration_seconds_bucket[5m]))` | 時系列 |
| 注文処理 p99 レイテンシ | `histogram_quantile(0.99, rate(ec_order_creation_duration_seconds_bucket[5m]))` | 時系列 |
| HTTP p95 レイテンシ（BFF） | `histogram_quantile(0.95, rate(http_server_request_duration_seconds_bucket{job="customer-bff"}[5m]))` | 時系列 |
| 認証失敗数 | `rate(ec_auth_login_failed_total[5m])` | 時系列 |

> p95/p99 は Timer（`publishPercentileHistogram=true`）によって生成される `_seconds_bucket` から算出。
> Counter だけでは実現不可能なため、注文処理には必ず Timer を使用すること。

---

## 9. 既存パターンとの整合性

| 項目 | 変更前 | 変更後 |
|-----|------|------|
| トレース伝播 | カスタム `X-Trace-Id`（UUID） | W3C `traceparent`（OTel標準） |
| BFF TraceInterceptor | UUID生成 + `X-Trace-Id` ヘッダ設定 | OTel trace_id を `request.traceId` に転写のみ。UUID fallback 廃止 |
| BFF LoggingInterceptor | `request.traceId` を参照（UUID fallback あり） | OTel API から取得。fallback は空文字 |
| Spring TraceIdFilter | `X-Trace-Id` を MDC に手動設定 | 削除（Micrometer Tracing が自動注入） |
| Spring ログパターン | `[traceId=%X{traceId}]` | `[traceId=%X{traceId},spanId=%X{spanId}]` |
| モニタリング | なし | Jaeger（トレース）+ Prometheus + Grafana（メトリクス）|

---

## 10. テスト観点

| 観点 | 確認方法 |
|-----|---------|
| 分散トレースの連結 | checkout API をリクエストし、Jaeger UI（localhost:16686）で全レイヤが1トレースに含まれること |
| gRPC 接続確認 | `docker logs ec-otel-collector` でBFF/CoreAPIからのspanが受信されていること |
| ログとトレースの相関 | BFF/CoreAPI ログの `traceId` が Jaeger の trace_id と一致すること |
| ビジネスメトリクス収集 | 注文を作成し Grafana/Prometheus で `ec_order_created_total` が増加すること |
| Timer Histogram 生成 | `ec_order_creation_duration_seconds_bucket` が Prometheus に存在すること |
| 在庫不足メトリクス | 在庫0の商品を注文し `ec_inventory_reservation_failed_total` が増加すること |
| フロントエンドトレース | ブラウザ devtools のネットワークタブで `traceparent` ヘッダが BFF リクエストに付与されること |
| Browser OTLP 送信 | `docker logs ec-otel-collector` でブラウザ発の span が受信されていること |
| OTel Collector 障害時 | Collector停止でアプリの主機能（注文・商品表示）が停止しないこと |

---

## 11. 影響範囲

### 変更ファイル一覧

**新規作成**
- `otel/otel-collector-config.yaml`
- `otel/prometheus.yml`
- `otel/grafana/provisioning/datasources/datasource.yml`
- `otel/grafana/provisioning/dashboards/dashboard.yml`
- `otel/grafana/provisioning/dashboards/ec-business.json`
- `bff/customer-bff/src/tracing.ts`
- `bff/backoffice-bff/src/tracing.ts`
- `frontend/src/lib/tracing.ts`

**変更**
- `docker-compose.yml`（サービス追加・環境変数追加）
- `bff/customer-bff/package.json`（OTel gRPC 依存追加）
- `bff/customer-bff/Dockerfile`（CMD に `--require ./dist/tracing` 追加）
- `bff/customer-bff/src/main.ts`（CORS に `traceparent`/`tracestate` 追加）
- `bff/customer-bff/src/common/interceptors/trace.interceptor.ts`（UUID生成廃止、OTel転写のみ）
- `bff/customer-bff/src/common/interceptors/logging.interceptor.ts`（OTel API統合、UUID fallback廃止）
- `bff/backoffice-bff/package.json` / `Dockerfile` / `src/main.ts` / interceptors（同上）
- `backend/pom.xml`（`micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`, `micrometer-registry-prometheus` 追加）
- `backend/src/main/resources/application.yml`（`management.otlp.tracing.*` 設定追加）
- `backend/src/main/java/com/example/aiec/modules/purchase/application/usecase/OrderUseCase.java`（Counter + Timer 追加）
- `backend/src/main/java/com/example/aiec/modules/customer/domain/service/AuthService.java`（Counter 追加）
- `frontend/package.json`（OTel HTTP 依存追加）
- `frontend/src/main.tsx`（`import './lib/tracing'` を先頭に追加）

**削除**
- `backend/src/main/java/com/example/aiec/config/TraceIdFilter.java`（Micrometer Tracing が代替）

### 影響なし

- `frontend/src/lib/api.ts`（変更不要。FetchInstrumentation が自動計装）
- BFF のコントローラ・サービス実装（インターセプタのみ変更）
- DB スキーマ
- Spring Boot のコントローラ・ドメインロジック（ビジネスメトリクス追加箇所を除く）
