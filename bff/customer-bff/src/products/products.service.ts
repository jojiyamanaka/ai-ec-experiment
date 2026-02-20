import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';
import { ApiResponse } from '@app/shared';

type AllocationType = 'REAL' | 'FRAME';

interface CoreProduct {
  id: number;
  productCode?: string;
  name: string;
  price: number;
  image?: string;
  description?: string;
  categoryId?: number;
  categoryName?: string;
  category?: string;
  allocationType?: AllocationType;
  effectiveStock?: number;
  isPublished?: boolean;
  published?: boolean;
  publishStartAt?: string | number;
  publishEndAt?: string | number;
  saleStartAt?: string | number;
  saleEndAt?: string | number;
}

interface CoreProductList {
  items: CoreProduct[];
  total: number;
  page: number;
  limit: number;
}

@Injectable()
export class ProductsService {
  constructor(
    private coreApiService: CoreApiService,
    private redisService: RedisService,
  ) {}

  async getProducts(page: number, limit: number, traceId?: string): Promise<ApiResponse<any>> {
    const safePage = Number.isFinite(page) && page > 0 ? page : 1;
    const safeLimit = Number.isFinite(limit) && limit > 0 ? limit : 20;
    const cacheKey = `cache:products:list:page:${safePage}:limit:${safeLimit}`;

    const cached = await this.redisService.get<any>(cacheKey);
    if (cached) {
      return { success: true, data: cached };
    }

    const response = await this.coreApiService.get<ApiResponse<CoreProductList>>(
      `/api/item?page=${safePage}&limit=${safeLimit}`,
      undefined,
      traceId,
    );

    if (!response.success || !response.data) {
      return response as ApiResponse<any>;
    }

    const items = response.data.items
      .filter((product) => this.isVisible(product))
      .map((product) => this.transformProduct(product));

    const data = {
      items,
      total: items.length,
      page: response.data.page,
      limit: response.data.limit,
    };

    await this.redisService.set(cacheKey, data, 180);
    return { success: true, data };
  }

  async getProductById(id: number, traceId?: string): Promise<ApiResponse<any>> {
    const cacheKey = `cache:product:${id}`;

    const cached = await this.redisService.get<any>(cacheKey);
    if (cached) {
      return { success: true, data: cached };
    }

    const response = await this.coreApiService.get<ApiResponse<CoreProduct>>(
      `/api/item/${id}`,
      undefined,
      traceId,
    );

    if (!response.success || !response.data) {
      return response as ApiResponse<any>;
    }

    if (!this.isVisible(response.data)) {
      return {
        success: false,
        error: {
          code: 'PRODUCT_NOT_FOUND',
          message: '商品が見つかりません',
        },
      };
    }

    const data = this.transformProduct(response.data);
    await this.redisService.set(cacheKey, data, 600);
    return { success: true, data };
  }

  async getProductFull(id: number, traceId?: string): Promise<ApiResponse<any>> {
    const relatedCacheKey = `cache:product:${id}:related`;

    const [product, relatedCached] = await Promise.all([
      this.getProductById(id, traceId),
      this.redisService.get<any>(relatedCacheKey),
    ]);

    if (!product.success) {
      return product;
    }

    let relatedProducts: any[];
    if (relatedCached) {
      relatedProducts = relatedCached;
    } else {
      const category = product.data.category ?? '';
      const relatedResponse = await this.coreApiService.get<ApiResponse<CoreProductList>>(
        `/api/item?limit=4${category ? `&category=${encodeURIComponent(category)}` : ''}`,
        undefined,
        traceId,
      );
      relatedProducts = ((relatedResponse.data?.items ?? []) as CoreProduct[])
        .filter((p) => p.id !== id)
        .slice(0, 4)
        .map((p) => this.transformProduct(p));
      await this.redisService.set(relatedCacheKey, relatedProducts, 180);
    }

    return {
      success: true,
      data: {
        product: product.data,
        relatedProducts,
      },
    };
  }

  transformProduct(product: CoreProduct): any {
    const effectiveStock = this.resolveEffectiveStock(product);
    const stockStatus = this.getStockStatus(effectiveStock);
    const isPublished = this.isPublished(product);
    const categoryName = product.categoryName ?? product.category ?? '';
    const publishStartAt = product.publishStartAt ?? null;
    const publishEndAt = product.publishEndAt ?? null;
    const saleStartAt = product.saleStartAt ?? null;
    const saleEndAt = product.saleEndAt ?? null;

    return {
      id: product.id,
      productCode: product.productCode ?? '',
      name: product.name,
      price: Number(product.price),
      image: product.image ?? '',
      imageUrl: product.image ?? '',
      description: product.description ?? '',
      categoryId: product.categoryId ?? null,
      categoryName,
      category: categoryName,
      allocationType: product.allocationType ?? 'REAL',
      effectiveStock,
      isPublished,
      publishStartAt,
      publishEndAt,
      saleStartAt,
      saleEndAt,
      stockStatus,
    };
  }

  private resolveEffectiveStock(product: CoreProduct): number {
    return Number(product.effectiveStock ?? 0);
  }

  private isVisible(product: CoreProduct): boolean {
    return this.isPublished(product) && this.isWithinWindow(product.publishStartAt, product.publishEndAt);
  }

  private isPublished(product: CoreProduct): boolean {
    return Boolean(product.isPublished ?? product.published);
  }

  private isWithinWindow(start?: string | number, end?: string | number): boolean {
    const now = Date.now();
    const startMs = this.toEpochMillis(start);
    const endMs = this.toEpochMillis(end);
    if (startMs !== null && now < startMs) {
      return false;
    }
    if (endMs !== null && now > endMs) {
      return false;
    }
    return true;
  }

  private toEpochMillis(value?: string | number): number | null {
    if (value === null || value === undefined) {
      return null;
    }
    if (typeof value === 'number') {
      return value < 10_000_000_000 ? Math.floor(value * 1000) : Math.floor(value);
    }
    const parsed = Date.parse(value);
    return Number.isNaN(parsed) ? null : parsed;
  }

  private getStockStatus(effectiveStock: number): string {
    if (effectiveStock === 0) {
      return '在庫なし';
    }
    if (effectiveStock <= 5) {
      return '残りわずか';
    }
    return '在庫あり';
  }
}
