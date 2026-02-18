import { SetMetadata } from '@nestjs/common';
import { RateLimitConfig, RATE_LIMIT_KEY } from '../guards/rate-limit.guard';

export const RateLimit = (config: RateLimitConfig) => SetMetadata(RATE_LIMIT_KEY, config);
