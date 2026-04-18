package com.practice.coupon.business.repository;

import com.practice.coupon.business.domain.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<CouponEntity, Integer> {
    boolean existsByUser_IdAndEvent_Id(Integer userId, Integer eventId);
}
