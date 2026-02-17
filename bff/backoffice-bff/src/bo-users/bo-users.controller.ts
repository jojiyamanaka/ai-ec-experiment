import { Controller, Get, Post, Body, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { BoUsersService } from './bo-users.service';
import { ApiResponse, BoUserDto } from '@app/shared';

@Controller('api/admin/bo-users')
@UseGuards(BoAuthGuard)
export class BoUsersController {
  constructor(private boUsersService: BoUsersService) {}

  @Get()
  async getBoUsers(@Req() req: any): Promise<ApiResponse<any>> {
    return this.boUsersService.getBoUsers(req.token, req.traceId);
  }

  @Post()
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
