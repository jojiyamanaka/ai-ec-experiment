import { Body, Controller, Get, Post, Param, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from '../auth/auth.guard';
import { OrdersService } from './orders.service';
import { ApiResponse } from '@app/shared';

@Controller('api/orders')
@UseGuards(AuthGuard)
export class OrdersController {
  constructor(private ordersService: OrdersService) {}

  @Post()
  async createOrder(
    @Body() body: { cartId?: string } = {},
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.ordersService.createOrder(req.token, req.user?.id, body.cartId, req.traceId);
  }

  @Get()
  async getOrders(@Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.getOrders(req.token, req.user?.id, req.traceId);
  }

  @Get('history')
  async getOrderHistory(@Req() req: any): Promise<ApiResponse<any>> {
    return this.ordersService.getOrders(req.token, req.user?.id, req.traceId);
  }

  @Get(':id')
  async getOrderById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.ordersService.getOrderById(parseInt(id, 10), req.token, req.user?.id, req.traceId);
  }

  @Post(':id/cancel')
  async cancelOrder(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.ordersService.cancelOrder(parseInt(id, 10), req.token, req.user?.id, req.traceId);
  }
}
