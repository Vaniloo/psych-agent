package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.UserProfileResponse;
import com.vanilo.psych.agent.dto.UserProfileUpdateRequest;
import com.vanilo.psych.agent.service.UserProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
public class UserProfileController {
    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public UserProfileResponse getProfile(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("未登陆");
        }
        return userProfileService.getProfile(authentication.getName());
    }

    @PutMapping
    public UserProfileResponse updateProfile(@RequestBody UserProfileUpdateRequest request,
                                             Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("未登陆");
        }
        return userProfileService.updateProfile(authentication.getName(), request);
    }
}
