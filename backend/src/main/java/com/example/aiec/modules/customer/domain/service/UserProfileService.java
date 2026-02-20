package com.example.aiec.modules.customer.domain.service;

import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.customer.domain.entity.UserAddress;
import com.example.aiec.modules.customer.domain.repository.UserAddressRepository;
import com.example.aiec.modules.customer.domain.repository.UserRepository;
import com.example.aiec.modules.shared.domain.model.ActorType;
import com.example.aiec.modules.shared.exception.ForbiddenException;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;

    @Transactional(readOnly = true)
    public User getProfile(Long userId) {
        return requireUser(userId);
    }

    @Transactional(readOnly = true)
    public List<UserAddress> getAddresses(Long userId) {
        requireUser(userId);
        return userAddressRepository.findByUserIdOrderByAddressOrderAscIdAsc(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public User updateMyProfile(Long userId, ProfileUpdateCommand command) {
        return updateProfile(userId, command, ActorType.USER, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public User updateMemberProfile(Long userId, ProfileUpdateCommand command, Long boUserId) {
        return updateProfile(userId, command, ActorType.BO_USER, boUserId);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserAddress addMyAddress(Long userId, AddressUpsertCommand command) {
        User user = requireUser(userId);
        return createAddress(user, command, ActorType.USER, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserAddress updateMyAddress(Long userId, Long addressId, AddressUpsertCommand command) {
        UserAddress address = requireAddress(addressId);
        assertAddressOwner(address, userId);

        if (Boolean.TRUE.equals(command.getIsDefault())) {
            userAddressRepository.clearDefaultByUserId(userId, addressId);
        }

        applyAddressFields(address, command);
        address.setUpdatedByType(ActorType.USER);
        address.setUpdatedById(userId);
        return userAddressRepository.save(address);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteMyAddress(Long userId, Long addressId) {
        UserAddress address = requireAddress(addressId);
        assertAddressOwner(address, userId);
        softDeleteAddress(address, ActorType.USER, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<UserAddress> applyAddressUpserts(
            Long userId,
            List<AddressUpsertCommand> commands,
            ActorType actorType,
            Long actorId
    ) {
        if (commands == null || commands.isEmpty()) {
            return getAddresses(userId);
        }

        User user = requireUser(userId);
        for (AddressUpsertCommand command : commands) {
            if (command.getId() == null) {
                if (Boolean.TRUE.equals(command.getDeleted())) {
                    continue;
                }
                createAddress(user, command, actorType, actorId);
                continue;
            }

            UserAddress existing = requireAddress(command.getId());
            assertAddressOwner(existing, userId);

            if (Boolean.TRUE.equals(command.getDeleted())) {
                softDeleteAddress(existing, actorType, actorId);
                continue;
            }

            if (Boolean.TRUE.equals(command.getIsDefault())) {
                userAddressRepository.clearDefaultByUserId(userId, existing.getId());
            }

            applyAddressFields(existing, command);
            existing.setUpdatedByType(actorType);
            existing.setUpdatedById(actorId);
            userAddressRepository.save(existing);
        }

        return getAddresses(userId);
    }

    @Transactional(readOnly = true)
    public List<UserAddress> getAddressesOrEmpty(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return userAddressRepository.findByUserIdOrderByAddressOrderAscIdAsc(userId);
    }

    private User updateProfile(Long userId, ProfileUpdateCommand command, ActorType actorType, Long actorId) {
        User user = requireUser(userId);
        if (command.getDisplayName() != null) {
            user.setDisplayName(command.getDisplayName());
        }
        if (command.getFullName() != null) {
            user.setFullName(command.getFullName());
        }
        if (command.getPhoneNumber() != null) {
            user.setPhoneNumber(command.getPhoneNumber());
        }
        if (command.getBirthDate() != null) {
            user.setBirthDate(command.getBirthDate());
        }
        if (command.getNewsletterOptIn() != null) {
            user.setNewsletterOptIn(command.getNewsletterOptIn());
        }
        if (command.getMemberRank() != null) {
            user.setMemberRank(command.getMemberRank());
        }
        if (command.getLoyaltyPoints() != null) {
            user.setLoyaltyPoints(command.getLoyaltyPoints());
        }
        if (command.getDeactivationReason() != null) {
            user.setDeactivationReason(command.getDeactivationReason());
        }
        if (command.getIsActive() != null) {
            user.setIsActive(command.getIsActive());
        }
        user.setUpdatedByType(actorType);
        user.setUpdatedById(actorId);
        return userRepository.save(user);
    }

    private UserAddress createAddress(User user, AddressUpsertCommand command, ActorType actorType, Long actorId) {
        if (Boolean.TRUE.equals(command.getIsDefault())) {
            userAddressRepository.clearDefaultByUserId(user.getId(), null);
        }
        UserAddress address = new UserAddress();
        address.setUser(user);
        applyAddressFields(address, command);
        address.setCreatedByType(actorType);
        address.setCreatedById(actorId);
        address.setUpdatedByType(actorType);
        address.setUpdatedById(actorId);
        return userAddressRepository.save(address);
    }

    private void applyAddressFields(UserAddress address, AddressUpsertCommand command) {
        address.setLabel(command.getLabel());
        address.setRecipientName(command.getRecipientName());
        address.setRecipientPhoneNumber(command.getRecipientPhoneNumber());
        address.setPostalCode(command.getPostalCode());
        address.setPrefecture(command.getPrefecture());
        address.setCity(command.getCity());
        address.setAddressLine1(command.getAddressLine1());
        address.setAddressLine2(command.getAddressLine2());
        address.setIsDefault(Boolean.TRUE.equals(command.getIsDefault()));
        address.setAddressOrder(command.getAddressOrder() == null ? 0 : command.getAddressOrder());
    }

    private void softDeleteAddress(UserAddress address, ActorType actorType, Long actorId) {
        address.setIsDeleted(true);
        address.setDeletedAt(Instant.now());
        address.setDeletedByType(actorType);
        address.setDeletedById(actorId);
        address.setUpdatedByType(actorType);
        address.setUpdatedById(actorId);
        userAddressRepository.save(address);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "会員が見つかりません"));
    }

    private UserAddress requireAddress(Long addressId) {
        return userAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_ADDRESS_NOT_FOUND", "住所が見つかりません"));
    }

    private void assertAddressOwner(UserAddress address, Long userId) {
        if (!address.getUser().getId().equals(userId)) {
            throw new ForbiddenException("FORBIDDEN", "この操作を実行する権限がありません");
        }
    }

    @Data
    public static class ProfileUpdateCommand {
        private String displayName;
        private String fullName;
        private String phoneNumber;
        private LocalDate birthDate;
        private Boolean newsletterOptIn;
        private User.MemberRank memberRank;
        private Integer loyaltyPoints;
        private String deactivationReason;
        private Boolean isActive;
    }

    @Data
    public static class AddressUpsertCommand {
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
    }
}
