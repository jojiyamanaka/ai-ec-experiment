import { Injectable, NestMiddleware } from '@nestjs/common';
import { Request, Response, NextFunction } from 'express';
import { v4 as uuidv4 } from 'uuid';
import { RedisService } from '../redis/redis.service';

const SESSION_TTL = 30 * 60; // 30分
const LOGGED_IN_TTL = 7 * 24 * 60 * 60; // 7日

@Injectable()
export class SessionMiddleware implements NestMiddleware {
  constructor(private redisService: RedisService) {}

  async use(req: Request, res: Response, next: NextFunction) {
    let sessionId = req.headers['x-session-id'] as string;

    if (!sessionId) {
      sessionId = uuidv4();
    }

    const sessionKey = `session:${sessionId}`;
    const now = Date.now();

    const existing = await this.redisService.get<any>(sessionKey);
    const session = existing ?? {
      sessionId,
      createdAt: now,
      ip: req.ip,
      userAgent: req.headers['user-agent'] ?? '',
    };

    session.lastAccessAt = now;

    // ログイン済みユーザーは TTL を延長（7日）
    const ttl = session.userId ? LOGGED_IN_TTL : SESSION_TTL;
    await this.redisService.set(sessionKey, session, ttl);

    (req as any)['session'] = session;
    res.setHeader('X-Session-Id', sessionId);
    next();
  }
}
