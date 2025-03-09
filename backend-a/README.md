Backend-A Conjur
Este projeto é um exemplo de aplicação Java Spring Boot que utiliza o Conjur (CyberArk) para gerenciar certificados e chaves privadas, configurando HTTPS com keystores e truststores gerados dinamicamente. Ele também se comunica com outro backend (Backend-B) para verificar o status da aplicação.

🚀 Como Executar o Projeto
Pré-requisitos
Java 17: Certifique-se de ter o JDK 17 instalado.

Maven: Para gerenciar as dependências e construir o projeto.

Conjur: Um servidor Conjur configurado com as variáveis necessárias (certificados, chaves privadas, senhas, etc.).

Configuração do Ambiente
Clone o repositório:

bash
Copy
git clone https://github.com/seu-usuario/projetoconjur.git
cd projetoconjur/certificados/backend-a
Configure as variáveis de ambiente:
Crie um arquivo .env na raiz do projeto com as seguintes variáveis:

bash
Copy
CONJUR_APPLIANCE_URL=<URL_DO_CONJUR>
CONJUR_ACCOUNT=<CONTA_DO_CONJUR>
CONJUR_AUTHN_LOGIN=<LOGIN_DO_CONJUR>
CONJUR_AUTHN_API_KEY=<API_KEY_DO_CONJUR>
KEYSTORE_PASSWORD=<SENHA_DO_KEYSTORE>
TRUSTSTORE_PASSWORD=<SENHA_DO_TRUSTSTORE>
Compile o projeto:

bash
Copy
mvn clean install
Execute a aplicação:

bash
Copy
mvn spring-boot:run
A aplicação estará disponível em:
Backend-A: https://localhost:8443/backend-a/status
Backend-B: https://localhost:8444/backend-b/status

📂 Estrutura do Projeto
A estrutura do projeto segue as boas práticas do Spring Boot:

Copy
backend-a/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── appsec/
│   │   │           └── projetoconjur/
│   │   │               ├── BackendApplication.java
│   │   │               ├── config/
│   │   │               │   ├── ConjurCertificateManager.java
│   │   │               │   ├── ConjurConfig.java
│   │   │               │   ├── ConjurSslConfig.java
│   │   │               │   └── RestTemplateConfig.java
│   │   │               ├── controller/
│   │   │               │   ├── BackendAController.java
│   │   │               │   ├── BackendBController.java
│   │   │               │   ├── CredentialController.java
│   │   │               │   └── StatusController.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── certs/
│   │       │   ├── keystore.p12
│   │       │   └── truststore.p12
│   │       └── static/
│   │           ├── favicon.icon
│   │           └── index.html
├── pom.xml
└── README.md
🛠️ Funcionalidades
1. Configuração de HTTPS
A aplicação gera dinamicamente os arquivos keystore.p12 e truststore.p12 usando certificados e chaves privadas armazenados no Conjur.

O Tomcat é configurado para rodar em HTTPS na porta 8443.

2. Comunicação com Backend-B
A aplicação possui um endpoint (/backend-b/status) que consulta o status do Backend-B via HTTPS.

A comunicação é segura, utilizando certificados válidos.

3. Interface Web
Uma página HTML simples (index.html) exibe o status do Backend-A, Backend-B e a conexão entre eles.

Botões permitem atualizar o status manualmente e testar a conexão.

4. Health Check
O endpoint /health retorna o status da aplicação.