import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class CartService {
  constructor(private coreApiService: CoreApiService) {}

  async getCart(token: string, userId?: number, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.get<ApiResponse<any>>(
      '/api/order/cart',
      token,
      traceId,
      this.buildSessionHeader(userId),
    );
  }

  async addCartItem(
    productId: number,
    quantity: number,
    token: string,
    userId?: number,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    return this.coreApiService.post<ApiResponse<any>>(
      '/api/order/cart/items',
      { productId, quantity },
      token,
      traceId,
      this.buildSessionHeader(userId),
    );
  }

  async updateCartItem(
    id: number,
    quantity: number,
    token: string,
    userId?: number,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    return this.coreApiService.put<ApiResponse<any>>(
      `/api/order/cart/items/${id}`,
      { quantity },
      token,
      traceId,
      this.buildSessionHeader(userId),
    );
  }

  async deleteCartItem(
    id: number,
    token: string,
    userId?: number,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    return this.coreApiService.delete<ApiResponse<any>>(
      `/api/order/cart/items/${id}`,
      token,
      traceId,
      this.buildSessionHeader(userId),
    );
  }

  private buildSessionHeader(userId?: number): Record<string, string> {
    const sessionId = userId ? `user-${userId}` : 'guest-session';
    return {
      'X-Session-Id': sessionId,
    };
  }
}
