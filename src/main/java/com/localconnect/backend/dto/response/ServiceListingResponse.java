package com.localconnect.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ServiceListingResponse {
    private Long id;
    private Long providerId;
    private String providerName;
    private String title;
    private String category;
    private String subCategory;
    private String description;
    private BigDecimal price;
    private String city;
    private String area;
    private Boolean available;
    private String imageUrl;
    private String approvalStatus;
    private LocalDateTime createdAt;
}