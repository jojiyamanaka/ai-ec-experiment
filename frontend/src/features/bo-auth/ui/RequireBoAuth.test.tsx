import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { RequireBoAuth } from './RequireBoAuth'
import * as BoAuthContext from '../model/BoAuthContext'

vi.mock('../model/BoAuthContext', async () => {
  const actual = await vi.importActual('../model/BoAuthContext')
  return {
    ...actual,
    useBoAuth: vi.fn(),
  }
})

function renderWithRouter() {
  render(
    <MemoryRouter initialEntries={['/protected']}>
      <Routes>
        <Route element={<RequireBoAuth />}>
          <Route path="/protected" element={<div>protected-page</div>} />
        </Route>
        <Route path="/bo/login" element={<div>login-page</div>} />
      </Routes>
    </MemoryRouter>
  )
}

describe('RequireBoAuth', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('renders protected route when boUser exists', () => {
    vi.spyOn(BoAuthContext, 'useBoAuth').mockReturnValue({ boUser: { id: 1 } } as never)

    renderWithRouter()

    expect(screen.getByText('protected-page')).toBeInTheDocument()
  })

  it('renders protected route when bo token exists', () => {
    localStorage.setItem('bo_token', 'token')
    vi.spyOn(BoAuthContext, 'useBoAuth').mockReturnValue({ boUser: null } as never)

    renderWithRouter()

    expect(screen.getByText('protected-page')).toBeInTheDocument()
  })

  it('redirects to login when boUser and token do not exist', () => {
    vi.spyOn(BoAuthContext, 'useBoAuth').mockReturnValue({ boUser: null } as never)

    renderWithRouter()

    expect(screen.getByText('login-page')).toBeInTheDocument()
  })
})
