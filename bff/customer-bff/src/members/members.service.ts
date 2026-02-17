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
}
