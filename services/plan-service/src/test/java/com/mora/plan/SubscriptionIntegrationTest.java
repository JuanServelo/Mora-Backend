package com.mora.plan;

import com.mora.plan.config.SubscriptionMigration;
import com.mora.plan.dto.*;
import com.mora.plan.entity.*;
import com.mora.plan.enums.StatusAssinatura;
import com.mora.plan.repository.*;
import com.mora.plan.service.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:plans;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
        "spring.cloud.consul.enabled=false", "spring.cloud.service-registry.auto-registration.enabled=false",
        "jwt.secret=segredo-de-teste-com-pelo-menos-32-caracteres"})
@AutoConfigureMockMvc
class SubscriptionIntegrationTest {
    private static final String SECRET = "segredo-de-teste-com-pelo-menos-32-caracteres";
    @Autowired SubscriptionService english;
    @Autowired AssinaturaService portugues;
    @Autowired AssinaturaRepository assinaturas;
    @Autowired PlanRepository plans;
    @Autowired SubscriptionMigration migration;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    Plan plan;

    @BeforeEach void setup() {
        jdbc.execute("DROP TABLE IF EXISTS tb_subscriptions");
        jdbc.execute("DROP TABLE IF EXISTS tb_subscriptions_legacy");
        assinaturas.deleteAll();
        plans.deleteAll();
        plan = plans.save(Plan.builder().name("Teste").monthlyPrice(BigDecimal.TEN)
                .maxCondominiums(1).maxUsersPerCondominium(50).activeModules(List.of("portaria")).build());
    }

    SubscriptionResponseDTO create(String tenant) {
        return english.createSubscription(SubscriptionRequestDTO.builder().condominioId(tenant)
                .planId(plan.getId()).startDate(LocalDate.now().minusDays(1)).build());
    }

    String token(String tenant, String perfil) {
        return "Bearer " + Jwts.builder().claim("condominioId", tenant).claim("perfil", perfil)
                .claim("email", "teste@mora.test").signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test void bothApisShareCreationCancellationAndPlanChanges() {
        var first = create("qa-teste");
        assertThat(portugues.buscarVigentePorCondominio("qa-teste").getId()).isEqualTo(first.getId());
        assertThat(english.getModulesByCondominioId("qa-teste")).containsExactly("portaria");
        english.cancel(first.getId());
        assertThat(portugues.modulosPermitidos("qa-teste")).isEmpty();
        for (int i = 0; i < 3; i++) {
            ChangePlanRequestDTO change = new ChangePlanRequestDTO();
            change.setNewPlanId(plan.getId());
            english.changePlan("qa-teste", change);
        }
        assertThat(portugues.historicoDoCondominio("qa-teste")).hasSize(4);
    }

    @Test void expiredFutureSuspendedAndInactivePlansDoNotGrantModules() {
        Long id = create("qa-teste").getId();
        Assinatura a = assinaturas.findById(id).orElseThrow();
        a.setVigenciaFim(LocalDate.now().minusDays(1));
        assinaturas.save(a);
        assertThat(portugues.modulosPermitidos("qa-teste")).isEmpty();
        a.setVigenciaFim(null);
        a.setVigenciaInicio(LocalDate.now().plusDays(1));
        assinaturas.save(a);
        assertThat(portugues.modulosPermitidos("qa-teste")).isEmpty();
        a.setVigenciaInicio(LocalDate.now());
        a.setStatus(StatusAssinatura.SUSPENSA);
        assinaturas.save(a);
        assertThat(portugues.modulosPermitidos("qa-teste")).isEmpty();
        a.setStatus(StatusAssinatura.ATIVA);
        assinaturas.save(a);
        plan.setIsActive(false);
        plans.save(plan);
        assertThat(portugues.modulosPermitidos("qa-teste")).isEmpty();
    }

    @Test void accessUsesTokenTenantAndCannotBePurchasedByResidents() throws Exception {
        create("qa-com-plano");
        mvc.perform(get("/api/plans/access/portaria").header("Authorization", token("qa-com-plano", "MORADOR")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/plans/access/financeiro").header("Authorization", token("qa-com-plano", "MORADOR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/plans/access/portaria?condominioId=qa-com-plano")
                .header("Authorization", token("qa-sem-plano", "MORADOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/plans/access/portaria")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/assinaturas").header("Authorization", token("qa-sem-plano", "MORADOR"))
                .contentType("application/json").content("{}" )).andExpect(status().isForbidden());
    }

    @Test void swaggerAssetsAndSpecificationArePublicButBusinessEndpointsRequireJwt() throws Exception {
        mvc.perform(get("/api/plans/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/api/plans/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/plans/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/api/plans")).andExpect(status().isUnauthorized());
    }

    @Test void migrationPreservesHistoryPrefersCanonicalAndRunsOnlyOnce() {
        create("qa-existente");
        jdbc.execute("CREATE TABLE tb_subscriptions (id BIGINT, condominio_id VARCHAR(50), plan_id BIGINT, "
                + "status VARCHAR(20), start_date DATE, end_date DATE, created_by VARCHAR(100), updated_by VARCHAR(100), "
                + "created_at TIMESTAMP, updated_at TIMESTAMP)");
        for (String tenant : List.of("qa-importado", "qa-existente")) {
            jdbc.update("INSERT INTO tb_subscriptions VALUES (?, ?, ?, 'ACTIVE', CURRENT_DATE, NULL, "
                    + "'autor', 'autor', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    tenant.equals("qa-importado") ? 100 : 101, tenant, plan.getId());
        }
        migration.run(null);
        migration.run(null);
        assertThat(portugues.modulosPermitidos("qa-importado")).containsExactly("portaria");
        assertThat(assinaturas.count()).isEqualTo(3);
        assertThat(assinaturas.findByCondominioIdOrderByCreatedAtDesc("qa-existente"))
                .extracting(Assinatura::getStatus).containsExactlyInAnyOrder(StatusAssinatura.ATIVA, StatusAssinatura.CANCELADA);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_subscriptions_legacy", Long.class)).isEqualTo(2);
    }
}
