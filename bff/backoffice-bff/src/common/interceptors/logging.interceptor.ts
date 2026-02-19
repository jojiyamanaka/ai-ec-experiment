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
