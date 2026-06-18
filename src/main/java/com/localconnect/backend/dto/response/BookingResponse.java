package com.localconnect.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BookingResponse {
    private Long id;

    private Long userId;
    private String userName;

    private Long serviceId;
    private String serviceTitle;

    private String message;
    private LocalDateTime bookingDate;
    private String status;
    private LocalDateTime createdAt;
}