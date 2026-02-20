export interface User {
  id: number
  email: string
  displayName: string
  fullName?: string
  phoneNumber?: string
  birthDate?: string
  newsletterOptIn?: boolean
  memberRank?: MemberRank
  loyaltyPoints?: number
  deactivationReason?: string
  lastLoginAt?: string
  termsAgreedAt?: string
  isActive?: boolean
  createdAt: string
  updatedAt?: string
  addresses?: UserAddress[]
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

export type MemberRank = 'STANDARD' | 'SILVER' | 'GOLD' | 'PLATINUM'

export interface UserAddress {
  id: number
  label?: string
  recipientName: string
  recipientPhoneNumber?: string
  postalCode: string
  prefecture: string
  city: string
  addressLine1: string
  addressLine2?: string
  isDefault: boolean
  addressOrder: number
}

export interface UpdateMyProfileRequest {
  displayName?: string
  fullName?: string
  phoneNumber?: string
  birthDate?: string
  newsletterOptIn?: boolean
}

export interface UpsertAddressRequest {
  id?: number
  label?: string
  recipientName: string
  recipientPhoneNumber?: string
  postalCode: string
  prefecture: string
  city: string
  addressLine1: string
  addressLine2?: string
  isDefault?: boolean
  addressOrder?: number
  deleted?: boolean
}

export interface CreateMemberRequest {
  email: string
  displayName: string
  password: string
  fullName?: string
  phoneNumber?: string
  birthDate?: string
  newsletterOptIn?: boolean
  memberRank?: MemberRank
  loyaltyPoints?: number
  isActive?: boolean
  deactivationReason?: string
  addresses?: UpsertAddressRequest[]
}

export interface UpdateMemberRequest {
  displayName?: string
  fullName?: string
  phoneNumber?: string
  birthDate?: string
  newsletterOptIn?: boolean
  memberRank?: MemberRank
  loyaltyPoints?: number
  deactivationReason?: string
  isActive?: boolean
  addresses?: UpsertAddressRequest[]
}
