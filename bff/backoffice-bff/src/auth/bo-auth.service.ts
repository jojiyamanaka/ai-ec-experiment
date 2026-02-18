import { Injectable } from '@nestjs/common';
import { createHash } from 'crypto';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class BoAuthService {
  constructor(
    private coreApiService: CoreApiService,
    private redisService: RedisService,
  ) {}

  async login(email: string, password: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.post(
      '/api/bo-auth/login',
      { email, password },
      undefined,
      traceId,
    );
  }

  async logout(token: string, traceId?: string): Promise<ApiResponse<void>> {
    const response = await this.coreApiService.post<ApiResponse<void>>(
      '/api/bo-auth/logout',
      {},
      token,
      traceId,
    );

    // ログアウト成功時、認証トークンキャッシュを削除
    if (response.success) {
      const tokenHash = createHash('sha256').update(token).digest('hex').slice(0, 32);
      await this.redisService.del(`bo-auth:token:${tokenHash}`);
    }

    return response;
  }
}
