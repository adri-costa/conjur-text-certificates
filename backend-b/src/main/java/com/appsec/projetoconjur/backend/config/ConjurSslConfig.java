package com.appsec.projetoconjur.backend.config;

import com.cyberark.conjur.api.Conjur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Component
public class ConjurSslConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    private static final Logger logger = LoggerFactory.getLogger(ConjurSslConfig.class);
    private final Conjur conjurClient;
    private static final String CERT_DIR = "src/main/resources/certs/";
    private static final String KEYSTORE_PASSWORD = "changeit";
    private static final String TRUSTSTORE_PASSWORD = "changeit";

    public ConjurSslConfig(Conjur conjurClient) {
        this.conjurClient = conjurClient;
    }

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        logger.info("🔒 Configurando HTTPS via certificados armazenados no Conjur...");

        try {
            // 🔹 1. Criar diretório para armazenar os certificados, se não existir
            File certDir = new File(CERT_DIR);
            if (!certDir.exists()) {
                certDir.mkdirs();
            }

            // 🔹 2. Recupera os certificados do Conjur
            String certPem = conjurClient.variables().retrieveSecret("certificates/backend-ms-b/cert");
            String keyPem = conjurClient.variables().retrieveSecret("certificates/backend-ms-b/key");
            String caCertPem = conjurClient.variables().retrieveSecret("certificates/ca/cert");

            if (certPem == null || keyPem == null || caCertPem == null) {
                throw new RuntimeException("❌ Certificados não encontrados no Conjur!");
            }

            // 🔹 3. Cria os Keystores e salva no classpath (resources/certs/)
            File keyStoreFile = saveKeystore(createKeyStore(certPem, keyPem, "backend-ms-b"), CERT_DIR + "keystore.p12");
            File trustStoreFile = saveKeystore(createTrustStore(caCertPem), CERT_DIR + "truststore.p12");

            // 🔹 4. Define propriedades do Spring Boot para utilizar os arquivos
            System.setProperty("server.ssl.key-store", keyStoreFile.getAbsolutePath());
            System.setProperty("server.ssl.key-store-password", KEYSTORE_PASSWORD);
            System.setProperty("server.ssl.key-store-type", "PKCS12");

            System.setProperty("server.ssl.trust-store", trustStoreFile.getAbsolutePath());
            System.setProperty("server.ssl.trust-store-password", TRUSTSTORE_PASSWORD);
            System.setProperty("server.ssl.trust-store-type", "PKCS12");

            logger.info("✅ HTTPS configurado com sucesso! Keystore e Truststore salvos em {}", CERT_DIR);
        } catch (Exception e) {
            logger.error("❌ Erro ao configurar o SSL", e);
            throw new RuntimeException("❌ Erro ao configurar o SSL", e);
        }
    }

    private KeyStore createKeyStore(String certPem, String keyPem, String alias) throws Exception {
        logger.info("📌 Criando KeyStore para {}...", alias);

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);

            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(certPem.getBytes()));

            byte[] keyBytes = decodePemPrivateKey(keyPem);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

            keyStore.setKeyEntry(alias, privateKey, KEYSTORE_PASSWORD.toCharArray(), new Certificate[]{cert});

            logger.info("✅ KeyStore criado com sucesso para {}.", alias);
            return keyStore;
        } catch (Exception e) {
            logger.error("❌ Erro ao criar Keystore para {}", alias, e);
            throw new RuntimeException("Erro ao criar Keystore", e);
        }
    }

    private KeyStore createTrustStore(String caCertPem) throws Exception {
        logger.info("📌 Criando TrustStore...");

        try {
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(null, null);

            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            Certificate caCert = certFactory.generateCertificate(new ByteArrayInputStream(caCertPem.getBytes()));

            trustStore.setCertificateEntry("ca-cert", caCert);

            logger.info("✅ TrustStore criado com sucesso.");
            return trustStore;
        } catch (Exception e) {
            logger.error("❌ Erro ao criar TrustStore", e);
            throw new RuntimeException("Erro ao criar TrustStore", e);
        }
    }

    private byte[] decodePemPrivateKey(String pem) {
        String privateKeyPEM = pem.replaceAll("(?m)^-----.*-----$", "").replaceAll("\\s+", "");
        return Base64.getDecoder().decode(privateKeyPEM);
    }

    private File saveKeystore(KeyStore keyStore, String filePath) throws Exception {
        File file = new File(filePath);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray());
        }

        logger.info("✅ Keystore salvo em: {}", file.getAbsolutePath());
        return file;
    }
}
