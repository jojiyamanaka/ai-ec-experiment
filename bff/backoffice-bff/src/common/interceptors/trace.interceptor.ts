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
