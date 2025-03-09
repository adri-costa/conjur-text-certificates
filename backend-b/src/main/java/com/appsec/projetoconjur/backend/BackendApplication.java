package com.appsec.projetoconjur.backend;

import com.appsec.projetoconjur.backend.config.ConjurCertificateManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class BackendApplication {
    public static void main(String[] args) {
        // 🚀 Inicializa o contexto Spring
        ApplicationContext context = SpringApplication.run(BackendApplication.class, args);

        // 🔧 Obtém o ConjurCertificateManager do Spring
        ConjurCertificateManager conjurCertificateManager = context.getBean(ConjurCertificateManager.class);

        // 🔑 Gera os KeyStores antes de iniciar a aplicação
        conjurCertificateManager.createKeyStores("certificates/backend-ms-b");

        System.out.println("✅ Keystore e Truststore gerados com sucesso para Backend-B.");
    }
}
