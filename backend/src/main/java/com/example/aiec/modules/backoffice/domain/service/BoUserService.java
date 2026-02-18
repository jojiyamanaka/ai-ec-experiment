package com.example.aiec.modules.backoffice.domain.service;

import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import com.example.aiec.modules.backoffice.domain.repository.BoUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoUserService {
    private final BoUserRepository boUserRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * BoUser を作成
     */
    @Transactional
    public BoUser createBoUser(String email, String displayName, String password, PermissionLevel permissionLevel) {
        if (boUserRepository.existsByEmail(email)) {
            throw new BusinessException("BO_USER_ALREADY_EXISTS", "このメールアドレスは既に登録されています");
        }

        BoUser boUser = new BoUser();
        boUser.setEmail(email);
        boUser.setDisplayName(displayName);
        boUser.setPasswordHash(passwordEncoder.encode(password));
        boUser.setPermissionLevel(permissionLevel);
        boUser.setIsActive(true);

        return boUserRepository.save(boUser);
    }

    /**
     * メールアドレスで BoUser を検索
     */
    public BoUser findByEmail(String email) {
        return boUserRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("BO_USER_NOT_FOUND", "BoUserが見つかりません"));
    }

    /**
     * IDで BoUser を検索
     */
    public BoUser findById(Long id) {
        return boUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("BO_USER_NOT_FOUND", "BoUserが見つかりません"));
    }

    /**
     * パスワード検証
     */
    public boolean verifyPassword(BoUser boUser, String rawPassword) {
        return passwordEncoder.matches(rawPassword, boUser.getPasswordHash());
    }

    /**
     * 全 BoUser 取得
     */
    public List<BoUser> findAll() {
        return boUserRepository.findAll();
    }

    /**
     * BoUser の状態変更
     */
    @Transactional
    public BoUser updateStatus(Long id, Boolean isActive) {
        BoUser boUser = boUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("BO_USER_NOT_FOUND", "BoUserが見つかりません"));
        boUser.setIsActive(isActive);
        return boUserRepository.save(boUser);
    }

    /**
     * BoUser の権限レベル変更
     */
    @Transactional
    public BoUser updatePermissionLevel(Long id, PermissionLevel permissionLevel) {
        BoUser boUser = boUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("BO_USER_NOT_FOUND", "BoUserが見つかりません"));
        boUser.setPermissionLevel(permissionLevel);
        return boUserRepository.save(boUser);
    }
}
