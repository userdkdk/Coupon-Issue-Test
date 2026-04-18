package com.practice.coupon.business.application;

import com.practice.coupon.business.api.dto.CreateEventRequest;
import com.practice.coupon.business.api.dto.CreateEventResponse;
import com.practice.coupon.business.api.dto.EventResponse;
import com.practice.coupon.business.application.dto.EventSeedCommand;
import com.practice.coupon.business.domain.EntityPolicy;
import com.practice.coupon.business.domain.EventEntity;
import com.practice.coupon.business.domain.EventShardEntity;
import com.practice.coupon.business.repository.EventRepository;
import com.practice.coupon.business.repository.EventShardRepository;
import com.practice.coupon.business.repository.jdbc.EventShardJdbcRepository;
import com.practice.coupon.common.exception.CustomException;
import com.practice.coupon.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventShardJdbcRepository eventShardJdbcRepository;
    private final EventRepository eventRepository;

    @Transactional
    public CreateEventResponse create(CreateEventRequest request) {
        int totalAmounts = EntityPolicy.DEFAULT_INIT_AMOUNT;
        int shardCount = EntityPolicy.DEFAULT_SHARD_COUNT;
        createEvent(new EventSeedCommand(request.name(), totalAmounts, shardCount));
        return new CreateEventResponse(true);
    }

    @Transactional(readOnly = true)
    public EventResponse getEvents(Integer eventId) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(()->new CustomException(ErrorCode.EVENT_NOT_FOUND));
        return new EventResponse(event.getName());
    }

    @Transactional
    public void generateTestEvent(int totalAmounts, int shardCount) {
        long counts = eventRepository.count();
        String name = String.format("event %d",counts+1);
        createEvent(new EventSeedCommand(name, totalAmounts, shardCount));
    }

    private void createEvent(EventSeedCommand command) {
        LocalDateTime now = LocalDateTime.now();
        int totalAmounts = command.totalAmounts();
        int shardCount = command.shardCount();

        if (totalAmounts <= 0 || shardCount <= 0) {
            throw new CustomException(ErrorCode.EVENT_CREATE_ERROR);
        }
        int baseAmount = totalAmounts / shardCount;
        int remainder = totalAmounts % shardCount;

        EventEntity event = eventRepository.save(
                EventEntity.create(command.name(), command.totalAmounts(), now));

        List<EventShardEntity> entities = IntStream.range(0, shardCount)
                .mapToObj(i -> {
                    int shardAmount = baseAmount + (i < remainder ? 1 : 0);
                    return EventShardEntity.create(event, i, shardAmount);
                })
                .toList();

        eventShardJdbcRepository.insertEvents(entities);
    }

}
