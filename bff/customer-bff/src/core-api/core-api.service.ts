import { Injectable, HttpException, Logger } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { firstValueFrom, timeout, retry } from 'rxjs';

@Injectable()
export class CoreApiService {
  private readonly logger = new Logger(CoreApiService.name);
  private readonly baseUrl: string;
  private readonly timeoutMs: number;
  private readonly retryCount: number;

  constructor(
    private httpService: HttpService,
    private configService: ConfigService,
  ) {
    this.baseUrl = this.getRequiredString('coreApi.url');
    this.timeoutMs = this.getRequiredNumber('coreApi.timeout');
    this.retryCount = this.getRequiredNumber('coreApi.retry');
  }

  async get<T>(
    path: string,
    token?: string,
    traceId?: string,
    extraHeaders: Record<string, string> = {},
  ): Promise<T> {
    try {
      const response = await firstValueFrom(
        this.httpService.get(`${this.baseUrl}${path}`, {
          headers: this.buildHeaders(token, traceId, extraHeaders),
        }).pipe(
          timeout(this.timeoutMs),
          retry({
            count: this.retryCount,
            delay: 1000,
          }),
        ),
      );
      return response.data;
    } catch (error) {
      this.handleError(error, path, 'GET');
    }
  }

  async post<T>(
    path: string,
    data: any,
    token?: string,
    traceId?: string,
    extraHeaders: Record<string, string> = {},
  ): Promise<T> {
    try {
      const response = await firstValueFrom(
        this.httpService.post(`${this.baseUrl}${path}`, data, {
          headers: this.buildHeaders(token, traceId, extraHeaders),
        }).pipe(
          timeout(this.timeoutMs),
        ),
      );
      return response.data;
    } catch (error) {
      this.handleError(error, path, 'POST');
    }
  }

  async put<T>(
    path: string,
    data: any,
    token?: string,
    traceId?: string,
    extraHeaders: Record<string, string> = {},
  ): Promise<T> {
    try {
      const response = await firstValueFrom(
        this.httpService.put(`${this.baseUrl}${path}`, data, {
          headers: this.buildHeaders(token, traceId, extraHeaders),
        }).pipe(
          timeout(this.timeoutMs),
        ),
      );
      return response.data;
    } catch (error) {
      this.handleError(error, path, 'PUT');
    }
  }

  async delete<T>(
    path: string,
    token?: string,
    traceId?: string,
    extraHeaders: Record<string, string> = {},
  ): Promise<T> {
    try {
      const response = await firstValueFrom(
        this.httpService.delete(`${this.baseUrl}${path}`, {
          headers: this.buildHeaders(token, traceId, extraHeaders),
        }).pipe(
          timeout(this.timeoutMs),
        ),
      );
      return response.data;
    } catch (error) {
      this.handleError(error, path, 'DELETE');
    }
  }

  private buildHeaders(
    token?: string,
    traceId?: string,
    extraHeaders: Record<string, string> = {},
  ): Record<string, string> {
    const headers: Record<string, string> = { ...extraHeaders };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    if (traceId) headers['X-Trace-Id'] = traceId;
    return headers;
  }

  private getRequiredString(key: string): string {
    const value = this.configService.get<string>(key);
    if (!value) {
      throw new Error(`Missing config: ${key}`);
    }
    return value;
  }

  private getRequiredNumber(key: string): number {
    const value = this.configService.get<number>(key);
    if (value === undefined || Number.isNaN(value)) {
      throw new Error(`Missing or invalid config: ${key}`);
    }
    return value;
  }

  private handleError(error: any, path: string, method: string): never {
    this.logger.error(`Core API error: ${method} ${path}`, error);

    if (error.response) {
      const statusCode = error.response.status;
      const responseData = error.response.data;

      if (responseData?.success === false && responseData?.error) {
        throw new HttpException(responseData, statusCode);
      }

      throw new HttpException({
        success: false,
        error: {
          code: 'BFF_CORE_API_ERROR',
          message: responseData?.message || 'Core APIエラー',
        },
      }, statusCode);
    } else if (this.isUnavailableError(error)) {
      throw new HttpException({
        success: false,
        error: {
          code: 'BFF_CORE_API_UNAVAILABLE',
          message: 'サービスが一時的に利用できません',
        },
      }, 503);
    } else if (error.name === 'TimeoutError') {
      throw new HttpException({
        success: false,
        error: {
          code: 'BFF_CORE_API_TIMEOUT',
          message: 'リクエストがタイムアウトしました',
        },
      }, 504);
    } else {
      throw new HttpException({
        success: false,
        error: {
          code: 'BFF_INTERNAL_ERROR',
          message: '予期しないエラーが発生しました',
        },
      }, 500);
    }
  }

  private isUnavailableError(error: any): boolean {
    const code = error?.code ?? error?.cause?.code;
    return code === 'ECONNREFUSED'
      || code === 'ENOTFOUND'
      || code === 'EAI_AGAIN';
  }
}
