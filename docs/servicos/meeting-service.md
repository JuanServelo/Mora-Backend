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

O serviço inicia sem `credentials.json`, com integração desabilitada ou sem tokens. Listagem, atas e votações continuam disponíveis conforme JWT e plano. Criar uma reunião exige um link real: configuração ausente devolve 503; falta de autorização ou token revogado devolve 409, sem persistir reunião parcial.

No Compose principal, coloque o cliente OAuth em `secrets/google-meet/credentials.json`. A pasta é montada somente para leitura em `/run/secrets/google-meet`, fora da imagem, e ignorada pelo Git. Se não houver arquivo, o backend ainda sobe. O overlay `docker-compose.meet.yml` continua disponível para montar um arquivo externo como Docker secret.

Cadastre no cliente web do Google Cloud a URI exata `http://localhost:8091/api/meetings/google/callback`. Em outro ambiente, ajuste `GOOGLE_MEET_REDIRECT_URI` e registre a mesma URL no Google. `GOOGLE_MEET_FRONTEND_URL` define o retorno fixo à tela, por padrão `http://localhost:5173/adm/reunioes`. O [fluxo OAuth oficial do Google](https://developers.google.com/identity/protocols/oauth2/web-server) exige consentimento e URI de retorno cadastrada.

Na primeira entrada da administração na seção de reuniões, o frontend consulta `GET /api/meetings/google/status`. Se houver configuração e faltar autorização, inicia `POST /api/meetings/google/authorize` com JWT e abre o consentimento Google na mesma aba. Não executa comandos nem abre navegador dentro do container. Moradores usam as reuniões existentes, sem autorizar a conta de gestão.

O retorno usa state aleatório de uso único, validade de dez minutos, cookie HttpOnly/SameSite=Lax vinculado ao navegador e PKCE S256. Somente o callback exato dispensa JWT; os endpoints de início/estado exigem administração e módulo contratado. O token nunca vai para o frontend. Cancelamento e erro não provocam redirecionamento contínuo, e o botão de reconexão permite tentar novamente.

O refresh token fica no volume `tokens/<hash-SHA256-do-condomínio>/StoredCredential`; o armazenamento antigo `tokens/StoredCredential` não é compartilhado automaticamente entre condomínios. Solicita-se `access_type=offline` e `prompt=consent`. Renovação e nova autorização são serializadas nesta instância e persistidas no mesmo armazenamento, para sobreviver a reinícios. Execute uma instância por volume de tokens. Token revogado exige novo consentimento.

O helper CLI antigo permanece como utilitário de diagnóstico independente. O fluxo da tela não depende dele. O Maven e `.dockerignore` excluem credenciais e tokens da imagem.

## Rotas e infraestrutura

`/api/meetings`, `/api/meetings/{id}/ata`, `/api/polls`. Consul anuncia as duas bases de negócio no Traefik. `/actuator/health`, `/api-docs`, `/swagger-ui.html` e assets do Swagger são públicos; isso não libera operações de negócio.

Atualize backend e frontend juntos, pois o estado do voto na resposta mudou. Faça build limpo para remover artefatos antigos (`mvn clean package`) e reconstrua a imagem. O Flyway aplica V3 para acrescentar `unidade_id` e seu índice único sem apagar votos antigos; Hibernate usa `ddl-auto=validate`. Execute uma versão por vez na atualização para impedir gravações pelo código anterior.

## Validação

`mvn clean test` cobre inicialização sem Google, JWT ausente/forjado/expirado, perfis, isolamento entre condomínios, falsificação da identidade, ausência de unidade, voto duplicado e concorrente, prazo, atas, presença, preservação de votos legados e falhas dos serviços consultados. A integração usa H2 e upstreams HTTP locais; não substitui a validação final no PostgreSQL/Consul/Traefik.

`npm run build` no frontend valida a integração das telas. Ao publicar, confirme o health check, login de síndico e morador, acesso recusado de porteiro/convidado e segundo voto de outro morador da mesma unidade. Google exige uma conta autorizada para validação real.
