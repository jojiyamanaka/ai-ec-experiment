package com.example.aiec.modules.backoffice.domain.repository;

import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BoUserRepository extends JpaRepository<BoUser, Long> {
    Optional<BoUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
