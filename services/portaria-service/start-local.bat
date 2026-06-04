@echo off
:: Lê a senha do .env do auth-api
for /f "tokens=2 delims==" %%a in ('findstr "POSTGRES_PASSWORD" ..\auth-api\.env') do set POSTGRES_PASSWORD=%%a

set JAVA_HOME=C:\Program Files\Java\jdk-21
set SPRING_PROFILES_ACTIVE=local

echo Iniciando portaria-service com Java 21...
echo Senha lida do auth-api/.env
mvnw.cmd spring-boot:run
