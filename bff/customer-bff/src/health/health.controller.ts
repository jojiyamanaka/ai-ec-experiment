import { Controller, Get } from '@nestjs/common';
import { ApiOkResponse, ApiOperation, ApiTags } from '@nestjs/swagger';

@Controller('health')
@ApiTags('health')
export class HealthController {
  @Get()
  @ApiOperation({ summary: 'ヘルスチェック' })
  @ApiOkResponse({ description: 'サービスの稼働状態を返却' })
  health(): { status: string; service: string } {
    return {
      status: 'ok',
      service: 'customer-bff',
    };
  }
}
