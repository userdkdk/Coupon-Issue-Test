package com.practice.coupon.data;

import com.practice.coupon.business.application.EventService;
import com.practice.coupon.business.domain.*;
import com.practice.coupon.business.repository.EventRepository;
import com.practice.coupon.business.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("local")
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EventService eventService;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count()<=100000) {
            List<UserEntity> users = new ArrayList<>();

            for (int i=1;i<=200060;i++) {
                String name = "user "+i;
                users.add(UserEntity.create(name));
            }

            userRepository.saveAll(users);
        }
        if (eventRepository.count()<5) {
            eventService.generateTestEvent(100000,1);
            eventService.generateTestEvent(100000,3);
            eventService.generateTestEvent(100000,5);
            eventService.generateTestEvent(100000,10);
            eventService.generateTestEvent(100000,50);
        }
    }
}
