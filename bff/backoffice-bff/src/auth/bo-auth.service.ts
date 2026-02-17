import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class BoAuthService {
  constructor(private coreApiService: CoreApiService) {}

  async login(email: string, password: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.post(
      '/api/bo-auth/login',
      { email, password },
      undefined,
      traceId,
    );
  }

  async logout(token: string, traceId?: string): Promise<ApiResponse<void>> {
    return this.coreApiService.post(
      '/api/bo-auth/logout',
      {},
      token,
      traceId,
    );
  }
}
