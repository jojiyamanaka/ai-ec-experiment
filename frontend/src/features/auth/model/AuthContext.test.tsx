import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider, useAuth } from './AuthContext'
import * as customerApi from '@entities/customer'

vi.mock('@entities/customer', () => ({
  getCurrentUser: vi.fn(),
  register: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
}))

function wrapper({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>
}

function buildUser() {
  return {
    id: 1,
    email: 'member01@example.com',
    displayName: 'member01',
    createdAt: '2026-02-20T00:00:00Z',
  }
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('finishes loading immediately when no auth token exists', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper })

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })
    expect(result.current.isAuthenticated).toBe(false)
  })

  it('restores user when token exists and getCurrentUser succeeds', async () => {
    localStorage.setItem('authToken', 'saved-token')
    vi.mocked(customerApi.getCurrentUser).mockResolvedValue({
      success: true,
      data: buildUser(),
    })

    const { result } = renderHook(() => useAuth(), { wrapper })

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
      expect(result.current.user?.email).toBe('member01@example.com')
    })
    expect(result.current.token).toBe('saved-token')
    expect(result.current.isAuthenticated).toBe(true)
  })

  it('saves auth state on successful login', async () => {
    vi.mocked(customerApi.login).mockResolvedValue({
      success: true,
      data: {
        user: buildUser(),
        token: 'new-token',
        expiresAt: '2026-03-01T00:00:00Z',
      },
    })

    const { result } = renderHook(() => useAuth(), { wrapper })
    await waitFor(() => expect(result.current.loading).toBe(false))

    await act(async () => {
      await result.current.login('member01@example.com', 'password')
    })

    expect(result.current.isAuthenticated).toBe(true)
    expect(localStorage.getItem('authToken')).toBe('new-token')
    expect(localStorage.getItem('authUser')).toContain('member01@example.com')
  })

  it('clears auth state when unauthorized event is emitted', async () => {
    localStorage.setItem('authToken', 'saved-token')
    vi.mocked(customerApi.getCurrentUser).mockResolvedValue({
      success: true,
      data: buildUser(),
    })

    const { result } = renderHook(() => useAuth(), { wrapper })
    await waitFor(() => expect(result.current.isAuthenticated).toBe(true))

    act(() => {
      window.dispatchEvent(new Event('auth:unauthorized'))
    })

    expect(result.current.user).toBeNull()
    expect(result.current.token).toBeNull()
    expect(result.current.isAuthenticated).toBe(false)
  })
})
