package com.practice.coupon.business.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "coupons",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id","event_id"})
})
public class CouponEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @Column(name = "issued_shard_no", nullable = false)
    private Integer issuedShardNo;

    public static CouponEntity create(UserEntity user, EventEntity event, Integer issuedShardNo) {
        return new CouponEntity(null, user, event, issuedShardNo);
    }
}
