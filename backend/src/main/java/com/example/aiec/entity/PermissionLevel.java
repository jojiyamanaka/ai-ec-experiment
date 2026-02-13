package com.example.aiec.entity;

public enum PermissionLevel {
    SUPER_ADMIN,  // スーパー管理者（全権限）
    ADMIN,        // 管理者（BoUser 管理以外の全権限）
    OPERATOR      // オペレーター（参照権限のみ）
}
