package com.practice.coupon.business.application;

import com.practice.coupon.business.domain.UserEntity;
import com.practice.coupon.business.repository.CouponRepository;
import com.practice.coupon.business.repository.EventShardRepository;
import com.practice.coupon.business.repository.UserRepository;
import com.practice.coupon.support.DbHelper;
import com.practice.coupon.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CouponTest implements IntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    CouponService couponService;
    @Autowired
    CouponRepository couponRepository;
    @Autowired
    EventShardRepository eventShardRepository;
    @Autowired
    UserRepository userRepository;

    @Autowired
    DbHelper dbHelper;

    @BeforeEach
    void clean() {
        dbHelper.truncateAll();
    }

    @Test
    @DisplayName("sql 조회용 테스트")
    void sqlTest() {
        dbHelper.insertInit();
        em.clear();
        System.out.println("start 1");
        UserEntity user = userRepository.findById(1)
                .orElseThrow();
        System.out.println("end 1");
    }

    @Test
    @DisplayName("sql 조회용 테스트")
    void sqlTest2() {
        dbHelper.insertInit();
        em.clear();
        System.out.println("start 2");
        UserEntity user = userRepository.getReferenceById(1);
        System.out.println("end 2");
    }
}
