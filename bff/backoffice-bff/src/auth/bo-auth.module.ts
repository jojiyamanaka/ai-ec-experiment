import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { BoAuthController } from './bo-auth.controller';
import { BoAuthService } from './bo-auth.service';
import { BoAuthGuard } from './bo-auth.guard';

@Module({
  imports: [CoreApiModule],
  controllers: [BoAuthController],
  providers: [BoAuthService, BoAuthGuard],
  exports: [BoAuthGuard],
})
export class BoAuthModule {}
