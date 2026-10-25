# Validação de meeting-service e plan-service — 25/10/2026

Os stashes `Antes de atualizar main - 2026-10-25 - feat/plan` foram reaplicados no backend e frontend com `stash apply`. As cópias continuam disponíveis. A correção local de compatibilidade da migração de planos foi preservada.

| Pendência | Resultado verificado |
|---|---|
| Meeting sem autenticação | JWT obrigatório; ausência, assinatura falsa e expiração recebem 401. Gestão exige administração; porteiro e convidado não acessam o módulo. |
| Voto falsificável | DTO aceita apenas a opção. Usuário vem do JWT e a unidade atual é consultada no auth-api. Campos forjados não trocam a identidade. |
| Condomínios misturados | Reuniões, atas e votações usam o condomínio do JWT; IDs de outro condomínio retornam 404. |
| RF-11, voto por unidade | Coluna `unidade_id`, trava transacional e unicidade `(poll_id, unidade_id)`. Teste concorrente aceita exatamente um voto de dois moradores da mesma unidade. Votos antigos são preservados e suas votações encerradas. |
| Credenciais dentro da imagem | Credencial montada fora da imagem. JAR efetivamente em execução foi inspecionado e não contém `credentials.json` nem `StoredCredential`. |
| Duas tabelas de assinatura | Ambas as APIs operam em `tb_assinaturas`. No PostgreSQL em execução, `tb_subscriptions` não existe e `tb_subscriptions_legacy` está preservada. |
| RF-4 só no frontend | Filtros consultam o plan-service nas APIs de meeting, portaria, comunicação, financeiro e nas rotas com módulos do auth-api. Plano ausente/inativo/fora da vigência e indisponibilidade não concedem módulos. |
| Planos fora do Consul | Serviço encontrado no catálogo saudável durante a verificação. |
| Sem testes / Swagger 401 | Cinco testes de integração de planos passaram. `/api/plans/swagger-ui.html` e `/api/plans/v3/api-docs` responderam 200 sem JWT na execução Docker. |

## Correções de integração e Google Meet

As migrações vindas da main não continham todas as colunas das entidades da feat/plan. Foram acrescentadas V3 de voto por unidade e de auditoria/importação da assinatura. Os scripts completos de ambos os serviços foram executados em schemas temporários no PostgreSQL 16, com rollback ao fim. A versão Docker aplicou V3 com sucesso nos bancos existentes.

O backend inicia sem credenciais ou tokens. A configuração é lida no uso, permitindo instalar o arquivo posteriormente sem reiniciar. Criar reunião sem configuração devolve 503; com configuração e sem autorização devolve 409. Não salva reunião sem link nem dados parciais.

A administração inicia o consentimento na primeira visita à seção de reuniões. O fluxo usa state aleatório de uso único com prazo de dez minutos, cookie HttpOnly/SameSite=Lax e PKCE S256. O callback é a única rota OAuth sem JWT; iniciar/verificar a conexão exige administração e plano. Tokens ficam no volume persistente, separados por hash do condomínio, e são renovados automaticamente. Cancelamento, repetição, state expirado, navegador incorreto, grant incompleto, reinício e revogação têm testes. Um grant incompleto não sobrescreve uma autorização válida.

A aplicação não executa o helper CLI no servidor: o navegador do usuário conduz o mesmo consentimento e o backend recebe o retorno. Tokens não são enviados ao frontend. Uma instância deve usar cada volume de tokens.

## Evidências

- Meeting: **29 testes**, zero falhas, incluindo 15 testes de integração de segurança/listagem/presença e 7 do fluxo OAuth com transporte Google simulado.
- Planos: **5 testes de integração**, zero falhas.
- Frontend: **256 testes**, zero falhas; build de produção concluído.
- RF-4 nos demais serviços: um teste de filtro na portaria, um na comunicação, dois no auth-api e três no financeiro, todos aprovados.
- PostgreSQL: scripts V1–V3 de ambos os serviços executados em schemas isolados, sem alteração persistente nessa verificação.
- Docker: imagens de meeting e planos reconstruídas; saúde e respostas HTTP verificadas. Sem JWT: 401; morador sem módulo: 403; morador iniciando OAuth: 403; estado Google: 200; início autenticado: 200 com PKCE e cookie.

## Busca, listagem e presença

A pesquisa por nome funciona enquanto se digita, ignora maiúsculas e acentos e pode ser combinada com data opcional. A administração consulta todas as reuniões do condomínio e conserva o histórico. Moradores recebem somente reuniões agendadas, futuras ou ainda em andamento; canceladas, finalizadas e reuniões cujo horário de término passou ficam ocultas também na API.

Entrar no Meet registra a presença do próprio convidado pelo endpoint existente antes de navegar. O frontend atualiza a presença local; o painel da administração consulta os participantes a cada 30 segundos e ao recuperar o foco. Se a gravação falhar, mantém um aviso e não abre o Meet. O organizador que não integra a lista de convidados entra sem alterar a presença de moradores. Essa confirmação registra a ação de entrada pelo Mora, não telemetria de participação fornecida pelo Google.

O aviso de conexão Google bem-sucedida fica oculto. Falhas de conexão mantêm aviso e opção de reconectar.

## Configuração para autorização real

Em cada ambiente, cadastre **`http://localhost:8091/api/meetings/google/callback`** (ou a URL configurada naquele ambiente) no cliente OAuth do Google Cloud e conclua o consentimento no navegador. Editar o JSON local não substitui esse cadastro. Os testes automáticos usam transporte Google simulado e não concedem consentimento em nome do usuário.

Configuração, endpoints e armazenamento estão documentados em [meeting-service.md](servicos/meeting-service.md). Referência: [OAuth para aplicações web do Google](https://developers.google.com/identity/protocols/oauth2/web-server).
