export interface User {
  id: number
  email: string
  displayName: string
  isActive?: boolean
  createdAt: string
  updatedAt?: string
}

export interface AuthResponse {
  user: User
  token: string
  expiresAt: string
}

export interface RegisterRequest {
  email: string
  displayName: string
  password: string
}

export interface LoginRequest {
  email: string
  password: string
}
