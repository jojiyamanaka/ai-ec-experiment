import { Injectable, CanActivate, ExecutionContext, HttpException, UnauthorizedException } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';

@Injectable()
export class BoAuthGuard implements CanActivate {
  constructor(private coreApiService: CoreApiService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest();
    const token = this.extractToken(request);

    if (!token) {
      throw new UnauthorizedException({
        success: false,
        error: {
          code: 'BFF_UNAUTHORIZED',
          message: '認証トークンが必要です',
        },
      });
    }

    try {
      // Core APIでBoUserトークン検証
      const boUser = await this.verifyBoToken(token, request.traceId);
      request.boUser = boUser;
      request.token = token;
      return true;
    } catch (error) {
      if (error instanceof HttpException) {
        const status = error.getStatus();
        if (status === 503 || status === 504) {
          throw error;
        }
      }
      throw new UnauthorizedException({
        success: false,
        error: {
          code: 'BFF_INVALID_TOKEN',
          message: '無効なトークンです',
        },
      });
    }
  }

  private extractToken(request: any): string | null {
    const authHeader = request.headers['authorization'];
    if (!authHeader?.startsWith('Bearer ')) return null;
    return authHeader.substring(7);
  }

  private async verifyBoToken(token: string, traceId?: string): Promise<any> {
    // 現行Core APIでは /api/bo-auth/me でトークン検証を兼ねる
    const response = await this.coreApiService.get<{ success: boolean; data?: any }>(
      '/api/bo-auth/me',
      token,
      traceId,
    );
    if (!response?.success || !response.data) {
      throw new Error('invalid token');
    }
    return response.data;
  }
}
