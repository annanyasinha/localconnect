package com.localconnect.backend.service.impl;

import com.localconnect.backend.dto.request.BookingCreateRequest;
import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.entity.Booking;
import com.localconnect.backend.entity.ServiceListing;
import com.localconnect.backend.entity.User;
import com.localconnect.backend.enums.ApprovalStatus;
import com.localconnect.backend.mapper.BookingMapper;
import com.localconnect.backend.repository.BookingRepository;
import com.localconnect.backend.repository.ServiceListingRepository;
import com.localconnect.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceListingRepository serviceListingRepository;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void createBooking_shouldCreateBookingSuccessfully() {

        BookingCreateRequest request = mock(BookingCreateRequest.class);

        when(request.getServiceId()).thenReturn(1L);
        when(request.getMessage()).thenReturn("Please come in the morning");
        when(request.getBookingDate()).thenReturn(null);

        User user = mock(User.class);

        ServiceListing serviceListing = mock(ServiceListing.class);

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(serviceListingRepository.findById(1L))
                .thenReturn(Optional.of(serviceListing));

        when(serviceListing.getApprovalStatus())
                .thenReturn(ApprovalStatus.APPROVED);

        Booking savedBooking = mock(Booking.class);

        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(savedBooking);

        BookingResponse bookingResponse = mock(BookingResponse.class);

        when(bookingMapper.toResponse(savedBooking))
                .thenReturn(bookingResponse);

        BookingResponse response =
                bookingService.createBooking(request, "test@gmail.com");

        assertNotNull(response);
        assertEquals(bookingResponse, response);

        verify(userRepository)
                .findByEmail("test@gmail.com");

        verify(serviceListingRepository)
                .findById(1L);

        verify(bookingRepository)
                .save(any(Booking.class));

        verify(bookingMapper)
                .toResponse(savedBooking);
    }

    @Test
    void createBooking_shouldThrowExceptionWhenUserDoesNotExist() {

        BookingCreateRequest request = mock(BookingCreateRequest.class);

        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> bookingService.createBooking(
                        request,
                        "unknown@gmail.com"
                )
        );

        assertEquals("User not found", exception.getMessage());

        verify(userRepository)
                .findByEmail("unknown@gmail.com");

        verify(serviceListingRepository, never())
                .findById(anyLong());

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void createBooking_shouldThrowExceptionWhenServiceIsNotApproved() {

        BookingCreateRequest request = mock(BookingCreateRequest.class);

        when(request.getServiceId()).thenReturn(1L);

        User user = mock(User.class);

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        ServiceListing serviceListing = mock(ServiceListing.class);

        when(serviceListingRepository.findById(1L))
                .thenReturn(Optional.of(serviceListing));

        when(serviceListing.getApprovalStatus())
                .thenReturn(ApprovalStatus.PENDING);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> bookingService.createBooking(
                        request,
                        "test@gmail.com"
                )
        );

        assertEquals("Service is not approved", exception.getMessage());

        verify(userRepository)
                .findByEmail("test@gmail.com");

        verify(serviceListingRepository)
                .findById(1L);

        verify(bookingRepository, never())
                .save(any(Booking.class));

        verify(bookingMapper, never())
                .toResponse(any());
    }
    @Test
    void getMyBookings_shouldReturnUserBookings() {

        User user = mock(User.class);

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        Booking booking1 = mock(Booking.class);
        Booking booking2 = mock(Booking.class);

        when(bookingRepository.findByUser(user))
                .thenReturn(List.of(booking1, booking2));

        BookingResponse response1 = mock(BookingResponse.class);
        BookingResponse response2 = mock(BookingResponse.class);

        when(bookingMapper.toResponse(booking1))
                .thenReturn(response1);

        when(bookingMapper.toResponse(booking2))
                .thenReturn(response2);

        List<BookingResponse> responses =
                bookingService.getMyBookings("test@gmail.com");

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(response1, responses.get(0));
        assertEquals(response2, responses.get(1));

        verify(userRepository)
                .findByEmail("test@gmail.com");

        verify(bookingRepository)
                .findByUser(user);

        verify(bookingMapper)
                .toResponse(booking1);

        verify(bookingMapper)
                .toResponse(booking2);
    }
}