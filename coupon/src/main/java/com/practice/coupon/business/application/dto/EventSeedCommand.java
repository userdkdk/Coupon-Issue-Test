package com.practice.coupon.business.application.dto;

public record EventSeedCommand(
        String name,
        Integer totalAmounts,
        Integer shardCount
) {}
