package com.practice.coupon.business.application;

import com.practice.coupon.business.api.dto.CouponIssueRequest;
import com.practice.coupon.business.api.dto.CouponIssueResponse;
import com.practice.coupon.business.api.dto.CouponResponse;
import com.practice.coupon.business.domain.*;
import com.practice.coupon.business.repository.CouponRepository;
import com.practice.coupon.business.repository.EventRepository;
import com.practice.coupon.business.repository.EventShardRepository;
import com.practice.coupon.business.repository.UserRepository;
import com.practice.coupon.common.exception.CustomException;
import com.practice.coupon.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final UserRepository userRepository;
    private final EventShardRepository eventShardRepository;
    private final CouponRepository couponRepository;
    private final EventRepository eventRepository;

    @Transactional
    public CouponIssueResponse issue(CouponIssueRequest request) {
        // get user
        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));
        // get event
        EventEntity event = eventRepository.findById(request.eventId())
                .orElseThrow(()-> new CustomException(ErrorCode.EVENT_NOT_FOUND));

        // discount amounts
        int shardCount = request.shardCount();
        int updated = 0;
        int shardNo = 0;
        for (int idx=0;idx<shardCount;idx++) {
            shardNo = (user.getId()+idx)%shardCount;
            updated = eventShardRepository.discountAmounts(event,shardNo);
            if (updated > 0) {
                break;
            }
        }
        if (updated==0) {
            throw new CustomException(ErrorCode.EVENT_COUPON_EXHAUSTED);
        }

        // save coupon
        try {
            couponRepository.saveAndFlush(CouponEntity.create(user, event, shardNo));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.COUPON_DUPLICATED);
        }

        return new CouponIssueResponse(true);
    }

    public CouponResponse getCoupons(Integer couponId) {

        return null;
    }

    @Transactional
    public void deleteAll() {
        couponRepository.deleteAll();
    }
}
