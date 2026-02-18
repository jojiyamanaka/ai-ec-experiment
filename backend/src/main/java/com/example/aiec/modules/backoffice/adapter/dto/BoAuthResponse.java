package com.example.aiec.modules.backoffice.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;

@Data
@AllArgsConstructor
public class BoAuthResponse {
    private BoUserDto user;
    private String token;
    private Instant expiresAt;
}
