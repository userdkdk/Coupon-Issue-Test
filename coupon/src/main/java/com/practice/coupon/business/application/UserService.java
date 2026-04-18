package com.practice.coupon.business.application;

import com.practice.coupon.business.api.dto.UserEventIssuedResponse;
import com.practice.coupon.business.repository.CouponRepository;
import com.practice.coupon.business.repository.EventShardRepository;
import com.practice.coupon.business.repository.UserRepository;
import com.practice.coupon.common.exception.CustomException;
import com.practice.coupon.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final EventShardRepository eventShardRepository;
    private final CouponRepository couponRepository;

    public UserEventIssuedResponse eventIssued(Integer userId, Integer eventId) {
        // user Exists
        if (userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        // event exists
        if (eventShardRepository.existsById(eventId)) {
            throw new CustomException(ErrorCode.EVENT_NOT_FOUND);
        }
        boolean isIssued = couponRepository.existsByUser_IdAndEvent_Id(
                userId, eventId);
        return new UserEventIssuedResponse(isIssued);
    }
}
