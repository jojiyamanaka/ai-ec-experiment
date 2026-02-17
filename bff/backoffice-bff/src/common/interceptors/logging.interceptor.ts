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
