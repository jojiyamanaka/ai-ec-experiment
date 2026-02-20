import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchApi, get, getSessionId } from './client'

describe('shared/api/client', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('creates and persists session id', () => {
    const sessionId = getSessionId()

    expect(sessionId).toMatch(/^session-/)
    expect(localStorage.getItem('sessionId')).toBe(sessionId)
    expect(getSessionId()).toBe(sessionId)
  })

  it('attaches customer auth and X-Session-Id for order endpoint', async () => {
    localStorage.setItem('authToken', 'customer-token')
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ success: true, data: [] }),
    } as Response)

    await get('/order/history')

    const [, options] = vi.mocked(globalThis.fetch).mock.calls[0]
    const headers = new Headers((options as RequestInit).headers)
    expect(headers.get('Authorization')).toBe('Bearer customer-token')
    expect(headers.get('X-Session-Id')).toBeTruthy()
    expect(headers.get('Content-Type')).toBe('application/json')
  })

  it('uses bo token in bo auth context', async () => {
    localStorage.setItem('bo_token', 'bo-token')
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ success: true, data: {} }),
    } as Response)

    await fetchApi('/api/admin/orders', {}, 'bo')

    const [, options] = vi.mocked(globalThis.fetch).mock.calls[0]
    const headers = new Headers((options as RequestInit).headers)
    expect(headers.get('Authorization')).toBe('Bearer bo-token')
  })

  it('does not attach customer token to auth login endpoint', async () => {
    localStorage.setItem('authToken', 'customer-token')
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ success: true, data: {} }),
    } as Response)

    await fetchApi('/api/auth/login', { method: 'POST' })

    const [, options] = vi.mocked(globalThis.fetch).mock.calls[0]
    const headers = new Headers((options as RequestInit).headers)
    expect(headers.get('Authorization')).toBeNull()
  })

  it('clears customer auth storage and emits unauthorized event on 401', async () => {
    localStorage.setItem('authToken', 'token')
    localStorage.setItem('authUser', '{"id":1}')
    const onUnauthorized = vi.fn()
    window.addEventListener('auth:unauthorized', onUnauthorized)
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ success: false, error: { code: 'UNAUTHORIZED' } }),
    } as Response)

    await fetchApi('/api/members/me')

    expect(localStorage.getItem('authToken')).toBeNull()
    expect(localStorage.getItem('authUser')).toBeNull()
    expect(onUnauthorized).toHaveBeenCalledTimes(1)

    window.removeEventListener('auth:unauthorized', onUnauthorized)
  })

  it('clears bo auth storage and emits unauthorized event on bo 401', async () => {
    localStorage.setItem('bo_token', 'bo-token')
    const onUnauthorized = vi.fn()
    window.addEventListener('bo-auth:unauthorized', onUnauthorized)
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ success: false, error: { code: 'UNAUTHORIZED' } }),
    } as Response)

    await fetchApi('/api/bo-auth/me')

    expect(localStorage.getItem('bo_token')).toBeNull()
    expect(onUnauthorized).toHaveBeenCalledTimes(1)

    window.removeEventListener('bo-auth:unauthorized', onUnauthorized)
  })

  it('returns NETWORK_ERROR when fetch throws', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('network down'))

    await expect(fetchApi('/api/products')).resolves.toEqual({
      success: false,
      error: {
        code: 'NETWORK_ERROR',
        message: 'ネットワークエラーが発生しました',
      },
    })
  })
})
