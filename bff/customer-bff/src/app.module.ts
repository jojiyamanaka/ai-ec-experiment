import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import configuration from './config/configuration';
import { CoreApiModule } from './core-api/core-api.module';
import { ProductsModule } from './products/products.module';
import { CartModule } from './cart/cart.module';
import { OrdersModule } from './orders/orders.module';
import { MembersModule } from './members/members.module';
import { AuthModule } from './auth/auth.module';
import { HealthController } from './health/health.controller';
import { TestModule } from './test/test.module';

const testModules = process.env.NODE_ENV === 'production' ? [] : [TestModule];

@Module({
  controllers: [HealthController],
  imports: [
    ConfigModule.forRoot({
      load: [configuration],
      isGlobal: true,
    }),
    CoreApiModule,
    ProductsModule,
    CartModule,
    AuthModule,
    OrdersModule,
    MembersModule,
    ...testModules,
  ],
})
export class AppModule {}
