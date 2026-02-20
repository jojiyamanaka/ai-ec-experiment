import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class OrdersService {
  constructor(private coreApiService: CoreApiService) {}

  async getOrders(token: string, traceId?: string): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.get<ApiResponse<any[]>>(
      '/api/order',
      token,
      traceId,
    );

    if (!response.success || !response.data) {
      return response as ApiResponse<any>;
    }

    return {
      success: true,
      data: {
        orders: response.data.map((order) => this.normalizeOrder(order)),
        pagination: {
          page: 1,
          pageSize: 20,
          totalCount: response.data.length,
        },
      },
    };
  }

  async getOrderById(id: number, token: string, traceId?: string): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.get<ApiResponse<any[]>>(
      '/api/order',
      token,
      traceId,
    );

    if (!response.success || !response.data) {
      return response as ApiResponse<any>;
    }

    const target = response.data.find((order) => {
      const orderId = order.orderId ?? order.id;
      return Number(orderId) === id;
    });
    if (!target) {
      return {
        success: false,
        error: {
          code: 'RESOURCE_NOT_FOUND',
          message: '注文が見つかりません',
        },
      };
    }

    return {
      success: true,
      data: this.normalizeOrder(target),
    };
  }

  async updateOrderStatus(
    id: number,
    status: string,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    const actionPath = this.resolveActionPath(id, status);
    if (!actionPath) {
      return {
        success: false,
        error: {
          code: 'INVALID_STATUS',
          message: '無効なステータスです',
        },
      };
    }

    const response = await this.coreApiService.post<ApiResponse<any>>(
      actionPath,
      {},
      token,
      traceId,
    );

    if (!response.success || !response.data) {
      return response as ApiResponse<any>;
    }

    return {
      success: true,
      data: this.normalizeOrder(response.data),
    };
  }

  async retryAllocation(id: number, token: string, traceId?: string): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.post<ApiResponse<any>>(
      `/api/order/${id}/allocation/retry`,
      {},
      token,
      traceId,
    );

    if (!response.success || !response.data) {
      return response as ApiResponse<any>;
    }

    return {
      success: true,
      data: this.normalizeOrder(response.data),
    };
  }

  private resolveActionPath(id: number, status: string): string | null {
    switch (status) {
      case 'CONFIRMED':
        return `/api/order/${id}/confirm`;
      case 'SHIPPED':
        return `/api/order/${id}/mark-shipped`;
      case 'DELIVERED':
        return `/api/order/${id}/deliver`;
      case 'CANCELLED':
        return `/api/order/${id}/cancel`;
      default:
        return null;
    }
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
