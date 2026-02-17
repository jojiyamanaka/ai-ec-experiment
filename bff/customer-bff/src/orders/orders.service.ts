import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class OrdersService {
  constructor(private coreApiService: CoreApiService) {}

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
    return {
      ...order,
      id: order.id ?? order.orderId,
      orderId: order.orderId ?? order.id,
      statusLabel: this.toStatusLabel(status),
    };
  }

  private toStatusLabel(status?: string): string {
    switch (status) {
      case 'PENDING':
        return '作成済み';
      case 'CONFIRMED':
        return '確認済み';
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
