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
                                Operações comerciais exigem ADMIN_GERAL. Leituras de assinatura ficam restritas ao próprio condomínio.

                                `/api/plans/subscriptions` é a API de compatibilidade em inglês de `/api/assinaturas`.
                                Ambas usam a mesma assinatura e o mesmo histórico em `tb_assinaturas`.
                                `/api/plans/access/{module}` verifica o módulo do condomínio identificado pelo JWT."""))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token devolvido por POST /api/auth/login no auth-api.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
