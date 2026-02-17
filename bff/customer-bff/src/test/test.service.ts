import { Injectable } from '@nestjs/common';
import { ApiResponse } from '@app/shared';
import { CoreApiService } from '../core-api/core-api.service';

@Injectable()
export class TestService {
  constructor(private coreApiService: CoreApiService) {}

  async slow(traceId?: string): Promise<ApiResponse<string>> {
    return this.coreApiService.get<ApiResponse<string>>('/test/slow', undefined, traceId);
  }
}
