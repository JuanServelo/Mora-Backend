# Portaria Service

Microsserviço responsável pelo controle da portaria do condomínio. Gerencia o acesso de visitantes e veículos, cadastro de moradores e funcionários, recebimento de encomendas, controle de chaves e registro de turnos.

---

## Sumário

- [Tecnologias](#tecnologias)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Domínios e Funcionalidades](#domínios-e-funcionalidades)
- [API — Endpoints](#api--endpoints)
- [Banco de Dados](#banco-de-dados)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Como Executar](#como-executar)
- [Integração com a Infraestrutura](#integração-com-a-infraestrutura)

---

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.5.6 | Framework base |
| Spring Data JPA | — | Persistência e ORM |
| Spring Validation | — | Validação de entrada |
| PostgreSQL | 16 | Banco de dados relacional |
| Hibernate | 6+ | Dialeto PostgreSQL, DDL automático |
| Spring Cloud Consul | 2025.0.0 | Service discovery e registro |
| Lombok | — | Redução de boilerplate |
| SpringDoc OpenAPI | 2.0.2 | Documentação automática (Swagger UI) |
| H2 (test) | — | Banco em memória para testes |

---

## Estrutura do Projeto

```
portaria-service/
├── src/
│   ├── main/
│   │   ├── java/portaria/
│   │   │   ├── PortariaServiceApplication.java   # Entry point
│   │   │   │
│   │   │   ├── model/                            # Entidades JPA
│   │   │   │   ├── enums/
│   │   │   │   │   ├── StatusAcesso.java          # DENTRO | SAIU
│   │   │   │   │   └── TipoResponsavel.java       # MORADOR | FUNCIONARIO
│   │   │   │   ├── Morador.java
│   │   │   │   ├── Funcionario.java
│   │   │   │   ├── Visitante.java
│   │   │   │   ├── Carro.java
│   │   │   │   ├── Chave.java
│   │   │   │   ├── Entrega.java
│   │   │   │   └── Turno.java
│   │   │   │
│   │   │   ├── repository/                       # Interfaces JpaRepository
│   │   │   │   ├── MoradorRepository.java
│   │   │   │   ├── FuncionarioRepository.java
│   │   │   │   ├── VisitanteRepository.java
│   │   │   │   ├── CarroRepository.java
│   │   │   │   ├── ChaveRepository.java
│   │   │   │   ├── EntregaRepository.java
│   │   │   │   └── TurnoRepository.java
│   │   │   │
│   │   │   ├── service/                          # Regras de negócio
│   │   │   │   ├── MoradorService.java
│   │   │   │   ├── FuncionarioService.java
│   │   │   │   ├── VisitanteService.java
│   │   │   │   ├── CarroService.java
│   │   │   │   ├── ChaveService.java
│   │   │   │   ├── EntregaService.java
│   │   │   │   └── TurnoService.java
│   │   │   │
│   │   │   ├── controller/                       # REST Controllers
│   │   │   │   ├── MoradorController.java
│   │   │   │   ├── FuncionarioController.java
│   │   │   │   ├── VisitanteController.java
│   │   │   │   ├── CarroController.java
│   │   │   │   ├── ChaveController.java
│   │   │   │   ├── EntregaController.java
│   │   │   │   └── TurnoController.java
│   │   │   │
│   │   │   ├── dto/                              # Data Transfer Objects
│   │   │   │   └── chave/
│   │   │   │       └── RetirarChaveRequest.java  # Request para retirada de chave
│   │   │   │
│   │   │   └── exception/                        # Tratamento de erros
│   │   │       ├── RecursoNaoEncontradoException.java
│   │   │       ├── OperacaoInvalidaException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       └── application.yml
│   │
│   └── test/
│       ├── java/portaria/
│       │   └── PortariaServiceApplicationTests.java
│       └── resources/
│           └── application-test.yml              # Config para testes (H2 em memória)
│
├── Dockerfile
└── pom.xml
```

---

## Domínios e Funcionalidades

### Cadastro de Pessoas (`/moradores`, `/funcionarios`)
Gerencia o cadastro fixo de moradores e funcionários do condomínio.
- Cadastro com CPF único por tipo
- Ativação/desativação (soft delete)
- Atualização de dados

### Controle de Acesso (`/visitantes`, `/carros`)
Registra a entrada e saída de pessoas externas e veículos.
- Registro de entrada com timestamp automático e status `DENTRO`
- Registro de saída com timestamp e status `SAIU`
- Listagem de quem está atualmente no condomínio

### Controle de Chaves (`/chaves`)
Gerencia o estado de cada chave do condomínio.
- Retirada vinculada a um **Morador ou Funcionário cadastrado** (validação real, não texto livre)
- Devolução com registro de horário
- Histórico de quem está com a chave e desde quando

### Recebimento de Encomendas (`/entregas`)
Registra o recebimento e retirada de encomendas e entregas.
- Registro de chegada com timestamp automático
- Registro de retirada com identificação de quem retirou
- Listagem de encomendas pendentes

### Turnos de Trabalho (`/turnos`)
Controla os horários de entrada e saída dos funcionários por turno.
- Suporte a múltiplas entradas e saídas no mesmo turno (intervalos)
- Início, pausa e retomada de turno

---

## API — Endpoints

> Prefixo via gateway: `/api/portaria`
> Porta direta: `8090`

### Moradores — `/moradores`

| Método | Endpoint | Descrição | Body |
|---|---|---|---|
| `POST` | `/moradores/cadastrar` | Cadastra morador | `Morador` |
| `GET` | `/moradores` | Lista moradores ativos | — |
| `GET` | `/moradores/todos` | Lista todos (incluindo inativos) | — |
| `GET` | `/moradores/{id}` | Busca por ID | — |
| `PUT` | `/moradores/{id}` | Atualiza dados | `Morador` |
| `DELETE` | `/moradores/{id}` | Desativa morador | — |

**Campos obrigatórios:** `nome`, `cpf`, `apartamento`

---

### Funcionários — `/funcionarios`

| Método | Endpoint | Descrição | Body |
|---|---|---|---|
| `POST` | `/funcionarios/cadastrar` | Cadastra funcionário | `Funcionario` |
| `GET` | `/funcionarios` | Lista funcionários ativos | — |
| `GET` | `/funcionarios/todos` | Lista todos | — |
| `GET` | `/funcionarios/{id}` | Busca por ID | — |
| `PUT` | `/funcionarios/{id}` | Atualiza dados | `Funcionario` |
| `DELETE` | `/funcionarios/{id}` | Desativa funcionário | — |

**Campos obrigatórios:** `nome`, `cpf`, `cargo`

---

### Visitantes — `/visitantes`

| Método | Endpoint | Descrição | Body |
|---|---|---|---|
| `POST` | `/visitantes/entrada` | Registra entrada | `Visitante` |
| `POST` | `/visitantes/{id}/saida` | Registra saída | — |
| `GET` | `/visitantes` | Lista todos | — |
| `GET` | `/visitantes/dentro` | Lista quem está no condomínio | — |
| `GET` | `/visitantes/{id}` | Busca por ID | — |

**Campos obrigatórios:** `nome`  
**Status automáticos:** entrada → `DENTRO` | saída → `SAIU`

---

### Veículos — `/carros`

| Método | Endpoint | Descrição | Body |
|---|---|---|---|
| `POST` | `/carros/entrada` | Registra entrada do veículo | `Carro` |
| `POST` | `/carros/{id}/saida` | Registra saída | — |
| `GET` | `/carros` | Lista todos | — |
| `GET` | `/carros/dentro` | Lista veículos no condomínio | — |
| `GET` | `/carros/{id}` | Busca por ID | — |

**Campos obrigatórios:** `placa`

---

### Chaves — `/chaves`

| Método | Endpoint | Descrição | Body |
|---|---|---|---|
| `POST` | `/chaves/cadastrar` | Cadastra chave | `Chave` |
| `DELETE` | `/chaves/{id}` | Remove chave (somente se disponível) | — |
| `POST` | `/chaves/{id}/retirar` | Retira chave | `RetirarChaveRequest` |
| `POST` | `/chaves/{id}/devolver` | Devolve chave | — |
| `GET` | `/chaves` | Lista todas | — |
| `GET` | `/chaves/disponiveis` | Lista disponíveis | — |
| `GET` | `/chaves/{id}` | Busca por ID | — |

**Body de retirada (`RetirarChaveRequest`):**
```json
{
  "responsavelId": "uuid-do-morador-ou-funcionario",
  "tipoResponsavel": "MORADOR"
}
```
> O serviço valida se o ID existe antes de registrar a retirada.

---

### Entregas — `/entregas`

| Método | Endpoint | Descrição | Body |
|---|---|---|---|
| `POST` | `/entregas/registrar` | Registra chegada de entrega | `Entrega` |
| `POST` | `/entregas/{id}/retirar` | Marca como retirada | `"nome do recebedor"` |
| `GET` | `/entregas` | Lista todas | — |
| `GET` | `/entregas/pendentes` | Lista não retiradas | — |
| `GET` | `/entregas/{id}` | Busca por ID | — |

**Campos obrigatórios:** `nomeEntregador`, `destinatario`

---

### Turnos — `/turnos`

| Método | Endpoint | Descrição | Body |
|---|---|---|---|
| `POST` | `/turnos/iniciar` | Inicia turno | `Turno` |
| `POST` | `/turnos/{id}/retomar` | Retoma turno após pausa | — |
| `POST` | `/turnos/{id}/finalizar` | Finaliza ou pausa turno | — |
| `GET` | `/turnos` | Lista todos | — |
| `GET` | `/turnos/{id}` | Busca por ID | — |

**Campos obrigatórios:** `funcionario`, `cargo`

---

## Banco de Dados

**SGBD:** PostgreSQL 16  
**Schema:** `public`

### Tabelas geradas automaticamente (DDL: `update`)

| Tabela | Entidade | Descrição |
|---|---|---|
| `moradores` | `Morador` | Cadastro de moradores |
| `funcionarios` | `Funcionario` | Cadastro de funcionários |
| `visitantes` | `Visitante` | Registro de visitas com entrada/saída |
| `carros` | `Carro` | Registro de veículos com entrada/saída |
| `chaves` | `Chave` | Estado e histórico de chaves |
| `entregas` | `Entrega` | Encomendas recebidas |
| `turnos` | `Turno` | Turnos de trabalho |
| `turno_entradas` | *(collection)* | Timestamps de entrada por turno |
| `turno_saidas` | *(collection)* | Timestamps de saída por turno |

### Relacionamentos
- `chaves.responsavel_id` → referencia `moradores.id` ou `funcionarios.id` (soft reference, sem FK rígida — resolvido na camada de serviço para flexibilidade)
- `turno_entradas` e `turno_saidas` → `@ElementCollection` da entidade `Turno`

---

## Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `POSTGRES_HOST` | `localhost` | Host do banco de dados |
| `POSTGRES_DB` | `mora` | Nome do banco de dados |
| `POSTGRES_USER` | `admin` | Usuário do PostgreSQL |
| `POSTGRES_PASSWORD` | `Vibers@2112` | Senha do PostgreSQL |
| `SPRING_CLOUD_CONSUL_HOST` | `consul` | Host do Consul (via Docker) |
| `SPRING_CLOUD_CONSUL_PORT` | `8500` | Porta do Consul |

---

## Como Executar

### Pré-requisitos
- Java 21+
- Maven 3.8+
- PostgreSQL 16 rodando localmente **ou** Docker Compose

### Execução Local (sem Docker)

1. Certifique-se que o PostgreSQL está rodando em `localhost:5432` com o banco `mora`
2. Execute:
```bash
cd services/portaria-service
mvn spring-boot:run
```

O serviço sobe em `http://localhost:8090`

### Execução via Docker Compose

```bash
cd docker
docker compose up postgres-portaria portaria-service
```

### Swagger UI

Após subir o serviço, acesse:
```
http://localhost:8090/swagger-ui/index.html
```

### Testes

```bash
cd services/portaria-service
mvn test
```
> Os testes usam H2 em memória — não é necessário PostgreSQL rodando.

---

## Integração com a Infraestrutura

```
Cliente
  └─▶ Traefik (:8087)
        └─▶ /api/portaria/** ──strip prefix──▶ portaria-service (:8090)
                                                      │
                                               Consul (service discovery)
                                                      │
                                               PostgreSQL (postgres-portaria:5432)
```

- **Rota no gateway:** `PathPrefix(/api/portaria)` → o prefixo é removido antes de chegar ao serviço
- **Health check:** `GET /actuator/health` (verificado pelo Consul a cada 15s)
- **Registro no Consul:** automático na inicialização com ID único `portaria-service:<random>`

---

## Tratamento de Erros

| Código HTTP | Exceção | Quando ocorre |
|---|---|---|
| `400 Bad Request` | Validação de campos | Campo obrigatório ausente ou inválido |
| `404 Not Found` | `RecursoNaoEncontradoException` | ID não existe no banco |
| `409 Conflict` | `OperacaoInvalidaException` | Operação inválida (ex: chave já retirada, CPF duplicado) |

---

## Testes no Postman

> **Base URL:** `http://localhost:8090`  
> **Header obrigatório em todos os requests com body:** `Content-Type: application/json`

Os fluxos abaixo seguem uma ordem lógica — alguns dependem de IDs retornados por requests anteriores (indicado com `⚠️ salve o ID`).

---

### 1. Moradores

#### 1.1 Cadastrar morador
```
POST http://localhost:8090/moradores/cadastrar
```
```json
{
  "nome": "Carlos Oliveira",
  "cpf": "123.456.789-00",
  "apartamento": "101",
  "bloco": "A",
  "telefone": "11999990000"
}
```
**Resposta `201 Created`:**
```json
{
  "id": "a1b2c3d4-...",
  "nome": "Carlos Oliveira",
  "cpf": "123.456.789-00",
  "apartamento": "101",
  "bloco": "A",
  "telefone": "11999990000",
  "ativo": true,
  "criadoEm": "2026-04-05T10:00:00"
}
```
> ⚠️ **Salve o `id` retornado** — ele é necessário para retirar chaves.

---

#### 1.2 Listar moradores ativos
```
GET http://localhost:8090/moradores
```
**Resposta `200 OK`:** array de moradores com `ativo: true`

---

#### 1.3 Buscar por ID
```
GET http://localhost:8090/moradores/{id}
```

---

#### 1.4 Atualizar morador
```
PUT http://localhost:8090/moradores/{id}
```
```json
{
  "nome": "Carlos Oliveira Silva",
  "cpf": "123.456.789-00",
  "apartamento": "102",
  "bloco": "B",
  "telefone": "11988880000"
}
```

---

#### 1.5 Desativar morador
```
DELETE http://localhost:8090/moradores/{id}
```
**Resposta `204 No Content`**

---

#### ❌ Erro — CPF duplicado
```
POST http://localhost:8090/moradores/cadastrar
```
```json
{
  "nome": "Outro Nome",
  "cpf": "123.456.789-00",
  "apartamento": "201"
}
```
**Resposta `409 Conflict`:**
```json
{
  "timestamp": "2026-04-05T10:01:00",
  "status": 409,
  "erro": "Já existe um morador cadastrado com o CPF: 123.456.789-00"
}
```

---

### 2. Funcionários

#### 2.1 Cadastrar funcionário
```
POST http://localhost:8090/funcionarios/cadastrar
```
```json
{
  "nome": "Ana Porteira",
  "cpf": "987.654.321-00",
  "cargo": "Porteiro",
  "matricula": "PORT-001",
  "telefone": "11977770000"
}
```
**Resposta `201 Created`:**
```json
{
  "id": "f1e2d3c4-...",
  "nome": "Ana Porteira",
  "cpf": "987.654.321-00",
  "cargo": "Porteiro",
  "matricula": "PORT-001",
  "telefone": "11977770000",
  "ativo": true,
  "criadoEm": "2026-04-05T10:00:00"
}
```
> ⚠️ **Salve o `id` retornado** — necessário para retirar chaves como funcionário.

---

#### 2.2 Listar funcionários ativos
```
GET http://localhost:8090/funcionarios
```

---

#### 2.3 Desativar funcionário
```
DELETE http://localhost:8090/funcionarios/{id}
```
**Resposta `204 No Content`**

---

### 3. Visitantes

#### 3.1 Registrar entrada
```
POST http://localhost:8090/visitantes/entrada
```
```json
{
  "nome": "Pedro Visitante",
  "documento": "RG-12345678",
  "motivoVisita": "Visita ao apartamento 101"
}
```
**Resposta `201 Created`:**
```json
{
  "id": "v1v2v3v4-...",
  "nome": "Pedro Visitante",
  "documento": "RG-12345678",
  "motivoVisita": "Visita ao apartamento 101",
  "status": "DENTRO",
  "dataEntrada": "2026-04-05T14:30:00",
  "dataSaida": null
}
```
> ⚠️ **Salve o `id`** para registrar a saída.

---

#### 3.2 Registrar saída
```
POST http://localhost:8090/visitantes/{id}/saida
```
*(sem body)*

**Resposta `200 OK`:**
```json
{
  "id": "v1v2v3v4-...",
  "nome": "Pedro Visitante",
  "status": "SAIU",
  "dataEntrada": "2026-04-05T14:30:00",
  "dataSaida": "2026-04-05T16:00:00"
}
```

---

#### 3.3 Listar visitantes no condomínio
```
GET http://localhost:8090/visitantes/dentro
```
**Resposta `200 OK`:** apenas visitantes com `status: "DENTRO"`

---

#### ❌ Erro — tentar registrar saída de quem já saiu
```
POST http://localhost:8090/visitantes/{id}/saida
```
**Resposta `409 Conflict`:**
```json
{
  "timestamp": "2026-04-05T16:01:00",
  "status": 409,
  "erro": "Visitante não está registrado como DENTRO do condomínio."
}
```

---

### 4. Veículos

#### 4.1 Registrar entrada de veículo
```
POST http://localhost:8090/carros/entrada
```
```json
{
  "placa": "ABC-1234",
  "modelo": "Honda Civic",
  "proprietario": "Carlos Oliveira"
}
```
**Resposta `201 Created`:**
```json
{
  "id": "c1a2r3r4-...",
  "placa": "ABC-1234",
  "modelo": "Honda Civic",
  "proprietario": "Carlos Oliveira",
  "status": "DENTRO",
  "dataEntrada": "2026-04-05T09:00:00",
  "dataSaida": null
}
```
> ⚠️ **Salve o `id`** para registrar a saída.

---

#### 4.2 Registrar saída de veículo
```
POST http://localhost:8090/carros/{id}/saida
```
*(sem body)*

**Resposta `200 OK`:**
```json
{
  "id": "c1a2r3r4-...",
  "placa": "ABC-1234",
  "status": "SAIU",
  "dataEntrada": "2026-04-05T09:00:00",
  "dataSaida": "2026-04-05T18:30:00"
}
```

---

#### 4.3 Listar veículos dentro do condomínio
```
GET http://localhost:8090/carros/dentro
```

---

### 5. Chaves

> ⚠️ **Pré-requisito:** ter um `id` de Morador ou Funcionário cadastrado (passos 1.1 ou 2.1).

#### 5.1 Cadastrar chave
```
POST http://localhost:8090/chaves/cadastrar
```
```json
{
  "nomeChave": "Chave Salão de Festas"
}
```
**Resposta `201 Created`:**
```json
{
  "id": "ch1a2v3e-...",
  "nomeChave": "Chave Salão de Festas",
  "disponivel": true,
  "responsavelId": null,
  "tipoResponsavel": null,
  "nomeResponsavel": null,
  "retirada": null,
  "devolucao": null
}
```
> ⚠️ **Salve o `id`** para retirar/devolver.

---

#### 5.2 Retirar chave (por Morador)
```
POST http://localhost:8090/chaves/{id}/retirar
```
```json
{
  "responsavelId": "a1b2c3d4-...",
  "tipoResponsavel": "MORADOR"
}
```
**Resposta `200 OK`:**
```json
{
  "id": "ch1a2v3e-...",
  "nomeChave": "Chave Salão de Festas",
  "disponivel": false,
  "responsavelId": "a1b2c3d4-...",
  "tipoResponsavel": "MORADOR",
  "nomeResponsavel": "Carlos Oliveira",
  "retirada": "2026-04-05T15:00:00",
  "devolucao": null
}
```

---

#### 5.3 Retirar chave (por Funcionário)
```
POST http://localhost:8090/chaves/{id}/retirar
```
```json
{
  "responsavelId": "f1e2d3c4-...",
  "tipoResponsavel": "FUNCIONARIO"
}
```

---

#### 5.4 Devolver chave
```
POST http://localhost:8090/chaves/{id}/devolver
```
*(sem body)*

**Resposta `200 OK`:**
```json
{
  "id": "ch1a2v3e-...",
  "nomeChave": "Chave Salão de Festas",
  "disponivel": true,
  "responsavelId": null,
  "tipoResponsavel": null,
  "nomeResponsavel": null,
  "retirada": "2026-04-05T15:00:00",
  "devolucao": "2026-04-05T17:45:00"
}
```

---

#### 5.5 Listar chaves disponíveis
```
GET http://localhost:8090/chaves/disponiveis
```

---

#### ❌ Erro — retirar chave já retirada
```
POST http://localhost:8090/chaves/{id}/retirar
```
**Resposta `409 Conflict`:**
```json
{
  "timestamp": "2026-04-05T15:01:00",
  "status": 409,
  "erro": "Chave indisponível — já está retirada."
}
```

---

#### ❌ Erro — ID de responsável inválido
```
POST http://localhost:8090/chaves/{id}/retirar
```
```json
{
  "responsavelId": "id-que-nao-existe",
  "tipoResponsavel": "MORADOR"
}
```
**Resposta `404 Not Found`:**
```json
{
  "timestamp": "2026-04-05T15:01:00",
  "status": 404,
  "erro": "Morador não encontrado com id: id-que-nao-existe"
}
```

---

### 6. Entregas

#### 6.1 Registrar chegada de entrega
```
POST http://localhost:8090/entregas/registrar
```
```json
{
  "nomeEntregador": "Correios",
  "destinatario": "Carlos Oliveira - Apto 101",
  "descricao": "Caixa média, produto eletrônico"
}
```
**Resposta `201 Created`:**
```json
{
  "id": "e1n2t3r4-...",
  "nomeEntregador": "Correios",
  "destinatario": "Carlos Oliveira - Apto 101",
  "descricao": "Caixa média, produto eletrônico",
  "recebedor": null,
  "dataRecebimento": "2026-04-05T10:30:00",
  "dataRetirada": null,
  "retirada": false
}
```
> ⚠️ **Salve o `id`** para registrar a retirada.

---

#### 6.2 Registrar retirada da entrega
```
POST http://localhost:8090/entregas/{id}/retirar
Content-Type: text/plain
```
```
Carlos Oliveira
```
> **Atenção:** este endpoint recebe uma `String` simples, não JSON. No Postman, selecione `raw → Text`.

**Resposta `200 OK`:**
```json
{
  "id": "e1n2t3r4-...",
  "nomeEntregador": "Correios",
  "destinatario": "Carlos Oliveira - Apto 101",
  "recebedor": "Carlos Oliveira",
  "dataRecebimento": "2026-04-05T10:30:00",
  "dataRetirada": "2026-04-05T19:00:00",
  "retirada": true
}
```

---

#### 6.3 Listar entregas pendentes
```
GET http://localhost:8090/entregas/pendentes
```
**Resposta `200 OK`:** apenas entregas com `retirada: false`

---

#### ❌ Erro — retirar entrega já retirada
**Resposta `409 Conflict`:**
```json
{
  "timestamp": "2026-04-05T19:01:00",
  "status": 409,
  "erro": "Entrega já foi retirada."
}
```

---

### 7. Turnos

#### 7.1 Iniciar turno
```
POST http://localhost:8090/turnos/iniciar
```
```json
{
  "funcionario": "Ana Porteira",
  "cargo": "Porteiro"
}
```
**Resposta `201 Created`:**
```json
{
  "id": "t1u2r3n4-...",
  "funcionario": "Ana Porteira",
  "cargo": "Porteiro",
  "entradas": ["2026-04-05T08:00:00"],
  "saidas": []
}
```
> ⚠️ **Salve o `id`** para pausar/retomar/finalizar.

---

#### 7.2 Pausar turno (finalizar período)
```
POST http://localhost:8090/turnos/{id}/finalizar
```
*(sem body)*

**Resposta `200 OK`:**
```json
{
  "id": "t1u2r3n4-...",
  "entradas": ["2026-04-05T08:00:00"],
  "saidas": ["2026-04-05T12:00:00"]
}
```

---

#### 7.3 Retomar turno após pausa
```
POST http://localhost:8090/turnos/{id}/retomar
```
*(sem body)*

**Resposta `200 OK`:**
```json
{
  "id": "t1u2r3n4-...",
  "entradas": ["2026-04-05T08:00:00", "2026-04-05T13:00:00"],
  "saidas": ["2026-04-05T12:00:00"]
}
```

---

#### 7.4 Encerrar turno
```
POST http://localhost:8090/turnos/{id}/finalizar
```
*(sem body)*

**Resposta `200 OK`:**
```json
{
  "id": "t1u2r3n4-...",
  "entradas": ["2026-04-05T08:00:00", "2026-04-05T13:00:00"],
  "saidas": ["2026-04-05T12:00:00", "2026-04-05T17:00:00"]
}
```

---

#### 7.5 Listar todos os turnos
```
GET http://localhost:8090/turnos
```

---

#### ❌ Erro — validação de campos obrigatórios
```
POST http://localhost:8090/turnos/iniciar
```
```json
{
  "funcionario": ""
}
```
**Resposta `400 Bad Request`:**
```json
{
  "timestamp": "2026-04-05T08:01:00",
  "status": 400,
  "erros": {
    "funcionario": "Nome do funcionário é obrigatório",
    "cargo": "Cargo é obrigatório"
  }
}
```

---

### Fluxo completo sugerido no Postman

Execute nesta ordem para testar a integração completa:

```
1. POST /moradores/cadastrar          → salve o moradorId
2. POST /funcionarios/cadastrar       → salve o funcionarioId
3. POST /chaves/cadastrar             → salve o chaveId
4. POST /chaves/{chaveId}/retirar     → use o moradorId
5. POST /visitantes/entrada           → salve o visitanteId
6. POST /carros/entrada               → salve o carroId
7. POST /entregas/registrar           → salve o entregaId
8. POST /turnos/iniciar               → salve o turnoId
9. POST /chaves/{chaveId}/devolver
10. POST /visitantes/{visitanteId}/saida
11. POST /carros/{carroId}/saida
12. POST /entregas/{entregaId}/retirar
13. POST /turnos/{turnoId}/finalizar
```
