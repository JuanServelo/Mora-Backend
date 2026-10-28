# plan-service

Java 21, Spring Boot 3.5.6, PostgreSQL (`mora_plan`), porta 8093. Responsável pelo catálogo comercial e pelas assinaturas (RF-4).

## Assinaturas

`tb_assinaturas` é a única fonte operacional. `/api/assinaturas` retorna os nomes em português; `/api/plans/subscriptions` mantém o contrato em inglês, mas usa os mesmos registros, IDs e transações. Catálogo em `tb_plans` e módulos em `tb_plan_modules`.

Na primeira inicialização da nova versão, `SubscriptionMigration` importa `tb_subscriptions` em uma transação e renomeia a origem para `tb_subscriptions_legacy`. Preserva datas, autores, status e o ID original em `legacy_subscription_id`. Os endpoints passam a devolver o ID canônico de `tb_assinaturas`; IDs antigos não devem ser reutilizados para alterações sem consultar a assinatura novamente.

Se já existe assinatura ATIVA no modelo canônico, ela tem prioridade; a assinatura ACTIVE importada fica CANCELADA no histórico. Entre várias ACTIVE legadas do mesmo condomínio, ganha a mais recente. O arquivo legado não volta a ser consultado pelas APIs. A inicialização seguinte não importa novamente. Falhas interrompem a inicialização e revertem a importação; nenhum registro de origem é apagado.

Em PostgreSQL, a migração também remove a antiga constraint `(condominio_id, status)` e garante o índice parcial de uma assinatura ATIVA por condomínio. Isso permite várias canceladas no histórico e sucessivas trocas de plano. Execute uma versão por vez durante a atualização; processos antigos não devem continuar escrevendo na tabela legada.

## Autorização

Alterações comerciais e listagens globais exigem `ADMIN_GERAL`. Os demais perfis podem ler a assinatura e o histórico do próprio condomínio. O condomínio vem do JWT verificado.

`GET /api/plans/access/{module}` retorna 204 para módulo contratado, 403 quando não contratado e 401 para token inválido ou ausente. Exige assinatura ATIVA dentro da vigência e plano ativo. O Admin Geral mantém acesso de plataforma; as permissões específicas de cada serviço continuam sendo verificadas localmente.

Filtros em portaria, comunicação e reuniões/votações, e middlewares no auth-api (portaria e reclamações) e financeiro, consultam esse endpoint inclusive em chamadas diretas às portas dos serviços. Usam `PLAN_SERVICE_URL` (local: `http://localhost:8093`; Compose: `http://plan-service:8093`). Uma falha ou timeout devolve 503 e não libera a operação. Não há cache de permissão que prolongue acesso após cancelamento.

Blocos, apartamentos, moradores e usuários são infraestrutura e não exigem módulo. Notificações internas e callbacks do gateway financeiro mantêm suas próprias credenciais. Os limites numéricos de usuários e condomínios continuam sendo parâmetros comerciais; esta correção aplica a contratação de módulos, não implementa cotas numéricas.

## Consul e documentação

As configurações foram consolidadas em `application.yml`; não há um `application.properties` sobrescrevendo o YAML. Consul habilitado por padrão, controlado por `SPRING_CLOUD_CONSUL_ENABLED`; o perfil `local` o desabilita. O Compose habilita explicitamente e configura o host `consul`. Health check em `/actuator/health`; rotas Traefik `/api/plans` e `/api/assinaturas`.

OpenAPI público em `/api/plans/v3/api-docs`; Swagger em `/api/plans/swagger-ui.html`. Assets e configuração do Swagger também são públicos; as operações de negócio exigem JWT. `JWT_SECRET` deve ser o mesmo usado pelo auth-api.

Após atualizar, faça build limpo (`mvn clean package`) ou reconstrua as imagens para remover classes e propriedades antigas. Para a stack Docker:

```powershell
docker compose -f docker/docker-compose.yml --env-file docker/.env up -d --build plan-service portaria-service comunicacao-service meeting-service auth-api financeiro
```

Valide o registro saudável de `plan-service` em `http://localhost:8500/v1/health/service/plan-service?passing=true`, a especificação pelo gateway em `http://localhost:8087/api/plans/v3/api-docs` e o acesso dos condomínios `qa-*` após importar as assinaturas.

## Testes

`mvn clean test` em plan-service: compatibilidade das duas APIs, histórico e trocas sucessivas, migração repetível, vigência/status/plano inativo, tenant do token, restrição comercial e Swagger público.

`mvn -Dtest=PlanModuleFilterTest test` em portaria, comunicação e meeting: recusa direta em todos os métodos, módulo autorizado, ausência de token e indisponibilidade do serviço de planos.

`npm test` em financeiro e auth-api: middleware de plano e regressões de autenticação/perfis. `npm run build` no frontend.

## Receita da plataforma

`GET /api/assinaturas/receita` exige `ADMIN_GERAL` e retorna o painel calculado no PostgreSQL pela função `receita_plataforma(meses, plano)`, criada na migração `V3__receita_plataforma.sql`. A função agrega receita por plano e por mês, vigências e histórico usando `GROUP BY`, `generate_series`, `daterange`, `DISTINCT ON` e JSON. O `gestao-geral` consome esse endpoint em `/api/gestao/receita`.
