package com.appsec.projetoconjur.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/backend-b")
public class BackendBController {

    private final RestTemplate restTemplate;

    @Value("${backend.b.url}")
    private String backendBUrl;

    public BackendBController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/status")
    public String checkBackendBStatus() {
        return restTemplate.getForObject(backendBUrl, String.class);
    }
}
