import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse } from '@app/shared';

@Injectable()
export class AuthService {
  constructor(private coreApiService: CoreApiService) {}

  async register(
    email: string,
    displayName: string,
    password: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    return this.coreApiService.post(
      '/api/auth/register',
      { email, displayName, password },
      undefined,
      traceId,
    );
  }

  async login(email: string, password: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.post(
      '/api/auth/login',
      { email, password },
      undefined,
      traceId,
    );
  }

  async logout(token: string, traceId?: string): Promise<ApiResponse<void>> {
    return this.coreApiService.post(
      '/api/auth/logout',
      {},
      token,
      traceId,
    );
  }
}
