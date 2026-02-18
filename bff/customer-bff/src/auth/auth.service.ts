import { Injectable } from '@nestjs/common';
import { createHash } from 'crypto';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class AuthService {
  constructor(
    private coreApiService: CoreApiService,
    private redisService: RedisService,
  ) {}

  async register(
    email: string,
    displayName: string,
    password: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    return this.coreApiService.post(
      '/api/auth/register',
      { email, displayName, password },
      undefined,
      traceId,
    );
  }

  async login(
    email: string,
    password: string,
    traceId?: string,
    sessionId?: string,
  ): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.post<ApiResponse<any>>(
      '/api/auth/login',
      { email, password },
      undefined,
      traceId,
    );

    // ログイン成功時、セッションにユーザーIDを紐付ける
    if (response.success && sessionId) {
      const sessionKey = `session:${sessionId}`;
      const session = await this.redisService.get<any>(sessionKey) ?? {};
      session.userId = response.data?.user?.id;
      await this.redisService.set(sessionKey, session, 7 * 24 * 60 * 60);
    }

    return response;
  }

  async logout(token: string, traceId?: string): Promise<ApiResponse<void>> {
    const response = await this.coreApiService.post<ApiResponse<void>>(
      '/api/auth/logout',
      {},
      token,
      traceId,
    );

    // ログアウト成功時、認証トークンキャッシュを削除
    if (response.success) {
      const tokenHash = createHash('sha256').update(token).digest('hex').slice(0, 32);
      await this.redisService.del(`auth:token:${tokenHash}`);
    }

    return response;
  }
}
