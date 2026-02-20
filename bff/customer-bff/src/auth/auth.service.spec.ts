import { createHash } from 'crypto';
import { ApiResponse } from '@app/shared';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  const coreApiService = {
    post: jest.fn(),
  };

  const redisService = {
    get: jest.fn(),
    set: jest.fn(),
    del: jest.fn(),
  };

  let service: AuthService;

  beforeEach(() => {
    jest.clearAllMocks();
    service = new AuthService(coreApiService as never, redisService as never);
  });

  it('stores user id in redis session on successful login', async () => {
    const response: ApiResponse<any> = {
      success: true,
      data: {
        user: { id: 42 },
      },
    };

    coreApiService.post.mockResolvedValue(response);
    redisService.get.mockResolvedValue({ cartId: 'abc' });

    await expect(service.login('a@example.com', 'password', 'trace-1', 'session-1')).resolves.toEqual(response);

    expect(coreApiService.post).toHaveBeenCalledWith('/api/auth/login', {
      email: 'a@example.com',
      password: 'password',
    }, undefined, 'trace-1');
    expect(redisService.get).toHaveBeenCalledWith('session:session-1');
    expect(redisService.set).toHaveBeenCalledWith('session:session-1', {
      cartId: 'abc',
      userId: 42,
    }, 7 * 24 * 60 * 60);
  });

  it('does not write redis session when login fails', async () => {
    const response: ApiResponse<any> = {
      success: false,
      error: {
        code: 'INVALID_CREDENTIALS',
        message: 'ログイン失敗',
      },
    };

    coreApiService.post.mockResolvedValue(response);

    await expect(service.login('a@example.com', 'password', 'trace-1', 'session-1')).resolves.toEqual(response);

    expect(redisService.set).not.toHaveBeenCalled();
  });

  it('removes auth token cache on successful logout', async () => {
    const response: ApiResponse<void> = {
      success: true,
      data: undefined,
    };
    const token = 'token-value';
    const expectedHash = createHash('sha256').update(token).digest('hex').slice(0, 32);

    coreApiService.post.mockResolvedValue(response);

    await expect(service.logout(token, 'trace-2')).resolves.toEqual(response);

    expect(coreApiService.post).toHaveBeenCalledWith('/api/auth/logout', {}, token, 'trace-2');
    expect(redisService.del).toHaveBeenCalledWith(`auth:token:${expectedHash}`);
  });
});
