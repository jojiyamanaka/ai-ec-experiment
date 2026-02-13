package com.example.aiec.repository;

import com.example.aiec.entity.BoAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BoAuthTokenRepository extends JpaRepository<BoAuthToken, Long> {
    Optional<BoAuthToken> findByTokenHash(String tokenHash);
}
