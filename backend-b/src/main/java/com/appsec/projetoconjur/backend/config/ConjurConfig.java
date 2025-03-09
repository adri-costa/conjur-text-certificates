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

    @Value("${conjur.ssl.verify:false}")
    private boolean conjurSslVerify;

    @Bean
    public Conjur conjurClient() {
        try {
            logger.info("🔑 Inicializando Conjur Client...");
            Credentials credentials = new Credentials(conjurAuthnLogin, conjurAuthnApiKey);
            SSLContext sslContext = conjurSslVerify ? SSLContext.getDefault() : createInsecureSslContext();
            Conjur conjur = new Conjur(credentials, sslContext);
            logger.info("✅ Conjur Client inicializado com sucesso para: {}", conjurAuthnLogin);
            return conjur;
        } catch (Exception e) {
            logger.error("❌ Erro ao inicializar o Conjur Client: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao conectar ao Conjur", e);
        }
    }

    private SSLContext createInsecureSslContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}

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
