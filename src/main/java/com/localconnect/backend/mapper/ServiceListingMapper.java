package com.localconnect.backend.mapper;

import com.localconnect.backend.dto.response.ServiceListingResponse;
import com.localconnect.backend.entity.ServiceListing;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ServiceListingMapper {

    @Mapping(target = "providerId", source = "provider.id")
    @Mapping(target = "providerName", source = "provider.fullName")
    @Mapping(target = "approvalStatus", expression = "java(serviceListing.getApprovalStatus().name())")
    ServiceListingResponse toResponse(ServiceListing serviceListing);
}