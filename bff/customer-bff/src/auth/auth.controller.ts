import { Controller, Post, Body, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from './auth.guard';
import { AuthService } from './auth.service';
import { ApiResponse } from '@app/shared';
import { RateLimitGuard } from '../common/guards/rate-limit.guard';
import { RateLimit } from '../common/decorators/rate-limit.decorator';
import { ApiBody, ApiOkResponse, ApiOperation, ApiTags, ApiUnauthorizedResponse } from '@nestjs/swagger';

@Controller('api/auth')
@ApiTags('auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  @Post('register')
  @UseGuards(RateLimitGuard)
  @RateLimit({ limit: 3, ttlSeconds: 600, keyType: 'ip' })
  @ApiOperation({ summary: '会員登録' })
  @ApiBody({
    schema: {
      properties: {
        email: { type: 'string' },
        username: { type: 'string' },
        password: { type: 'string' },
        displayName: { type: 'string' },
        fullName: { type: 'string' },
      },
      required: ['password'],
    },
  })
  @ApiOkResponse({ description: '登録結果を返却' })
  async register(
    @Body() body: { email?: string; password: string; displayName?: string; username?: string; fullName?: string },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    const email = body.email ?? body.username ?? '';
    const displayName = body.displayName ?? body.fullName ?? body.username ?? email;
    return this.authService.register(email, displayName, body.password, req.traceId);
  }

  @Post('login')
  @UseGuards(RateLimitGuard)
  @RateLimit({ limit: 5, ttlSeconds: 60, keyType: 'ip' })
  @ApiOperation({ summary: 'ログイン' })
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
    return this.authService.login(email, body.password, req.traceId, req['session']?.sessionId);
  }

  @Post('logout')
  @UseGuards(AuthGuard)
  @ApiOperation({ summary: 'ログアウト' })
  @ApiOkResponse({ description: 'ログアウト完了' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async logout(@Req() req: any): Promise<ApiResponse<void>> {
    return this.authService.logout(req.token, req.traceId);
  }
}
