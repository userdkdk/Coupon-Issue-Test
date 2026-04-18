package com.practice.coupon.business.domain;

import com.practice.coupon.common.exception.CustomException;
import com.practice.coupon.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "event_shards",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"event_id","shard_no"},
                name = "uk_event_event_id_shard_no")
        })
public class EventShardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @Column(name = "shard_no", nullable = false)
    private Integer shardNo;

    @Column(name = "amounts", nullable = false)
    private Integer amounts;

    public static EventShardEntity create(EventEntity event, Integer shardNo, Integer amounts) {
        return new EventShardEntity(null, event, shardNo, amounts);
    }

    public void canDecrease() {
        if (this.amounts <= 0) {
            throw new CustomException(ErrorCode.EVENT_COUPON_EXHAUSTED);
        }
    }
}
