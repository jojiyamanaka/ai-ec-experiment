import { Controller, Get, Post, Put, Param, Body, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { OrdersService } from './orders.service';
import { ApiResponse } from '@app/shared';

@Controller(['api/admin/orders', 'api/order'])
@UseGuards(BoAuthGuard)
export class OrdersController {
  constructor(private ordersService: OrdersService) {}

  @Get()
  async getOrders(@Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.getOrders(req.token, req.traceId);
  }

  @Get(':id')
  async getOrderById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.ordersService.getOrderById(parseInt(id, 10), req.token, req.traceId);
  }

  @Put(':id')
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
  async confirmOrder(@Param('id') id: string, @Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.updateOrderStatus(
      parseInt(id, 10),
      'CONFIRMED',
      req.token,
      req.traceId,
    );
  }

  @Post(':id/ship')
  async shipOrder(@Param('id') id: string, @Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.updateOrderStatus(
      parseInt(id, 10),
      'SHIPPED',
      req.token,
      req.traceId,
    );
  }

  @Post(':id/deliver')
  async deliverOrder(@Param('id') id: string, @Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.updateOrderStatus(
      parseInt(id, 10),
      'DELIVERED',
      req.token,
      req.traceId,
    );
  }

  @Post(':id/cancel')
  async cancelOrder(@Param('id') id: string, @Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.updateOrderStatus(
      parseInt(id, 10),
      'CANCELLED',
      req.token,
      req.traceId,
    );
  }
}
