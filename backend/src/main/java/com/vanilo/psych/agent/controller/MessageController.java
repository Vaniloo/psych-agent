package com.vanilo.psych.agent.controller;


import com.vanilo.psych.agent.dto.MessageRequest;
import com.vanilo.psych.agent.dto.MessageResponse;

import com.vanilo.psych.agent.service.MessageRouterService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/message")
public class MessageController {
    private final MessageRouterService messageRouterService;

    public MessageController(MessageRouterService messageRouterService) {
        this.messageRouterService = messageRouterService;
    }
    @PostMapping
    public MessageResponse routeMessage(@RequestBody MessageRequest messageRequest, Authentication authentication){
        if (messageRequest == null || messageRequest.getMessage() == null || messageRequest.getMessage().isBlank()) {
            throw new RuntimeException("message不能为空");
        }
        if(authentication == null){
            throw new RuntimeException("未登陆");
        }
        String username = authentication.getName();
        return messageRouterService.route(messageRequest.getMessage(), username);

    }
}
