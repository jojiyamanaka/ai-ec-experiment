import { Body, Controller, Delete, Get, Param, Post, Put, Req, UseGuards } from '@nestjs/common';
import { AuthGuard } from '../auth/auth.guard';
import { MembersService } from './members.service';
import { ApiResponse } from '@app/shared';
import {
  ApiBody,
  ApiOkResponse,
  ApiOperation,
  ApiParam,
  ApiTags,
  ApiUnauthorizedResponse,
} from '@nestjs/swagger';

@Controller('api/members')
@UseGuards(AuthGuard)
@ApiTags('members')
export class MembersController {
  constructor(private membersService: MembersService) {}

  @Get('me')
  @ApiOperation({ summary: 'ログインユーザー情報取得' })
  @ApiOkResponse({ description: '会員情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getMe(@Req() req: any): Promise<ApiResponse<any>> {
    return this.membersService.getMe(req.token, req.traceId);
  }

  @Put('me')
  @ApiOperation({ summary: 'ログインユーザー情報更新' })
  @ApiBody({
    schema: {
      properties: {
        displayName: { type: 'string' },
        fullName: { type: 'string' },
        phoneNumber: { type: 'string' },
        birthDate: { type: 'string', format: 'date' },
        newsletterOptIn: { type: 'boolean' },
      },
    },
  })
  @ApiOkResponse({ description: '更新後会員情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async updateMe(@Body() body: Record<string, unknown>, @Req() req: any): Promise<ApiResponse<any>> {
    return this.membersService.updateMe(body, req.token, req.traceId);
  }

  @Post('me/addresses')
  @ApiOperation({ summary: 'ログインユーザー住所追加' })
  @ApiBody({
    schema: {
      properties: {
        label: { type: 'string' },
        recipientName: { type: 'string' },
        recipientPhoneNumber: { type: 'string' },
        postalCode: { type: 'string' },
        prefecture: { type: 'string' },
        city: { type: 'string' },
        addressLine1: { type: 'string' },
        addressLine2: { type: 'string' },
        isDefault: { type: 'boolean' },
        addressOrder: { type: 'number' },
      },
      required: ['recipientName', 'postalCode', 'prefecture', 'city', 'addressLine1'],
    },
  })
  @ApiOkResponse({ description: '追加後住所を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async addMyAddress(@Body() body: Record<string, unknown>, @Req() req: any): Promise<ApiResponse<any>> {
    return this.membersService.addMyAddress(body, req.token, req.traceId);
  }

  @Put('me/addresses/:addressId')
  @ApiOperation({ summary: 'ログインユーザー住所更新' })
  @ApiParam({ name: 'addressId', type: String })
  @ApiBody({
    schema: {
      properties: {
        label: { type: 'string' },
        recipientName: { type: 'string' },
        recipientPhoneNumber: { type: 'string' },
        postalCode: { type: 'string' },
        prefecture: { type: 'string' },
        city: { type: 'string' },
        addressLine1: { type: 'string' },
        addressLine2: { type: 'string' },
        isDefault: { type: 'boolean' },
        addressOrder: { type: 'number' },
      },
      required: ['recipientName', 'postalCode', 'prefecture', 'city', 'addressLine1'],
    },
  })
  @ApiOkResponse({ description: '更新後住所を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async updateMyAddress(
    @Param('addressId') addressId: string,
    @Body() body: Record<string, unknown>,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.membersService.updateMyAddress(parseInt(addressId, 10), body, req.token, req.traceId);
  }

  @Delete('me/addresses/:addressId')
  @ApiOperation({ summary: 'ログインユーザー住所削除' })
  @ApiParam({ name: 'addressId', type: String })
  @ApiOkResponse({ description: '削除結果を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async deleteMyAddress(
    @Param('addressId') addressId: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.membersService.deleteMyAddress(parseInt(addressId, 10), req.token, req.traceId);
  }
}
