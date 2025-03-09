package com.appsec.projetoconjur.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/backend-b")
public class BackendBController {

    @GetMapping("/status")
    public String getBackendBStatus() {
        return "Backend-B está rodando!";
    }
}
