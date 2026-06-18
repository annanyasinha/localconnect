package com.localconnect.backend.dto.request;

import com.localconnect.backend.enums.RoleName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private RoleName role;
}