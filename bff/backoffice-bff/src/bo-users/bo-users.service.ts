import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse, BoUserDto } from '@app/shared';

@Injectable()
export class BoUsersService {
  constructor(private coreApiService: CoreApiService) {}

  async getBoUsers(token: string, traceId?: string): Promise<ApiResponse<any>> {
    const response = await this.coreApiService.get<ApiResponse<BoUserDto[]>>(
      '/api/bo/admin/bo-users',
      token,
      traceId,
    );

    if (!response.success || !response.data) {
      return response;
    }

    return {
      success: true,
      data: {
        boUsers: response.data,
        pagination: {
          page: 1,
          pageSize: 20,
          totalCount: response.data.length,
        },
      },
    };
  }

  async createBoUser(
    displayName: string,
    password: string,
    email: string,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<BoUserDto>> {
    return this.coreApiService.post<ApiResponse<BoUserDto>>(
      '/api/bo/admin/bo-users',
      { displayName, password, email },
      token,
      traceId,
    );
  }
}
