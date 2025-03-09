package com.appsec.projetoconjur.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
public class CredentialController {
    private static final Logger logger = LoggerFactory.getLogger(CredentialController.class);

    // Inicializa os status corretamente
    private static final Map<String, String> STATUS = new HashMap<>();

    static {
        STATUS.put("backend-ms-a", "Em Execução");
        STATUS.put("backend-ms-b", "Desconhecido");
        STATUS.put("conexao", "Desconhecido");
    }

    @GetMapping("/status")
    public Map<String, String> getStatus() {
        return STATUS;
    }

    @GetMapping("/atualizar-status")
    public Map<String, String> atualizarStatus() {
        String backendBStatus = "Desconhecido";
        String conexaoStatus = "Desconhecido";

        try {
            // A verificação de backend-b já implica que a comunicação mTLS deve estar correta
            URL url = new URL("https://backend-b:8444/backend-b/status");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.connect();

            if (conn.getResponseCode() == 200) {
                backendBStatus = "Em Execução";
                conexaoStatus = "Conectado"; // Se a resposta for OK, significa que o mTLS está funcionando
            }
        } catch (Exception e) {
            logger.error("Erro ao verificar backend-b: {}", e.getMessage());
            conexaoStatus = "Erro"; // Se falhar, significa que houve erro na comunicação mTLS
        }

        // Atualiza os status globalmente
        STATUS.put("backend-ms-b", backendBStatus);
        STATUS.put("conexao", conexaoStatus);

        return STATUS;
    }

    @GetMapping("/testar-conexao")
    public Map<String, String> testarConexao() {
        Map<String, String> resposta = new HashMap<>();
        resposta.put("status", "Conectado");
        return resposta;
    }
}
