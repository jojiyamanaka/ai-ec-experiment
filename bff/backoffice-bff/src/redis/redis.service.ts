import { Injectable, Logger, OnModuleDestroy, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import Redis from 'ioredis';

@Injectable()
export class RedisService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(RedisService.name);
  private _client: Redis;

  constructor(private configService: ConfigService) {}

  onModuleInit() {
    const host = this.configService.get<string>('redis.host', 'localhost');
    const port = this.configService.get<number>('redis.port', 6379);
    this._client = new Redis({
      host,
      port,
      lazyConnect: true,
      connectTimeout: 5000,
      maxRetriesPerRequest: 1,
    });
    this._client.on('error', (err) =>
      this.logger.warn('Redis connection error', err.message),
    );
    this._client.connect().catch((err) =>
      this.logger.warn('Redis connect failed, running without cache', err.message),
    );
  }

  async onModuleDestroy() {
    await this._client.quit();
  }

  get client(): Redis {
    return this._client;
  }

  async get<T>(key: string): Promise<T | null> {
    try {
      const value = await this._client.get(key);
      return value ? (JSON.parse(value) as T) : null;
    } catch (err) {
      this.logger.warn(`Redis GET failed: ${key}`, err.message);
      return null;
    }
  }

  async set(key: string, value: unknown, ttlSeconds: number): Promise<void> {
    try {
      await this._client.setex(key, ttlSeconds, JSON.stringify(value));
    } catch (err) {
      this.logger.warn(`Redis SET failed: ${key}`, err.message);
    }
  }

  async del(...keys: string[]): Promise<void> {
    try {
      if (keys.length > 0) await this._client.del(...keys);
    } catch (err) {
      this.logger.warn(`Redis DEL failed: ${keys.join(',')}`, err.message);
    }
  }

  async delPattern(pattern: string): Promise<void> {
    try {
      const keys = await this._client.keys(pattern);
      if (keys.length > 0) await this._client.del(...keys);
    } catch (err) {
      this.logger.warn(`Redis delPattern failed: ${pattern}`, err.message);
    }
  }

  private static readonly INCR_SCRIPT = `
    local count = redis.call('INCR', KEYS[1])
    if count == 1 then
      redis.call('EXPIRE', KEYS[1], ARGV[1])
    end
    return count
  `;

  async incr(key: string, ttlSeconds: number): Promise<number> {
    try {
      const count = await this._client.eval(
        RedisService.INCR_SCRIPT, 1, key, String(ttlSeconds),
      ) as number;
      return count;
    } catch (err) {
      this.logger.warn(`Redis INCR failed: ${key}`, err.message);
      return 0;
    }
  }

  async ttl(key: string): Promise<number> {
    try {
      return await this._client.ttl(key);
    } catch {
      return -1;
    }
  }
}
