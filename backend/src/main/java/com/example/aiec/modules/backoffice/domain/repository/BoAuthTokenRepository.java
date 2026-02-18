package com.example.aiec.modules.backoffice.domain.repository;

import com.example.aiec.modules.backoffice.domain.entity.BoAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BoAuthTokenRepository extends JpaRepository<BoAuthToken, Long> {
    Optional<BoAuthToken> findByTokenHash(String tokenHash);
}
