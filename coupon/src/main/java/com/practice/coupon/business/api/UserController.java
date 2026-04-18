package com.practice.coupon.business.api;

import com.practice.coupon.business.api.dto.UserEventIssuedResponse;
import com.practice.coupon.business.application.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/{userId}/{eventId}")
    public ResponseEntity<UserEventIssuedResponse> eventIssued(
            @PathVariable Integer userId,
            @PathVariable Integer eventId
    ) {
        UserEventIssuedResponse response = userService.eventIssued(userId, eventId);
        return ResponseEntity.ok(response);
    }
}
