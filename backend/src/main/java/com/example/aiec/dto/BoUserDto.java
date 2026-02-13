package com.example.aiec.dto;

import com.example.aiec.entity.BoUser;
import com.example.aiec.entity.PermissionLevel;
import lombok.Data;
import java.time.Instant;

@Data
public class BoUserDto {
    private Long id;
    private String email;
    private String displayName;
    private PermissionLevel permissionLevel;
    private Instant lastLoginAt;
    private Boolean isActive;
    private Instant createdAt;
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
