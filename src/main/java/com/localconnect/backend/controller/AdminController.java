package com.localconnect.backend.controller;

import com.localconnect.backend.dto.response.ServiceListingResponse;
import com.localconnect.backend.dto.response.UserResponse;
import com.localconnect.backend.mapper.UserMapper;
import com.localconnect.backend.repository.UserRepository;
import com.localconnect.backend.service.ServiceListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin("*")

public class AdminController {

    private final ServiceListingService serviceListingService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @GetMapping("/pending-services")
    public List<ServiceListingResponse> getPendingServices() {
        return serviceListingService.getPendingServices();
    }

    @PutMapping("/services/{id}/approve")
    public ServiceListingResponse approveService(@PathVariable Long id) {
        return serviceListingService.approveService(id);
    }

    @PutMapping("/services/{id}/reject")
    public ServiceListingResponse rejectService(@PathVariable Long id) {
        return serviceListingService.rejectService(id);
    }

    @GetMapping("/users")
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }
}