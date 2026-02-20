import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { BoAuthProvider, useBoAuth } from './BoAuthContext'
import * as boApi from '@entities/bo-user'

vi.mock('@entities/bo-user', () => ({
  boLogin: vi.fn(),
  boLogout: vi.fn(),
  getBoUser: vi.fn(),
}))

function wrapper({ children }: { children: ReactNode }) {
  return <BoAuthProvider>{children}</BoAuthProvider>
}

function buildBoUser() {
  return {
    id: 1,
    email: 'admin@example.com',
    displayName: 'admin',
    permissionLevel: 'ADMIN' as const,
    isActive: true,
    createdAt: '2026-02-20T00:00:00Z',
    updatedAt: '2026-02-20T00:00:00Z',
  }
}

describe('BoAuthContext', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('restores bo user when token exists and getBoUser succeeds', async () => {
    localStorage.setItem('bo_token', 'bo-token')
    vi.mocked(boApi.getBoUser).mockResolvedValue({
      success: true,
      data: buildBoUser(),
    })

    const { result } = renderHook(() => useBoAuth(), { wrapper })

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
      expect(result.current.boUser?.email).toBe('admin@example.com')
    })
  })

  it('removes stale token when getBoUser fails', async () => {
    localStorage.setItem('bo_token', 'stale-token')
    vi.mocked(boApi.getBoUser).mockResolvedValue({
      success: false,
      error: {
        code: 'UNAUTHORIZED',
        message: '認証が必要です',
      },
    })

    const { result } = renderHook(() => useBoAuth(), { wrapper })

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })
    expect(localStorage.getItem('bo_token')).toBeNull()
    expect(result.current.boUser).toBeNull()
  })

  it('saves token and user on successful boLogin', async () => {
    vi.mocked(boApi.boLogin).mockResolvedValue({
      success: true,
      data: {
        user: buildBoUser(),
        token: 'new-bo-token',
        expiresAt: '2026-03-01T00:00:00Z',
      },
    })

    const { result } = renderHook(() => useBoAuth(), { wrapper })
    await waitFor(() => expect(result.current.loading).toBe(false))

    await act(async () => {
      await result.current.boLogin('admin@example.com', 'password')
    })

    expect(localStorage.getItem('bo_token')).toBe('new-bo-token')
    expect(result.current.boUser?.email).toBe('admin@example.com')
  })

  it('clears state on bo-auth:unauthorized event', async () => {
    localStorage.setItem('bo_token', 'token')
    vi.mocked(boApi.getBoUser).mockResolvedValue({
      success: true,
      data: buildBoUser(),
    })

    const { result } = renderHook(() => useBoAuth(), { wrapper })
    await waitFor(() => expect(result.current.boUser).not.toBeNull())

    act(() => {
      window.dispatchEvent(new Event('bo-auth:unauthorized'))
    })

    expect(localStorage.getItem('bo_token')).toBeNull()
    expect(result.current.boUser).toBeNull()
    expect(result.current.error).toBeNull()
  })
})
