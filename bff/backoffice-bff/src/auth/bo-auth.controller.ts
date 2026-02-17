import { Controller, Post, Body, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from './bo-auth.guard';
import { BoAuthService } from './bo-auth.service';
import { ApiResponse } from '@app/shared';

@Controller('api/bo-auth')
export class BoAuthController {
  constructor(private boAuthService: BoAuthService) {}

  @Post('login')
  async login(
    @Body() body: { email?: string; username?: string; password: string },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    const email = body.email ?? body.username ?? '';
    return this.boAuthService.login(email, body.password, req.traceId);
  }

  @Post('logout')
  @UseGuards(BoAuthGuard)
  async logout(@Req() req: any): Promise<ApiResponse<void>> {
    return this.boAuthService.logout(req.token, req.traceId);
  }
}
