package com.example.aiec.modules.purchase.cart.repository;

import com.example.aiec.modules.purchase.cart.entity.Cart;
import com.example.aiec.modules.purchase.cart.entity.CartItem;
import com.example.aiec.modules.product.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * カートアイテムリポジトリ
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * カートと商品でカートアイテムを検索
     */
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

}
