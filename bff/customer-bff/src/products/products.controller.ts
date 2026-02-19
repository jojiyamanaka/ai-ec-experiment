import { Controller, Get, Param, Query, Req } from '@nestjs/common';
import { ProductsService } from './products.service';
import { ApiResponse } from '@app/shared';
import { ApiOkResponse, ApiOperation, ApiParam, ApiQuery, ApiTags } from '@nestjs/swagger';

@Controller('api/products')
@ApiTags('products')
export class ProductsController {
  constructor(private productsService: ProductsService) {}

  @Get()
  @ApiOperation({ summary: '商品一覧を取得' })
  @ApiQuery({ name: 'page', required: false, type: String })
  @ApiQuery({ name: 'limit', required: false, type: String })
  @ApiOkResponse({ description: '商品一覧を返却' })
  async getProducts(
    @Query('page') page = '1',
    @Query('limit') limit = '20',
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.productsService.getProducts(parseInt(page, 10), parseInt(limit, 10), req.traceId);
  }

  @Get(':id')
  @ApiOperation({ summary: '商品詳細を取得' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '商品詳細を返却' })
  async getProductById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.productsService.getProductById(parseInt(id, 10), req.traceId);
  }

  @Get(':id/full')
  @ApiOperation({ summary: '商品詳細(拡張)を取得' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '商品詳細(拡張)を返却' })
  async getProductFull(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.productsService.getProductFull(parseInt(id, 10), req.traceId);
  }
}
