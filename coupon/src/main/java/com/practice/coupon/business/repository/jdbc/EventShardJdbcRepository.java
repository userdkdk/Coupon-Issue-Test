package com.practice.coupon.business.repository.jdbc;

import com.practice.coupon.business.domain.EventShardEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventShardJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void insertEvents(List<EventShardEntity> entities) {
        String sql = """
            insert into event_shards (event_id, shard_no, amounts)
            values (?, ?, ?)
            """;

        jdbcTemplate.batchUpdate(sql, entities, entities.size(), (ps, entity) -> {
            ps.setInt(1, entity.getEvent().getId());
            ps.setInt(2, entity.getShardNo());
            ps.setInt(3, entity.getAmounts());
        });
    }
}
