import { Controller, Get, Post, Body, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { BoUsersService } from './bo-users.service';
import { ApiResponse, BoUserDto } from '@app/shared';
import { ApiBody, ApiOkResponse, ApiOperation, ApiTags, ApiUnauthorizedResponse } from '@nestjs/swagger';

@Controller('api/admin/bo-users')
@UseGuards(BoAuthGuard)
@ApiTags('bo-users')
export class BoUsersController {
  constructor(private boUsersService: BoUsersService) {}

  @Get()
  @ApiOperation({ summary: '管理ユーザー一覧取得' })
  @ApiOkResponse({ description: '管理ユーザー一覧を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getBoUsers(@Req() req: any): Promise<ApiResponse<any>> {
    return this.boUsersService.getBoUsers(req.token, req.traceId);
  }

  @Post()
  @ApiOperation({ summary: '管理ユーザー作成' })
  @ApiBody({
    schema: {
      properties: {
        username: { type: 'string' },
        displayName: { type: 'string' },
        password: { type: 'string' },
        email: { type: 'string' },
      },
      required: ['password', 'email'],
    },
  })
  @ApiOkResponse({ description: '作成した管理ユーザーを返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async createBoUser(
    @Body() body: { username?: string; displayName?: string; password: string; email: string },
    @Req() req: any,
  ): Promise<ApiResponse<BoUserDto>> {
    const displayName = body.displayName ?? body.username ?? body.email;
    return this.boUsersService.createBoUser(
      displayName,
      body.password,
      body.email,
      req.token,
      req.traceId,
    );
  }
}
