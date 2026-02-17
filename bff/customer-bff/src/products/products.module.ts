import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { ProductsController } from './products.controller';
import { ProductsService } from './products.service';

@Module({
  imports: [CoreApiModule],
  controllers: [ProductsController],
  providers: [ProductsService],
})
export class ProductsModule {}
