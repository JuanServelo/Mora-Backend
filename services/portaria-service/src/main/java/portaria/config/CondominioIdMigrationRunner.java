package portaria.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class CondominioIdMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        garantirColuna("blocos");
        garantirColuna("apartamentos");
        garantirColuna("areas_comuns");

        jdbcTemplate.update("""
            UPDATE blocos SET "condominioId" = 'default'
            WHERE "condominioId" IS NULL OR TRIM("condominioId") = ''
            """);
        jdbcTemplate.update("""
            UPDATE apartamentos SET "condominioId" = 'default'
            WHERE "condominioId" IS NULL OR TRIM("condominioId") = ''
            """);
        jdbcTemplate.update("""
            UPDATE areas_comuns SET "condominioId" = 'default'
            WHERE "condominioId" IS NULL OR TRIM("condominioId") = ''
            """);

        trocarUnicidadePorCondominio("blocos", "blocos_nome_key", "blocos_nome_condominioId_key");
        trocarUnicidadePorCondominio("areas_comuns", "areas_comuns_nome_key", "areas_comuns_nome_condominioId_key");

        log.info("Migração condominioId/uniqueness aplicada.");
    }

    private void garantirColuna(String tabela) {
        jdbcTemplate.execute("""
            ALTER TABLE %s ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(50)
            """.formatted(tabela));
    }

    private void trocarUnicidadePorCondominio(String tabela, String constraintAntiga, String constraintNova) {
        jdbcTemplate.execute("ALTER TABLE %s DROP CONSTRAINT IF EXISTS %s".formatted(tabela, constraintAntiga));
        jdbcTemplate.execute("ALTER TABLE %s DROP CONSTRAINT IF EXISTS \"%s\"".formatted(tabela, constraintAntiga));

        Integer existe = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM pg_constraint
                WHERE conname = ?
                """,
                Integer.class,
                constraintNova
        );

        if (existe == null || existe == 0) {
            jdbcTemplate.execute("""
                ALTER TABLE %s
                ADD CONSTRAINT %s UNIQUE (nome, "condominioId")
                """.formatted(tabela, constraintNova));
        }
    }
}
