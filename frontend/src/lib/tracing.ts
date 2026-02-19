import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { BatchSpanProcessor, WebTracerProvider } from '@opentelemetry/sdk-trace-web';
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
