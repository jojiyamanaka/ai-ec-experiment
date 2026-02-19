export interface BoUser {
  id: number
  email: string
  displayName: string
  permissionLevel: 'SUPER_ADMIN' | 'ADMIN' | 'OPERATOR'
  lastLoginAt?: string
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface BoAuthResponse {
  user: BoUser
  token: string
  expiresAt: string
}

export interface BoLoginRequest {
  email: string
  password: string
}
