import { Controller, Get, Put, Param, Body, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { MembersService } from './members.service';
import { ApiResponse } from '@app/shared';
import { ApiBody, ApiOkResponse, ApiOperation, ApiParam, ApiTags, ApiUnauthorizedResponse } from '@nestjs/swagger';

@Controller(['api/admin/members', 'api/bo/admin/members'])
@UseGuards(BoAuthGuard)
@ApiTags('members')
export class MembersController {
  constructor(private membersService: MembersService) {}

  @Get()
  @ApiOperation({ summary: '会員一覧取得' })
  @ApiOkResponse({ description: '会員一覧を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getMembers(@Req() req: any): Promise<ApiResponse<any>> {
    return this.membersService.getMembers(req.token, req.traceId);
  }

  @Get(':id')
  @ApiOperation({ summary: '会員詳細取得' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '会員詳細を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getMemberById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.membersService.getMemberById(parseInt(id, 10), req.token, req.traceId);
  }

  @Put(':id/status')
  @ApiOperation({ summary: '会員有効状態更新' })
  @ApiParam({ name: 'id', type: String })
  @ApiBody({
    schema: {
      properties: {
        isActive: { type: 'boolean' },
      },
      required: ['isActive'],
    },
  })
  @ApiOkResponse({ description: '更新後会員情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
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
