import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { TestController } from './test.controller';
import { TestService } from './test.service';

@Module({
  imports: [CoreApiModule],
  controllers: [TestController],
  providers: [TestService],
})
export class TestModule {}
