import { CanActivate, ExecutionContext, HttpException, Injectable } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { RedisService } from '../../redis/redis.service';

export interface RateLimitConfig {
  limit: number;
  ttlSeconds: number;
  keyType: 'ip' | 'user';
}

export const RATE_LIMIT_KEY = 'rateLimit';

@Injectable()
export class RateLimitGuard implements CanActivate {
  constructor(
    private reflector: Reflector,
    private redisService: RedisService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const config = this.reflector.get<RateLimitConfig>(RATE_LIMIT_KEY, context.getHandler())
      ?? { limit: 100, ttlSeconds: 60, keyType: 'ip' };

    const request = context.switchToHttp().getRequest();
    const identifier = config.keyType === 'user'
      ? `user:${request.boUser?.id ?? 'anonymous'}`
      : `ip:${request.ip}`;
    const endpoint = context.getHandler().name;
    const key = `ratelimit:${identifier}:${endpoint}`;

    const count = await this.redisService.incr(key, config.ttlSeconds);
    if (count === 0) return true;

    const remaining = Math.max(0, config.limit - count);
    const resetAt = Math.floor(Date.now() / 1000) + config.ttlSeconds;

    const response = context.switchToHttp().getResponse();
    response.setHeader('X-RateLimit-Limit', config.limit);
    response.setHeader('X-RateLimit-Remaining', remaining);
    response.setHeader('X-RateLimit-Reset', resetAt);

    if (count > config.limit) {
      throw new HttpException(
        {
          success: false,
          error: {
            code: 'RATE_LIMIT_EXCEEDED',
            message: 'リクエスト制限を超えました。しばらくしてから再試行してください。',
            retryAfter: config.ttlSeconds,
          },
        },
        429,
      );
    }
    return true;
  }
}
