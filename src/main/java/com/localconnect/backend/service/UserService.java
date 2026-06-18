package com.localconnect.backend.service;

import com.localconnect.backend.dto.request.LoginRequest;
import com.localconnect.backend.dto.request.RegisterRequest;
import com.localconnect.backend.dto.response.AuthResponse;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}