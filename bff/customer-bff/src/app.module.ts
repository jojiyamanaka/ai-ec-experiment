import { MiddlewareConsumer, Module, NestModule } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { ConfigModule } from '@nestjs/config';
import configuration from './config/configuration';
import { CoreApiModule } from './core-api/core-api.module';
import { RedisModule } from './redis/redis.module';
import { ProductsModule } from './products/products.module';
import { CartModule } from './cart/cart.module';
import { OrdersModule } from './orders/orders.module';
import { ReturnsModule } from './returns/returns.module';
import { MembersModule } from './members/members.module';
import { AuthModule } from './auth/auth.module';
import { HealthController } from './health/health.controller';
import { TestModule } from './test/test.module';
import { SessionMiddleware } from './session/session.middleware';
import { RateLimitGuard } from './common/guards/rate-limit.guard';

const testModules = process.env.NODE_ENV === 'production' ? [] : [TestModule];

@Module({
  controllers: [HealthController],
  imports: [
    ConfigModule.forRoot({
      load: [configuration],
      isGlobal: true,
    }),
    RedisModule,
    CoreApiModule,
    ProductsModule,
    CartModule,
    AuthModule,
    OrdersModule,
    ReturnsModule,
    MembersModule,
    ...testModules,
  ],
  providers: [
    {
      provide: APP_GUARD,
      useClass: RateLimitGuard,
    },
  ],
})
export class AppModule implements NestModule {
  configure(consumer: MiddlewareConsumer) {
    consumer.apply(SessionMiddleware).forRoutes('*');
  }
}
