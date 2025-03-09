package com.appsec.projetoconjur.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/backend-a")
public class BackendAController {

    @GetMapping("/status")
    public String getBackendAStatus() {
        return "Backend-A está rodando!";
    }
}
