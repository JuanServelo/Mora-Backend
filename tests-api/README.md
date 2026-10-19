# Teste de API — Postman + Newman

Fluxos ponta a ponta contra a stack rodando: 50 requisições e 98 asserções. A execução apaga o que criou.

## Rodar

Pré-requisitos: a stack no ar (`docker compose -f docker/docker-compose.yml up -d`) e o seed aplicado (`npm run seed` na raiz).

```bash
cd tests-api
npm install
npm test
```

O relatório sai em `tests-api/relatorio/relatorio-newman.html`. Ele esconde o corpo e a resposta dos logins e os cabeçalhos com token, então pode ser compartilhado.

## Abrir no Postman

Importe `mora.postman_collection.json` e `local.postman_environment.json`. No ambiente, preencha:

- `senha`: a senha das contas do seed, definida em `scripts/seed-dev.mjs`;
- `servicoToken`: o `SERVICO_TOKEN` do `docker/.env`.

Os dois ficam vazios nos arquivos de propósito. O `npm test` os lê na hora da execução.

## O que cobre

| Pasta | Verifica |
|---|---|
| 00 Infraestrutura | Serviços no Consul; auth-api sem `/api/portaria`; Swagger do comunicacao e do portaria |
| 01 Autenticação | Login de cada perfil; e-mail inexistente recusado |
| 02 auth-api | Estatísticas de ocorrências agregadas no banco; recorte por condomínio; 401/403; gateway leva `/api/portaria` ao portaria |
| 03 comunicacao | Aviso por público-alvo; confirmação de leitura; panorama e relatório "x de y"; upload de imagem (PNG aceito, texto disfarçado recusado); rota interna com token de serviço |
| 04 financeiro | Multa aplicada → notificação no sino do morador, uma vez só (reenvio não duplica); multas por mês |
| 05 gestao-geral | Painel operacional e estratégico com as fontes agregadas; 400/403 |
| 06 Limpeza | Exclui os avisos criados |

Fica de fora a rota interna com `SERVICO_TOKEN` vazio (deve dar 503): testar isso exige subir o comunicacao sem a variável. Esse caso é coberto pelo teste JUnit `NotificacaoInternaControllerTest`.

## Mudar ou acrescentar um teste

Edite `montar-collection.mjs`, não o `.json`. O `npm test` remonta a collection antes de rodar. Tudo que o teste cria leva `[newman]` no texto, para a limpeza achar.
