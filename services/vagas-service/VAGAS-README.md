# Vagas Service - Gerenciar Vagas de Estacionamento

Serviço responsável pelo gerenciamento completo de vagas de estacionamento do condomínio Mora.

## Tecnologias

- **Java 21**
- **Spring Boot 3.5.6**
- **Spring Data JPA**
- **PostgreSQL 18**
- **Spring Cloud Consul** (Service Discovery)
- **Traefik** (API Gateway)

## Endpoints

### Vagas
- `POST /api/vagas/vagas/cadastrar` - Cadastrar nova vaga
- `GET /api/vagas/vagas` - Listar vagas ativas
- `GET /api/vagas/vagas/todas` - Listar todas as vagas
- `GET /api/vagas/vagas/{id}` - Buscar vaga por ID
- `GET /api/vagas/vagas/apartamento/{apartamentoId}` - Buscar vagas de um apartamento
- `PUT /api/vagas/vagas/{id}` - Atualizar vaga
- `DELETE /api/vagas/vagas/{id}` - Desativar vaga
- `POST /api/vagas/vagas/{id}/ativar` - Ativar vaga

## Estrutura

```
src/main/java/portaria/
├── VagasServiceApplication.java        # Entry point
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
│   └── Apartamento.java
├── dto/
│   ├── VagaRequestDTO.java
│   └── VagaResponseDTO.java
└── exception/
    ├── GlobalExceptionHandler.java
    ├── RecursoNaoEncontradoException.java
    └── OperacaoInvalidaException.java
```

## Rodando Localmente

### Pré-requisitos
- Java 21
- Maven 3.9+
- PostgreSQL 18 rodando
- Consul rodando na porta 8500

### Passos

1. **Build**
```bash
cd services/vagas-service
mvn clean install
```

2. **Run**
```bash
mvn spring-boot:run
```

O serviço estará disponível em `http://localhost:8092`

## Docker

### Build
```bash
docker build -t vagas-service:latest .
```

### Run
```bash
docker run -p 8092:8092 \
  -e POSTGRES_HOST=postgres \
  -e POSTGRES_DB=mora \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=Vibers@2112 \
  vagas-service:latest
```

## Integração via Docker Compose

O serviço já está configurado no `docker-compose.yml` raiz. Execute:

```bash
docker-compose up -d vagas-service
```

## Validação

Testes de funcionalidade básicos:

```bash
# Cadastrar nova vaga
curl -X POST http://localhost:8092/api/vagas/vagas/cadastrar \
  -H "Content-Type: application/json" \
  -d '{
    "numero": "A-001",
    "localizacao": "Subsolo 1",
    "tipo": "Coberta",
    "apartamentoId": "uuid-do-apartamento"
  }'

# Listar vagas ativas
curl http://localhost:8092/api/vagas/vagas

# Listar vagas de um apartamento
curl http://localhost:8092/api/vagas/vagas/apartamento/uuid-do-apartamento
```

## Implementação de RF09

Este serviço implementa completamente o **RF09 - Gerenciar Vagas de Estacionamento**, incluindo:

✅ Cadastro de novas vagas com validação de duplicidade
✅ Associação de vagas a apartamentos
✅ Bloqueio de vagas duplicadas
✅ Consulta de vagas por morador (via apartamento)
✅ Ativação/desativação de vagas

## Autor

Mora Backend Team
