import { Controller, Get, Put, Post, Param, Body, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { InventoryService } from './inventory.service';
import { ApiResponse } from '@app/shared';
import { ApiBody, ApiOkResponse, ApiOperation, ApiParam, ApiTags, ApiUnauthorizedResponse } from '@nestjs/swagger';

@Controller(['api/inventory', 'api/bo/admin/inventory'])
@UseGuards(BoAuthGuard)
@ApiTags('inventory')
export class InventoryController {
  constructor(private inventoryService: InventoryService) {}

  @Get()
  @ApiOperation({ summary: '在庫一覧取得' })
  @ApiOkResponse({ description: '在庫一覧を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getInventory(@Req() req: any): Promise<ApiResponse<any>> {
    return this.inventoryService.getInventory(req.token, req.traceId);
  }

  @Get('adjustments')
  @ApiOperation({ summary: '在庫調整履歴取得' })
  @ApiOkResponse({ description: '在庫調整履歴を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getAdjustments(@Req() req: any): Promise<ApiResponse<any>> {
    return this.inventoryService.getAdjustments(req.token, req.traceId);
  }

  @Post('adjust')
  @ApiOperation({ summary: '在庫調整' })
  @ApiBody({
    schema: {
      properties: {
        productId: { type: 'number' },
        quantityDelta: { type: 'number' },
        reason: { type: 'string' },
      },
      required: ['productId', 'quantityDelta', 'reason'],
    },
  })
  @ApiOkResponse({ description: '在庫調整結果を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async adjustInventory(
    @Body() body: { productId: number; quantityDelta: number; reason: string },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.inventoryService.adjustInventory(
      body.productId,
      body.quantityDelta,
      body.reason,
      req.token,
      req.traceId,
    );
  }

  @Put(':id')
  @ApiOperation({ summary: '在庫更新' })
  @ApiParam({ name: 'id', type: String })
  @ApiBody({
    schema: {
      properties: {
        stock: { type: 'number' },
      },
      required: ['stock'],
    },
  })
  @ApiOkResponse({ description: '更新後在庫情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async updateInventory(
    @Param('id') id: string,
    @Body() body: { stock: number },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.inventoryService.updateInventory(
      parseInt(id, 10),
      body.stock,
      req.token,
      req.traceId,
    );
  }
}
