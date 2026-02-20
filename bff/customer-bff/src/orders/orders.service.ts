import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class OrdersService {
  constructor(
    private coreApiService: CoreApiService,
    private redisService: RedisService,
  ) {}

  async createOrder(
    token: string,
    userId?: number,
    cartId?: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    const sessionId = userId ? this.resolveSessionId(userId) : this.resolveSessionId(undefined, cartId);
    const createResponse = await this.coreApiService.post<ApiResponse<any>>(
      '/api/order',
      { cartId: sessionId },
      token,
      traceId,
      this.buildSessionHeader(sessionId),
    );

    if (!createResponse.success || !createResponse.data) {
      return createResponse as ApiResponse<any>;
    }

    const createdOrderId = createResponse.data.orderId ?? createResponse.data.id;
    if (!createdOrderId) {
      return createResponse as ApiResponse<any>;
    }
    return this.getOrderById(Number(createdOrderId), token, userId, traceId, sessionId);
  }

  async getOrders(token: string, userId?: number, traceId?: string): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.get<ApiResponse<any[]>>(
      '/api/order/history',
      token,
      traceId,
    );

    if (!response.success || !response.data) {
      return response as ApiResponse<any>;
    }

    return {
      success: true,
      data: response.data.map((order) => this.normalizeOrder(order)),
    };
  }

  async getOrderById(
    id: number,
    token: string,
    userId?: number,
    traceId?: string,
    sessionId?: string,
  ): Promise<ApiResponse<any>> {
    const resolvedSessionId = sessionId ?? this.resolveSessionId(userId);
    const response = await this.coreApiService.get<ApiResponse<any>>(
      `/api/order/${id}`,
      token,
      traceId,
      this.buildSessionHeader(resolvedSessionId),
    );

    if (!response.success || !response.data) {
      return response as ApiResponse<any>;
    }

    return {
      success: true,
      data: this.normalizeOrder(response.data),
    };
  }

  async cancelOrder(
    id: number,
    token: string,
    userId?: number,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    const sessionId = this.resolveSessionId(userId);
    const response = await this.coreApiService.post<ApiResponse<any>>(
      `/api/order/${id}/cancel`,
      {},
      token,
      traceId,
      this.buildSessionHeader(sessionId),
    );

    if (!response.success || !response.data) {
      return response as ApiResponse<any>;
    }

    return {
      success: true,
      data: this.normalizeOrder(response.data),
    };
  }

  async getOrderFull(
    id: number,
    token: string,
    userId?: number,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    // 注文詳細取得（キャッシュなし — 最新状態を返す）
    const orderResponse = await this.getOrderById(id, token, userId, traceId);
    if (!orderResponse.success) return orderResponse;

    const order = orderResponse.data;
    const items = order.items ?? [];

    // 各注文アイテムの商品情報を並列取得（商品キャッシュ活用）
    const enrichedItems = await Promise.all(
      items.map(async (item: any) => {
        const cacheKey = `cache:product:${item.productId}`;
        let product = await this.redisService.get<any>(cacheKey);
        if (!product) {
          const productResponse = await this.coreApiService.get<any>(
            `/api/item/${item.productId}`,
            undefined,
            traceId,
          );
          product = productResponse.success ? productResponse.data : null;
          if (product) await this.redisService.set(cacheKey, product, 600);
        }
        return { ...item, product };
      }),
    );

    return {
      success: true,
      data: { order, items: enrichedItems },
    };
  }

  private resolveSessionId(userId?: number, fallback?: string): string {
    if (fallback && fallback.trim() !== '') {
      return fallback;
    }
    if (userId) {
      return `user-${userId}`;
    }
    return 'guest-session';
  }

  private buildSessionHeader(sessionId: string): Record<string, string> {
    return {
      'X-Session-Id': sessionId,
    };
  }

  private normalizeOrder(order: any): any {
    const status = order.status;
    const orderedQuantity = Number(order.orderedQuantity ?? 0);
    const committedQuantity = Number(order.committedQuantity ?? 0);
    return {
      ...order,
      id: order.id ?? order.orderId,
      orderId: order.orderId ?? order.id,
      orderedQuantity,
      committedQuantity,
      items: (order.items ?? []).map((item: any) => ({
        ...item,
        orderedQuantity: Number(item.orderedQuantity ?? item.quantity ?? 0),
        committedQuantity: Number(item.committedQuantity ?? 0),
      })),
      statusLabel: this.toStatusLabel(status),
    };
  }

  private toStatusLabel(status?: string): string {
    switch (status) {
      case 'PENDING':
        return '作成済み';
      case 'CONFIRMED':
        return '確認済み';
      case 'PREPARING_SHIPMENT':
        return '出荷準備中';
      case 'SHIPPED':
        return '発送済み';
      case 'DELIVERED':
        return '配達完了';
      case 'CANCELLED':
        return 'キャンセル';
      default:
        return status ?? '';
    }
  }
}
