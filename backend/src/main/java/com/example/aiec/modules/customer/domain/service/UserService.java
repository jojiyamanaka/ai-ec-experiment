package com.example.aiec.modules.customer.domain.service;

import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ConflictException;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import com.example.aiec.modules.customer.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * ユーザーサービス
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * ユーザー作成
     */
    @Transactional(rollbackFor = Exception.class)
    public User createUser(String email, String displayName, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "このメールアドレスは既に登録されています");
        }

        User user = new User();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    /**
     * 管理側からユーザー作成
     */
    @Transactional(rollbackFor = Exception.class)
    public User createUserByAdmin(
            String email,
            String displayName,
            String password,
            String fullName,
            String phoneNumber,
            LocalDate birthDate,
            Boolean newsletterOptIn,
            User.MemberRank memberRank,
            Integer loyaltyPoints,
            Boolean isActive,
            String deactivationReason
    ) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "このメールアドレスは既に登録されています");
        }

        User user = new User();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setPhoneNumber(phoneNumber);
        user.setBirthDate(birthDate);
        user.setNewsletterOptIn(newsletterOptIn != null ? newsletterOptIn : false);
        user.setMemberRank(memberRank != null ? memberRank : User.MemberRank.STANDARD);
        user.setLoyaltyPoints(loyaltyPoints != null ? loyaltyPoints : 0);
        user.setIsActive(isActive != null ? isActive : true);
        user.setDeactivationReason(deactivationReason);
        return userRepository.save(user);
    }

    /**
     * メールアドレスでユーザー検索
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "ユーザーが見つかりません"));
    }

    /**
     * ID でユーザー検索
     */
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "会員が見つかりません"));
    }

    /**
     * パスワード検証
     */
    public boolean verifyPassword(User user, String password) {
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    /**
     * 会員状態変更
     */
    @Transactional(rollbackFor = Exception.class)
    public User updateStatus(Long userId, Boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "会員が見つかりません"));

        user.setIsActive(isActive);
        return userRepository.save(user);
    }

}
