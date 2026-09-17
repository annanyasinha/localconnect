package com.localconnect.backend.entity;

import com.localconnect.backend.enums.ApprovalStatus;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_listings", indexes = {
        @Index(name = "idx_service_approval_availability", columnList = "approval_status, available"),
        @Index(name = "idx_service_category_subcategory", columnList = "approval_status, category, sub_category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(name = "sub_category", nullable = false, length = 120)
    private String subCategory;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 120)
    private String area;

    @Column(nullable = false)
    private Boolean available;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private ApprovalStatus approvalStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}