import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { BoUsersController } from './bo-users.controller';
import { BoUsersService } from './bo-users.service';

@Module({
  imports: [CoreApiModule],
  controllers: [BoUsersController],
  providers: [BoUsersService],
})
export class BoUsersModule {}
