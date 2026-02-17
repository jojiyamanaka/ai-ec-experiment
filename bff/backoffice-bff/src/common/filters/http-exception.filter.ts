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

    const message = typeof exceptionResponse === 'string'
      ? exceptionResponse
      : typeof exceptionResponse === 'object' && exceptionResponse !== null && 'message' in exceptionResponse
        ? String((exceptionResponse as { message?: unknown }).message ?? '予期しないエラー')
        : '予期しないエラー';

    // それ以外はBFF形式にラップ
    response.status(status).json({
      success: false,
      error: {
        code: 'BFF_INTERNAL_ERROR',
        message,
      },
    });
  }
}
