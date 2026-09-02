# Documentação por serviço

Um arquivo por microsserviço, com responsabilidade, fluxos de usuário, tabelas e integrações.

---

## Em operação

| Serviço | Domínio | Porta | Banco |
|---|---|---|---|
| [auth-api](auth-api.md) | Identidade, perfis, condomínios clientes, convites | 3001 | `auth_db` |
| [portaria-service](portaria-service.md) | Estrutura física, acessos, entregas, chaves, vagas | 8090 | `mora` |
| [meeting-service](meeting-service.md) | Assembleias, atas e votações | 8091 | `mora_meeting` |
| [plan-service](plan-service.md) | Planos comerciais | 8093 | `mora_plan` |
| [gestao-geral](gestao-geral.md) | Agregação de indicadores | 3002 | — |
| [vagas-service](vagas-service.md) | Aluguel de vagas — **a fundir no portaria** | 8092 | `vagas_db` |

## Planejados

| Serviço | Domínio | Porta | Banco |
|---|---|---|---|
| [financeiro-service](financeiro-service.md) | Contratos, faturas, multas, prestação de contas | 8094 | `mora_financeiro` |
| [comunicacao-service](comunicacao-service.md) | Avisos, base de conhecimento, chat, notificações | 3003 | `mora_comunicacao` |
| [ocorrencias-service](ocorrencias-service.md) | Reclamações e ordens de serviço | 8095 | `mora_ocorrencias` |

---

## Requisitos por serviço

| RF | Requisito | Serviço |
|---|---|---|
| 1 | Realizar Autenticação de Usuário | auth-api |
| 2 | Gerenciar Usuários e Vínculos de Unidade | auth-api |
| 3 | Gerenciar Clientes (Condomínios) | auth-api |
| 4 | Gerenciar Planos e Assinaturas | plan-service |
| 5 | Gerenciar Estrutura do Condomínio | portaria-service |
| 6 | Controlar Acessos e Visitantes | portaria-service |
| 7 | Gerenciar Entregas e Encomendas | portaria-service |
| 8 | Controlar Retirada e Devolução de Chaves | portaria-service |
| 9 | Gerenciar Funcionários e Turnos | portaria-service |
| 10 | Gerenciar Reservas de Áreas Comuns | portaria-service |
| 11 | Gerenciar Assembleias, Atas e Votações | meeting-service |
| 12 | Gerenciar Comunicados e Base de Conhecimento | comunicacao-service |
| 13 | Gerenciar Mensagens e Notificações | comunicacao-service |
| 14 | Gerenciar Ocorrências e Ordens de Serviço | ocorrencias-service |
| 15 | Gerenciar Contratos de Locação | financeiro-service |
| 16 | Gerenciar Cobranças da Unidade | financeiro-service |
| 17 | Registrar Prestação de Contas | financeiro-service |
| 18 | Gerar Relatórios e Dashboards Analíticos | gestao-geral |

---

## Domínios fora do lugar

Três recursos rodam hoje num serviço e pertencem a outro, por terem sido implementados antes da
divisão de domínios:

| Recurso | Está em | Vai para |
|---|---|---|
| `avisos` | portaria-service | comunicacao-service |
| `artigos_conhecimento` | portaria-service | comunicacao-service |
| `reclamacoes` | auth-api | ocorrencias-service |

---

## Sobre as tabelas dos serviços planejados

Nos três serviços ainda não implementados, as tabelas listadas são **proposta**, esboçada a
partir do escopo dos requisitos — não decisão fechada. Cada documento marca isso explicitamente
e traz uma seção "Em aberto" com as questões que faltam resolver.

As exceções são `avisos`, `artigos_conhecimento` e `reclamacoes`, que já existem e apenas mudam
de casa.

---

## Referências

- [ESPECIFICACAO-PROJETO.md](../ESPECIFICACAO-PROJETO.md) — requisitos, perfis e arquitetura
- [FLUXOS-DE-USUARIO-E-TELAS.md](../FLUXOS-DE-USUARIO-E-TELAS.md) — telas por perfil
- [ARQUITETURA-E-FLUXOS.md](../ARQUITETURA-E-FLUXOS.md) — comunicação entre serviços
