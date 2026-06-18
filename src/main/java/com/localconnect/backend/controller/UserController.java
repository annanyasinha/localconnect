
package com.localconnect.backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/profile")
    public String profile() {
        return "User Profile Accessed Successfully!";
    }
}
