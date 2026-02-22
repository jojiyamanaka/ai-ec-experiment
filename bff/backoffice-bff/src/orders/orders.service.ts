import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class OrdersService {
  constructor(private coreApiService: CoreApiService) {}

  async getOrders(
    params: {
      orderNumber?: string;
      customerEmail?: string;
      statuses?: string;
      dateFrom?: string;
      dateTo?: string;
      totalPriceMin?: string;
      totalPriceMax?: string;
      allocationIncomplete?: string;
      unshipped?: string;
      page?: string;
      limit?: string;
    },
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    const query = new URLSearchParams();
    if (params.orderNumber) query.set('orderNumber', params.orderNumber);
    if (params.customerEmail) query.set('customerEmail', params.customerEmail);
    if (params.statuses) query.set('statuses', params.statuses);
    if (params.dateFrom) query.set('dateFrom', params.dateFrom);
    if (params.dateTo) query.set('dateTo', params.dateTo);
    if (params.totalPriceMin) query.set('totalPriceMin', params.totalPriceMin);
    if (params.totalPriceMax) query.set('totalPriceMax', params.totalPriceMax);
    if (params.allocationIncomplete) query.set('allocationIncomplete', params.allocationIncomplete);
    if (params.unshipped) query.set('unshipped', params.unshipped);
    if (params.page) query.set('page', params.page);
    if (params.limit) query.set('limit', params.limit);
    return this.coreApiService.get<ApiResponse<any>>(
      `/api/order?${query.toString()}`,
      token,
      traceId,
    );
  }

  async getOrderById(id: number, token: string, traceId?: string): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.get<ApiResponse<any>>(
      `/api/order/${id}`,
      token,
      traceId,
    );
    return response as ApiResponse<any>;
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

    return response as ApiResponse<any>;
  }

  async retryAllocation(id: number, token: string, traceId?: string): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.post<ApiResponse<any>>(
      `/api/order/${id}/allocation/retry`,
      {},
      token,
      traceId,
    );

    return response as ApiResponse<any>;
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

}
