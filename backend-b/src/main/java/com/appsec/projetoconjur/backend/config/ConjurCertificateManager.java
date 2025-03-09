package com.appsec.projetoconjur.backend.config;

import com.cyberark.conjur.api.Conjur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Component
public class ConjurCertificateManager {

    private static final Logger logger = LoggerFactory.getLogger(ConjurCertificateManager.class);
    private final Conjur conjurClient;

    // Define o diretório de certificados dentro de src/main/resources
    private static final String CERTS_DIR = System.getProperty("user.dir") + "/src/main/resources/certs/";

    @Autowired
    public ConjurCertificateManager(Conjur conjurClient) {
        this.conjurClient = conjurClient;
    }

    private String getSecret(String variablePath) {
        try {
            logger.info("🔍 Buscando variável no Conjur: {}", variablePath);
            String secret = conjurClient.variables().retrieveSecret(variablePath);

            if (secret == null || secret.isEmpty()) {
                logger.warn("⚠️ Variável '{}' está vazia ou não encontrada.", variablePath);
                return null;
            }

            return secret;
        } catch (Exception e) {
            logger.error("❌ Erro ao buscar variável no Conjur: {}", variablePath, e);
            return null;
        }
    }

    public Certificate getCertificate(String variablePath) {
        try {
            String certPem = getSecret(variablePath);
            if (certPem == null) {
                throw new RuntimeException("Certificado não encontrado no Conjur: " + variablePath);
            }

            logger.info("✅ Certificado recebido do Conjur para '{}':\n{}", variablePath, certPem);

            return CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)));

        } catch (Exception e) {
            logger.error("❌ Erro ao recuperar certificado do Conjur", e);
            throw new RuntimeException("Erro ao recuperar certificado", e);
        }
    }

    public PrivateKey getPrivateKey(String variablePath) {
        try {
            String keyPem = getSecret(variablePath);
            if (keyPem == null) {
                throw new RuntimeException("Chave privada não encontrada no Conjur: " + variablePath);
            }

            logger.info("🔑 Chave privada recebida do Conjur para '{}'", variablePath);

            keyPem = keyPem.replace("-----BEGIN PRIVATE KEY-----", "")
                           .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                           .replace("-----END PRIVATE KEY-----", "")
                           .replace("-----END RSA PRIVATE KEY-----", "")
                           .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(keyPem);

            // Carrega a chave privada no formato PKCS8 (padrão moderno)
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return keyFactory.generatePrivate(keySpec);

        } catch (Exception e) {
            logger.error("❌ Erro ao recuperar chave privada do Conjur", e);
            throw new RuntimeException("Erro ao recuperar chave privada", e);
        }
    }

    public void createKeyStores(String basePath) {
        try {
            logger.info("🔑 Criando Keystore `keystore.p12` e Truststore `truststore.p12`...");

            // Criar diretório se não existir
            File certsDir = new File(CERTS_DIR);
            if (!certsDir.exists()) {
                if (certsDir.mkdirs()) {
                    logger.info("📂 Diretório '{}' criado com sucesso.", CERTS_DIR);
                } else {
                    logger.error("❌ Falha ao criar diretório '{}'.", CERTS_DIR);
                    return;
                }
            }

            // Verificar se os arquivos já existem
            File keystoreFile = new File(CERTS_DIR + "keystore.p12");
            File truststoreFile = new File(CERTS_DIR + "truststore.p12");

            if (keystoreFile.exists() && truststoreFile.exists()) {
                logger.info("✅ Keystore e Truststore já existem. Pulando criação.");
                return;
            }

            // Buscar os certificados e chave privada
            Certificate fullchain = getCertificate(basePath + "/fullchain");
            PrivateKey privateKey = getPrivateKey(basePath + "/key");
            Certificate caCert = getCertificate("certificates/ca/cert");
            String keystorePassword = getSecret(basePath.replace("certificates", "certificates-secrets") + "/keystore-password");
            String truststorePassword = getSecret(basePath.replace("certificates", "certificates-secrets") + "/truststore-password");

            if (fullchain == null || privateKey == null || caCert == null || keystorePassword == null || truststorePassword == null) {
                logger.error("❌ Certificados, chave privada ou senhas ausentes. Não será possível gerar os stores.");
                return;
            }

            // Criar Keystore
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(null, keystorePassword.toCharArray());

            // 🚀 Montando a cadeia corretamente:
            Certificate[] chain = {fullchain, caCert};

            // Inserir chave privada + cadeia de certificados
            keystore.setKeyEntry("backend-ms-b", privateKey, keystorePassword.toCharArray(), chain);

            try (FileOutputStream fos = new FileOutputStream(CERTS_DIR + "keystore.p12")) {
                keystore.store(fos, keystorePassword.toCharArray());
            }

            logger.info("✅ Keystore `keystore.p12` salvo com sucesso!");

            // Criar Truststore
            KeyStore truststore = KeyStore.getInstance("PKCS12");
            truststore.load(null, truststorePassword.toCharArray());

            // Adicionar o CA ao Truststore
            truststore.setCertificateEntry("ca-cert", caCert);

            try (FileOutputStream fos = new FileOutputStream(CERTS_DIR + "truststore.p12")) {
                truststore.store(fos, truststorePassword.toCharArray());
            }

            logger.info("✅ Truststore `truststore.p12` salvo com sucesso!");

        } catch (Exception e) {
            logger.error("❌ Erro ao criar Keystore/Truststore", e);
        }
    }
}
