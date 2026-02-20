package com.example.aiec.modules.product.domain.repository;

import com.example.aiec.modules.product.domain.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    Optional<ProductCategory> findByIdAndIsPublishedTrue(Long id);

    Optional<ProductCategory> findByName(String name);

    boolean existsByName(String name);

    List<ProductCategory> findAllByOrderByDisplayOrderAscIdAsc();
}
