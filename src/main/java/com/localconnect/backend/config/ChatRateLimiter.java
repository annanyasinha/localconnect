package com.localconnect.backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ChatRateLimiter {

    private static final int REQUEST_LIMIT = 15;

    private final Cache<String, AtomicInteger> requests = Caffeine.newBuilder()
            .expireAfterWrite(
                    Duration.ofMinutes(1))
            .maximumSize(10_000)
            .build();

    public boolean allowRequest(String userEmail) {

        AtomicInteger counter = requests.get(
                userEmail,
                key -> new AtomicInteger(0));

        return counter.incrementAndGet() <= REQUEST_LIMIT;
    }
}