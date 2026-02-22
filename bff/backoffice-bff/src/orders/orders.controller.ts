import { Controller, Get, Post, Put, Param, Body, UseGuards, Req, Query } from '@nestjs/common';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { OrdersService } from './orders.service';
import { ApiResponse } from '@app/shared';
import { ApiBody, ApiOkResponse, ApiOperation, ApiParam, ApiQuery, ApiTags, ApiUnauthorizedResponse } from '@nestjs/swagger';

@Controller(['api/admin/orders', 'api/order'])
@UseGuards(BoAuthGuard)
@ApiTags('orders')
export class OrdersController {
  constructor(private ordersService: OrdersService) {}

  @Get()
  @ApiOperation({ summary: '注文一覧取得' })
  @ApiQuery({ name: 'orderNumber', required: false, type: String })
  @ApiQuery({ name: 'customerEmail', required: false, type: String })
  @ApiQuery({ name: 'statuses', required: false, type: String })
  @ApiQuery({ name: 'dateFrom', required: false, type: String })
  @ApiQuery({ name: 'dateTo', required: false, type: String })
  @ApiQuery({ name: 'totalPriceMin', required: false, type: String })
  @ApiQuery({ name: 'totalPriceMax', required: false, type: String })
  @ApiQuery({ name: 'allocationIncomplete', required: false, type: String })
  @ApiQuery({ name: 'unshipped', required: false, type: String })
  @ApiQuery({ name: 'page', required: false, type: String })
  @ApiQuery({ name: 'limit', required: false, type: String })
  @ApiOkResponse({ description: '注文一覧を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getOrders(
    @Query('orderNumber') orderNumber?: string,
    @Query('customerEmail') customerEmail?: string,
    @Query('statuses') statuses?: string,
    @Query('dateFrom') dateFrom?: string,
    @Query('dateTo') dateTo?: string,
    @Query('totalPriceMin') totalPriceMin?: string,
    @Query('totalPriceMax') totalPriceMax?: string,
    @Query('allocationIncomplete') allocationIncomplete?: string,
    @Query('unshipped') unshipped?: string,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Req() req?: any,
  ): Promise<ApiResponse<any>> {
    return this.ordersService.getOrders(
      {
        orderNumber,
        customerEmail,
        statuses,
        dateFrom,
        dateTo,
        totalPriceMin,
        totalPriceMax,
        allocationIncomplete,
        unshipped,
        page,
        limit,
      },
      req.token,
      req.traceId,
    );
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
    return this.ordersService.getOrderById(parseInt(id, 10), req.token, req.traceId);
  }

  @Put(':id')
  @ApiOperation({ summary: '注文ステータス更新' })
  @ApiParam({ name: 'id', type: String })
  @ApiBody({
    schema: {
      properties: {
        status: { type: 'string' },
      },
      required: ['status'],
    },
  })
  @ApiOkResponse({ description: '更新後注文情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async updateOrderStatus(
    @Param('id') id: string,
    @Body() body: { status: string },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.ordersService.updateOrderStatus(
      parseInt(id, 10),
      body.status,
      req.token,
      req.traceId,
    );
  }

  @Post(':id/confirm')
  @ApiOperation({ summary: '注文確定' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '更新後注文情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async confirmOrder(@Param('id') id: string, @Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.updateOrderStatus(
      parseInt(id, 10),
      'CONFIRMED',
      req.token,
      req.traceId,
    );
  }

  @Post(':id/ship')
  @ApiOperation({ summary: '注文出荷' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '更新後注文情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async shipOrder(@Param('id') id: string, @Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.updateOrderStatus(
      parseInt(id, 10),
      'SHIPPED',
      req.token,
      req.traceId,
    );
  }

  @Post(':id/deliver')
  @ApiOperation({ summary: '注文配送完了' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '更新後注文情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async deliverOrder(@Param('id') id: string, @Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.updateOrderStatus(
      parseInt(id, 10),
      'DELIVERED',
      req.token,
      req.traceId,
    );
  }

  @Post(':id/cancel')
  @ApiOperation({ summary: '注文キャンセル' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '更新後注文情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async cancelOrder(@Param('id') id: string, @Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.updateOrderStatus(
      parseInt(id, 10),
      'CANCELLED',
      req.token,
      req.traceId,
    );
  }

  @Post(':id/allocation/retry')
  @ApiOperation({ summary: '本引当再試行' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '更新後注文情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async retryAllocation(@Param('id') id: string, @Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.retryAllocation(parseInt(id, 10), req.token, req.traceId);
  }
}
