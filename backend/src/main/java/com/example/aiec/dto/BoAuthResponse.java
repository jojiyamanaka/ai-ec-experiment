package com.example.aiec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BoAuthResponse {
    private BoUserDto user;
    private String token;
    private LocalDateTime expiresAt;
}
