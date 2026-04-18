package com.practice.coupon.business.application;

import com.practice.coupon.business.api.dto.CreateEventRequest;
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

class EventServiceTest implements IntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    EventService eventService;
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
    @DisplayName("sql 입력 테스트")
    void inputTest() {
        System.out.println("start input");
        UserEntity entity = dbHelper.insertUser("user");
        long start = System.currentTimeMillis();
        for (int i=1;i<=10000;i++) {
            eventService.create(new CreateEventRequest("event: "+i));
        }
        long end = System.currentTimeMillis();
        System.out.println("end input: "+(end-start));
    }

    @Test
    @DisplayName("sql 조회 테스트")
    void getTest() {
        System.out.println("start view");
        for (int i=1;i<=10000;i++) {
            dbHelper.insertEvent("event: "+i);
        }
        em.clear();
        UserEntity entity = dbHelper.insertUser("user");
        long start = System.currentTimeMillis();
        for (int i=1;i<=10000;i++) {
            eventService.getEvents(i);
            em.clear();
        }
        long end = System.currentTimeMillis();
        System.out.println("end view: "+(end-start));
    }
}