package com.localconnect.backend.controller;

import com.localconnect.backend.dto.request.LoginRequest;
import com.localconnect.backend.dto.request.RegisterRequest;
import com.localconnect.backend.dto.response.AuthResponse;
import com.localconnect.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return userService.login(request);
    }
}