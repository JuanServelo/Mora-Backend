# Gerenciamento de Estruturas Físicas — Portaria Service

Funcionalidade para cadastro, organização e gerenciamento das estruturas físicas do condomínio: blocos, apartamentos e áreas comuns.

---

## Componentes Principais

### 1. Blocos (`/blocos`)
Representa os blocos/prédios do condomínio.

**Modelo:**
```json
{
  "id": "UUID",
  "nome": "Bloco A",
  "descricao": "Bloco principal com 4 andares",
  "andares": 4,
  "apartamentosPorAndar": 4,
  "ativo": true,
  "criadoEm": "2024-01-15T10:30:00",
  "atualizadoEm": "2024-01-15T10:30:00"
}
```

**Endpoints:**
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/blocos/cadastrar` | Cadastrar novo bloco |
| `GET` | `/blocos` | Listar blocos ativos |
| `GET` | `/blocos/todos` | Listar todos os blocos |
| `GET` | `/blocos/{id}` | Buscar bloco por ID |
| `PUT` | `/blocos/{id}` | Atualizar bloco |
| `PUT` | `/blocos/{id}/desativar` | Desativar bloco |
| `PUT` | `/blocos/{id}/ativar` | Ativar bloco |
| `DELETE` | `/blocos/{id}` | Deletar bloco |

**Exemplo de Cadastro:**
```bash
curl -X POST http://localhost:8090/blocos/cadastrar \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Bloco A",
    "descricao": "Bloco principal com 4 andares",
    "andares": 4,
    "apartamentosPorAndar": 4
  }'
```

---

### 2. Apartamentos (`/apartamentos`)
Representa os apartamentos dentro de blocos.

**Modelo:**
```json
{
  "id": "UUID",
  "numero": "101",
  "andar": 1,
  "blocoId": "UUID",
  "blocoNome": "Bloco A",
  "quartos": 3,
  "areaMxComTotal": 85.5,
  "observacoes": "Apto com varanda",
  "ativo": true,
  "criadoEm": "2024-01-15T10:30:00",
  "atualizadoEm": "2024-01-15T10:30:00"
}
```

**Endpoints:**
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/apartamentos/cadastrar` | Cadastrar novo apartamento |
| `GET` | `/apartamentos` | Listar apartamentos ativos |
| `GET` | `/apartamentos/todos` | Listar todos os apartamentos |
| `GET` | `/apartamentos/{id}` | Buscar apartamento por ID |
| `GET` | `/apartamentos/bloco/{blocoId}` | Listar apartamentos de um bloco |
| `GET` | `/apartamentos/bloco/{blocoId}/ativos` | Listar apartamentos ativos de um bloco |
| `PUT` | `/apartamentos/{id}` | Atualizar apartamento |
| `PUT` | `/apartamentos/{id}/desativar` | Desativar apartamento |
| `PUT` | `/apartamentos/{id}/ativar` | Ativar apartamento |
| `DELETE` | `/apartamentos/{id}` | Deletar apartamento |

**Validações:**
- O bloco deve existir
- Não pode haver dois apartamentos com o mesmo número no mesmo bloco
- O andar não pode exceder o número de andares do bloco

**Exemplo de Cadastro:**
```bash
curl -X POST http://localhost:8090/apartamentos/cadastrar \
  -H "Content-Type: application/json" \
  -d '{
    "numero": "101",
    "andar": 1,
    "blocoId": "123e4567-e89b-12d3-a456-426614174000",
    "quartos": 3,
    "areaMxComTotal": 85.5,
    "observacoes": "Apto com varanda"
  }'
```

**Exemplo de Listagem por Bloco:**
```bash
curl -X GET http://localhost:8090/apartamentos/bloco/123e4567-e89b-12d3-a456-426614174000
```

---

### 3. Áreas Comuns (`/areas-comuns`)
Representa espaços compartilhados do condomínio (churrasqueiras, salão de festas, gym, playground, etc).

**Modelo:**
```json
{
  "id": "UUID",
  "nome": "Churrasqueira 01",
  "tipo": "churrasqueira",
  "descricao": "Churrasqueira com 2 churrascos",
  "localizacao": "Andar térreo - Lateral direita",
  "capacidadeMaxima": 30,
  "area": 50.0,
  "podeReservar": true,
  "observacoes": "Necessário agendamento prévio",
  "ativo": true,
  "criadoEm": "2024-01-15T10:30:00",
  "atualizadoEm": "2024-01-15T10:30:00"
}
```

**Tipos Pré-configurados (sugestão):**
- `churrasqueira` — Churrascaria
- `salao_festas` — Salão de festas/eventos
- `playground` — Playground infantil
- `quadra` — Quadra poliesportiva
- `academia` — Academia/gym
- `piscina` — Piscina
- `sauna` — Sauna
- `biblioteca` — Biblioteca
- `sala_reuniao` — Sala de reunião
- `outro` — Outro

**Endpoints:**
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/areas-comuns/cadastrar` | Cadastrar nova área comum |
| `GET` | `/areas-comuns` | Listar áreas ativas |
| `GET` | `/areas-comuns/todas` | Listar todas as áreas |
| `GET` | `/areas-comuns/{id}` | Buscar área por ID |
| `GET` | `/areas-comuns/tipo/{tipo}` | Listar áreas por tipo |
| `GET` | `/areas-comuns/tipo/{tipo}/ativas` | Listar áreas ativas de um tipo |
| `PUT` | `/areas-comuns/{id}` | Atualizar área |
| `PUT` | `/areas-comuns/{id}/desativar` | Desativar área |
| `PUT` | `/areas-comuns/{id}/ativar` | Ativar área |
| `DELETE` | `/areas-comuns/{id}` | Deletar área |

**Validações:**
- Nome deve ser único
- Campo `podeReservar` indica se a área pode ser reservada (campo booleano)

**Exemplo de Cadastro:**
```bash
curl -X POST http://localhost:8090/areas-comuns/cadastrar \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Churrasqueira 01",
    "tipo": "churrasqueira",
    "descricao": "Churrasqueira com 2 churrascos",
    "localizacao": "Andar térreo - Lateral direita",
    "capacidadeMaxima": 30,
    "area": 50.0,
    "podeReservar": true,
    "observacoes": "Necessário agendamento prévio"
  }'
```

**Exemplo de Listagem por Tipo:**
```bash
curl -X GET http://localhost:8090/areas-comuns/tipo/churrasqueira
```

---

## Estrutura do Banco de Dados

### Tabela `blocos`
```sql
CREATE TABLE blocos (
  id UUID PRIMARY KEY,
  nome VARCHAR(255) NOT NULL UNIQUE,
  descricao TEXT,
  andares INTEGER,
  apartamentosPorAndar INTEGER,
  ativo BOOLEAN DEFAULT true,
  criadoEm TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  atualizadoEm TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_blocos_ativo ON blocos(ativo);
```

### Tabela `apartamentos`
```sql
CREATE TABLE apartamentos (
  id UUID PRIMARY KEY,
  numero VARCHAR(50) NOT NULL,
  andar INTEGER NOT NULL,
  blocoId UUID NOT NULL REFERENCES blocos(id),
  quartos INTEGER,
  areaMxComTotal DOUBLE PRECISION,
  observacoes TEXT,
  ativo BOOLEAN DEFAULT true,
  criadoEm TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  atualizadoEm TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(numero, blocoId)
);

CREATE INDEX idx_apartamentos_blocoId ON apartamentos(blocoId);
CREATE INDEX idx_apartamentos_ativo ON apartamentos(ativo);
CREATE INDEX idx_apartamentos_numero_blocoId ON apartamentos(numero, blocoId);
```

### Tabela `areas_comuns`
```sql
CREATE TABLE areas_comuns (
  id UUID PRIMARY KEY,
  nome VARCHAR(255) NOT NULL UNIQUE,
  tipo VARCHAR(100) NOT NULL,
  descricao TEXT,
  localizacao VARCHAR(255),
  capacidadeMaxima INTEGER,
  area DOUBLE PRECISION,
  podeReservar BOOLEAN DEFAULT false,
  observacoes TEXT,
  ativo BOOLEAN DEFAULT true,
  criadoEm TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  atualizadoEm TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_areas_comuns_tipo ON areas_comuns(tipo);
CREATE INDEX idx_areas_comuns_ativo ON areas_comuns(ativo);
CREATE INDEX idx_areas_comuns_pode_reservar ON areas_comuns(podeReservar);
```

---

## Tratamento de Erros

Todos os endpoints retornam erros padronizados:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Já existe um bloco cadastrado com o nome: Bloco A",
  "path": "/blocos/cadastrar"
}
```

**Códigos de Erro Comuns:**
| Código | Erro | Descrição |
|---|---|---|
| `400` | Bad Request | Dados inválidos ou falta validação |
| `404` | Not Found | Recurso não encontrado |
| `409` | Conflict | Violação de unicidade |
| `500` | Internal Server Error | Erro do servidor |

---

## Workflow de Uso Típico

### Cenário 1: Cadastrar Nova Estrutura
1. **Cadastrar Bloco**
   ```bash
   POST /blocos/cadastrar
   ```
   
2. **Cadastrar Apartamentos do Bloco**
   ```bash
   POST /apartamentos/cadastrar (x múltiplos)
   ```

3. **Cadastrar Áreas Comuns**
   ```bash
   POST /areas-comuns/cadastrar
   ```

### Cenário 2: Manutenção Estrutural
1. **Listar Apartamentos de um Bloco**
   ```bash
   GET /apartamentos/bloco/{blocoId}
   ```

2. **Desativar Apartamento em Manutenção**
   ```bash
   PUT /apartamentos/{id}/desativar
   ```

3. **Reativar Após Conclusão**
   ```bash
   PUT /apartamentos/{id}/ativar
   ```

### Cenário 3: Consulta de Áreas Reserváveis
1. **Listar Áreas que Podem Ser Reservadas**
   ```bash
   GET /areas-comuns/todas
   # Filtrar as que têm podeReservar = true
   ```

---

## Integrações Futuras

- **Sistema de Reservas**: Integrar com agendamento de áreas comuns
- **Documentos de Apartamentos**: Armazenar fotos, plantas, documentos
- **Manutenção Preventiva**: Agendar manutenções em blocos/áreas
- **Alertas Estruturais**: Notificações sobre manutenções pendentes
- **Relatorios**: Gerar relatórios de ocupação, manutenção, etc.

---

## Arquivos Criados

### Models (Entidades JPA)
- `portaria/model/Bloco.java`
- `portaria/model/Apartamento.java`
- `portaria/model/AreaComum.java`

### Repositories (Interfaces JPA)
- `portaria/repository/BlocoRepository.java`
- `portaria/repository/ApartamentoRepository.java`
- `portaria/repository/AreaComunRepository.java`

### Services (Lógica de Negócio)
- `portaria/service/BlocoService.java`
- `portaria/service/ApartamentoService.java`
- `portaria/service/AreaComunService.java`

### Controllers (REST Endpoints)
- `portaria/controller/BlocoController.java`
- `portaria/controller/ApartamentoController.java`
- `portaria/controller/AreaComunController.java`

### DTOs (Transfer Objects)
- `portaria/dto/BlocoResponseDTO.java`
- `portaria/dto/ApartamentoResponseDTO.java`
- `portaria/dto/AreaComunResponseDTO.java`

### Database
- Tabelas e índices criados em `docker/init-databases.sql`
