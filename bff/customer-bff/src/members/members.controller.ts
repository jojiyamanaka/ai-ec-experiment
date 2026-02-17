import { Controller, Get, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from '../auth/auth.guard';
import { MembersService } from './members.service';
import { ApiResponse } from '@app/shared';

@Controller('api/members')
@UseGuards(AuthGuard)
export class MembersController {
  constructor(private membersService: MembersService) {}

  @Get('me')
  async getMe(@Req() req: any): Promise<ApiResponse<any>> {
    return this.membersService.getMe(req.token, req.traceId);
  }
}
