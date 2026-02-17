import { Controller, Post, Body, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from './auth.guard';
import { AuthService } from './auth.service';
import { ApiResponse } from '@app/shared';

@Controller('api/auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  @Post('register')
  async register(
    @Body() body: { email?: string; password: string; displayName?: string; username?: string; fullName?: string },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    const email = body.email ?? body.username ?? '';
    const displayName = body.displayName ?? body.fullName ?? body.username ?? email;
    return this.authService.register(email, displayName, body.password, req.traceId);
  }

  @Post('login')
  async login(
    @Body() body: { email?: string; username?: string; password: string },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    const email = body.email ?? body.username ?? '';
    return this.authService.login(email, body.password, req.traceId);
  }

  @Post('logout')
  @UseGuards(AuthGuard)
  async logout(@Req() req: any): Promise<ApiResponse<void>> {
    return this.authService.logout(req.token, req.traceId);
  }
}
