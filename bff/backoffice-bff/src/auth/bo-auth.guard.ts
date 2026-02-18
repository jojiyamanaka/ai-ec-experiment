import { Injectable, CanActivate, ExecutionContext, HttpException, UnauthorizedException } from '@nestjs/common';
import { createHash } from 'crypto';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';

@Injectable()
export class BoAuthGuard implements CanActivate {
  constructor(
    private coreApiService: CoreApiService,
    private redisService: RedisService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest();
    const token = this.extractToken(request);

    if (!token) {
      throw new UnauthorizedException({
        success: false,
        error: {
          code: 'BFF_UNAUTHORIZED',
          message: '認証トークンが必要です',
        },
      });
    }

    try {
      const boUser = await this.verifyBoToken(token, request.traceId);
      request.boUser = boUser;
      request.token = token;
      return true;
    } catch (error) {
      if (error instanceof HttpException) {
        const status = error.getStatus();
        if (status === 503 || status === 504) {
          throw error;
        }
      }
      throw new UnauthorizedException({
        success: false,
        error: {
          code: 'BFF_INVALID_TOKEN',
          message: '無効なトークンです',
        },
      });
    }
  }

  private extractToken(request: any): string | null {
    const authHeader = request.headers['authorization'];
    if (!authHeader?.startsWith('Bearer ')) return null;
    return authHeader.substring(7);
  }

  private async verifyBoToken(token: string, traceId?: string): Promise<any> {
    const tokenHash = createHash('sha256').update(token).digest('hex').slice(0, 32);
    const cacheKey = `bo-auth:token:${tokenHash}`;

    // 1. キャッシュ確認
    const cached = await this.redisService.get<any>(cacheKey);
    if (cached) return cached;

    // 2. Core API 検証
    const response = await this.coreApiService.get<{ success: boolean; data?: any }>(
      '/api/bo-auth/me',
      token,
      traceId,
    );
    if (!response?.success || !response.data) throw new Error('invalid token');

    // 3. キャッシュ保存（TTL: 60秒）
    await this.redisService.set(cacheKey, response.data, 60);
    return response.data;
  }
}
