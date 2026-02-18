package com.example.aiec.modules.customer.adapter.dto;

import com.example.aiec.modules.customer.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

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
    @Schema(description = "有効状態", example = "true")
    private Boolean isActive;
    @Schema(description = "登録日時")
    private Instant createdAt;

    /**
     * エンティティから DTO を生成
     */
    public static UserDto fromEntity(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setDisplayName(user.getDisplayName());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

}
