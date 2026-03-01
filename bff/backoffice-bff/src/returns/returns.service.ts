import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import type { ApiResponse } from '@app/shared';

@Injectable()
export class ReturnsService {
  constructor(private coreApiService: CoreApiService) {}

  async getReturn(orderId: number, token: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.get<ApiResponse<any>>(
      `/api/order/${orderId}/return`,
      token,
      traceId,
    );
  }

  async approveReturn(orderId: number, token: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.post<ApiResponse<any>>(
      `/api/order/${orderId}/return/approve`,
      {},
      token,
      traceId,
    );
  }

  async rejectReturn(
    orderId: number,
    body: {
      reason: string;
    },
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    return this.coreApiService.post<ApiResponse<any>>(
      `/api/order/${orderId}/return/reject`,
      body,
      token,
      traceId,
    );
  }

  async confirmReturn(orderId: number, token: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.post<ApiResponse<any>>(
      `/api/order/${orderId}/return/confirm`,
      {},
      token,
      traceId,
    );
  }

  async getReturns(
    params: { status?: string; page?: string; limit?: string },
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    const query = new URLSearchParams();
    if (params.status) query.set('status', params.status);
    if (params.page) query.set('page', params.page);
    if (params.limit) query.set('limit', params.limit);
    const suffix = query.toString();
    return this.coreApiService.get<ApiResponse<any>>(
      suffix ? `/api/return?${suffix}` : '/api/return',
      token,
      traceId,
    );
  }
}
