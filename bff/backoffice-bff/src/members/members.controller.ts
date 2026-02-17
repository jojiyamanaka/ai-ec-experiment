import { Controller, Get, Put, Param, Body, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { MembersService } from './members.service';
import { ApiResponse } from '@app/shared';

@Controller(['api/admin/members', 'api/bo/admin/members'])
@UseGuards(BoAuthGuard)
export class MembersController {
  constructor(private membersService: MembersService) {}

  @Get()
  async getMembers(@Req() req: any): Promise<ApiResponse<any>> {
    return this.membersService.getMembers(req.token, req.traceId);
  }

  @Get(':id')
  async getMemberById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.membersService.getMemberById(parseInt(id, 10), req.token, req.traceId);
  }

  @Put(':id/status')
  async updateMemberStatus(
    @Param('id') id: string,
    @Body() body: { isActive: boolean },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.membersService.updateMemberStatus(
      parseInt(id, 10),
      body.isActive,
      req.token,
      req.traceId,
    );
  }
}
