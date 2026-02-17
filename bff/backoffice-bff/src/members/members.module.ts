import { Module } from '@nestjs/common';
import { CoreApiModule } from '../core-api/core-api.module';
import { MembersController } from './members.controller';
import { MembersService } from './members.service';

@Module({
  imports: [CoreApiModule],
  controllers: [MembersController],
  providers: [MembersService],
})
export class MembersModule {}
