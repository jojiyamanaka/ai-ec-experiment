import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { OrdersController } from './orders.controller';
import { OrdersService } from './orders.service';

@Module({
  imports: [CoreApiModule],
  controllers: [OrdersController],
  providers: [OrdersService],
})
export class OrdersModule {}
