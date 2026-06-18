package com.localconnect.backend.repository;

import com.localconnect.backend.entity.ServiceListing;
import com.localconnect.backend.entity.User;
import com.localconnect.backend.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceListingRepository extends JpaRepository<ServiceListing, Long> {

    List<ServiceListing> findByApprovalStatus(ApprovalStatus approvalStatus);

    List<ServiceListing> findByProvider(User provider);

    List<ServiceListing> findByApprovalStatusAndAvailableTrue(ApprovalStatus approvalStatus);

    List<ServiceListing> findByCategoryContainingIgnoreCaseAndApprovalStatus(
            String category,
            ApprovalStatus approvalStatus
    );

    List<ServiceListing> findByCityContainingIgnoreCaseAndApprovalStatus(
            String city,
            ApprovalStatus approvalStatus
    );

    List<ServiceListing> findByCategoryIgnoreCaseAndApprovalStatus(
            String category,
            ApprovalStatus approvalStatus
    );

    List<ServiceListing> findByCategoryIgnoreCaseAndSubCategoryIgnoreCaseAndApprovalStatus(
            String category,
            String subCategory,
            ApprovalStatus approvalStatus
    );
}