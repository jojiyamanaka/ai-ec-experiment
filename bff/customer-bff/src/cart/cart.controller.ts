import { Controller, Get, Post, Put, Delete, Body, Param, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from '../auth/auth.guard';
import { CartService } from './cart.service';
import { ApiResponse } from '@app/shared';
import { RateLimitGuard } from '../common/guards/rate-limit.guard';
import { RateLimit } from '../common/decorators/rate-limit.decorator';
import { ApiBody, ApiOkResponse, ApiOperation, ApiParam, ApiTags, ApiUnauthorizedResponse } from '@nestjs/swagger';

@Controller('api/cart')
@UseGuards(AuthGuard)
@ApiTags('cart')
export class CartController {
  constructor(private cartService: CartService) {}

  @Get()
  @ApiOperation({ summary: 'カート取得' })
  @ApiOkResponse({ description: 'カート情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getCart(@Req() req: any): Promise<ApiResponse<any>> {
    return this.cartService.getCart(req.token, req.user?.id, req.traceId);
  }

  @Post('items')
  @UseGuards(RateLimitGuard)
  @RateLimit({ limit: 10, ttlSeconds: 60, keyType: 'user' })
  @ApiOperation({ summary: 'カートへ商品追加' })
  @ApiBody({
    schema: {
      properties: {
        productId: { type: 'number' },
        quantity: { type: 'number' },
      },
      required: ['productId', 'quantity'],
    },
  })
  @ApiOkResponse({ description: '追加後カート情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
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
  @ApiOperation({ summary: 'カート商品更新' })
  @ApiParam({ name: 'id', type: String })
  @ApiBody({
    schema: {
      properties: {
        quantity: { type: 'number' },
      },
      required: ['quantity'],
    },
  })
  @ApiOkResponse({ description: '更新後カート情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
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
  @ApiOperation({ summary: 'カート商品削除' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '削除後カート情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
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
