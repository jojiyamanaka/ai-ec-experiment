import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CartProvider, useCart } from './CartContext'
import * as cartApi from '@entities/cart'

vi.mock('@entities/cart', () => ({
  getCart: vi.fn(),
  addToCart: vi.fn(),
  updateCartItemQuantity: vi.fn(),
  removeFromCart: vi.fn(),
}))

function wrapper({ children }: { children: ReactNode }) {
  return <CartProvider>{children}</CartProvider>
}

function buildProduct() {
  return {
    id: 1,
    productCode: 'P000001',
    name: '商品A',
    categoryId: 1,
    categoryName: 'カテゴリ',
    price: 1200,
    image: '/img/a.jpg',
    description: 'desc',
    allocationType: 'REAL' as const,
    effectiveStock: 20,
    isPublished: true,
    publishStartAt: null,
    publishEndAt: null,
    saleStartAt: null,
    saleEndAt: null,
  }
}

describe('CartContext', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(cartApi.getCart).mockResolvedValue({
      success: true,
      data: {
        items: [],
        totalQuantity: 0,
        totalPrice: 0,
      },
    })
  })

  it('loads cart on mount', async () => {
    const { result } = renderHook(() => useCart(), { wrapper })

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(cartApi.getCart).toHaveBeenCalledTimes(1)
    expect(result.current.totalQuantity).toBe(0)
  })

  it('updates cart after addToCart succeeds', async () => {
    vi.mocked(cartApi.addToCart).mockResolvedValue({
      success: true,
      data: {
        items: [{ id: 1, product: buildProduct(), quantity: 2 }],
        totalQuantity: 2,
        totalPrice: 2400,
      },
    })

    const { result } = renderHook(() => useCart(), { wrapper })
    await waitFor(() => expect(result.current.loading).toBe(false))

    await act(async () => {
      await result.current.addToCart(buildProduct(), 2)
    })

    expect(result.current.totalQuantity).toBe(2)
    expect(result.current.totalPrice).toBe(2400)
  })

  it('calls removeFromCart when updateQuantity is less than or equal to 0', async () => {
    vi.mocked(cartApi.removeFromCart).mockResolvedValue({
      success: true,
      data: {
        items: [],
        totalQuantity: 0,
        totalPrice: 0,
      },
    })

    const { result } = renderHook(() => useCart(), { wrapper })
    await waitFor(() => expect(result.current.loading).toBe(false))

    await act(async () => {
      await result.current.updateQuantity(10, 0)
    })

    expect(cartApi.removeFromCart).toHaveBeenCalledWith(10)
    expect(cartApi.updateCartItemQuantity).not.toHaveBeenCalled()
  })

  it('skips updateCartItemQuantity when quantity is greater than 9', async () => {
    const { result } = renderHook(() => useCart(), { wrapper })
    await waitFor(() => expect(result.current.loading).toBe(false))

    await act(async () => {
      await result.current.updateQuantity(11, 10)
    })

    expect(cartApi.updateCartItemQuantity).not.toHaveBeenCalled()
    expect(cartApi.removeFromCart).not.toHaveBeenCalled()
  })
})
