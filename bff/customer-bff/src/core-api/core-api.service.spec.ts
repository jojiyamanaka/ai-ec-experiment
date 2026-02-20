import { HttpException } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { of, throwError } from 'rxjs';
import { AxiosResponse } from 'axios';
import { CoreApiService } from './core-api.service';

describe('CoreApiService', () => {
  const configMap: Record<string, string | number> = {
    'coreApi.url': 'http://localhost:8080',
    'coreApi.timeout': 1000,
    'coreApi.retry': 0,
  };

  let httpService: jest.Mocked<Pick<HttpService, 'get' | 'post' | 'put' | 'delete'>>;
  let configService: Pick<ConfigService, 'get'>;
  let service: CoreApiService;

  beforeEach(() => {
    httpService = {
      get: jest.fn(),
      post: jest.fn(),
      put: jest.fn(),
      delete: jest.fn(),
    };
    configService = {
      get: jest.fn((key: string) => configMap[key]),
    };
    service = new CoreApiService(httpService as unknown as HttpService, configService as ConfigService);
  });

  it('returns data when core api responds successfully', async () => {
    const response = {
      data: { success: true, data: [1, 2, 3] },
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {} as never,
    } as AxiosResponse;
    httpService.get.mockReturnValue(of(response));

    await expect(service.get('/api/sample', 'token', 'trace-id')).resolves.toEqual({ success: true, data: [1, 2, 3] });

    expect(httpService.get).toHaveBeenCalledWith('http://localhost:8080/api/sample', {
      headers: {
        Authorization: 'Bearer token',
        'X-Trace-Id': 'trace-id',
      },
    });
  });

  it('maps timeout error to BFF_CORE_API_TIMEOUT', async () => {
    httpService.get.mockReturnValue(throwError(() => ({ name: 'TimeoutError' })));

    try {
      await service.get('/api/sample');
      throw new Error('should throw');
    } catch (error) {
      const exception = error as HttpException;
      expect(exception.getStatus()).toBe(504);
      expect(exception.getResponse()).toMatchObject({
        error: {
          code: 'BFF_CORE_API_TIMEOUT',
        },
      });
    }
  });

  it('maps unavailable error to BFF_CORE_API_UNAVAILABLE', async () => {
    httpService.get.mockReturnValue(throwError(() => ({ code: 'ECONNREFUSED' })));

    try {
      await service.get('/api/sample');
      throw new Error('should throw');
    } catch (error) {
      const exception = error as HttpException;
      expect(exception.getStatus()).toBe(503);
      expect(exception.getResponse()).toMatchObject({
        error: {
          code: 'BFF_CORE_API_UNAVAILABLE',
        },
      });
    }
  });

  it('passes through core error response when success=false payload exists', async () => {
    httpService.post.mockReturnValue(throwError(() => ({
      response: {
        status: 409,
        data: {
          success: false,
          error: {
            code: 'EMAIL_ALREADY_EXISTS',
            message: '既に登録済みです',
          },
        },
      },
    })));

    try {
      await service.post('/api/auth/register', {});
      throw new Error('should throw');
    } catch (error) {
      const exception = error as HttpException;
      expect(exception.getStatus()).toBe(409);
      expect(exception.getResponse()).toMatchObject({
        success: false,
        error: {
          code: 'EMAIL_ALREADY_EXISTS',
        },
      });
    }
  });
});
