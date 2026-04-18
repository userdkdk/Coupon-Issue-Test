package com.practice.coupon.business.application;

import com.practice.coupon.business.api.dto.CouponIssueRequest;
import com.practice.coupon.business.domain.EventEntity;
import com.practice.coupon.business.domain.EventShardEntity;
import com.practice.coupon.business.domain.UserEntity;
import com.practice.coupon.business.repository.CouponRepository;
import com.practice.coupon.business.repository.EventShardRepository;
import com.practice.coupon.business.repository.UserRepository;
import com.practice.coupon.common.exception.CustomException;
import com.practice.coupon.common.exception.ErrorCode;
import com.practice.coupon.support.ConcurrentRunner;
import com.practice.coupon.support.DbHelper;
import com.practice.coupon.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CouponServiceTest implements IntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired CouponService couponService;
    @Autowired CouponRepository couponRepository;
    @Autowired
    EventShardRepository eventShardRepository;
    @Autowired
    EventService eventService;
    @Autowired
    UserRepository userRepository;

    @Autowired
    DbHelper dbHelper;

    @BeforeEach
    void clean() {
        dbHelper.truncateAll();
    }

    @Test
    @DisplayName("유저는 같은 이벤트 쿠폰 최대 1개만")
    void DuplicatedUserEventIssueTest()  throws Exception {
        UserEntity user1 = dbHelper.insertUser("user 1");
        EventShardEntity event = eventShardRepository.findById(1)
                .orElseThrow();
        int shardCount = 1;
        int threads = 3;
        em.flush();
        em.clear();
        System.out.println("start");
        ConcurrentRunner.Result result = ConcurrentRunner.run(threads, (i)-> {
            CouponIssueRequest request = new CouponIssueRequest(user1.getId(), event.getId(), shardCount);
            couponService.issue(request);
        });

        // 성공은 하나
        assertEquals(1, dbHelper.CouponCountAll());
        List<CustomException> domainErrors = result.errorsOf(CustomException.class);
        assertEquals(2, domainErrors.size());
    }

    @Test
    @DisplayName("get reference와 find by 속도 비교")
    void compareTimeAboutRockAndConflictTest()  throws Exception {
        for (int i=1;i<=11020;i++) {
            String name = "user " + i;
            dbHelper.insertUser(name);
            dbHelper.insertEvent("event"+i);
        }

        em.clear();

        long start = System.currentTimeMillis();
        int threads = 2000;
        ConcurrentRunner.Result result = ConcurrentRunner.run(threads, (i)-> {
            UserEntity user = userRepository.getReferenceById(i+1);
            EventShardEntity event = eventShardRepository.getReferenceById(i+1);

            user.getId();
            event.getId();
        });

        long end = System.currentTimeMillis();

        em.clear();

        long start2 = System.currentTimeMillis();
        ConcurrentRunner.run(threads, (i)-> {
            UserEntity user = userRepository.findById(i+threads+10)
                    .orElseThrow();
            EventShardEntity event = eventShardRepository.findById(i+threads+10)
                    .orElseThrow();
            user.getId();
            event.getId();
        });

        long end2 = System.currentTimeMillis();
        System.out.println("get reference: "+(end-start));
        System.out.println("get id: "+(end2-start2));
    }

    @Test
    @DisplayName("시나리오 2")
    void scenario2Test()  throws Exception {
        UserEntity[] users = new UserEntity[8001];
        for (int i=1;i<=8000;i++) {
            String name = "user " + i;
            users[i] = dbHelper.insertUser(name);
        }
        eventService.generateTestEvent(100000, 1);

        EventShardEntity event = eventShardRepository.findById(1)
                .orElseThrow();
        int shardCount = 1;

        int threads = 8000;
        long start = System.currentTimeMillis();
        ConcurrentRunner.Result result = ConcurrentRunner.run(threads, (i)-> {
            CouponIssueRequest request = new CouponIssueRequest(users[i+1].getId(), event.getId(), shardCount);
            couponService.issue(request);
        });

        long end = System.currentTimeMillis();
        System.out.println(end - start);

        assertEquals(8000, couponRepository.count());

        result.errors().stream()
                .filter(e -> !(e instanceof CustomException))
                .forEach(e -> System.out.println("non custom error = " + e.getClass().getName()));
    }

}