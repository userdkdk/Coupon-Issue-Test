package com.practice.coupon.business.domain;

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
@Table(name = "events",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name"})
        })
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "total_amounts")
    private Integer totalAmounts;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static EventEntity create(String name, Integer totalAmounts, LocalDateTime now) {
        return new EventEntity(null, name, totalAmounts, now);
    }
}
