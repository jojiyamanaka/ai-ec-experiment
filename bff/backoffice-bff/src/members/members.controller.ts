import { Controller, Get, Put, Post, Param, Body, UseGuards, Req } from '@nestjs/common';
import { BoAuthGuard } from '../auth/bo-auth.guard';
import { MembersService } from './members.service';
import { ApiResponse } from '@app/shared';
import { ApiBody, ApiOkResponse, ApiOperation, ApiParam, ApiTags, ApiUnauthorizedResponse } from '@nestjs/swagger';

@Controller(['api/admin/members', 'api/bo/admin/members'])
@UseGuards(BoAuthGuard)
@ApiTags('members')
export class MembersController {
  constructor(private membersService: MembersService) {}

  @Get()
  @ApiOperation({ summary: '会員一覧取得' })
  @ApiOkResponse({ description: '会員一覧を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getMembers(@Req() req: any): Promise<ApiResponse<any>> {
    return this.membersService.getMembers(req.token, req.traceId);
  }

  @Get(':id')
  @ApiOperation({ summary: '会員詳細取得' })
  @ApiParam({ name: 'id', type: String })
  @ApiOkResponse({ description: '会員詳細を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async getMemberById(
    @Param('id') id: string,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.membersService.getMemberById(parseInt(id, 10), req.token, req.traceId);
  }

  @Post()
  @ApiOperation({ summary: '会員新規作成' })
  @ApiBody({
    schema: {
      properties: {
        email: { type: 'string' },
        displayName: { type: 'string' },
        password: { type: 'string' },
        fullName: { type: 'string' },
        phoneNumber: { type: 'string' },
        birthDate: { type: 'string', format: 'date' },
        newsletterOptIn: { type: 'boolean' },
        memberRank: { type: 'string' },
        loyaltyPoints: { type: 'number' },
        isActive: { type: 'boolean' },
        deactivationReason: { type: 'string' },
        addresses: {
          type: 'array',
          items: {
            type: 'object',
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
          },
        },
      },
      required: ['email', 'displayName', 'password'],
    },
  })
  @ApiOkResponse({ description: '作成後会員情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async createMember(
    @Body() body: Record<string, unknown>,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.membersService.createMember(body, req.token, req.traceId);
  }

  @Put(':id')
  @ApiOperation({ summary: '会員情報更新' })
  @ApiParam({ name: 'id', type: String })
  @ApiBody({
    schema: {
      properties: {
        displayName: { type: 'string' },
        fullName: { type: 'string' },
        phoneNumber: { type: 'string' },
        birthDate: { type: 'string', format: 'date' },
        newsletterOptIn: { type: 'boolean' },
        memberRank: { type: 'string' },
        loyaltyPoints: { type: 'number' },
        deactivationReason: { type: 'string' },
        isActive: { type: 'boolean' },
        addresses: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              id: { type: 'number' },
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
              deleted: { type: 'boolean' },
            },
          },
        },
      },
    },
  })
  @ApiOkResponse({ description: '更新後会員情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async updateMember(
    @Param('id') id: string,
    @Body() body: Record<string, unknown>,
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.membersService.updateMember(parseInt(id, 10), body, req.token, req.traceId);
  }

  @Put(':id/status')
  @ApiOperation({ summary: '会員有効状態更新' })
  @ApiParam({ name: 'id', type: String })
  @ApiBody({
    schema: {
      properties: {
        isActive: { type: 'boolean' },
      },
      required: ['isActive'],
    },
  })
  @ApiOkResponse({ description: '更新後会員情報を返却' })
  @ApiUnauthorizedResponse({ description: '認証エラー' })
  async updateMemberStatus(
    @Param('id') id: string,
    @Body() body: { isActive: boolean },
    @Req() req: any,
  ): Promise<ApiResponse<any>> {
    return this.membersService.updateMemberStatus(
      parseInt(id, 10),
      body.isActive,
      req.token,
      req.traceId,
    );
  }
}
