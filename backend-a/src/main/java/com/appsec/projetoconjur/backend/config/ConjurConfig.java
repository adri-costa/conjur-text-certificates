package com.appsec.projetoconjur.backend.config;

import com.cyberark.conjur.api.Conjur;
import com.cyberark.conjur.api.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

@Configuration
public class ConjurConfig {
    private static final Logger logger = LoggerFactory.getLogger(ConjurConfig.class);

    @Value("${conjur.appliance-url}")
    private String conjurApplianceUrl;

    @Value("${conjur.account}")
    private String conjurAccount;

    @Value("${conjur.authn-login}")
    private String conjurAuthnLogin;

    @Value("${conjur.authn-api-key}")
    private String conjurAuthnApiKey;

    @Value("${conjur.ssl.verify:false}") // Caso seja false, não validará o certificado
    private boolean conjurSslVerify;

    // Variáveis armazenadas no Conjur (sem duplicação do nome da conta)
    private static final String KEYSTORE_PASSWORD_PATH = "certificates-secrets/backend-ms-a/keystore-password";
    private static final String TRUSTSTORE_PASSWORD_PATH = "certificates-secrets/backend-ms-a/truststore-password";

    @Bean
    public Conjur conjurClient() {
        try {
            logger.info("🔑 Inicializando Conjur Client...");

            // Configura as credenciais
            Credentials credentials = new Credentials(conjurAuthnLogin, conjurAuthnApiKey);

            // Configura SSLContext
            SSLContext sslContext = conjurSslVerify ? SSLContext.getDefault() : createInsecureSslContext();

            // Inicializa o cliente do Conjur
            Conjur conjur = new Conjur(credentials, sslContext);

            logger.info("✅ Conjur Client inicializado com sucesso para: {}", conjurAuthnLogin);
            return conjur;
        } catch (Exception e) {
            logger.error("❌ Erro ao inicializar o Conjur Client: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao conectar ao Conjur", e);
        }
    }

    @Bean
    public String getKeystorePassword(Conjur conjur) {
        try {
            String path = conjurAccount + ":variable:" + KEYSTORE_PASSWORD_PATH;
            logger.info("🔍 Buscando Keystore password no Conjur: {}", path);
            String password = conjur.variables().retrieveSecret(KEYSTORE_PASSWORD_PATH); // Corrigido

            if (password == null || password.isEmpty()) {
                logger.error("❌ Keystore password não encontrado no Conjur! Verifique se a variável existe e tem valor.");
                throw new RuntimeException("Keystore password não encontrado no Conjur.");
            }

            logger.info("✅ Keystore password recuperado com sucesso.");
            return password;
        } catch (Exception e) {
            logger.error("❌ Erro ao obter Keystore password: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao recuperar Keystore password", e);
        }
    }

    @Bean
    public String getTruststorePassword(Conjur conjur) {
        try {
            String path = conjurAccount + ":variable:" + TRUSTSTORE_PASSWORD_PATH;
            logger.info("🔍 Buscando Truststore password no Conjur: {}", path);
            String password = conjur.variables().retrieveSecret(TRUSTSTORE_PASSWORD_PATH); // Corrigido

            if (password == null || password.isEmpty()) {
                logger.error("❌ Truststore password não encontrado no Conjur! Verifique se a variável existe e tem valor.");
                throw new RuntimeException("Truststore password não encontrado no Conjur.");
            }

            logger.info("✅ Truststore password recuperado com sucesso.");
            return password;
        } catch (Exception e) {
            logger.error("❌ Erro ao obter Truststore password: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao recuperar Truststore password", e);
        }
    }

    /**
     * Cria um SSLContext que ignora validação de certificados, útil para ambientes com certificados autoassinados.
     */
    private SSLContext createInsecureSslContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new java.security.SecureRandom());
            return sslContext;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao criar SSLContext inseguro", e);
        } catch (Exception e) {
            throw new RuntimeException("Erro inesperado ao configurar SSLContext", e);
        }
    }
}
