package com.localconnect.backend.service.impl;

import com.localconnect.backend.dto.request.ServiceCreateRequest;
import com.localconnect.backend.dto.response.ServiceListingResponse;
import com.localconnect.backend.entity.ServiceListing;
import com.localconnect.backend.entity.User;
import com.localconnect.backend.enums.ApprovalStatus;
import com.localconnect.backend.enums.RoleName;
import com.localconnect.backend.mapper.ServiceListingMapper;
import com.localconnect.backend.repository.ServiceListingRepository;
import com.localconnect.backend.repository.UserRepository;
import com.localconnect.backend.service.ServiceListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceListingServiceImpl implements ServiceListingService {

    private final ServiceListingRepository serviceListingRepository;
    private final UserRepository userRepository;
    private final ServiceListingMapper serviceListingMapper;

    @Override
    public ServiceListingResponse createService(ServiceCreateRequest request, String providerEmail) {
        User provider = userRepository.findByEmail(providerEmail)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        if (provider.getRole() != RoleName.PROVIDER) {
            throw new RuntimeException("Only providers can create services");
        }

        if (request.getSubCategory() == null || request.getSubCategory().isBlank()) {
            throw new RuntimeException("Sub-category is required");
        }

        ServiceListing serviceListing = ServiceListing.builder()
                .provider(provider)
                .title(request.getTitle())
                .category(request.getCategory())
                .subCategory(request.getSubCategory())
                .description(request.getDescription())
                .price(request.getPrice())
                .city(request.getCity())
                .area(request.getArea())
                .available(request.getAvailable() != null ? request.getAvailable() : true)
                .imageUrl(request.getImageUrl())
                .approvalStatus(ApprovalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return serviceListingMapper.toResponse(serviceListingRepository.save(serviceListing));
    }

    @Override
    public List<ServiceListingResponse> getApprovedServices() {
        return serviceListingRepository.findByApprovalStatusAndAvailableTrue(ApprovalStatus.APPROVED)
                .stream()
                .map(serviceListingMapper::toResponse)
                .toList();
    }

    @Override
    public List<ServiceListingResponse> getMyServices(String providerEmail) {
        User provider = userRepository.findByEmail(providerEmail)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        return serviceListingRepository.findByProvider(provider)
                .stream()
                .map(serviceListingMapper::toResponse)
                .toList();
    }

    @Override
    public List<ServiceListingResponse> getPendingServices() {
        return serviceListingRepository.findByApprovalStatus(ApprovalStatus.PENDING)
                .stream()
                .map(serviceListingMapper::toResponse)
                .toList();
    }

    @Override
    public ServiceListingResponse approveService(Long serviceId) {
        ServiceListing serviceListing = serviceListingRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        serviceListing.setApprovalStatus(ApprovalStatus.APPROVED);
        return serviceListingMapper.toResponse(serviceListingRepository.save(serviceListing));
    }

    @Override
    public ServiceListingResponse rejectService(Long serviceId) {
        ServiceListing serviceListing = serviceListingRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        serviceListing.setApprovalStatus(ApprovalStatus.REJECTED);
        return serviceListingMapper.toResponse(serviceListingRepository.save(serviceListing));
    }

    @Override
    public List<ServiceListingResponse> getApprovedServicesByCategory(String category) {
        return serviceListingRepository
                .findByCategoryIgnoreCaseAndApprovalStatus(category, ApprovalStatus.APPROVED)
                .stream()
                .filter(service -> Boolean.TRUE.equals(service.getAvailable()))
                .map(serviceListingMapper::toResponse)
                .toList();
    }

    @Override
    public List<ServiceListingResponse> getApprovedServicesByCategoryAndSubCategory(String category, String subCategory) {
        return serviceListingRepository
                .findByCategoryIgnoreCaseAndSubCategoryIgnoreCaseAndApprovalStatus(
                        category,
                        subCategory,
                        ApprovalStatus.APPROVED
                )
                .stream()
                .filter(service -> Boolean.TRUE.equals(service.getAvailable()))
                .map(serviceListingMapper::toResponse)
                .toList();
    }
}