
package com.localconnect.backend.service.impl;

import com.localconnect.backend.dto.request.LoginRequest;
import com.localconnect.backend.dto.request.RegisterRequest;
import com.localconnect.backend.dto.response.AuthResponse;
import com.localconnect.backend.dto.response.UserResponse;
import com.localconnect.backend.entity.User;
import com.localconnect.backend.enums.RoleName;
import com.localconnect.backend.mapper.UserMapper;
import com.localconnect.backend.repository.UserRepository;
import com.localconnect.backend.security.JwtService;
import com.localconnect.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole() == null ? RoleName.USER : request.getRole())
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser.getEmail());

        UserResponse userResponse = userMapper.toResponse(savedUser);

        return AuthResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail());

        UserResponse userResponse = userMapper.toResponse(user);

        return AuthResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }
}