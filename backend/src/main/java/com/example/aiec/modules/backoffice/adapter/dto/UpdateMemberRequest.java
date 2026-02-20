package com.example.aiec.modules.backoffice.adapter.dto;

import com.example.aiec.modules.customer.domain.entity.User;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class UpdateMemberRequest {

    @Size(max = 100, message = "表示名は1〜100文字である必要があります")
    private String displayName;

    @Size(max = 100, message = "氏名は100文字以内である必要があります")
    private String fullName;

    @Size(max = 30, message = "電話番号は30文字以内である必要があります")
    private String phoneNumber;

    private LocalDate birthDate;
    private Boolean newsletterOptIn;
    private User.MemberRank memberRank;
    private Integer loyaltyPoints;
    private String deactivationReason;
    private Boolean isActive;

    @Valid
    private List<AddressUpsert> addresses;

    private final Map<String, Object> disallowedFields = new LinkedHashMap<>();

    @JsonAnySetter
    void captureUnknownField(String key, Object value) {
        disallowedFields.put(key, value);
    }

    public boolean hasDisallowedFields() {
        return !disallowedFields.isEmpty();
    }

    @Data
    public static class AddressUpsert {
        private Long id;
        private String label;
        private String recipientName;
        private String recipientPhoneNumber;
        private String postalCode;
        private String prefecture;
        private String city;
        private String addressLine1;
        private String addressLine2;
        private Boolean isDefault;
        private Integer addressOrder;
        private Boolean deleted;
        private final Map<String, Object> disallowedFields = new LinkedHashMap<>();

        @JsonAnySetter
        void captureUnknownField(String key, Object value) {
            disallowedFields.put(key, value);
        }

        public boolean hasDisallowedFields() {
            return !disallowedFields.isEmpty();
        }
    }
}
