package com.practice.coupon.business.repository;

import com.practice.coupon.business.domain.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<EventEntity, Integer> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update EventEntity e
            SET e.totalAmounts = e.totalAmounts - 1
            where e.id = :id and e.totalAmounts > 0
            """)
    Integer discountAmounts(
            @Param("id") Integer id);
}
