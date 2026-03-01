import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { ReturnsController } from './returns.controller';
import { ReturnsService } from './returns.service';

@Module({
  imports: [CoreApiModule],
  controllers: [ReturnsController],
  providers: [ReturnsService],
})
export class ReturnsModule {}
