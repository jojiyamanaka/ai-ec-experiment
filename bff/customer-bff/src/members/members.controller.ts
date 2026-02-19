import { Controller, Get, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from '../auth/auth.guard';
import { MembersService } from './members.service';
import { ApiResponse } from '@app/shared';
import { ApiOkResponse, ApiOperation, ApiTags, ApiUnauthorizedResponse } from '@nestjs/swagger';

@Controller('api/members')
@UseGuards(AuthGuard)
@ApiTags('members')
export class MembersController {
  constructor(private membersService: MembersService) {}

  @Get('me')
  @ApiOperation({ summary: 'ログインユーザー情報取得' })
  @ApiOkResponse({ description: '会員情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getMe(@Req() req: any): Promise<ApiResponse<any>> {
    return this.membersService.getMe(req.token, req.traceId);
  }
}
