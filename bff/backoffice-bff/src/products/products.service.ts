import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class ProductsService {
  constructor(
    private coreApiService: CoreApiService,
    private redisService: RedisService,
  ) {}

  async getProducts(page: number, limit: number, token: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.get<ApiResponse<any>>(
      `/api/bo/admin/items?page=${page}&limit=${limit}`,
      token,
      traceId,
    );
  }

  async getProductById(id: number, token: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.get<ApiResponse<any>>(
      `/api/bo/admin/items/${id}`,
      token,
      traceId,
    );
  }

  async createProduct(payload: Record<string, unknown>, token: string, traceId?: string): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.post<ApiResponse<any>>(
      '/api/bo/admin/items',
      payload,
      token,
      traceId,
    );
    if (response.success) {
      await this.clearProductCache();
    }
    return response;
  }

  async updateProduct(
    id: number,
    payload: Record<string, unknown>,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.put<ApiResponse<any>>(
      `/api/bo/admin/items/${id}`,
      payload,
      token,
      traceId,
    );
    if (response.success) {
      await this.clearProductCache();
    }
    return response;
  }

  async getCategories(token: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.get<ApiResponse<any>>(
      '/api/bo/admin/item-categories',
      token,
      traceId,
    );
  }

  async createCategory(payload: Record<string, unknown>, token: string, traceId?: string): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.post<ApiResponse<any>>(
      '/api/bo/admin/item-categories',
      payload,
      token,
      traceId,
    );
    if (response.success) {
      await this.clearProductCache();
    }
    return response;
  }

  async updateCategory(
    id: number,
    payload: Record<string, unknown>,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.put<ApiResponse<any>>(
      `/api/bo/admin/item-categories/${id}`,
      payload,
      token,
      traceId,
    );
    if (response.success) {
      await this.clearProductCache();
    }
    return response;
  }

  private async clearProductCache(): Promise<void> {
    await this.redisService.delPattern('cache:product:*');
    await this.redisService.delPattern('cache:products:list:*');
  }
}
