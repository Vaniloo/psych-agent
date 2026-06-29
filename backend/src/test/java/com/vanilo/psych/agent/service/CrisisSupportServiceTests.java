package com.vanilo.psych.agent.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrisisSupportServiceTests {
    private final CrisisSupportService service = new CrisisSupportService();

    @Test
    void returnsDeterministicCrisisResources() {
        assertTrue(service.crisisReply().contains("12356"));
        assertTrue(service.resources().stream().anyMatch(resource -> "120".equals(resource.getContact())));
        assertTrue(service.resources().stream().anyMatch(resource -> "110".equals(resource.getContact())));
    }
}
