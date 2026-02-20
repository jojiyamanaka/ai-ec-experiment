package com.example.aiec.modules.backoffice.adapter.rest;

import com.example.aiec.modules.backoffice.adapter.dto.CreateMemberRequest;
import com.example.aiec.modules.backoffice.adapter.dto.MemberDetailDto;
import com.example.aiec.modules.backoffice.adapter.dto.UpdateMemberRequest;
import com.example.aiec.modules.customer.adapter.dto.UserAddressDto;
import com.example.aiec.modules.customer.adapter.dto.UserDto;
import com.example.aiec.modules.shared.dto.ApiResponse;
import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.shared.domain.model.ActorType;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ForbiddenException;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import com.example.aiec.modules.purchase.order.repository.OrderRepository;
import com.example.aiec.modules.customer.domain.repository.UserRepository;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.customer.domain.service.UserProfileService;
import com.example.aiec.modules.customer.domain.service.UserService;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/bo/admin/members", "/api/admin/members"})
@RequiredArgsConstructor
@Tag(name = "管理（会員）", description = "会員管理")
public class BoAdminController {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final UserProfileService userProfileService;
    private final BoAuthService boAuthService;
    private final OutboxEventPublisher outboxEventPublisher;

    private void requireAdmin(BoUser boUser, String requestPath) {
        if (boUser.getPermissionLevel() != PermissionLevel.ADMIN
            && boUser.getPermissionLevel() != PermissionLevel.SUPER_ADMIN) {
            outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                    "operationType", "AUTHORIZATION_ERROR",
                    "performedBy", boUser.getEmail(),
                    "requestPath", requestPath,
                    "details", "BoUser attempted to access admin resource without permission"));
            throw new ForbiddenException("FORBIDDEN", "この操作を実行する権限がありません");
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("UNAUTHORIZED", "認証が必要です");
        }
        return authHeader.substring(7);
    }

    /**
     * 会員一覧取得
     */
    @GetMapping
    @Operation(summary = "会員一覧取得", description = "全会員の一覧を取得")
    public ApiResponse<List<MemberDetailDto>> getMembers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/bo/admin/members");

        List<MemberDetailDto> members = userRepository.findAll().stream()
                .map(this::toMemberDetailDto)
                .collect(Collectors.toList());

        return ApiResponse.success(members);
    }

    /**
     * 会員詳細取得
     */
    @GetMapping("/{id}")
    @Operation(summary = "会員詳細取得", description = "指定IDの会員詳細と注文サマリを取得")
    public ApiResponse<MemberDetailDto> getMemberById(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/bo/admin/members/" + id);

        User member = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "会員が見つかりません"));

        Long totalOrders = orderRepository.countByUserId(id);
        BigDecimal totalAmount = orderRepository.sumTotalPriceByUserId(id).orElse(BigDecimal.ZERO);

        MemberDetailDto dto = toMemberDetailDto(member);
        MemberDetailDto.OrderSummary orderSummary = new MemberDetailDto.OrderSummary();
        orderSummary.setTotalOrders(totalOrders);
        orderSummary.setTotalAmount(totalAmount);
        dto.setOrderSummary(orderSummary);

        return ApiResponse.success(dto);
    }

    /**
     * 会員新規作成
     */
    @PostMapping
    @Operation(summary = "会員新規作成", description = "管理者が会員を新規作成")
    public ApiResponse<MemberDetailDto> createMember(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody CreateMemberRequest request) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/bo/admin/members");

        if (request.hasDisallowedFields() || hasDisallowedAddressFields(request.getAddresses())) {
            throw new BusinessException("INVALID_REQUEST", "更新対象外の項目が含まれています");
        }

        User created = userService.createUserByAdmin(
                request.getEmail(),
                request.getDisplayName(),
                request.getPassword(),
                request.getFullName(),
                request.getPhoneNumber(),
                request.getBirthDate(),
                request.getNewsletterOptIn(),
                request.getMemberRank(),
                request.getLoyaltyPoints(),
                request.getIsActive(),
                request.getDeactivationReason()
        );

        List<UserProfileService.AddressUpsertCommand> addressCommands = toCreateAddressCommands(request.getAddresses());
        List<UserAddressDto> addresses = userProfileService.applyAddressUpserts(
                        created.getId(),
                        addressCommands,
                        ActorType.BO_USER,
                        boUser.getId())
                .stream()
                .map(UserAddressDto::fromEntity)
                .collect(Collectors.toList());

        outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                "operationType", "ADMIN_ACTION",
                "performedBy", boUser.getEmail(),
                "requestPath", "/api/bo/admin/members",
                "details", "Created member: " + created.getEmail()));

        return ApiResponse.success(MemberDetailDto.fromEntity(created, addresses));
    }

    /**
     * 会員情報更新
     */
    @PutMapping("/{id}")
    @Operation(summary = "会員情報更新", description = "管理者が会員情報を更新")
    public ApiResponse<MemberDetailDto> updateMember(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRequest request) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/bo/admin/members/" + id);

        if (request.hasDisallowedFields() || hasDisallowedAddressFieldsForUpdate(request.getAddresses())) {
            throw new BusinessException("INVALID_REQUEST", "更新対象外の項目が含まれています");
        }

        UserProfileService.ProfileUpdateCommand command = new UserProfileService.ProfileUpdateCommand();
        command.setDisplayName(request.getDisplayName());
        command.setFullName(request.getFullName());
        command.setPhoneNumber(request.getPhoneNumber());
        command.setBirthDate(request.getBirthDate());
        command.setNewsletterOptIn(request.getNewsletterOptIn());
        command.setMemberRank(request.getMemberRank());
        command.setLoyaltyPoints(request.getLoyaltyPoints());
        command.setDeactivationReason(request.getDeactivationReason());
        command.setIsActive(request.getIsActive());

        User updated = userProfileService.updateMemberProfile(id, command, boUser.getId());

        List<UserAddressDto> addresses = userProfileService.applyAddressUpserts(
                        id,
                        toUpdateAddressCommands(request.getAddresses()),
                        ActorType.BO_USER,
                        boUser.getId())
                .stream()
                .map(UserAddressDto::fromEntity)
                .collect(Collectors.toList());

        outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                "operationType", "ADMIN_ACTION",
                "performedBy", boUser.getEmail(),
                "requestPath", "/api/bo/admin/members/" + id,
                "details", "Updated member: " + updated.getEmail()));

        return ApiResponse.success(MemberDetailDto.fromEntity(updated, addresses));
    }

    /**
     * 会員状態変更
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "会員状態変更", description = "会員の有効/無効状態を変更")
    public ApiResponse<UserDto> updateMemberStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/bo/admin/members/" + id + "/status");

        User updated = userService.updateStatus(id, request.getIsActive());

        String details = String.format("Updated member status: %s (%s → %s)",
                updated.getEmail(),
                !request.getIsActive() ? "active" : "inactive",
                request.getIsActive() ? "active" : "inactive");
        outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                "operationType", "ADMIN_ACTION",
                "performedBy", boUser.getEmail(),
                "requestPath", "/api/bo/admin/members/" + id + "/status",
                "details", details));

        return ApiResponse.success(UserDto.fromEntity(updated));
    }

    private MemberDetailDto toMemberDetailDto(User member) {
        List<UserAddressDto> addresses = userProfileService.getAddressesOrEmpty(member.getId()).stream()
                .map(UserAddressDto::fromEntity)
                .collect(Collectors.toList());
        return MemberDetailDto.fromEntity(member, addresses);
    }

    private boolean hasDisallowedAddressFields(List<CreateMemberRequest.AddressUpsert> addresses) {
        if (addresses == null) {
            return false;
        }
        return addresses.stream().anyMatch(CreateMemberRequest.AddressUpsert::hasDisallowedFields);
    }

    private boolean hasDisallowedAddressFieldsForUpdate(List<UpdateMemberRequest.AddressUpsert> addresses) {
        if (addresses == null) {
            return false;
        }
        return addresses.stream().anyMatch(UpdateMemberRequest.AddressUpsert::hasDisallowedFields);
    }

    private List<UserProfileService.AddressUpsertCommand> toCreateAddressCommands(List<CreateMemberRequest.AddressUpsert> addresses) {
        if (addresses == null) {
            return List.of();
        }
        return addresses.stream().map(this::toAddressCommand).collect(Collectors.toList());
    }

    private List<UserProfileService.AddressUpsertCommand> toUpdateAddressCommands(List<UpdateMemberRequest.AddressUpsert> addresses) {
        if (addresses == null) {
            return List.of();
        }
        return addresses.stream().map(this::toAddressCommand).collect(Collectors.toList());
    }

    private UserProfileService.AddressUpsertCommand toAddressCommand(CreateMemberRequest.AddressUpsert address) {
        UserProfileService.AddressUpsertCommand command = new UserProfileService.AddressUpsertCommand();
        command.setId(address.getId());
        command.setLabel(address.getLabel());
        command.setRecipientName(address.getRecipientName());
        command.setRecipientPhoneNumber(address.getRecipientPhoneNumber());
        command.setPostalCode(address.getPostalCode());
        command.setPrefecture(address.getPrefecture());
        command.setCity(address.getCity());
        command.setAddressLine1(address.getAddressLine1());
        command.setAddressLine2(address.getAddressLine2());
        command.setIsDefault(address.getIsDefault());
        command.setAddressOrder(address.getAddressOrder());
        command.setDeleted(address.getDeleted());
        return command;
    }

    private UserProfileService.AddressUpsertCommand toAddressCommand(UpdateMemberRequest.AddressUpsert address) {
        UserProfileService.AddressUpsertCommand command = new UserProfileService.AddressUpsertCommand();
        command.setId(address.getId());
        command.setLabel(address.getLabel());
        command.setRecipientName(address.getRecipientName());
        command.setRecipientPhoneNumber(address.getRecipientPhoneNumber());
        command.setPostalCode(address.getPostalCode());
        command.setPrefecture(address.getPrefecture());
        command.setCity(address.getCity());
        command.setAddressLine1(address.getAddressLine1());
        command.setAddressLine2(address.getAddressLine2());
        command.setIsDefault(address.getIsDefault());
        command.setAddressOrder(address.getAddressOrder());
        command.setDeleted(address.getDeleted());
        return command;
    }

    @lombok.Data
    public static class UpdateStatusRequest {
        private Boolean isActive;
    }
}
