package ocorrencias.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cabeçalho da especificação OpenAPI, servida em `/v3/api-docs` e reunida no
 * portal `/docs` do gateway junto com a dos outros serviços.
 *
 * O endereço do servidor não é fixado aqui: com
 * `server.forward-headers-strategy: framework` o springdoc respeita o
 * `X-Forwarded-Prefix` do Traefik, e o "Try it out" chama `/api/ocorrencias/...`.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ocorrencias-service")
                        .version("1.0")
                        .description("""
                                Ocorrências dos moradores, ordens de serviço e fornecedores (RF-14): registro, \
                                atendimento com histórico, OS vinculada à ocorrência ou preventiva, prazo e \
                                responsável, e o job diário que marca as OS atrasadas.

                                - **Responsável:** Ray Govaski
                                - **Banco:** `mora_ocorrencias` (Flyway)
                                - **Pelo gateway:** `/api/ocorrencias`, prefixo removido antes de chegar aqui
                                - **Autenticação:** JWT emitido pelo auth-api (`POST /api/auth/login`), no botão \
                                **Authorize**. O condomínio sai sempre do token, nunca do corpo.
                                - **Notificações:** saem pela fila `mora.notificacoes` (RabbitMQ); o \
                                comunicacao-service as entrega no sino.

                                Os exemplos usam o condomínio de teste `cond-teste`."""))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token devolvido por POST /api/auth/login no auth-api.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
