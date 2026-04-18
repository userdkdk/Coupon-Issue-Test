package com.practice.coupon.business.repository;

import com.practice.coupon.business.domain.EventEntity;
import com.practice.coupon.business.domain.EventShardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventShardRepository extends JpaRepository<EventShardEntity, Integer> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update EventShardEntity e
            SET e.amounts = e.amounts - 1
            where e.event = :event and e.shardNo = :shardNo and e.amounts > 0
            """)
    Integer discountAmounts(
            @Param("event") EventEntity event,
            @Param("shardNo") Integer shardNo);
}
