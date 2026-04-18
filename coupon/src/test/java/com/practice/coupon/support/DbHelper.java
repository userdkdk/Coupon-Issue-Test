package com.practice.coupon.support;

import com.practice.coupon.business.api.dto.CreateEventRequest;
import com.practice.coupon.business.application.EventService;
import com.practice.coupon.business.domain.*;
import com.practice.coupon.business.repository.CouponRepository;
import com.practice.coupon.business.repository.EventShardRepository;
import com.practice.coupon.business.repository.UserRepository;
import com.practice.coupon.business.repository.jdbc.EventShardJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class DbHelper {

    private final JdbcTemplate jdbcTemplate;
    private final EventService eventService;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final EventShardJdbcRepository eventShardJdbcRepository;

    public UserEntity insertUser(String name) {
        return userRepository.save(UserEntity.create(name));
    }

    public void insertEvent(String name) {
        eventService.create(new CreateEventRequest(name));
    }

    public void insertEventWithInfo(Integer totalAmounts, int shardCount) {
        eventService.generateTestEvent(totalAmounts, shardCount);
    }

    public CouponEntity insertCoupon(UserEntity user, EventEntity event, Integer shardCount) {
        return couponRepository.save(CouponEntity.create(user, event, shardCount));
    }

    public long CouponCountAll() {
        return couponRepository.count();
    }

    public void insertInit() {
        userRepository.save(UserEntity.create("user 1"));
        userRepository.save(UserEntity.create("user 2"));
        int totalAmounts = EntityPolicy.DEFAULT_INIT_AMOUNT;
        int shardCount = EntityPolicy.DEFAULT_SHARD_COUNT;
        eventService.generateTestEvent(totalAmounts, shardCount);
    }

    public void truncateAll() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE `users`");
        jdbcTemplate.execute("TRUNCATE TABLE `events`");
        jdbcTemplate.execute("TRUNCATE TABLE `event_shards`");
        jdbcTemplate.execute("TRUNCATE TABLE `coupons`");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
