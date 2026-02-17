import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { InventoryController } from './inventory.controller';
import { InventoryService } from './inventory.service';

@Module({
  imports: [CoreApiModule],
  controllers: [InventoryController],
  providers: [InventoryService],
})
export class InventoryModule {}
