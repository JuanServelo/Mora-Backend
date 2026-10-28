package com.mora.plan.config;

import com.mora.plan.entity.Assinatura;
import com.mora.plan.enums.StatusAssinatura;
import com.mora.plan.repository.AssinaturaRepository;
import com.mora.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Imports the old table once, retaining its rows as a read-only archive. */
@Component
@RequiredArgsConstructor
public class SubscriptionMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final AssinaturaRepository assinaturas;
    private final PlanRepository plans;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // The production database is PostgreSQL. Tests also exercise the import on H2.
        boolean postgres = Boolean.TRUE.equals(jdbc.execute((java.sql.Connection c) ->
                c.getMetaData().getDatabaseProductName().equals("PostgreSQL")));
        if (postgres) {
            jdbc.execute("SELECT pg_advisory_xact_lock(8093004)");
            jdbc.execute("ALTER TABLE tb_assinaturas DROP CONSTRAINT IF EXISTS uq_assinatura_condominio_ativa");
        }
        Integer tables = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE LOWER(table_name) = 'tb_subscriptions' AND LOWER(table_schema) = LOWER(current_schema())", Integer.class);
        if (tables != null && tables > 0) {
            jdbc.query("SELECT * FROM tb_subscriptions ORDER BY created_at DESC, id DESC", rs -> {
                String tenant = rs.getString("condominio_id");
                StatusAssinatura status = switch (rs.getString("status")) {
                    case "ACTIVE" -> StatusAssinatura.ATIVA;
                    case "SUSPENDED" -> StatusAssinatura.SUSPENSA;
                    case "CANCELLED" -> StatusAssinatura.CANCELADA;
                    default -> throw new IllegalStateException("Status de assinatura legado desconhecido");
                };
                // An existing canonical contract wins. Keep the imported row as history.
                if (status == StatusAssinatura.ATIVA && assinaturas
                        .findByCondominioIdAndStatus(tenant, StatusAssinatura.ATIVA).isPresent()) {
                    status = StatusAssinatura.CANCELADA;
                }
                Assinatura a = Assinatura.builder().condominioId(tenant)
                        .plan(plans.findById(rs.getLong("plan_id")).orElseThrow())
                        .status(status).vigenciaInicio(rs.getDate("start_date").toLocalDate())
                        .vigenciaFim(rs.getDate("end_date") == null ? null : rs.getDate("end_date").toLocalDate())
                        .createdBy(rs.getString("created_by")).updatedBy(rs.getString("updated_by"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                        .legacySubscriptionId(rs.getLong("id"))
                        .observacao("Importada de tb_subscriptions; ID original preservado em legacy_subscription_id")
                        .build();
                assinaturas.saveAndFlush(a);
            });
            jdbc.execute("ALTER TABLE tb_subscriptions RENAME TO tb_subscriptions_legacy");
        }
        if (postgres) {
            jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_assinatura_condominio_ativa "
                    + "ON tb_assinaturas (condominio_id) WHERE status = 'ATIVA'");
        }
    }
}
