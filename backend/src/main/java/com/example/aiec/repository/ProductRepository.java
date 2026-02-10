package com.example.aiec.repository;

import com.example.aiec.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 商品リポジトリ
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 公開されている商品のみを取得
     */
    List<Product> findByIsPublishedTrue();

    /**
     * 公開されている商品のみをページネーションで取得
     */
    Page<Product> findByIsPublishedTrue(Pageable pageable);

}
