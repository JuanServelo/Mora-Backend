# Vagas Service - Gerenciar Vagas de Estacionamento

Serviço responsável pelo gerenciamento completo de vagas de estacionamento do condomínio Mora.

## Tecnologias

- **Java 21**
- **Spring Boot 3.5.6**
- **Spring Data JPA**
- **PostgreSQL 18**
- **Spring Cloud Consul** (Service Discovery)
- **Traefik** (API Gateway)

## Controle de Acesso

As operações de escrita (cadastrar, atualizar, desativar, ativar) são restritas aos perfis **ADMINISTRADOR** e **SINDICO**, conforme RF09.

O controle é feito via header `X-User-Role` propagado pelo Traefik após validação do JWT no API Gateway. Endpoints de leitura são abertos para qualquer usuário autenticado.

## Endpoints

### Vagas

| Método | Rota | Acesso | Descrição |
|--------|------|--------|-----------|
| `POST` | `/api/vagas/vagas/cadastrar` | ADMINISTRADOR, SINDICO | Cadastrar nova vaga |
| `GET` | `/api/vagas/vagas` | Todos | Listar vagas ativas |
| `GET` | `/api/vagas/vagas/todas` | ADMINISTRADOR, SINDICO | Listar todas as vagas |
| `GET` | `/api/vagas/vagas/{id}` | Todos | Buscar vaga por ID |
| `GET` | `/api/vagas/vagas/apartamento/{apartamentoId}` | Todos | Buscar vagas de um apartamento |
| `PUT` | `/api/vagas/vagas/{id}` | ADMINISTRADOR, SINDICO | Atualizar vaga |
| `DELETE` | `/api/vagas/vagas/{id}` | ADMINISTRADOR, SINDICO | Desativar vaga |
| `POST` | `/api/vagas/vagas/{id}/ativar` | ADMINISTRADOR, SINDICO | Ativar vaga |

## Estrutura

```
src/main/
├── VagasServiceApplication.java
├── config/
│   └── CorsConfig.java
├── controller/
│   └── VagaController.java
├── service/
│   └── VagaService.java
├── repository/
│   ├── VagaRepository.java
│   └── ApartamentoRepository.java
├── model/
│   ├── Vaga.java
│   ├── Apartamento.java
│   ├── Bloco.java
│   ├── Morador.java
│   └── enums/
│       ├── TipoVaga.java
│       └── TipoProprietario.java
├── dto/
│   ├── VagaRequestDTO.java      ← entrada (record imutável)
│   └── VagaResponseDTO.java     ← saída
└── exception/
    ├── GlobalExceptionHandler.java
    ├── RecursoNaoEncontradoException.java
    └── OperacaoInvalidaException.java
```

## Request / Response

### Criar ou Atualizar Vaga — `VagaRequestDTO`

```json
{
  "numero": "A-001",
  "localizacao": "Subsolo 1",
  "tipo": "COBERTA",
  "apartamentoId": "uuid-do-apartamento"
}
```

**Valores válidos para `tipo`:** `COBERTA`, `DESCOBERTA`, `MOTO`, `DEFICIENTE`

### Resposta — `VagaResponseDTO`

```json
{
  "id": "uuid-gerado",
  "numero": "A-001",
  "localizacao": "Subsolo 1",
  "tipo": "COBERTA",
  "ativa": true,
  "apartamentoId": "uuid-do-apartamento",
  "apartamentoNumero": "101"
}
```

## Rodando Localmente

### Pré-requisitos
- Java 21
- Maven 3.9+
- PostgreSQL 18 rodando
- Consul rodando na porta 8500

### Passos

```bash
cd services/vagas-service
mvn clean install
mvn spring-boot:run
```

O serviço estará disponível em `http://localhost:8092`

## Docker

```bash
docker build -t vagas-service:latest .
docker run -p 8092:8092 \
  -e POSTGRES_HOST=postgres \
  -e POSTGRES_DB=mora \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=Vibers@2112 \
  vagas-service:latest
```

## Validação (curl)

```bash
# Cadastrar nova vaga (requer role ADMINISTRADOR)
curl -X POST http://localhost:8092/api/vagas/vagas/cadastrar \
  -H "Content-Type: application/json" \
  -H "X-User-Role: ADMINISTRADOR" \
  -d '{
    "numero": "A-001",
    "localizacao": "Subsolo 1",
    "tipo": "COBERTA",
    "apartamentoId": "uuid-do-apartamento"
  }'

# Listar vagas ativas (aberto)
curl http://localhost:8092/api/vagas/vagas

# Listar vagas de um apartamento
curl http://localhost:8092/api/vagas/vagas/apartamento/uuid-do-apartamento
```

## Implementação de RF09

Este serviço implementa o **RF09 - Gerenciar Vagas de Estacionamento**:

✅ Cadastro de novas vagas com `VagaRequestDTO` (sem expor entidade JPA)
✅ `apartamentoId` obrigatório — toda vaga deve estar vinculada a uma unidade
✅ Validação de tipo via enum `TipoVaga` (COBERTA, DESCOBERTA, MOTO, DEFICIENTE)
✅ Bloqueio de vagas duplicadas por número
✅ Consulta de vagas por morador (via apartamento)
✅ Ativação/desativação de vagas
✅ Controle de acesso por role (ADMINISTRADOR / SINDICO) via header `X-User-Role`

## Autor

Mora Backend Team
