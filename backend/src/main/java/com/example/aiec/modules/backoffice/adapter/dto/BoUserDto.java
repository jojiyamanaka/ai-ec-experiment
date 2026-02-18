package com.example.aiec.modules.backoffice.adapter.dto;

import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Data
@Schema(description = "管理者ユーザー情報")
public class BoUserDto {
    @Schema(description = "管理者ID", example = "1")
    private Long id;
    @Schema(description = "メールアドレス", example = "admin@example.com")
    private String email;
    @Schema(description = "表示名", example = "管理者")
    private String displayName;
    @Schema(description = "権限レベル", example = "ADMIN")
    private PermissionLevel permissionLevel;
    @Schema(description = "最終ログイン日時")
    private Instant lastLoginAt;
    @Schema(description = "有効状態", example = "true")
    private Boolean isActive;
    @Schema(description = "作成日時")
    private Instant createdAt;
    @Schema(description = "更新日時")
    private Instant updatedAt;

    public static BoUserDto fromEntity(BoUser boUser) {
        BoUserDto dto = new BoUserDto();
        dto.setId(boUser.getId());
        dto.setEmail(boUser.getEmail());
        dto.setDisplayName(boUser.getDisplayName());
        dto.setPermissionLevel(boUser.getPermissionLevel());
        dto.setLastLoginAt(boUser.getLastLoginAt());
        dto.setIsActive(boUser.getIsActive());
        dto.setCreatedAt(boUser.getCreatedAt());
        dto.setUpdatedAt(boUser.getUpdatedAt());
        return dto;
    }
}
