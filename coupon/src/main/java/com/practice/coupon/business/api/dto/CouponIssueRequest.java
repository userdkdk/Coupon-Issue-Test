package com.practice.coupon.business.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CouponIssueRequest(
        @Valid @NotNull Integer userId,
        @Valid @NotNull Integer eventId,
        @Valid @NotNull Integer shardCount
) {}
