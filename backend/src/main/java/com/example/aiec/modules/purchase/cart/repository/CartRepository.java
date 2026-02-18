package com.example.aiec.modules.purchase.cart.repository;

import com.example.aiec.modules.purchase.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * カートリポジトリ
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * セッションIDでカートを検索
     */
    Optional<Cart> findBySessionId(String sessionId);

}
