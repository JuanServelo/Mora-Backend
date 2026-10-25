package portaria.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Especificação OpenAPI do portaria, servida em `/v3/api-docs` e reunida no
 * portal `/docs` do gateway com a dos outros serviços.
 *
 * Os exemplos não ficam anotados campo a campo: as entidades daqui se aninham
 * (vaga → apartamento → bloco), e anotar cada campo dessa árvore seria
 * impraticável de manter. Ficam em `openapi/exemplos.json`, um exemplo
 * completo por esquema, aplicados aqui na especificação gerada:
 *
 * - `schemas`: exemplo do objeto inteiro, por nome de esquema;
 * - `respostas`: para resposta sem esquema próprio, por "MÉTODO caminho status";
 * - `corpos`: para corpo sem esquema próprio, por "MÉTODO caminho".
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("portaria-service")
                        .version("1.0")
                        .description("""
                                Estrutura física e o dia a dia da portaria: blocos, apartamentos, vagas e áreas \
                                comuns; moradores, funcionários, jornadas e turnos; visitantes, pré-autorizações, \
                                entregas, chaves, veículos, reservas e aluguel de vagas (RF-05 a RF-10).

                                - **Responsável:** Juan Servelo
                                - **Banco:** `mora` (compartilhado com o comunicacao-service)
                                - **Pelo gateway:** `/api/portaria`, prefixo removido antes de chegar aqui; `/api/vagas` \
                                também chega aqui, por compatibilidade
                                - **Autenticação:** JWT do auth-api (`POST /api/auth/login`), no botão **Authorize**. \
                                O condomínio sai do token.

                                Os exemplos seguem o seed de desenvolvimento: Residencial Parque Verde, Bloco A, \
                                apartamento 101."""))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token devolvido por POST /api/auth/login no auth-api.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OpenApiCustomizer exemplosDoArquivo(ObjectMapper mapper) {
        JsonNode exemplos = ler(mapper);
        return openApi -> {
            var schemas = openApi.getComponents() == null ? null : openApi.getComponents().getSchemas();
            if (schemas != null) {
                exemplos.path("schemas").fields().forEachRemaining(e -> {
                    var schema = schemas.get(e.getKey());
                    if (schema != null) schema.setExample(mapper.convertValue(e.getValue(), Object.class));
                });
            }
            exemplos.path("respostas").fields().forEachRemaining(e -> {
                String[] partes = e.getKey().split(" ");
                Operation op = operacao(openApi, partes[0], partes[1]);
                if (op == null || op.getResponses() == null) return;
                var resposta = op.getResponses().get(partes[2]);
                if (resposta != null) aplicar(resposta.getContent(), mapper.convertValue(e.getValue(), Object.class));
            });
            exemplos.path("corpos").fields().forEachRemaining(e -> {
                String[] partes = e.getKey().split(" ");
                Operation op = operacao(openApi, partes[0], partes[1]);
                if (op != null && op.getRequestBody() != null) {
                    aplicar(op.getRequestBody().getContent(), mapper.convertValue(e.getValue(), Object.class));
                }
            });
        };
    }

    private static void aplicar(Content content, Object exemplo) {
        if (content == null) return;
        content.values().forEach(media -> media.setExample(exemplo));
    }

    private static Operation operacao(OpenAPI openApi, String metodo, String caminho) {
        PathItem item = openApi.getPaths() == null ? null : openApi.getPaths().get(caminho);
        if (item == null) return null;
        return item.readOperationsMap().get(PathItem.HttpMethod.valueOf(metodo));
    }

    private static JsonNode ler(ObjectMapper mapper) {
        try (InputStream in = new ClassPathResource("openapi/exemplos.json").getInputStream()) {
            return mapper.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("openapi/exemplos.json não encontrado ou inválido", e);
        }
    }
}
