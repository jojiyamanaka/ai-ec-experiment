import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { RedisService } from '../redis/redis.service';
import { ApiResponse } from '@app/shared';

interface CoreProduct {
  id: number;
  name: string;
  price: number;
  image?: string;
  description?: string;
  category?: string;
  stock: number;
  isPublished?: boolean;
  published?: boolean;
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

    // 1. キャッシュ確認
    const cached = await this.redisService.get<any>(cacheKey);
    if (cached) return { success: true, data: cached };

    // 2. Core API 呼び出し
    const response = await this.coreApiService.get<ApiResponse<CoreProductList>>(
      `/api/item?page=${safePage}&limit=${safeLimit}`,
      undefined,
      traceId,
    );

    if (!response.success || !response.data) {
      return response as ApiResponse<any>;
    }

    const items = response.data.items
      .filter((product) => this.isPublished(product))
      .map((product) => this.transformProduct(product));

    const data = {
      items,
      total: items.length,
      page: response.data.page,
      limit: response.data.limit,
    };

    // 3. キャッシュ保存（TTL: 180秒）
    await this.redisService.set(cacheKey, data, 180);
    return { success: true, data };
  }

  async getProductById(id: number, traceId?: string): Promise<ApiResponse<any>> {
    const cacheKey = `cache:product:${id}`;

    // 1. キャッシュ確認
    const cached = await this.redisService.get<any>(cacheKey);
    if (cached) return { success: true, data: cached };

    // 2. Core API 呼び出し
    const response = await this.coreApiService.get<ApiResponse<CoreProduct>>(
      `/api/item/${id}`,
      undefined,
      traceId,
    );

    if (!response.success || !response.data) {
      return response as ApiResponse<any>;
    }

    if (!this.isPublished(response.data)) {
      return {
        success: false,
        error: {
          code: 'PRODUCT_NOT_FOUND',
          message: '商品が見つかりません',
        },
      };
    }

    const data = this.transformProduct(response.data);

    // 3. キャッシュ保存（TTL: 600秒）
    await this.redisService.set(cacheKey, data, 600);
    return { success: true, data };
  }

  async getProductFull(id: number, traceId?: string): Promise<ApiResponse<any>> {
    const relatedCacheKey = `cache:product:${id}:related`;

    // 商品詳細 + 関連商品キャッシュを並列取得
    const [product, relatedCached] = await Promise.all([
      this.getProductById(id, traceId),
      this.redisService.get<any>(relatedCacheKey),
    ]);

    if (!product.success) return product;

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
    const stockStatus = this.getStockStatus(product.stock);
    const isPublished = this.isPublished(product);
    return {
      id: product.id,
      name: product.name,
      price: Number(product.price),
      image: product.image ?? '',
      imageUrl: product.image ?? '',
      description: product.description ?? '',
      category: product.category ?? '',
      stock: product.stock,
      isPublished,
      stockStatus,
    };
  }

  private isPublished(product: CoreProduct): boolean {
    return Boolean(product.isPublished ?? product.published);
  }

  private getStockStatus(stock: number): string {
    if (stock === 0) {
      return '在庫なし';
    }
    if (stock <= 5) {
      return '残りわずか';
    }
    return '在庫あり';
  }
}
