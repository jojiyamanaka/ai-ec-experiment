import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';
import { ApiResponse } from '@app/shared';

interface CoreInventoryItem {
  productId: number;
  productName: string;
  physicalStock: number;
  tentativeReserved: number;
  committedReserved: number;
  availableStock: number;
}

@Injectable()
export class InventoryService {
  constructor(
    private coreApiService: CoreApiService,
    private redisService: RedisService,
  ) {}

  async getInventory(token: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.get<ApiResponse<any>>(
      '/api/bo/admin/inventory',
      token,
      traceId,
    );
  }

  async getAdjustments(token: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.get<ApiResponse<any>>(
      '/api/bo/admin/inventory/adjustments',
      token,
      traceId,
    );
  }

  async adjustInventory(
    productId: number,
    quantityDelta: number,
    reason: string,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.post<ApiResponse<any>>(
      '/api/bo/admin/inventory/adjust',
      { productId, quantityDelta, reason },
      token,
      traceId,
    );

    if (response.success) {
      // 商品キャッシュとリスト全体を無効化
      await this.redisService.del(`cache:product:${productId}`);
      await this.redisService.delPattern('cache:products:list:*');
    }
    return response;
  }

  async updateInventory(
    id: number,
    stock: number,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    const listResponse = await this.getInventory(token, traceId);
    if (!listResponse.success || !Array.isArray(listResponse.data)) {
      return listResponse as ApiResponse<any>;
    }

    const current = listResponse.data.find(
      (item: CoreInventoryItem) => item.productId === id,
    ) as CoreInventoryItem | undefined;

    if (!current) {
      return {
        success: false,
        error: {
          code: 'RESOURCE_NOT_FOUND',
          message: '在庫情報が見つかりません',
        },
      };
    }

    const quantityDelta = stock - current.physicalStock;
    if (quantityDelta === 0) {
      return {
        success: true,
        data: current,
      };
    }

    const adjustResponse = await this.adjustInventory(
      id,
      quantityDelta,
      'Updated from BackOffice BFF',
      token,
      traceId,
    );
    if (!adjustResponse.success) {
      return adjustResponse as ApiResponse<any>;
    }

    const refreshed = await this.getInventory(token, traceId);
    if (!refreshed.success || !Array.isArray(refreshed.data)) {
      return refreshed as ApiResponse<any>;
    }

    const updated = refreshed.data.find(
      (item: CoreInventoryItem) => item.productId === id,
    ) as CoreInventoryItem | undefined;

    return {
      success: true,
      data: updated ?? current,
    };
  }
}
