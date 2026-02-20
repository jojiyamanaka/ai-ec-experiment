package com.example.aiec.modules.customer.adapter.dto;

import com.example.aiec.modules.customer.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ユーザーDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ユーザー情報")
public class UserDto {

    @Schema(description = "ユーザーID", example = "1")
    private Long id;
    @Schema(description = "メールアドレス", example = "user@example.com")
    private String email;
    @Schema(description = "表示名", example = "田中太郎")
    private String displayName;
    @Schema(description = "氏名", example = "田中 太郎")
    private String fullName;
    @Schema(description = "電話番号", example = "09012345678")
    private String phoneNumber;
    @Schema(description = "生年月日")
    private LocalDate birthDate;
    @Schema(description = "メルマガ購読", example = "false")
    private Boolean newsletterOptIn;
    @Schema(description = "会員ランク", example = "STANDARD")
    private User.MemberRank memberRank;
    @Schema(description = "ポイント", example = "0")
    private Integer loyaltyPoints;
    @Schema(description = "有効状態", example = "true")
    private Boolean isActive;
    @Schema(description = "登録日時")
    private Instant createdAt;
    @Schema(description = "更新日時")
    private Instant updatedAt;
    @Schema(description = "住所一覧")
    private List<UserAddressDto> addresses = new ArrayList<>();

    /**
     * エンティティから DTO を生成
     */
    public static UserDto fromEntity(User user) {
        return fromEntity(user, List.of());
    }

    /**
     * エンティティから DTO を生成（住所込み）
     */
    public static UserDto fromEntity(User user, List<UserAddressDto> addresses) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setDisplayName(user.getDisplayName());
        dto.setFullName(user.getFullName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setBirthDate(user.getBirthDate());
        dto.setNewsletterOptIn(user.getNewsletterOptIn());
        dto.setMemberRank(user.getMemberRank());
        dto.setLoyaltyPoints(user.getLoyaltyPoints());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setAddresses(addresses);
        return dto;
    }

}
