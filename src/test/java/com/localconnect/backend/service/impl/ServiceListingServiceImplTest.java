
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceListingServiceImplTest
{

    @Mock
    private ServiceListingRepository serviceListingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceListingMapper serviceListingMapper;

    @InjectMocks
    private ServiceListingServiceImpl serviceListingService;

    @Test
    void createService_shouldCreateServiceSuccessfully() {

        ServiceCreateRequest request = mock(ServiceCreateRequest.class);

        when(request.getTitle()).thenReturn("Plumbing Service");
        when(request.getCategory()).thenReturn("Home Repair");
        when(request.getSubCategory()).thenReturn("Plumbing");
        when(request.getDescription()).thenReturn("Professional plumbing");
        when(request.getPrice()).thenReturn(BigDecimal.valueOf(500));
        when(request.getCity()).thenReturn("Raipur");
        when(request.getArea()).thenReturn("Telibandha");
        when(request.getAvailable()).thenReturn(true);
        when(request.getImageUrl()).thenReturn("image.jpg");

        User provider = mock(User.class);

        when(userRepository.findByEmail("provider@gmail.com"))
                .thenReturn(Optional.of(provider));

        when(provider.getRole())
                .thenReturn(RoleName.PROVIDER);

        ServiceListing savedService = mock(ServiceListing.class);

        when(serviceListingRepository.save(any(ServiceListing.class)))
                .thenReturn(savedService);

        ServiceListingResponse response = mock(ServiceListingResponse.class);

        when(serviceListingMapper.toResponse(savedService))
                .thenReturn(response);

        ServiceListingResponse result =
                serviceListingService.createService(
                        request,
                        "provider@gmail.com"
                );

        assertNotNull(result);
        assertEquals(response, result);

        verify(userRepository)
                .findByEmail("provider@gmail.com");

        verify(serviceListingRepository)
                .save(any(ServiceListing.class));

        verify(serviceListingMapper)
                .toResponse(savedService);
    }

    @Test
    void createService_shouldThrowExceptionWhenUserIsNotProvider() {

        ServiceCreateRequest request = mock(ServiceCreateRequest.class);

        User user = mock(User.class);

        when(userRepository.findByEmail("user@gmail.com"))
                .thenReturn(Optional.of(user));

        when(user.getRole())
                .thenReturn(RoleName.USER);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> serviceListingService.createService(
                        request,
                        "user@gmail.com"
                )
        );

        assertEquals(
                "Only providers can create services",
                exception.getMessage()
        );

        verify(userRepository)
                .findByEmail("user@gmail.com");

        verify(serviceListingRepository, never())
                .save(any(ServiceListing.class));

        verify(serviceListingMapper, never())
                .toResponse(any());
    }
    @Test
    void createService_shouldThrowExceptionWhenSubCategoryIsMissing() {

        ServiceCreateRequest request = mock(ServiceCreateRequest.class);

        User provider = mock(User.class);

        when(userRepository.findByEmail("provider@gmail.com"))
                .thenReturn(Optional.of(provider));

        when(provider.getRole())
                .thenReturn(RoleName.PROVIDER);

        when(request.getSubCategory())
                .thenReturn("");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> serviceListingService.createService(
                        request,
                        "provider@gmail.com"
                )
        );

        assertEquals(
                "Sub-category is required",
                exception.getMessage()
        );

        verify(userRepository)
                .findByEmail("provider@gmail.com");

        verify(provider)
                .getRole();

        verify(serviceListingRepository, never())
                .save(any(ServiceListing.class));

        verify(serviceListingMapper, never())
                .toResponse(any());
    }

    @Test
    void approveService_shouldApproveServiceSuccessfully() {

        ServiceListing serviceListing = mock(ServiceListing.class);

        when(serviceListingRepository.findById(1L))
                .thenReturn(Optional.of(serviceListing));

        ServiceListingResponse response = mock(ServiceListingResponse.class);

        when(serviceListingRepository.save(serviceListing))
                .thenReturn(serviceListing);

        when(serviceListingMapper.toResponse(serviceListing))
                .thenReturn(response);

        ServiceListingResponse result =
                serviceListingService.approveService(1L);

        assertNotNull(result);
        assertEquals(response, result);

        verify(serviceListingRepository)
                .findById(1L);

        verify(serviceListing)
                .setApprovalStatus(ApprovalStatus.APPROVED);

        verify(serviceListingRepository)
                .save(serviceListing);

        verify(serviceListingMapper)
                .toResponse(serviceListing);
    }
}
