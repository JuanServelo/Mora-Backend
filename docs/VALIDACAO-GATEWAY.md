# Service Discovery e acesso pelo API Gateway

Validação local em 28/10/2026, após integrar a `develop`, com oito serviços na stack Docker.

## Topologia

O frontend usa somente `VITE_GATEWAY_URL`, por padrão `http://localhost:8087`. APIs, fotos, imagens de avisos e início do login Google usam essa origem. As variáveis antigas por serviço e os proxies diretos do Vite não são mais usados.

As oito aplicações usam `expose`, sem `ports` publicados no host. As chamadas de negócio entre serviços também usam `http://traefik:8087`, com o prefixo público apropriado. Consul, PostgreSQL, RabbitMQ e pgAdmin continuam acessíveis para administração; registro, health checks do Consul, conexões ao banco e mensagens usam a rede interna.

| Serviço | Prefixos do gateway |
|---|---|
| auth-api | `/api/auth`, `/api/acessos`, `/api/interno`, usuários, convites, condomínios, perfis, estatísticas e avatares |
| gestao-geral | `/api/gestao`, `/docs` |
| financeiro | `/api/financeiro` |
| meeting-service | `/api/meetings`, `/api/polls` |
| plan-service | `/api/plans`, `/api/assinaturas` |
| portaria-service | `/api/portaria`, `/api/vagas` (compatibilidade) |
| comunicacao-service | `/api/comunicacao` |
| ocorrencias-service | `/api/ocorrencias` |

O controle de entrada/saída de pessoas no auth-api mudou de `/api/portaria` para `/api/acessos`, evitando colisão com o serviço Java. Frontend, inventário de autorização e OpenAPI foram atualizados juntos. A documentação de meeting fica em `/api/meetings/v3/api-docs` e `/api/meetings/swagger-ui.html`, sem exigir JWT ou módulo para ler a especificação; operações de negócio continuam protegidas.

Os registros usam IDs estáveis nesta implantação de uma instância por aplicação. Foram removidas 21 entradas obsoletas de containers anteriores, após verificar que os sete registros atuais estavam saudáveis. Em uma implantação com múltiplas instâncias, cada instância deve usar um ID próprio.

## Resultado observado

- Oito serviços registrados, cada um com exatamente uma instância saudável no Consul.
- Nenhum dos oito microsserviços publica portas no host.
- Sete especificações OpenAPI responderam HTTP 200 pelo gateway.
- Leituras autenticadas dos sete serviços e de `/api/acessos` responderam HTTP 200 pelo gateway.
- Requisições sem JWT às rotas verificadas continuaram recebendo 401.
- Preflight CORS passou nos sete serviços. O filtro da portaria foi corrigido para permitir `OPTIONS` sem dispensar JWT nas chamadas de negócio.
- Swagger de meeting e portal `/docs` responderam HTTP 200 pelo gateway.
- Após o merge: 412 testes do auth-api, cinco de plan-service e 298 do frontend passaram, além do build de produção do frontend. Antes do merge, passaram também 29 testes de meeting e dois testes de filtros da portaria.
- Validação complementar de ocorrências: OpenAPI e consultas autenticadas de reclamações, ordens de serviço e fornecedores retornaram 200; sem JWT, a consulta retornou 401.

O financeiro precisou ser reiniciado após tentar conectar durante a inicialização do PostgreSQL. No volume existente, foi necessário executar `docker/criar-banco-ocorrencias.sql` para criar o banco ausente; depois o Flyway aplicou `V1__estrutura_inicial.sql` com sucesso. O script de criação do banco é idempotente e não apaga dados.

O teste integrado é somente leitura, usa uma conta administrativa já existente com JWT de dois minutos, não cria usuários nem reuniões e não imprime tokens ou dados das respostas:

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml exec -T auth-api node scripts/validar-gateway.mjs
```

Para reconstruir e executar a stack:

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml up -d --build
```

## Callbacks do Google

Para iniciar novos fluxos de consentimento pelo gateway, cadastre no respectivo cliente OAuth do Google Cloud:

- Login: `http://localhost:8087/api/auth/google/callback`
- Google Meet: `http://localhost:8087/api/meetings/google/callback`

Os ambientes locais já foram ajustados para essas URLs. O cadastro no Google Cloud é externo ao repositório e exige acesso ao projeto Google. Tokens de Meet já salvos continuam no volume; não foram apagados. O teste integrado verifica o roteamento local, sem concluir consentimento em nome do usuário.
