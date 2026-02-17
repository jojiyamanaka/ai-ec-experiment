import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { CartController } from './cart.controller';
import { CartService } from './cart.service';

@Module({
  imports: [CoreApiModule],
  controllers: [CartController],
  providers: [CartService],
})
export class CartModule {}
