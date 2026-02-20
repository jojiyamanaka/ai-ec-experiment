import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { RedisModule } from '../redis/redis.module';
import { ProductsController } from './products.controller';
import { ProductsService } from './products.service';

@Module({
  imports: [CoreApiModule, RedisModule],
  controllers: [ProductsController],
  providers: [ProductsService],
})
export class ProductsModule {}
