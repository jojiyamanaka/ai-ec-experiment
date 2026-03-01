import { Body, Controller, Get, Param, Post, Req, UseGuards } from '@nestjs/common';
import { ApiBody, ApiOkResponse, ApiOperation, ApiParam, ApiTags, ApiUnauthorizedResponse } from '@nestjs/swagger';
import type { ApiResponse } from '@app/shared';
import { AuthGuard } from '../auth/auth.guard';
import { ReturnsService } from './returns.service';

@Controller('api/orders')
@UseGuards(AuthGuard)
@ApiTags('returns')
export class ReturnsController {
  constructor(private returnsService: ReturnsService) {}

  @Post(':id/return')
  @ApiOperation({ summary: '返品申請' })
  @ApiParam({ name: 'id', type: String })
  @ApiBody({
    schema: {
      properties: {
        reason: { type: 'string' },
        items: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              orderItemId: { type: 'number' },
              quantity: { type: 'number' },
            },
          },
        },
      },
      required: ['reason', 'items'],
    },
  })
  @ApiOkResponse({ description: '返品情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async createReturn(
    @Param('id') id: string,
    @Body() body: {
      reason: string;
      items: Array<{
        orderItemId: number;
        quantity: number;
      }>;
    },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.returnsService.createReturn(parseInt(id, 10), body, req.token, req.traceId);
  }

  @Get(':id/return')
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
}
