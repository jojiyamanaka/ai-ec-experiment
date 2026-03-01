import { Body, Controller, Get, Param, Post, Query, Req, UseGuards } from '@nestjs/common';
import {
  ApiBody,
  ApiOkResponse,
  ApiOperation,
  ApiParam,
  ApiQuery,
  ApiTags,
  ApiUnauthorizedResponse,
} from '@nestjs/swagger';
import type { ApiResponse } from '@app/shared';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { ReturnsService } from './returns.service';

@Controller('api/admin')
@UseGuards(BoAuthGuard)
@ApiTags('returns')
export class ReturnsController {
  constructor(private returnsService: ReturnsService) {}

  @Get('orders/:id/return')
  @ApiOperation({ summary: '返品取得' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '返品情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getReturn(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.returnsService.getReturn(parseInt(id, 10), req.token, req.traceId);
  }

  @Post('orders/:id/return/approve')
  @ApiOperation({ summary: '返品承認' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '返品情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async approveReturn(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.returnsService.approveReturn(parseInt(id, 10), req.token, req.traceId);
  }

  @Post('orders/:id/return/reject')
  @ApiOperation({ summary: '返品拒否' })
  @ApiParam({ name: 'id', type: String })
  @ApiBody({
    schema: {
      properties: {
        reason: { type: 'string' },
      },
      required: ['reason'],
    },
  })
  @ApiOkResponse({ description: '返品情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async rejectReturn(
    @Param('id') id: string,
    @Body() body: { reason: string },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.returnsService.rejectReturn(parseInt(id, 10), body, req.token, req.traceId);
  }

  @Post('orders/:id/return/confirm')
  @ApiOperation({ summary: '返品確定' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '返品情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async confirmReturn(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.returnsService.confirmReturn(parseInt(id, 10), req.token, req.traceId);
  }

  @Get('returns')
  @ApiOperation({ summary: '返品一覧取得' })
  @ApiQuery({ name: 'status', required: false, type: String })
  @ApiQuery({ name: 'page', required: false, type: String })
  @ApiQuery({ name: 'limit', required: false, type: String })
  @ApiOkResponse({ description: '返品一覧を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getReturns(
    @Query('status') status: string | undefined,
    @Query('page') page: string | undefined,
    @Query('limit') limit: string | undefined,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.returnsService.getReturns({ status, page, limit }, req.token, req.traceId);
  }
}
