package com.example.aiec.modules.customer.adapter.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Schema(description = "会員プロフィール更新リクエスト")
public class UpdateMyProfileRequest {

    @Size(max = 100, message = "表示名は1〜100文字である必要があります")
    @Schema(description = "表示名", example = "田中太郎")
    private String displayName;

    @Size(max = 100, message = "氏名は100文字以内である必要があります")
    @Schema(description = "氏名", example = "田中 太郎")
    private String fullName;

    @Size(max = 30, message = "電話番号は30文字以内である必要があります")
    @Schema(description = "電話番号", example = "09012345678")
    private String phoneNumber;

    @Schema(description = "生年月日")
    private LocalDate birthDate;

    @Schema(description = "メルマガ購読", example = "false")
    private Boolean newsletterOptIn;

    @Schema(hidden = true)
    private final Map<String, Object> disallowedFields = new LinkedHashMap<>();

    @JsonAnySetter
    void captureUnknownField(String key, Object value) {
        disallowedFields.put(key, value);
    }

    public boolean hasDisallowedFields() {
        return !disallowedFields.isEmpty();
    }
}
