import { Module } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { CoreApiService } from './core-api.service';

@Module({
  imports: [HttpModule],
  providers: [CoreApiService],
  exports: [CoreApiService],
})
export class CoreApiModule {}
