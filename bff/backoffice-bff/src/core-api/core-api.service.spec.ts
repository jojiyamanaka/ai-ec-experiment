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

  it('returns data from core api response', async () => {
    const response = {
      data: { success: true, data: { id: 1 } },
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {} as never,
    } as AxiosResponse;
    httpService.get.mockReturnValue(of(response));

    await expect(service.get('/api/order', 'token', 'trace-id')).resolves.toEqual({ success: true, data: { id: 1 } });

    expect(httpService.get).toHaveBeenCalledWith('http://localhost:8080/api/order', {
      headers: {
        Authorization: 'Bearer token',
        'X-Trace-Id': 'trace-id',
      },
    });
  });

  it('maps unavailable error to BFF_CORE_API_UNAVAILABLE', async () => {
    httpService.get.mockReturnValue(throwError(() => ({ code: 'ENOTFOUND' })));

    try {
      await service.get('/api/order');
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

  it('maps unknown error to BFF_INTERNAL_ERROR', async () => {
    httpService.post.mockReturnValue(throwError(() => ({ message: 'unexpected' })));

    try {
      await service.post('/api/order/1/confirm', {});
      throw new Error('should throw');
    } catch (error) {
      const exception = error as HttpException;
      expect(exception.getStatus()).toBe(500);
      expect(exception.getResponse()).toMatchObject({
        error: {
          code: 'BFF_INTERNAL_ERROR',
        },
      });
    }
  });
});
