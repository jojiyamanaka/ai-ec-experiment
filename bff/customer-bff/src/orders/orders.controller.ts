import { Body, Controller, Get, Post, Param, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from '../auth/auth.guard';
import { OrdersService } from './orders.service';
import { ApiResponse } from '@app/shared';
import { ApiBody, ApiOkResponse, ApiOperation, ApiParam, ApiTags, ApiUnauthorizedResponse } from '@nestjs/swagger';

@Controller('api/orders')
@UseGuards(AuthGuard)
@ApiTags('orders')
export class OrdersController {
  constructor(private ordersService: OrdersService) {}

  @Post()
  @ApiOperation({ summary: '注文作成' })
  @ApiBody({
    schema: {
      properties: {
        cartId: { type: 'string' },
      },
    },
  })
  @ApiOkResponse({ description: '作成した注文を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async createOrder(
    @Body() body: { cartId?: string } = {},
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.ordersService.createOrder(req.token, req.user?.id, body.cartId, req.traceId);
  }

  @Get()
  @ApiOperation({ summary: '注文一覧取得' })
  @ApiOkResponse({ description: '注文一覧を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getOrders(@Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.getOrders(req.token, req.user?.id, req.traceId);
  }

  @Get('history')
  @ApiOperation({ summary: '注文履歴取得' })
  @ApiOkResponse({ description: '注文履歴を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getOrderHistory(@Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.getOrders(req.token, req.user?.id, req.traceId);
  }

  @Get(':id')
  @ApiOperation({ summary: '注文詳細取得' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '注文詳細を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getOrderById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.ordersService.getOrderById(parseInt(id, 10), req.token, req.user?.id, req.traceId);
  }

  @Get(':id/full')
  @ApiOperation({ summary: '注文詳細(拡張)取得' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '注文詳細(拡張)を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getOrderFull(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.ordersService.getOrderFull(parseInt(id, 10), req.token, req.user?.id, req.traceId);
  }

  @Post(':id/cancel')
  @ApiOperation({ summary: '注文キャンセル' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: 'キャンセル後注文情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async cancelOrder(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.ordersService.cancelOrder(parseInt(id, 10), req.token, req.user?.id, req.traceId);
  }
}
