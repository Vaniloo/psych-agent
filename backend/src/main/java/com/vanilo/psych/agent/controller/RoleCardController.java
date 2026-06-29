package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.RoleCardRequest;
import com.vanilo.psych.agent.dto.RoleCardResponse;
import com.vanilo.psych.agent.service.RoleCardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/role-cards")
public class RoleCardController {
    private final RoleCardService roleCardService;

    public RoleCardController(RoleCardService roleCardService) {
        this.roleCardService = roleCardService;
    }

    @GetMapping
    public List<RoleCardResponse> list(Authentication authentication) {
        requireAuthentication(authentication);
        return roleCardService.listAvailable(authentication.getName());
    }

    @PostMapping
    public RoleCardResponse create(@RequestBody RoleCardRequest request, Authentication authentication) {
        requireAuthentication(authentication);
        return roleCardService.create(authentication.getName(), request);
    }

    @PostMapping("/{id}/activate")
    public RoleCardResponse activate(@PathVariable Long id, Authentication authentication) {
        requireAuthentication(authentication);
        return roleCardService.activate(authentication.getName(), id);
    }

    private void requireAuthentication(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("未登录");
        }
    }
}
