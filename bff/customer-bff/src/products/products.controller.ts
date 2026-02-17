import { Controller, Get, Param, Query, Req } from '@nestjs/common';
import { ProductsService } from './products.service';
import { ApiResponse } from '@app/shared';

@Controller('api/products')
export class ProductsController {
  constructor(private productsService: ProductsService) {}

  @Get()
  async getProducts(
    @Query('page') page = '1',
    @Query('limit') limit = '20',
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.productsService.getProducts(parseInt(page, 10), parseInt(limit, 10), req.traceId);
  }

  @Get(':id')
  async getProductById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.productsService.getProductById(parseInt(id, 10), req.traceId);
  }
}
