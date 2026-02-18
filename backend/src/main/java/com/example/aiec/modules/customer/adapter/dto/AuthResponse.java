package com.example.aiec.modules.customer.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 認証レスポンス（登録・ログイン時）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private UserDto user;
    private String token;
    private Instant expiresAt;

}
