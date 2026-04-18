package com.practice.coupon.business.api;

import com.practice.coupon.business.api.dto.CouponIssueRequest;
import com.practice.coupon.business.api.dto.CouponIssueResponse;
import com.practice.coupon.business.api.dto.CouponResponse;
import com.practice.coupon.business.application.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupon")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    @PostMapping("")
    public ResponseEntity<CouponIssueResponse> issue(
            @Valid @RequestBody CouponIssueRequest request
    ) {
        CouponIssueResponse response = couponService.issue(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<CouponResponse> getCoupons(
            @PathVariable Integer couponId
    ) {
        CouponResponse response = couponService.getCoupons(couponId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("")
    public ResponseEntity<Void> deleteAll() {
        couponService.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
