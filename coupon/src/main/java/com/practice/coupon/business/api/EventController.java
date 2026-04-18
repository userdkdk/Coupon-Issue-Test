package com.practice.coupon.business.api;

import com.practice.coupon.business.api.dto.*;
import com.practice.coupon.business.application.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @PostMapping("")
    public ResponseEntity<CreateEventResponse> create(
            @Valid @RequestBody CreateEventRequest request
    ) {
        CreateEventResponse response = eventService.create(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> geEvents(
            @PathVariable Integer eventId
    ) {
        EventResponse response = eventService.getEvents(eventId);
        return ResponseEntity.ok(response);
    }
}
