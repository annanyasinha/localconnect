package com.localconnect.backend.service.impl;

import com.localconnect.backend.dto.request.RegisterRequest;
import com.localconnect.backend.dto.response.AuthResponse;
import com.localconnect.backend.dto.response.UserResponse;
import com.localconnect.backend.entity.User;
import com.localconnect.backend.mapper.UserMapper;
import com.localconnect.backend.repository.UserRepository;
import com.localconnect.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.localconnect.backend.dto.request.LoginRequest;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_shouldCreateUserSuccessfully() {

        RegisterRequest request = mock(RegisterRequest.class);

        when(request.getEmail()).thenReturn("test@gmail.com");
        when(request.getFullName()).thenReturn("Test User");
        when(request.getPassword()).thenReturn("password123");
        when(request.getPhone()).thenReturn("9876543210");
        when(request.getRole()).thenReturn(null);

        when(userRepository.existsByEmail("test@gmail.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");

        User savedUser = mock(User.class);

        when(savedUser.getEmail())
                .thenReturn("test@gmail.com");

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        when(jwtService.generateToken("test@gmail.com"))
                .thenReturn("jwt-token");

        UserResponse userResponse = mock(UserResponse.class);

        when(userMapper.toResponse(savedUser))
                .thenReturn(userResponse);

        AuthResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(userResponse, response.getUser());

        verify(userRepository).existsByEmail("test@gmail.com");
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
        verify(jwtService).generateToken("test@gmail.com");
        verify(userMapper).toResponse(savedUser);
    }
    @Test
    void register_shouldThrowExceptionWhenEmailAlreadyExists() {

        RegisterRequest request = mock(RegisterRequest.class);

        when(request.getEmail()).thenReturn("test@gmail.com");

        when(userRepository.existsByEmail("test@gmail.com"))
                .thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.register(request)
        );

        assertEquals("Email already exists", exception.getMessage());

        verify(userRepository).existsByEmail("test@gmail.com");

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtService, never()).generateToken(anyString());
    }
    @Test
    void login_shouldLoginSuccessfully() {

        LoginRequest request = mock(LoginRequest.class);

        when(request.getEmail()).thenReturn("test@gmail.com");
        when(request.getPassword()).thenReturn("password123");

        User user = mock(User.class);

        when(user.getEmail()).thenReturn("test@gmail.com");
        when(user.getPassword()).thenReturn("encodedPassword");

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "encodedPassword"
        )).thenReturn(true);

        when(jwtService.generateToken("test@gmail.com"))
                .thenReturn("jwt-token");

        UserResponse userResponse = mock(UserResponse.class);

        when(userMapper.toResponse(user))
                .thenReturn(userResponse);

        AuthResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(userResponse, response.getUser());

        verify(userRepository).findByEmail("test@gmail.com");
        verify(passwordEncoder)
                .matches("password123", "encodedPassword");
        verify(jwtService).generateToken("test@gmail.com");
        verify(userMapper).toResponse(user);
    }
    @Test
    void login_shouldThrowExceptionWhenUserDoesNotExist() {

        LoginRequest request = mock(LoginRequest.class);

        when(request.getEmail()).thenReturn("unknown@gmail.com");

        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.login(request)
        );

        assertEquals("User not found", exception.getMessage());

        verify(userRepository)
                .findByEmail("unknown@gmail.com");

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(jwtService, never())
                .generateToken(anyString());
    }
}