# meeting-service

Java 21, Spring Boot 3.2.4, JPA e MapStruct. Porta 8091; banco `mora_meeting`. Gerencia reuniões, atas e votações.

## Autenticação, perfis e condomínio

Todas as operações de negócio exigem JWT assinado com o mesmo `JWT_SECRET` do auth-api. Tokens inválidos, expirados ou ausentes recebem 401. O filtro valida também o perfil e a existência do condomínio na sessão; não aceita um header de condomínio como autoridade.

| Operação | Quem pode |
|---|---|
| Criar/editar/cancelar/finalizar reuniões; publicar/editar atas; criar/editar/encerrar votações | `ADMIN_SINDICO` e `ADMIN_GERAL`, vinculados ao condomínio |
| Consultar reuniões e atas | Gestão do condomínio; `MORADOR` e `DONO_ALUGUEL` convidados ou organizadores |
| Consultar votações | Gestão, `MORADOR` e `DONO_ALUGUEL` do condomínio |
| Confirmar presença e avaliar reunião | O próprio convidado, identificado pelo JWT; avaliação exige reunião finalizada e presença confirmada |
| Votar | Perfil de leitura permitido e unidade confirmada no auth-api; um voto por unidade |

`PORTEIRO`, `CONVIDADO` e perfis desconhecidos recebem 403. Administração sem condomínio vinculado não ganha acesso global às reuniões. O frontend aplica perfis e módulos às rotas de reunião/votação; as rotas administrativas continuam usando `AdminRoute`.

O condomínio das novas reuniões, atas e votações vem da sessão. O organizador vem do JWT. IDs de outro condomínio devolvem 404 em leitura e escrita. Listagens de moradores ignoram `usuarioId` ou `condominioId` enviados para ampliar o acesso; consultam apenas suas reuniões. Convidados são validados no auth-api como usuários ativos do mesmo condomínio.

O filtro de plano consulta `PLAN_SERVICE_URL` para os módulos `reunioes` e `votacoes`, inclusive em chamadas diretas à porta 8091. Falha na consulta devolve 503.

## Votos por unidade

`POST /api/polls/{id}/votes` aceita apenas o dado operacional:

```json
{"pollOptionId": 123}
```

Usuário vem da claim `id`; unidade vem de `GET /api/auth/me`, consultado com o Bearer do próprio usuário. O serviço verifica ID, perfil, status ativo e condomínio da identidade atual. Campos extras antigos como `usuarioId` e `unidadeId` não são usados. Indisponibilidade do auth-api resulta em 503; ausência de unidade ou divergência de vínculo resulta em 403.

`tb_poll_vote` guarda `usuario_id` para auditoria e `unidade_id` como unidade representada no momento do voto. A constraint `uq_poll_vote_unidade (poll_id, unidade_id)` garante unicidade no banco. O bloqueio pessimista da votação serializa votos concorrentes e alterações de estado. Opções de outra votação são recusadas e o prazo é conferido na gravação, sem depender do job de encerramento.

As respostas das votações trazem `minhaUnidadeVotou` e `podeVotar`; o frontend usa esses campos. A lista pública de usuários que votaram foi removida. O primeiro voto válido exerce o voto da unidade; não há um cadastro separado de procuração nesta implementação.

### Dados anteriores à correção

- Votos antigos sem unidade permanecem no banco e nos totais históricos. `LegacyVoteMigration` encerra votações ABERTA que tenham esses votos. Não atribui uma unidade ao voto a partir de uma identidade que antes podia ser falsificada. Para deliberar novamente sob a regra nova, crie outra votação.
- Reuniões/votações antigas sem `condominio_id` permanecem no banco e ficam inacessíveis às APIs até que seu condomínio seja verificado. Não existe fallback que libere registros sem dono. Antes de atualizar, confira `SELECT id, id_organizador FROM tb_meetings WHERE condominio_id IS NULL` e `SELECT id FROM poll WHERE condominio_id IS NULL`.
- Após verificar a origem de cada registro, atualize apenas os IDs confirmados, em transação. Atas herdam o condomínio da reunião: `UPDATE tb_atas a SET condominio_id = m.condominio_id FROM tb_meetings m WHERE a.meeting_id = m.id AND m.condominio_id IS NOT NULL AND a.condominio_id IS NULL`. Não atribua o mesmo condomínio a toda a base sem conferir.

## Google Meet e credenciais

A aplicação inicia sem credenciais Google. Por padrão `GOOGLE_MEET_ENABLED=false`: reuniões são salvas com link nulo, sem link fictício. Para habilitar, configure arquivo OAuth e tokens externos:

| Variável | Finalidade |
|---|---|
| `GOOGLE_MEET_ENABLED` | Habilita criação de links |
| `GOOGLE_MEET_CREDENTIALS_PATH` | Caminho do arquivo OAuth dentro do ambiente de execução |
| `GOOGLE_MEET_TOKENS_PATH` | Diretório dos tokens; padrão `/app/tokens` |
| `AUTH_API_URL` | Padrão local `http://localhost:3001` |
| `PLAN_SERVICE_URL` | Padrão local `http://localhost:8093` |
| `JWT_SECRET` | Mesmo segredo do auth-api, com pelo menos 32 bytes |

O cliente Google é inicializado no primeiro uso. Se habilitado e sem arquivo/token válido, a criação de link devolve 503; a aplicação continua iniciando. Conexão e leitura têm timeout. O helper local `GoogleMeetAuthHelper` recebe o caminho externo como primeiro argumento ou em `GOOGLE_MEET_CREDENTIALS_PATH`; não lê o classpath.

O Maven exclui `credentials.json` e tokens dos recursos. `.dockerignore` exclui esses arquivos do contexto de build. O arquivo local antigo não é apagado.

No Docker, a integração opcional usa `docker/docker-compose.meet.yml`. Defina `GOOGLE_MEET_CREDENTIALS_FILE` no `docker/.env` com o caminho do arquivo no host; gere os tokens antes com o helper. O secret é montado somente em execução, em `/run/secrets/google-meet-credentials.json`:

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml -f docker/docker-compose.meet.yml up -d --build meeting-service
```

Sem a integração Google, basta o Compose principal. Credenciais e tokens não devem ser versionados.

## Rotas e infraestrutura

`/api/meetings`, `/api/meetings/{id}/ata`, `/api/polls`. Consul anuncia as duas bases de negócio no Traefik. `/actuator/health`, `/api-docs`, `/swagger-ui.html` e assets do Swagger são públicos; isso não libera operações de negócio.

Atualize backend e frontend juntos, pois o estado do voto na resposta mudou. Faça build limpo para remover artefatos antigos (`mvn clean package`) e reconstrua a imagem. A aplicação mantém `ddl-auto=update`, que acrescenta `unidade_id` e a constraint sem apagar os votos antigos. Execute uma versão por vez na atualização para impedir gravações pelo código anterior.

## Validação

`mvn clean test` cobre inicialização sem Google, JWT ausente/forjado/expirado, perfis, isolamento entre condomínios, falsificação da identidade, ausência de unidade, voto duplicado e concorrente, prazo, atas, presença, preservação de votos legados e falhas dos serviços consultados. A integração usa H2 e upstreams HTTP locais; não substitui a validação final no PostgreSQL/Consul/Traefik.

`npm run build` no frontend valida a integração das telas. Ao publicar, confirme o health check, login de síndico e morador, acesso recusado de porteiro/convidado e segundo voto de outro morador da mesma unidade. Google exige uma conta autorizada para validação real.
