import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import configuration from './config/configuration';
import { CoreApiModule } from './core-api/core-api.module';
import { RedisModule } from './redis/redis.module';
import { BoAuthModule } from './auth/bo-auth.module';
import { InventoryModule } from './inventory/inventory.module';
import { OrdersModule } from './orders/orders.module';
import { MembersModule } from './members/members.module';
import { BoUsersModule } from './bo-users/bo-users.module';
import { ProductsModule } from './products/products.module';
import { HealthController } from './health/health.controller';

@Module({
  controllers: [HealthController],
  imports: [
    ConfigModule.forRoot({
      load: [configuration],
      isGlobal: true,
    }),
    RedisModule,
    CoreApiModule,
    BoAuthModule,
    InventoryModule,
    OrdersModule,
    MembersModule,
    BoUsersModule,
    ProductsModule,
  ],
})
export class AppModule {}
