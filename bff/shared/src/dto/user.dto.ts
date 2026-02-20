import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class UserAddressDto {
  @ApiProperty()
  id!: number;

  @ApiPropertyOptional()
  label?: string;

  @ApiProperty()
  recipientName!: string;

  @ApiPropertyOptional()
  recipientPhoneNumber?: string;

  @ApiProperty()
  postalCode!: string;

  @ApiProperty()
  prefecture!: string;

  @ApiProperty()
  city!: string;

  @ApiProperty()
  addressLine1!: string;

  @ApiPropertyOptional()
  addressLine2?: string;

  @ApiProperty()
  isDefault!: boolean;

  @ApiProperty()
  addressOrder!: number;
}

export class UserDto {
  @ApiProperty()
  id!: number;

  @ApiProperty()
  email!: string;

  @ApiProperty()
  displayName!: string;

  @ApiPropertyOptional()
  fullName?: string;

  @ApiPropertyOptional()
  phoneNumber?: string;

  @ApiPropertyOptional()
  birthDate?: string;

  @ApiProperty()
  newsletterOptIn!: boolean;

  @ApiProperty()
  memberRank!: string;

  @ApiProperty()
  loyaltyPoints!: number;

  @ApiProperty()
  isActive!: boolean;

  @ApiProperty()
  createdAt!: string;

  @ApiProperty()
  updatedAt!: string;

  @ApiProperty({ type: () => [UserAddressDto] })
  addresses!: UserAddressDto[];
}
