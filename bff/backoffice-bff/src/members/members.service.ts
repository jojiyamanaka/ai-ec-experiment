import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse, UserDto } from '@app/shared';

@Injectable()
export class MembersService {
  constructor(private coreApiService: CoreApiService) {}

  async getMembers(token: string, traceId?: string): Promise<ApiResponse<any>> {
    return this.coreApiService.get<ApiResponse<UserDto[]>>(
      '/api/bo/admin/members',
      token,
      traceId,
    );
  }

  async getMemberById(id: number, token: string, traceId?: string): Promise<ApiResponse<any>> {
    const memberResponse = await this.coreApiService.get<ApiResponse<any>>(
      `/api/bo/admin/members/${id}`,
      token,
      traceId,
    );

    if (!memberResponse.success || !memberResponse.data) {
      return memberResponse;
    }

    return {
      success: true,
      data: memberResponse.data,
    };
  }

  async updateMemberStatus(
    id: number,
    isActive: boolean,
    token: string,
    traceId?: string,
  ): Promise<ApiResponse<any>> {
    return this.coreApiService.put<ApiResponse<any>>(
      `/api/bo/admin/members/${id}/status`,
      { isActive },
      token,
      traceId,
    );
  }
}
