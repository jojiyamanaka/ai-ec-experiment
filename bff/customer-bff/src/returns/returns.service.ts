import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import type { ApiResponse } from '@app/shared';

@Injectable()
export class ReturnsService {
  constructor(private coreApiService: CoreApiService) {}

  async createReturn(
    orderId: number,
    body: {
      reason: string;
      items: Array<{
        orderItemId: number;
        quantity: number;
      }>;
    },
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    return this.coreApiService.post<ApiResponse<any>>(
      `/api/order/${orderId}/return`,
      body,
      token,
      traceId,
    );
  }

  async getReturn(
    orderId: number,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    return this.coreApiService.get<ApiResponse<any>>(
      `/api/order/${orderId}/return`,
      token,
      traceId,
    );
  }
}
