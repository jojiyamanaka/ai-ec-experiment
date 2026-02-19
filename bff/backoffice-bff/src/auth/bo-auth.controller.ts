import { Controller, Post, Body, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from './bo-auth.guard';
import { BoAuthService } from './bo-auth.service';
import { ApiResponse } from '@app/shared';
import { ApiBody, ApiOkResponse, ApiOperation, ApiTags, ApiUnauthorizedResponse } from '@nestjs/swagger';

@Controller('api/bo-auth')
@ApiTags('bo-auth')
export class BoAuthController {
  constructor(private boAuthService: BoAuthService) {}

  @Post('login')
  @ApiOperation({ summary: '管理者ログイン' })
  @ApiBody({
    schema: {
      properties: {
        email: { type: 'string' },
        username: { type: 'string' },
        password: { type: 'string' },
      },
      required: ['password'],
    },
  })
  @ApiOkResponse({ description: 'ログイン結果を返却' })
  async login(
    @Body() body: { email?: string; username?: string; password: string },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    const email = body.email ?? body.username ?? '';
    return this.boAuthService.login(email, body.password, req.traceId);
  }

  @Post('logout')
  @UseGuards(BoAuthGuard)
  @ApiOperation({ summary: '管理者ログアウト' })
  @ApiOkResponse({ description: 'ログアウト完了' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async logout(@Req() req: any): Promise<ApiResponse<void>> {
    return this.boAuthService.logout(req.token, req.traceId);
  }
}
