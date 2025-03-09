package com.appsec.projetoconjur.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health") // ✅ Alterado para evitar conflito com /status
public class StatusController {
    private static final Logger logger = LoggerFactory.getLogger(StatusController.class);

    @GetMapping
    public String checkHealth() {
        logger.info("✅ BACKEND-A está ativo!");
        return "BACKEND-A está ativo ✅";
    }
}
