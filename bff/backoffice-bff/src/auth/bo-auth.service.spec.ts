import { createHash } from 'crypto';
import { ApiResponse } from '@app/shared';
import { BoAuthService } from './bo-auth.service';

describe('BoAuthService', () => {
  const coreApiService = {
    post: jest.fn(),
  };

  const redisService = {
    del: jest.fn(),
  };

  let service: BoAuthService;

  beforeEach(() => {
    jest.clearAllMocks();
    service = new BoAuthService(coreApiService as never, redisService as never);
  });

  it('forwards login request to core api', async () => {
    const response: ApiResponse<any> = {
      success: true,
      data: { token: 'raw-token' },
    };
    coreApiService.post.mockResolvedValue(response);

    await expect(service.login('admin@example.com', 'password', 'trace-1')).resolves.toEqual(response);

    expect(coreApiService.post).toHaveBeenCalledWith('/api/bo-auth/login', {
      email: 'admin@example.com',
      password: 'password',
    }, undefined, 'trace-1');
  });

  it('removes bo-auth token cache on successful logout', async () => {
    const response: ApiResponse<void> = {
      success: true,
      data: undefined,
    };
    const token = 'bo-token';
    const expectedHash = createHash('sha256').update(token).digest('hex').slice(0, 32);

    coreApiService.post.mockResolvedValue(response);

    await expect(service.logout(token, 'trace-2')).resolves.toEqual(response);

    expect(coreApiService.post).toHaveBeenCalledWith('/api/bo-auth/logout', {}, token, 'trace-2');
    expect(redisService.del).toHaveBeenCalledWith(`bo-auth:token:${expectedHash}`);
  });

  it('does not remove bo-auth token cache when logout fails', async () => {
    const response: ApiResponse<void> = {
      success: false,
      error: {
        code: 'INVALID_TOKEN',
        message: '無効なトークンです',
      },
    };

    coreApiService.post.mockResolvedValue(response);

    await expect(service.logout('token', 'trace-3')).resolves.toEqual(response);

    expect(redisService.del).not.toHaveBeenCalled();
  });
});
