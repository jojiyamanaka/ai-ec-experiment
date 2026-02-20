import { Body, Controller, Get, Param, Post, Put, Query, Req, UseGuards } from '@nestjs/common';
import { ApiBody, ApiOkResponse, ApiOperation, ApiParam, ApiQuery, ApiTags, ApiUnauthorizedResponse } from '@nestjs/swagger';
import { ApiResponse } from '@app/shared';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { ProductsService } from './products.service';

@Controller(['api/admin', 'api/bo/admin'])
@UseGuards(BoAuthGuard)
@ApiTags('products')
export class ProductsController {
  constructor(private productsService: ProductsService) {}

  @Get('items')
  @ApiOperation({ summary: '管理向け商品一覧取得' })
  @ApiQuery({ name: 'page', required: false, type: String })
  @ApiQuery({ name: 'limit', required: false, type: String })
  @ApiOkResponse({ description: '商品一覧を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getProducts(
    @Query('page') page = '1',
    @Query('limit') limit = '20',
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.productsService.getProducts(parseInt(page, 10), parseInt(limit, 10), req.token, req.traceId);
  }

  @Get('items/:id')
  @ApiOperation({ summary: '管理向け商品詳細取得' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '商品詳細を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getProductById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.productsService.getProductById(parseInt(id, 10), req.token, req.traceId);
  }

  @Post('items')
  @ApiOperation({ summary: '管理向け商品新規登録' })
  @ApiBody({ schema: { type: 'object' } })
  @ApiOkResponse({ description: '作成後商品情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async createProduct(
    @Body() body: Record<string, unknown>,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.productsService.createProduct(body, req.token, req.traceId);
  }

  @Put('items/:id')
  @ApiOperation({ summary: '管理向け商品更新' })
  @ApiParam({ name: 'id', type: String })
  @ApiBody({ schema: { type: 'object' } })
  @ApiOkResponse({ description: '更新後商品情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async updateProduct(
    @Param('id') id: string,
    @Body() body: Record<string, unknown>,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.productsService.updateProduct(parseInt(id, 10), body, req.token, req.traceId);
  }

  @Get('item-categories')
  @ApiOperation({ summary: '管理向けカテゴリ一覧取得' })
  @ApiOkResponse({ description: 'カテゴリ一覧を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getCategories(@Req() req: any): Promise<ApiResponse<any>> {
    return this.productsService.getCategories(req.token, req.traceId);
  }

  @Post('item-categories')
  @ApiOperation({ summary: '管理向けカテゴリ新規登録' })
  @ApiBody({ schema: { type: 'object' } })
  @ApiOkResponse({ description: '作成後カテゴリ情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async createCategory(
    @Body() body: Record<string, unknown>,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.productsService.createCategory(body, req.token, req.traceId);
  }

  @Put('item-categories/:id')
  @ApiOperation({ summary: '管理向けカテゴリ更新' })
  @ApiParam({ name: 'id', type: String })
  @ApiBody({ schema: { type: 'object' } })
  @ApiOkResponse({ description: '更新後カテゴリ情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async updateCategory(
    @Param('id') id: string,
    @Body() body: Record<string, unknown>,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.productsService.updateCategory(parseInt(id, 10), body, req.token, req.traceId);
  }
}
