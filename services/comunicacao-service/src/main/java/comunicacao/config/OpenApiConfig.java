package comunicacao.config;

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
 * O endereço do servidor não é fixado aqui: o springdoc o monta a partir da
 * requisição, e com `server.forward-headers-strategy: framework` ele respeita o
 * `X-Forwarded-Prefix` que o Traefik envia. Assim o "Try it out" chama
 * `/api/comunicacao/...` pelo gateway, e não a raiz.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("comunicacao-service")
                        .version("1.0")
                        .description("""
                                Comunicados, base de conhecimento, conversas e a caixa de notificações \
                                de toda a plataforma (RF-12 e RF-13).

                                - **Responsável:** João Victor
                                - **Banco:** `mora` (compartilhado com o portaria-service)
                                - **Pelo gateway:** `/api/comunicacao`, prefixo removido antes de chegar aqui
                                - **Autenticação:** JWT emitido pelo auth-api (`POST /api/auth/login`), no botão \
                                **Authorize**. O condomínio sai sempre do token, nunca do corpo.

                                Os exemplos usam os dados do seed de desenvolvimento \
                                (`scripts/seed-dev.mjs`): condomínio Residencial Parque Verde."""))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token devolvido por POST /api/auth/login no auth-api.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
