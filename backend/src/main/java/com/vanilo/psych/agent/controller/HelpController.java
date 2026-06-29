package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.HelpResourceResponse;
import com.vanilo.psych.agent.service.CrisisSupportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/help")
public class HelpController {
    private final CrisisSupportService crisisSupportService;

    public HelpController(CrisisSupportService crisisSupportService) {
        this.crisisSupportService = crisisSupportService;
    }

    @GetMapping("/resources")
    public List<HelpResourceResponse> resources() {
        return crisisSupportService.resources();
    }

    @GetMapping("/crisis")
    public String crisisGuide() {
        return crisisSupportService.crisisReply();
    }
}
