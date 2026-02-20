package com.example.aiec.modules.customer.domain.repository;

import com.example.aiec.modules.customer.domain.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserIdOrderByAddressOrderAscIdAsc(Long userId);

    @Modifying
    @Query("""
            UPDATE UserAddress a
               SET a.isDefault = FALSE
             WHERE a.user.id = :userId
               AND a.isDeleted = FALSE
               AND (:exceptAddressId IS NULL OR a.id <> :exceptAddressId)
            """)
    int clearDefaultByUserId(@Param("userId") Long userId, @Param("exceptAddressId") Long exceptAddressId);
}
