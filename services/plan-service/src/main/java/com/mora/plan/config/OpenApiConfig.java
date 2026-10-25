package com.mora.plan.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cabeçalho da especificação OpenAPI, servida em `/api/plans/v3/api-docs` e
 * reunida no portal `/docs` do gateway com a dos outros serviços.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("plan-service")
                        .version("1.0")
                        .description("""
                                Planos comerciais da plataforma e a assinatura de cada condomínio: o que ele contratou, \
                                por quanto, com quais módulos e até quando (RF-01 e RF-04).

                                - **Responsável:** Luana Akemi
                                - **Banco:** `mora_plan`
                                - **Pelo gateway:** `/api/plans` e `/api/assinaturas` (sem remover prefixo)
                                - **Autenticação:** JWT do auth-api (`POST /api/auth/login`), no botão **Authorize**. \
                                Todas as rotas exigem token; o serviço ainda não restringe por perfil.

                                Há dois modelos de assinatura: `/api/plans/subscriptions` (o original) e \
                                `/api/assinaturas` (o que o front e o gestao-geral leem, com vigência e status em \
                                português). Os exemplos usam o condomínio Residencial Parque Verde do seed."""))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token devolvido por POST /api/auth/login no auth-api.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
