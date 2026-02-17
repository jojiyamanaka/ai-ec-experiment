import { Controller, Get, Post, Put, Delete, Body, Param, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from '../auth/auth.guard';
import { CartService } from './cart.service';
import { ApiResponse } from '@app/shared';

@Controller('api/cart')
@UseGuards(AuthGuard)
export class CartController {
  constructor(private cartService: CartService) {}

  @Get()
  async getCart(@Req() req: any): Promise<ApiResponse<any>> {
    return this.cartService.getCart(req.token, req.user?.id, req.traceId);
  }

  @Post('items')
  async addCartItem(
    @Body() body: { productId: number; quantity: number },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.cartService.addCartItem(
      body.productId,
      body.quantity,
      req.token,
      req.user?.id,
      req.traceId,
    );
  }

  @Put('items/:id')
  async updateCartItem(
    @Param('id') id: string,
    @Body() body: { quantity: number },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.cartService.updateCartItem(
      parseInt(id, 10),
      body.quantity,
      req.token,
      req.user?.id,
      req.traceId,
    );
  }

  @Delete('items/:id')
  async deleteCartItem(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.cartService.deleteCartItem(
      parseInt(id, 10),
      req.token,
      req.user?.id,
      req.traceId,
    );
  }
}
