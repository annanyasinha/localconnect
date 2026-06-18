package com.localconnect.backend.mapper;

import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.fullName")
    @Mapping(target = "serviceId", source = "serviceListing.id")
    @Mapping(target = "serviceTitle", source = "serviceListing.title")
    @Mapping(target = "status", expression = "java(booking.getStatus().name())")
    BookingResponse toResponse(Booking booking);
}