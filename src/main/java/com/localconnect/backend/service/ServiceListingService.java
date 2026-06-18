package com.localconnect.backend.service;

import com.localconnect.backend.dto.request.ServiceCreateRequest;
import com.localconnect.backend.dto.response.ServiceListingResponse;

import java.util.List;

public interface ServiceListingService {

    ServiceListingResponse createService(ServiceCreateRequest request, String providerEmail);

    List<ServiceListingResponse> getApprovedServices();

    List<ServiceListingResponse> getMyServices(String providerEmail);

    List<ServiceListingResponse> getPendingServices();

    ServiceListingResponse approveService(Long serviceId);

    ServiceListingResponse rejectService(Long serviceId);

    List<ServiceListingResponse> getApprovedServicesByCategory(String category);

    List<ServiceListingResponse> getApprovedServicesByCategoryAndSubCategory(String category, String subCategory);
}