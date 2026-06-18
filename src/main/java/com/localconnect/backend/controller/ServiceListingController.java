package com.localconnect.backend.controller;

import com.localconnect.backend.dto.request.ServiceCreateRequest;
import com.localconnect.backend.dto.response.ServiceListingResponse;
import com.localconnect.backend.service.ServiceListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ServiceListingController {

    private final ServiceListingService serviceListingService;

    @PostMapping
    public ServiceListingResponse createService(@Valid @RequestBody ServiceCreateRequest request,
                                                @RequestParam String providerEmail) {
        return serviceListingService.createService(request, providerEmail);
    }

    @GetMapping
    public List<ServiceListingResponse> getServices(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subCategory
    ) {
        if (category != null && !category.isBlank() && subCategory != null && !subCategory.isBlank()) {
            return serviceListingService.getApprovedServicesByCategoryAndSubCategory(category, subCategory);
        }

        if (category != null && !category.isBlank()) {
            return serviceListingService.getApprovedServicesByCategory(category);
        }

        return serviceListingService.getApprovedServices();
    }

    @GetMapping("/my")
    public List<ServiceListingResponse> getMyServices(@RequestParam String providerEmail) {
        return serviceListingService.getMyServices(providerEmail);
    }
}