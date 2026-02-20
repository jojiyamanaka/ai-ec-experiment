package com.example.aiec.modules.backoffice.adapter.dto;

import com.example.aiec.modules.customer.adapter.dto.UserAddressDto;
import com.example.aiec.modules.customer.domain.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class MemberDetailDto {
    private Long id;
    private String email;
    private String displayName;
    private String fullName;
    private String phoneNumber;
    private LocalDate birthDate;
    private Boolean newsletterOptIn;
    private User.MemberRank memberRank;
    private Integer loyaltyPoints;
    private String deactivationReason;
    private Instant lastLoginAt;
    private Instant termsAgreedAt;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private List<UserAddressDto> addresses = new ArrayList<>();
    private OrderSummary orderSummary;

    public static MemberDetailDto fromEntity(User member, List<UserAddressDto> addresses) {
        MemberDetailDto dto = new MemberDetailDto();
        dto.setId(member.getId());
        dto.setEmail(member.getEmail());
        dto.setDisplayName(member.getDisplayName());
        dto.setFullName(member.getFullName());
        dto.setPhoneNumber(member.getPhoneNumber());
        dto.setBirthDate(member.getBirthDate());
        dto.setNewsletterOptIn(member.getNewsletterOptIn());
        dto.setMemberRank(member.getMemberRank());
        dto.setLoyaltyPoints(member.getLoyaltyPoints());
        dto.setDeactivationReason(member.getDeactivationReason());
        dto.setLastLoginAt(member.getLastLoginAt());
        dto.setTermsAgreedAt(member.getTermsAgreedAt());
        dto.setIsActive(member.getIsActive());
        dto.setCreatedAt(member.getCreatedAt());
        dto.setUpdatedAt(member.getUpdatedAt());
        dto.setAddresses(addresses);
        return dto;
    }

    @Data
    public static class OrderSummary {
        private Long totalOrders;
        private BigDecimal totalAmount;
    }
}
