import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class MembersService {
  constructor(private coreApiService: CoreApiService) {}

  async getMe(token: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.get<ApiResponse<any>>(
      '/api/auth/me',
      token,
      traceId,
    );
  }

  async updateMe(payload: Record<string, unknown>, token: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.put<ApiResponse<any>>(
      '/api/auth/me',
      payload,
      token,
      traceId,
    );
  }

  async addMyAddress(payload: Record<string, unknown>, token: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.post<ApiResponse<any>>(
      '/api/auth/me/addresses',
      payload,
      token,
      traceId,
    );
  }

  async updateMyAddress(
    addressId: number,
    payload: Record<string, unknown>,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    return this.coreApiService.put<ApiResponse<any>>(
      `/api/auth/me/addresses/${addressId}`,
      payload,
      token,
      traceId,
    );
  }

  async deleteMyAddress(addressId: number, token: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.delete<ApiResponse<any>>(
      `/api/auth/me/addresses/${addressId}`,
      token,
      traceId,
    );
  }
}
