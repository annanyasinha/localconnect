package com.localconnect.backend.service.impl;

import com.localconnect.backend.config.CacheConfig;

import com.localconnect.backend.entity.ServiceListing;
import com.localconnect.backend.enums.ApprovalStatus;

import com.localconnect.backend.mapper.ServiceListingMapper;

import com.localconnect.backend.repository.ServiceListingRepository;
import com.localconnect.backend.repository.UserRepository;

import com.localconnect.backend.service.ServiceListingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(classes = {
        CacheConfig.class,
        ServiceListingCacheTest.TestConfig.class
})
class ServiceListingCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        ServiceListingRepository serviceListingRepository() {

            return mock(
                    ServiceListingRepository.class);
        }

        @Bean
        UserRepository userRepository() {

            return mock(
                    UserRepository.class);
        }

        @Bean
        ServiceListingMapper serviceListingMapper() {

            return mock(
                    ServiceListingMapper.class);
        }

        @Bean
        ServiceListingService serviceListingService(
                ServiceListingRepository repository,
                UserRepository users,
                ServiceListingMapper mapper) {

            return new ServiceListingServiceImpl(
                    repository,
                    users,
                    mapper);
        }
    }

    @Autowired
    private ServiceListingService service;

    @Autowired
    private ServiceListingRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceListingMapper mapper;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void resetState() {

        reset(
                repository,
                userRepository,
                mapper);

        for (String name : cacheManager.getCacheNames()) {

            cacheManager.getCache(name).clear();
        }
    }

    @Test
    void repeatedCallsShouldUseCache() {

        when(
                repository.findByApprovalStatusAndAvailableTrue(
                        ApprovalStatus.APPROVED))
                .thenReturn(List.of());

        service.getApprovedServices();

        service.getApprovedServices();

        verify(
                repository,
                times(1)).findByApprovalStatusAndAvailableTrue(
                        ApprovalStatus.APPROVED);
    }

    @Test
    void approvingServiceShouldEvictCache() {

        ServiceListing listing = ServiceListing.builder()
                .id(5L)
                .approvalStatus(
                        ApprovalStatus.PENDING)
                .build();

        when(
                repository.findByApprovalStatusAndAvailableTrue(
                        ApprovalStatus.APPROVED))
                .thenReturn(List.of());

        when(
                repository.findById(5L)).thenReturn(Optional.of(listing));

        when(
                repository.save(listing)).thenReturn(listing);

        // First call populates cache.
        service.getApprovedServices();

        // Approval must invalidate cache.
        service.approveService(5L);

        // Second read must access repository again.
        service.getApprovedServices();

        verify(
                repository,
                times(2)).findByApprovalStatusAndAvailableTrue(
                        ApprovalStatus.APPROVED);
    }
}