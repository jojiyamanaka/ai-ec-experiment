import { Navigate, Outlet, useLocation } from 'react-router'
import { useBoAuth } from '../model/BoAuthContext'

function hasBoToken(): boolean {
  if (typeof window === 'undefined') {
    return false
  }
  return Boolean(window.localStorage.getItem('bo_token'))
}

export function RequireBoAuth() {
  const { boUser } = useBoAuth()
  const location = useLocation()

  if (boUser || hasBoToken()) {
    return <Outlet />
  }

  return (
    <Navigate
      to="/bo/login"
      replace
      state={{ from: `${location.pathname}${location.search}` }}
    />
  )
}
