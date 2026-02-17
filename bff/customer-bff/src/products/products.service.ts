import { Injectable } from '@nestjs/common';
import { CoreApiService } from '../core-api/core-api.service';
import { ApiResponse } from '@app/shared';

interface CoreProduct {
  id: number;
  name: string;
  price: number;
  image?: string;
  description?: string;
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
  constructor(private coreApiService: CoreApiService) {}

  async getProducts(page: number, limit: number, traceId?: string): Promise<ApiResponse<any>> {
    const safePage = Number.isFinite(page) && page > 0 ? page : 1;
    const safeLimit = Number.isFinite(limit) && limit > 0 ? limit : 20;
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

    return {
      success: true,
      data: {
        items,
        total: items.length,
        page: response.data.page,
        limit: response.data.limit,
      },
    };
  }

  async getProductById(id: number, traceId?: string): Promise<ApiResponse<any>> {
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

    return {
      success: true,
      data: this.transformProduct(response.data),
    };
  }

  private transformProduct(product: CoreProduct): any {
    const stockStatus = this.getStockStatus(product.stock);
    const isPublished = this.isPublished(product);
    return {
      id: product.id,
      name: product.name,
      price: Number(product.price),
      image: product.image ?? '',
      imageUrl: product.image ?? '',
      description: product.description ?? '',
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
