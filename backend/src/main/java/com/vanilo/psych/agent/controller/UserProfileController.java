package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.UserProfileResponse;
import com.vanilo.psych.agent.dto.UserProfileUpdateRequest;
import com.vanilo.psych.agent.service.UserProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile")
public class UserProfileController {
    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public List<UserProfileResponse> listProfiles(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("未登陆");
        }
        return userProfileService.listAllProfiles();
    }

    @GetMapping("/{username}")
    public UserProfileResponse getProfile(@PathVariable String username, Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("未登陆");
        }
        return userProfileService.getProfile(username);
    }

    @PutMapping("/{username}")
    public UserProfileResponse updateProfile(@RequestBody UserProfileUpdateRequest request,
                                             @PathVariable String username,
                                             Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("未登陆");
        }
        return userProfileService.updateProfile(username, request);
    }
}
