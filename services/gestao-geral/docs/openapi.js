/**
 * Especificação OpenAPI do gestao-geral.
 *
 * Escrita à mão, e não gerada de comentários: as rotas aqui só agregam dados de
 * outros serviços, e o que importa documentar é a forma da resposta — que só um
 * exemplo real mostra. Os exemplos partem de respostas da stack de
 * desenvolvimento com as contas do seed (`scripts/seed-dev.mjs`); onde o dado do
 * ambiente estava vazio (zeros, plano chamado "teste"), entrou um valor
 * ilustrativo para a forma ficar visível.
 *
 * `tests/documentacao.test.js` compara esta especificação com as rotas que o
 * Express registrou: rota nova sem documentação quebra os testes.
 */

const json = (exemplo) => ({ 'application/json': { example: exemplo } });
const resposta = (description, exemplo) => ({ description, content: json(exemplo) });
const falha = (description, mensagem) => resposta(description, { sucesso: false, mensagem });

const ERROS = {
  401: falha('Sem token, ou token inválido ou expirado.', 'Token não fornecido'),
  403: falha('O perfil de quem chama não tem acesso.', 'Apenas o Admin Geral pode consultar dados da plataforma.'),
  503: falha('A fonte principal do dado não respondeu.', 'O auth-api não respondeu. Tente novamente.'),
};

const meses = {
  name: 'meses', in: 'query', required: false,
  description: 'Janela das séries mensais, em meses (padrão 12).',
  schema: { type: 'integer', minimum: 1, maximum: 36 }, example: 12,
};

// ── Exemplos: respostas reais do ambiente de desenvolvimento ────────────────

const EX_PREFERENCIA = {
  versao: 2,
  blocos: [
    { id: 'ordens-servico', visivel: true, largura: null, altura: null, tipo: null, cor: 'coral' },
    { id: 'ocorrencias', visivel: true, largura: null, altura: null, tipo: null, cor: null },
    { id: 'entregas', visivel: false, largura: null, altura: null, tipo: null, cor: null },
  ],
};

const chavePreferencia = {
  name: 'chave', in: 'path', required: true,
  description: 'Qual painel: `painel.geral`, `painel.condominio.operacional` ou `painel.condominio.estrategico`.',
  schema: { type: 'string', pattern: '^painel(\\.[a-z0-9-]{1,30}){1,3}$' }, example: 'painel.condominio.operacional',
};

const ERROS_PREFERENCIA = {
  400: falha('Chave fora do padrão, ou valor que não é objeto ou passa de 8 KB.', 'Chave de preferência desconhecida.'),
  401: falha('Sem token, token inválido, ou sessão encerrada/usuário desativado no auth-api.', 'Sessão inválida ou encerrada. Entre de novo.'),
  503: falha('O auth-api não confirmou a sessão: recusa em vez de deixar passar.', 'Não foi possível confirmar a sessão agora. Tente novamente.'),
};

const EX_DASHBOARD = {
  sucesso: true,
  geradoEm: '2026-10-20T23:33:19.774Z',
  filtros: { meses: 12, status: 'todos' },
  condominios: {
    total: 10, ativos: 10, inativos: 0, novosUltimos30: 0, meses: 12,
    criadosPorMes: [{ mes: '2025-11', total: 0 }, { mes: '2025-12', total: 1 }],
  },
  usuarios: {
    total: 388, ativos: 321,
    porPerfil: { ADMIN_GERAL: 1, ADMIN_SINDICO: 11, CONVIDADO: 22, MORADOR: 293, PORTEIRO: 16, DONO_ALUGUEL: 45 },
    porStatus: { inactive: 24, pending_activation: 43, active: 321 },
    porCondominio: [
      { condominioId: 'qa-vila-serena', nome: 'Residencial Vila Serena', total: 122 },
      { condominioId: 'qa-parque-atlantico', nome: 'Condomínio Parque Atlântico', total: 99 },
    ],
    convitesPendentes: 0,
  },
  ocorrencias: { total: 35, abertas: 23, porStatus: { EM_ANALISE: 11, RESOLVIDO: 12, PENDENTE: 12 } },
  fontesIndisponiveis: [],
};

const EX_RECEITA = {
  sucesso: true,
  geradoEm: '2026-10-20T23:33:20.819Z',
  filtros: { meses: 12, plano: 'todos' },
  planosDisponiveis: ['Essencial', 'Completo'],
  indicadores: {
    mrrCentavos: 260000, arrCentavos: 3120000, clientesAtivos: 4,
    clientesSemAssinatura: 6, ticketMedioCentavos: 65000, acumuladoCentavos: 1640000,
  },
  porPlano: [
    { plano: 'Completo', assinaturas: 2, mrrCentavos: 200000 },
    { plano: 'Essencial', assinaturas: 2, mrrCentavos: 60000 },
  ],
  evolucao: [{ mes: '2026-09', clientes: 4, mrrCentavos: 260000 }, { mes: '2026-10', clientes: 4, mrrCentavos: 260000 }],
  clientes: [{
    condominioId: 'cond-parque-verde', nome: 'Residencial Parque Verde', plano: 'Completo', status: 'ATIVA',
    vigente: true, desde: '2026-01-01', mensalidadeCentavos: 100000, acumuladoCentavos: 1000000,
  }],
  semAssinatura: [{ condominioId: 'qa-alameda-flores', nome: 'Condomínio Alameda das Flores' }],
  fontesIndisponiveis: [],
};

const EX_RESUMO = {
  sucesso: true,
  geradoEm: '2026-10-20T23:34:09.786Z',
  resumo: {
    condominioId: 'cond-parque-verde',
    usuarios: {
      total: 8, ativos: 8,
      porPerfil: { ADMIN_SINDICO: 1, PORTEIRO: 1, MORADOR: 4, DONO_ALUGUEL: 1, CONVIDADO: 1 },
      porStatus: { active: 8 },
    },
    convitesPendentes: 0,
    ocorrencias: 0,
  },
  estrutura: { blocos: 3, apartamentos: 5, areasComuns: 1 },
  assinatura: {
    id: 7, condominioId: 'cond-parque-verde', status: 'ATIVA', vigenciaInicio: '2026-01-01', vigenciaFim: null,
    vigente: true, planId: 1, planNome: 'Completo', mensalidade: 1000, maxCondominios: 10,
    maxUsuariosPorCondominio: 200, modulosAtivos: ['portaria', 'reunioes'], observacao: 'Assinatura de desenvolvimento',
  },
  fontesIndisponiveis: [],
};

const EX_PAINEL = {
  sucesso: true,
  geradoEm: '2026-10-20T23:33:21.013Z',
  condominioId: 'cond-parque-verde',
  indicadores: {
    usuariosAtivos: 8, usuariosTotal: 8, convitesPendentes: 0, ocorrenciasAbertas: 0,
    apartamentos: 5, blocos: 3, areasComuns: 1, taxaOcupacao: 80,
  },
  usuariosPorPerfil: { ADMIN_SINDICO: 1, PORTEIRO: 1, MORADOR: 4, DONO_ALUGUEL: 1, CONVIDADO: 1 },
  usuariosPorStatus: { active: 8 },
  plano: {
    nome: 'Completo', mensalidade: 1000, status: 'ATIVA', vigente: true, limiteUsuarios: 200, modulos: ['portaria', 'reunioes'],
  },
  financeiro: null,
  fontesIndisponiveis: [],
};

const EX_OPERACIONAL = {
  sucesso: true,
  geradoEm: '2026-10-20T23:33:21.210Z',
  condominioId: 'cond-parque-verde',
  entregas: { aguardandoRetirada: 2, maisAntigaDesde: '2026-10-18T09:40:00' },
  reservas: { aguardandoAprovacao: 1, proximoPrazo: '2026-10-22T18:00:00' },
  presenca: { atendimentosAbertos: 0, visitantesDentro: 1 },
  ocorrencias: { abertas: 1, pendentes: 1, emAnalise: 0, maisAntigaDesde: '2026-10-19T14:12:03.000Z' },
  ordensServico: { abertas: 2, atrasadas: 1, criticasAtrasadas: 0, percentualNoPrazo: 75 },
  conversas: { abertas: 3, comMensagemNova: 0 },
  avisos: {
    vigentes: 2,
    leitura: [
      { id: '766730a5-0658-475f-89fe-aad79bb287c1', titulo: 'Manutenção da caixa d\'água', dataFim: '2026-12-31', leituras: 2, destinatarios: 6, percentual: 33 },
      { id: 'b2566a90-f968-41a2-93d7-870b0efd9917', titulo: 'Nova regra para uso do salão de festas', dataFim: '2026-12-31', leituras: 2, destinatarios: 5, percentual: 40 },
    ],
  },
  financeiro: { faturasEmAtraso: 0, faturasAbertas: 4, totalCentavos: 446499, recebidoCentavos: 0 },
  fontesIndisponiveis: [],
};

const EX_ESTRATEGICO = {
  sucesso: true,
  geradoEm: '2026-10-20T23:33:21.323Z',
  condominioId: 'cond-parque-verde',
  meses: ['2026-09', '2026-10'],
  reservas: { porMes: [{ mes: '2026-09', total: 3 }, { mes: '2026-10', total: 5 }], porArea: [{ area: 'Salão de festas', total: 8 }] },
  ocorrencias: {
    porMes: [{ mes: '2026-09', abertas: 2, resolvidas: 2 }, { mes: '2026-10', abertas: 1, resolvidas: 0 }],
    porCategoria: [{ categoria: 'Barulho', total: 2 }, { categoria: 'Limpeza', total: 1 }],
    tempoMedioResolucaoDias: 1.5,
  },
  inadimplencia: {
    porMes: [
      { mes: '2026-09', faturas: 4, emAtraso: 0, taxa: 0, recebidoCentavos: 389048 },
      { mes: '2026-10', faturas: 4, emAtraso: 1, taxa: 25, recebidoCentavos: 334874 },
    ],
  },
  multas: { porMes: [{ mes: '2026-09', aplicadas: 0, canceladas: 0, valorCentavos: 0 }, { mes: '2026-10', aplicadas: 1, canceladas: 0, valorCentavos: 15000 }] },
  fontesIndisponiveis: [],
};

// ── Especificação ───────────────────────────────────────────────────────────

export const openapi = {
  openapi: '3.0.3',
  info: {
    title: 'gestao-geral',
    version: '1.0',
    description: [
      'Agrega os números dos outros serviços para os painéis: o da plataforma, do Admin Geral, e os do síndico',
      '(RF-18). Não tem banco próprio — cada número é pedido ao dono do dado, que agrega em SQL (RNF-20).',
      '',
      '- **Responsável:** João Victor',
      '- **Banco:** nenhum (consulta auth-api, portaria, comunicacao, financeiro e plan)',
      '- **Pelo gateway:** `/api/gestao`',
      '- **Autenticação:** JWT do auth-api (`POST /api/auth/login`), no botão **Authorize**.',
      '',
      'Uma fonte fora do ar não derruba o painel: o bloco dela vem `null` e o nome entra em',
      '`fontesIndisponiveis`. Os exemplos partem de respostas do ambiente de desenvolvimento (condomínio',
      'Residencial Parque Verde, do seed).',
    ].join('\n'),
  },
  servers: [{ url: '/', description: 'Gateway (Traefik)' }],
  tags: [
    { name: 'Plataforma', description: 'Números de todos os condomínios. Exclusivo do Admin Geral.' },
    { name: 'Condomínio', description: 'Números de um condomínio: o Admin Geral escolhe qual; a gestão vê o próprio.' },
    { name: 'Painéis do síndico', description: 'RF-18: o operacional (o que pede ação agora) e o estratégico (séries mensais).' },
    { name: 'Preferências', description: 'Como cada usuário monta os próprios painéis, guardado no banco `mora_gestao`.' },
    { name: 'Documentação', description: 'O portal de documentação e esta especificação.' },
    { name: 'Saúde', description: 'Verificação usada pelo Consul.' },
  ],
  components: {
    securitySchemes: {
      bearerAuth: {
        type: 'http', scheme: 'bearer', bearerFormat: 'JWT',
        description: 'Token devolvido por POST /api/auth/login no auth-api.',
      },
    },
  },
  security: [{ bearerAuth: [] }],
  paths: {
    '/api/gestao/dashboard': {
      get: {
        tags: ['Plataforma'],
        summary: 'Indicadores da plataforma',
        description: 'Condomínios, usuários e ocorrências de toda a plataforma, com séries mensais. `meses` e `status` são repassados ao auth-api, que agrega já recortado.',
        parameters: [meses, {
          name: 'status', in: 'query', required: false, description: 'Restringe aos condomínios com este status.',
          schema: { type: 'string', enum: ['todos', 'ATIVO', 'INATIVO'] }, example: 'todos',
        }],
        responses: { 200: resposta('Indicadores.', EX_DASHBOARD), 401: ERROS[401], 403: ERROS[403], 503: ERROS[503] },
      },
    },
    '/api/gestao/receita': {
      get: {
        tags: ['Plataforma'],
        summary: 'Receita da plataforma',
        description: 'MRR, ARR, ticket médio e evolução mensal, calculados sobre as assinaturas do plan-service. Separado do dashboard porque percorre todas as assinaturas.',
        parameters: [meses, {
          name: 'plano', in: 'query', required: false, description: 'Restringe a um plano pelo nome.',
          schema: { type: 'string' }, example: 'Completo',
        }],
        responses: { 200: resposta('Receita.', EX_RECEITA), 401: ERROS[401], 403: ERROS[403], 503: ERROS[503] },
      },
    },
    '/api/gestao/dashboard/exportar': {
      get: {
        tags: ['Plataforma'],
        summary: 'Exportar indicadores em CSV',
        description: 'Os mesmos números do dashboard, com o mesmo recorte de filtros, em CSV separado por `;`.',
        parameters: [meses],
        responses: {
          200: {
            description: 'Arquivo CSV.',
            content: { 'text/csv': { example: 'secao;chave;valor\r\ncondominios_por_mes;2026-09;1\r\nusuarios_por_perfil;MORADOR;293\r\nocorrencias_por_status;PENDENTE;12\r\n' } },
          },
          401: ERROS[401],
          403: ERROS[403],
        },
      },
    },
    '/api/gestao/condominios/{id}/resumo': {
      get: {
        tags: ['Condomínio'],
        summary: 'Resumo de um condomínio',
        description: 'Usuários por perfil e status, estrutura física (portaria) e assinatura (plan). Estrutura e assinatura são acessórias: se a fonte cair, vêm `null`.',
        parameters: [{
          name: 'id', in: 'path', required: true, description: 'Id do condomínio.',
          schema: { type: 'string' }, example: 'cond-parque-verde',
        }],
        responses: { 200: resposta('Resumo.', EX_RESUMO), 401: ERROS[401], 403: falha('Condomínio de outro cliente.', 'Acesso negado'), 503: ERROS[503] },
      },
    },
    '/api/gestao/painel': {
      get: {
        tags: ['Condomínio'],
        summary: 'Painel do condomínio',
        description: 'Indicadores do condomínio, plano contratado e situação financeira. A gestão vê o próprio condomínio; o Admin Geral informa qual em `condominioId`.',
        parameters: [{
          name: 'condominioId', in: 'query', required: false,
          description: 'Só para o Admin Geral. Para os demais, o condomínio sai do token.',
          schema: { type: 'string' }, example: 'cond-parque-verde',
        }],
        responses: {
          200: resposta('Painel.', EX_PAINEL),
          400: falha('Admin Geral sem condomínio informado.', 'Informe o condomínio em ?condominioId=.'),
          401: ERROS[401],
          403: falha('Perfil sem acesso.', 'Apenas perfis de gestão acessam o painel do condomínio.'),
        },
      },
    },
    '/api/gestao/painel/operacional': {
      get: {
        tags: ['Painéis do síndico'],
        summary: 'Painel operacional',
        description: 'O que pede ação agora: entregas esperando retirada, reservas a aprovar, quem está dentro, ocorrências abertas e ordens de serviço atrasadas (ocorrencias-service), conversas, leitura dos avisos vigentes e faturas da competência. Só o síndico.',
        responses: {
          200: resposta('Painel operacional.', EX_OPERACIONAL),
          400: falha('Síndico sem condomínio no cadastro.', 'Seu usuário não está vinculado a um condomínio.'),
          401: ERROS[401],
          403: falha('Não é síndico.', 'Os painéis do condomínio são do síndico.'),
        },
      },
    },
    '/api/gestao/painel/estrategico': {
      get: {
        tags: ['Painéis do síndico'],
        summary: 'Painel estratégico',
        description: 'Séries mensais da janela: reservas por mês e por área, ocorrências abertas e resolvidas, inadimplência e multas. Só o síndico.',
        parameters: [{
          name: 'meses', in: 'query', required: false, description: 'Janela: 3, 6 ou 12 meses (padrão 6).',
          schema: { type: 'integer', enum: [3, 6, 12] }, example: 6,
        }],
        responses: {
          200: resposta('Painel estratégico.', EX_ESTRATEGICO),
          400: falha('Janela fora das permitidas.', 'Use meses=3, 6 ou 12.'),
          401: ERROS[401],
          403: falha('Não é síndico.', 'Os painéis do condomínio são do síndico.'),
        },
      },
    },
    '/api/gestao/preferencias/{chave}': {
      get: {
        tags: ['Preferências'],
        summary: 'Ler a preferência de um painel',
        description: 'A configuração que quem chama salvou para o painel, ou `valor: null` se nunca personalizou (a tela usa o padrão). O token é conferido e a sessão é confirmada no auth-api; o usuário sai dessa confirmação, nunca da URL. Qualquer perfil, só a própria.',
        parameters: [chavePreferencia],
        responses: {
          200: resposta('Preferência.', { sucesso: true, chave: 'painel.condominio.operacional', valor: EX_PREFERENCIA, atualizadoEm: '2026-10-27T18:20:11.204Z' }),
          ...ERROS_PREFERENCIA,
        },
      },
      put: {
        tags: ['Preferências'],
        summary: 'Salvar a preferência de um painel',
        description: 'Grava (ou substitui) a configuração do painel para quem chama: blocos visíveis, ordem, largura, altura, tipo de gráfico e cor. Até 8 KB. Qualquer perfil, só a própria.',
        parameters: [chavePreferencia],
        requestBody: {
          required: true,
          content: json({ valor: EX_PREFERENCIA }),
        },
        responses: {
          200: resposta('Salva.', { sucesso: true, chave: 'painel.condominio.operacional', valor: EX_PREFERENCIA }),
          ...ERROS_PREFERENCIA,
        },
      },
      delete: {
        tags: ['Preferências'],
        summary: 'Voltar o painel ao padrão',
        description: 'Apaga a preferência de quem chama para o painel; ele volta a aparecer no padrão da tela. Qualquer perfil, só a própria.',
        parameters: [chavePreferencia],
        responses: {
          200: resposta('Removida.', { sucesso: true, chave: 'painel.condominio.operacional', valor: null }),
          ...ERROS_PREFERENCIA,
        },
      },
    },
    '/docs': {
      get: {
        tags: ['Documentação'],
        summary: 'Portal de documentação',
        description: 'Página com o sumário dos serviços e o Swagger UI de todos eles, reunidos. Pública.',
        security: [],
        responses: { 200: { description: 'Página HTML.', content: { 'text/html': { example: '<!doctype html><title>Mora — documentação da API</title>…' } } } },
      },
    },
    '/api/gestao/docs/openapi.json': {
      get: {
        tags: ['Documentação'],
        summary: 'Esta especificação',
        description: 'A especificação OpenAPI do gestao-geral, em JSON. Pública.',
        security: [],
        responses: { 200: resposta('Especificação OpenAPI.', { openapi: '3.0.3', info: { title: 'gestao-geral', version: '1.0' } }) },
      },
    },
    '/health': {
      get: {
        tags: ['Saúde'],
        summary: 'Saúde do serviço',
        description: 'Usada pelo Consul. Fora do gateway: responde só na porta do serviço.',
        security: [],
        servers: [{ url: 'http://localhost:3002', description: 'Porta do serviço' }],
        responses: { 200: resposta('No ar.', { status: 'ok', servico: 'gestao-geral' }) },
      },
    },
  },
};
