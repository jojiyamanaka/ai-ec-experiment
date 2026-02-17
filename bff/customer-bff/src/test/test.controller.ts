import { Controller, Get, Req } from '@nestjs/common';
import { ApiResponse } from '@app/shared';
import { TestService } from './test.service';

@Controller('api/test')
export class TestController {
  constructor(private testService: TestService) {}

  @Get('slow')
  async slow(@Req() req: any): Promise<ApiResponse<string>> {
    return this.testService.slow(req.traceId);
  }
}
