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
    private final ConjurConfig conjurConfig;
    private static final String CERT_DIR = "src/main/resources/certs/";

    public ConjurSslConfig(Conjur conjurClient, ConjurConfig conjurConfig) {
        this.conjurClient = conjurClient;
        this.conjurConfig = conjurConfig;
    }

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        logger.info("🔒 Configurando HTTPS via certificados armazenados no Conjur...");

        try {
            // Criar diretório para armazenar os certificados
            Files.createDirectories(Paths.get(CERT_DIR));

            // Recupera os certificados do Conjur
            String certPem = conjurClient.variables().retrieveSecret("certificates/backend-ms-a/cert");
            String keyPem = conjurClient.variables().retrieveSecret("certificates/backend-ms-a/key");
            String caCertPem = conjurClient.variables().retrieveSecret("certificates/ca/cert");

            if (certPem == null || keyPem == null || caCertPem == null) {
                throw new RuntimeException("❌ Certificados não encontrados no Conjur!");
            }

            // Obter as senhas do keystore e truststore do ConjurConfig
            String keystorePassword = conjurConfig.getKeystorePassword(conjurClient);
            String truststorePassword = conjurConfig.getTruststorePassword(conjurClient);

            // Criar os Keystores e salvar no classpath (resources/certs/)
            File keyStoreFile = saveKeystore(createKeyStore(certPem, keyPem, "backend-ms-a", keystorePassword), CERT_DIR + "keystore.p12");
            File trustStoreFile = saveKeystore(createTrustStore(caCertPem), CERT_DIR + "truststore.p12");

            // Definir variáveis de ambiente dinamicamente
            System.setProperty("KEYSTORE_PATH", keyStoreFile.getAbsolutePath());
            System.setProperty("KEYSTORE_PASSWORD", keystorePassword);
            System.setProperty("TRUSTSTORE_PATH", trustStoreFile.getAbsolutePath());
            System.setProperty("TRUSTSTORE_PASSWORD", truststorePassword);

            logger.info("✅ HTTPS configurado com sucesso! Keystore e Truststore salvos em {}", CERT_DIR);
        } catch (Exception e) {
            logger.error("❌ Erro ao configurar o SSL", e);
            throw new RuntimeException("❌ Erro ao configurar o SSL", e);
        }
    }

    private KeyStore createKeyStore(String certPem, String keyPem, String alias, String keystorePassword) throws Exception {
        logger.info("📌 Criando KeyStore...");

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(certPem.getBytes()));

        byte[] keyBytes = decodePemPrivateKey(keyPem);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        keyStore.setKeyEntry(alias, privateKey, keystorePassword.toCharArray(), new Certificate[]{cert});

        logger.info("✅ KeyStore criado com sucesso.");
        return keyStore;
    }

    private KeyStore createTrustStore(String caCertPem) throws Exception {
        logger.info("📌 Criando TrustStore...");

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        Certificate caCert = certFactory.generateCertificate(new ByteArrayInputStream(caCertPem.getBytes()));

        trustStore.setCertificateEntry("ca-cert", caCert);

        logger.info("✅ TrustStore criado com sucesso.");
        return trustStore;
    }

    private byte[] decodePemPrivateKey(String pem) {
        String privateKeyPEM = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                                  .replace("-----END PRIVATE KEY-----", "")
                                  .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(privateKeyPEM);
    }

    private File saveKeystore(KeyStore keyStore, String filePath) throws Exception {
        File file = new File(filePath);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            keyStore.store(fos, "".toCharArray());  // Não precisa de senha para salvar
        }

        logger.info("✅ Keystore salvo em: {}", file.getAbsolutePath());
        return file;
    }
}
