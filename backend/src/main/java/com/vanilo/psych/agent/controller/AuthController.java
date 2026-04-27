package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.LoginRequest;
import com.vanilo.psych.agent.dto.LoginResponse;
import com.vanilo.psych.agent.dto.RegisterRequest;
import com.vanilo.psych.agent.dto.RegisterResponse;
import com.vanilo.psych.agent.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest registerRequest) {
        return userService.register(registerRequest);
    }
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest loginRequest) {
        return userService.login(loginRequest);
    }

}
