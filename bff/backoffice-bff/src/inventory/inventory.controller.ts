import { Controller, Get, Put, Post, Param, Body, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { InventoryService } from './inventory.service';
import { ApiResponse } from '@app/shared';

@Controller(['api/inventory', 'api/bo/admin/inventory'])
@UseGuards(BoAuthGuard)
export class InventoryController {
  constructor(private inventoryService: InventoryService) {}

  @Get()
  async getInventory(@Req() req: any): Promise<ApiResponse<any>> {
    return this.inventoryService.getInventory(req.token, req.traceId);
  }

  @Get('adjustments')
  async getAdjustments(@Req() req: any): Promise<ApiResponse<any>> {
    return this.inventoryService.getAdjustments(req.token, req.traceId);
  }

  @Post('adjust')
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
