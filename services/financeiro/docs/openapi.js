/**
 * Especificação OpenAPI do financeiro.
 *
 * Escrita à mão: o que importa documentar é a regra de cada rota (quem pode,
 * o que é validado) e a forma da resposta, que só um exemplo mostra. Os
 * exemplos partem de respostas da stack de desenvolvimento, com os ids do seed
 * (`scripts/seed-dev.mjs`): condomínio `cond-parque-verde`, unidade 101 do
 * Bloco A. Onde o ambiente não tinha dado (nenhuma multa, nenhum contrato),
 * o exemplo segue as colunas que o model devolve.
 *
 * `tests/documentacao.test.js` compara esta especificação com as rotas do
 * Express: rota nova sem documentação quebra os testes.
 */

// ── Ajudantes ───────────────────────────────────────────────────────────────

const json = (exemplo) => ({ 'application/json': { example: exemplo } });
const resposta = (description, exemplo) => ({ description, content: json(exemplo) });
const falha = (description, mensagem) => resposta(description, { sucesso: false, mensagem });
const corpo = (description, exemplo, schema) => ({
  required: true, description, content: { 'application/json': { schema, example: exemplo } },
});

const E401 = falha('Sem token, ou token inválido ou expirado.', 'Token não fornecido');
const E403 = falha('O perfil de quem chama não tem acesso.', 'Acesso negado para este perfil.');
const E404 = (oque) => falha(`${oque} não encontrado(a) no condomínio de quem chama.`, `${oque} não encontrado(a).`);
const E400 = (mensagem) => falha('Dado inválido.', mensagem);

const pId = (nome, description, example) => ({ name: nome, in: 'path', required: true, description, schema: { type: 'string' }, example });
const pQuery = (nome, description, example, schema = { type: 'string' }) => ({ name: nome, in: 'query', required: false, description, schema, example });
const competenciaPath = pId('competencia', 'Competência no formato AAAA-MM.', '2026-09');

const GESTAO = 'Perfis: ADMIN_GERAL (lê) e ADMIN_SINDICO.';
const SINDICO = 'Perfil: ADMIN_SINDICO.';
const UNIDADE = 'Perfis: MORADOR e DONO_ALUGUEL, sempre sobre a própria unidade.';

// ── Ids do seed, usados nos exemplos ────────────────────────────────────────

const COND = 'cond-parque-verde';
const UNIDADE_101 = 'e064d8a3-2835-4bdf-8e9f-06fa3a108e6d';
const BLOCO_A = '5c27d9b1-3cf7-4cef-8128-ec4011d93434';
const FATURA = '1b743401-2fda-41a4-8a98-0206ea296125';
const TIPO_TAXA = '44677112-9ca6-4edd-8266-5cb829065d99';
const CONTA = '3b4eb550-9f4e-4b17-9bb1-db99743472f0';
const MULTA = '9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9';
const CONTRATO = 'd2a7c9e1-3f4b-4c8d-9e6a-7b1f0c2d5e83';
const LANCAMENTO = 'a6e3b1d4-8c2f-4e7a-b9d0-3f5c1e8a2b7d';

// ── Exemplos ────────────────────────────────────────────────────────────────

const EX_CONFIG = {
  condominioId: COND, modo: 'FIXO', diaFechamento: 25, diaVencimento: 10, diasRecursoMulta: 15, incluirTaxaPlataforma: true,
};
const EX_TIPO = {
  id: TIPO_TAXA, condominioId: COND, nome: 'Fundo de reserva', descricao: 'Reserva para obras e emergências',
  valorCentavos: 5000, baseCalculo: 'POR_UNIDADE', periodicidade: 'MENSAL', extraordinaria: false, parcelas: 1,
  ativo: true, criadoEm: '2026-09-09T20:46:27.343Z',
};
const EX_FRACAO = { id: UNIDADE_101, numero: '101', andar: 1, blocoId: BLOCO_A, blocoNome: 'Bloco A', area: 62.5, milesimos: 174 };
const EX_CONTA = {
  id: CONTA, condominioId: COND, tipo: 'AGUA', descricao: 'Conta de água do prédio', competencia: '2026-09-01T03:00:00.000Z',
  valorTotalCentavos: 200000, modoRateio: 'FIXO_POR_UNIDADE', vencimento: '2026-09-30T03:00:00.000Z', status: 'PENDENTE',
  comprovanteUrl: null, criadoPorId: 2, criadoEm: '2026-09-09T20:52:36.959Z', atualizadoEm: '2026-09-09T20:52:36.959Z',
};
const EX_FATURA = {
  id: FATURA, condominioId: COND, unidadeId: UNIDADE_101, competencia: '2026-09-01T03:00:00.000Z',
  vencimento: '2026-09-10T03:00:00.000Z', valorCentavos: 82840, status: 'EM_ATRASO', responsavelUsuarioId: 4,
  pagoEm: null, valorPagoCentavos: null, formaBaixa: null, criadoEm: '2026-09-09T20:52:43.955Z', atualizadoEm: '2026-09-17T04:00:00.320Z',
};
// PIX de exemplo, com o condomínio fictício como recebedor.
const PIX = '00020101021226820014br.gov.bcb.pix2560pix-h.asaas.com/qr/cobv/ca31fb18-012f-44dc-a89a-3703534cfa71'
  + '5204000053039865802BR5925Residencial Parque Verde6008Curitiba62070503***6304A1B2';
const EX_COBRANCA = {
  id: '796b2c46-6659-45a7-a98c-974e143fabce', faturaId: FATURA, condominioId: COND, asaasId: 'pay_c6gm14r22ejzs4nc',
  billingType: 'PIX', status: 'PENDENTE', valorCentavos: 82840, vencimento: '2026-09-10T03:00:00.000Z', urlBoleto: null,
  pixPayload: PIX, pixQrcode: 'iVBORw0KGgoAAAANSUhEUgAAAcIAAAHCAQAAAABUY/ToAAADEklEQVR4Xu2TS67bUAxDPfP+d9Rlefb6eEg5...',
  criadoEm: '2026-09-10T22:48:01.491Z',
};
const EX_MULTA = {
  id: MULTA, condominioId: COND, unidadeId: UNIDADE_101, usuarioId: 4, valorCentavos: 15000,
  motivo: 'Barulho após as 22h em dia de semana', dataInfracao: '2026-10-12', status: 'APLICADA', ocorrenciaId: null,
  aplicadaPorId: 2, recursoTexto: null, recursoEm: null, julgadoPorId: null, julgadoEm: null, julgamentoJustificativa: null,
  faturaItemId: null, estornoFaturaItemId: null, criadoEm: '2026-10-13T14:20:05.112Z', atualizadoEm: '2026-10-13T14:20:05.112Z',
  prazoRecurso: '2026-10-28', podeRecorrer: true, aguardandoCobranca: false,
};
const EX_CONTRATO = {
  id: CONTRATO, condominioId: COND, unidadeId: UNIDADE_101, proprietarioUsuarioId: 6, inquilinoUsuarioId: 4,
  inicio: '2026-03-01', fim: '2027-02-28', valorAluguelCentavos: 220000, responsavelTaxas: 'INQUILINO', ativo: true,
  criadoEm: '2026-03-01T12:00:00.000Z', atualizadoEm: '2026-03-01T12:00:00.000Z',
};
const EX_LANCAMENTO = {
  id: LANCAMENTO, condominioId: COND, competencia: '2026-09', tipo: 'DESPESA', categoria: 'Manutenção',
  descricao: 'Troca da bomba da caixa d\'água', valorCentavos: 185000, dataLancamento: '2026-09-18',
  comprovanteUrl: null, criadoPorId: 2, criadoEm: '2026-09-18T16:02:11.000Z',
};
const EX_PRESTACAO = {
  competencia: '2026-09', status: 'RASCUNHO', publicadoEm: null, publicadoPorId: null, receitaTaxasCentavos: 331360,
  lancamentos: [EX_LANCAMENTO],
  totais: {
    receitasCentavos: 331360, despesasCentavos: 185000, saldoCentavos: 146360,
    porCategoria: [{ tipo: 'RECEITA', categoria: 'Taxas condominiais', valorCentavos: 331360 }, { tipo: 'DESPESA', categoria: 'Manutenção', valorCentavos: 185000 }],
  },
};

// ── Especificação ───────────────────────────────────────────────────────────

const ESTORNO = '7e1d3c5a-2b4f-4a6e-8c9d-0f1e2d3c4b5a';
const ASSINATURA = 'f3b8a1c2-4d5e-4f6a-9b7c-8d9e0f1a2b3c';
const PAGAMENTO_PLAT = '0c9d8e7f-6a5b-4c3d-2e1f-0a9b8c7d6e5f';
const EX_ESTORNO = {
  id: ESTORNO, condominioId: COND, faturaId: FATURA, pagamentoPlataformaId: null,
  asaasPagamentoId: 'pay_vzmknqbhb01tz8hf', valorCentavos: 82840, motivo: 'Valor da taxa cobrado em dobro',
  status: 'SOLICITADO', detalhe: null, solicitadoPorId: 2, solicitadoEm: '2026-10-28T13:10:00.000Z', concluidoEm: null,
};
const EX_PAGAMENTO_PLAT = {
  id: PAGAMENTO_PLAT, assinaturaId: ASSINATURA, asaasPagamentoId: 'pay_uhh6xsthuokakhpx', valorCentavos: 100000,
  vencimento: '2026-11-05', status: 'PENDENTE', forma: 'BOLETO', urlPagamento: 'https://sandbox.asaas.com/i/uhh6xsthuokakhpx',
  pagoEm: null, atualizadoEm: '2026-10-28T13:00:00.000Z',
};
const EX_ASSINATURA_PLAT = {
  id: ASSINATURA, condominioId: COND, planoNome: 'Completo', planoAssinaturaId: 7, asaasAssinaturaId: 'sub_ojd9nic5735hrci8',
  asaasClienteId: 'cus_000009708966', forma: 'BOLETO', valorCentavos: 100000, ciclo: 'MONTHLY', status: 'ATIVA',
  ativadaPorId: 1, criadoEm: '2026-10-28T13:00:00.000Z', canceladaEm: null, pagamentos: [EX_PAGAMENTO_PLAT],
};
const SO_PIX = falha('Fora da regra do gateway ou já estornado.', 'O gateway só estorna pagamentos feitos por PIX. Boleto pago se devolve por transferência, fora do sistema.');
const MOTIVO = corpo('Motivo do estorno, que vai para o gateway e para o aviso do morador.', { motivo: 'Valor da taxa cobrado em dobro' }, {
  type: 'object', required: ['motivo'], properties: { motivo: { type: 'string', minLength: 5, maxLength: 300 } },
});
const E502 = falha('O gateway não respondeu.', 'O gateway de pagamento não respondeu. Tente de novo em instantes.');

const op = (tag, summary, description, extra = {}) => ({ tags: [tag], summary, description, ...extra });

export const openapi = {
  openapi: '3.0.3',
  info: {
    title: 'financeiro',
    version: '1.0',
    description: [
      'Taxas, rateio, faturas mensais, multas, contratos de locação, prestação de contas e cobrança via Asaas',
      '(boleto e PIX, cobrança recorrente por assinatura e estorno), RF-15 a RF-17.',
      '',
      '- **Responsável:** João Victor',
      '- **Banco:** `mora_financeiro` (migrações com `node-pg-migrate`)',
      '- **Pelo gateway:** `/api/financeiro`',
      '- **Autenticação:** JWT do auth-api (`POST /api/auth/login`), no botão **Authorize**. O condomínio e a',
      '  unidade saem do token e do auth-api, nunca do corpo ou da query.',
      '- **Valores:** sempre em centavos (`15000` = R$ 150,00). Onde a rota aceita `valor`, aceita também texto',
      '  em reais (`"150,00"`).',
      '',
      'Os exemplos usam o condomínio Residencial Parque Verde do seed de desenvolvimento.',
    ].join('\n'),
  },
  servers: [{ url: '/', description: 'Gateway (Traefik)' }],
  tags: [
    { name: 'Escopo', description: 'O que o serviço resolveu sobre quem chama.' },
    { name: 'Regras e taxas', description: 'Regras de cobrança do condomínio e os tipos de taxa (RF-15).' },
    { name: 'Frações ideais', description: 'Milésimos de cada unidade, usados no rateio por fração ideal.' },
    { name: 'Contas de consumo', description: 'Água, luz, gás: lançadas pelo síndico e rateadas entre as unidades.' },
    { name: 'Faturas', description: 'Faturas mensais da unidade, fechamento da competência e baixa (RF-16).' },
    { name: 'Gateway de pagamento', description: 'Integração com o Asaas: cobrança PIX e boleto. As rotas de teste só rodam no sandbox.' },
    { name: 'Webhooks', description: 'Retorno do Asaas sobre pagamentos. Chamado pelo gateway, não pelo front.' },
    { name: 'Multas', description: 'Aplicação, recurso do morador e julgamento pelo síndico.' },
    { name: 'Contratos de locação', description: 'Quem é inquilino de qual unidade, e quem paga a taxa.' },
    { name: 'Prestação de contas', description: 'Receitas e despesas da competência, publicadas para os moradores (RF-17).' },
    { name: 'Estornos', description: 'Devolução, pelo Asaas, de um pagamento recebido por PIX. Assíncrona: o pedido é aceito na hora e se conclui quando o gateway confirma (webhook PAYMENT_REFUNDED ou conciliação).' },
    { name: 'Cobrança recorrente da plataforma', description: 'A mensalidade do Mora cobrada do condomínio por assinatura no Asaas: o gateway gera a cobrança de cada mês sozinho. O Admin Geral ativa e acompanha; o síndico vê e paga.' },
    { name: 'Documentação', description: 'Esta especificação.' },
    { name: 'Saúde', description: 'Verificação usada pelo Consul.' },
  ],
  components: {
    securitySchemes: {
      bearerAuth: { type: 'http', scheme: 'bearer', bearerFormat: 'JWT', description: 'Token devolvido por POST /api/auth/login no auth-api.' },
    },
  },
  security: [{ bearerAuth: [] }],
  paths: {
    '/api/financeiro/contexto': {
      get: op('Escopo', 'Escopo de quem chama', `Condomínio, usuário e unidade que o serviço resolveu a partir do token e do auth-api. Serve para conferir de fora que o escopo não vem da query. Qualquer perfil autenticado.`, {
        responses: {
          200: resposta('Escopo resolvido.', { sucesso: true, perfil: 'ADMIN_SINDICO', escopo: { condominioId: COND, usuarioId: 2, unidadeId: null, responsavelFinanceiro: false, somenteLeitura: false } }),
          401: E401,
        },
      }),
    },
    '/api/financeiro/config': {
      get: op('Regras e taxas', 'Regras de cobrança', `Modo de rateio, dias de fechamento e vencimento, prazo de recurso de multa e se a taxa da plataforma entra na fatura. ${GESTAO}`, {
        responses: { 200: resposta('Regras.', { sucesso: true, config: EX_CONFIG }), 401: E401, 403: E403 },
      }),
      put: op('Regras e taxas', 'Atualizar regras de cobrança', `Envie só o que muda. Dias de fechamento e vencimento entre 1 e 28. ${SINDICO}`, {
        requestBody: corpo('Campos a alterar.', { modo: 'FRACAO_IDEAL', diaFechamento: 25, diaVencimento: 10, diasRecursoMulta: 15, incluirTaxaPlataforma: true }, {
          type: 'object',
          properties: {
            modo: { type: 'string', enum: ['FIXO', 'FRACAO_IDEAL'] },
            diaFechamento: { type: 'integer', minimum: 1, maximum: 28 },
            diaVencimento: { type: 'integer', minimum: 1, maximum: 28 },
            diasRecursoMulta: { type: 'integer', minimum: 0 },
            incluirTaxaPlataforma: { type: 'boolean' },
          },
        }),
        responses: { 200: resposta('Regras atualizadas.', { sucesso: true, config: { ...EX_CONFIG, modo: 'FRACAO_IDEAL' } }), 400: E400('Dia de vencimento deve estar entre 1 e 28.'), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/tipos-taxa': {
      get: op('Regras e taxas', 'Listar tipos de taxa', `Taxas que entram na fatura. Por padrão só as ativas; \`todos=true\` inclui as desativadas. ${GESTAO}`, {
        parameters: [pQuery('todos', 'Incluir as desativadas.', 'false', { type: 'string', enum: ['true', 'false'] })],
        responses: { 200: resposta('Tipos de taxa.', { sucesso: true, total: 1, itens: [EX_TIPO] }), 401: E401, 403: E403 },
      }),
      post: op('Regras e taxas', 'Criar tipo de taxa', `\`baseCalculo\`: POR_UNIDADE cobra o valor de cada unidade; TOTAL_CONDOMINIO divide o valor entre as unidades. ${SINDICO}`, {
        requestBody: corpo('Nova taxa.', {
          nome: 'Fundo de reserva', descricao: 'Reserva para obras e emergências', valor: '50,00',
          baseCalculo: 'POR_UNIDADE', periodicidade: 'MENSAL', extraordinaria: false, parcelas: 1,
        }, {
          type: 'object', required: ['nome', 'valor', 'baseCalculo', 'periodicidade'],
          properties: {
            nome: { type: 'string' }, descricao: { type: 'string' }, valor: { type: 'string', description: 'Em reais; ou envie valorCentavos.' },
            baseCalculo: { type: 'string', enum: ['POR_UNIDADE', 'TOTAL_CONDOMINIO'] }, periodicidade: { type: 'string', enum: ['MENSAL', 'ANUAL', 'UNICA'] },
            extraordinaria: { type: 'boolean' }, parcelas: { type: 'integer', minimum: 1 },
          },
        }),
        responses: { 201: resposta('Taxa criada.', { sucesso: true, tipo: EX_TIPO }), 400: E400('Informe o nome da taxa.'), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/tipos-taxa/{id}': {
      put: op('Regras e taxas', 'Atualizar tipo de taxa', `Envie só o que muda. Faturas já emitidas não mudam. ${SINDICO}`, {
        parameters: [pId('id', 'Id do tipo de taxa.', TIPO_TAXA)],
        requestBody: corpo('Campos a alterar.', { valor: '55,00', descricao: 'Reserva para obras, reajustada em outubro' }, { type: 'object' }),
        responses: { 200: resposta('Taxa atualizada.', { sucesso: true, tipo: { ...EX_TIPO, valorCentavos: 5500 } }), 401: E401, 403: E403, 404: E404('Tipo de taxa') },
      }),
      delete: op('Regras e taxas', 'Desativar tipo de taxa', `Desativa em vez de apagar: as faturas antigas continuam apontando para ela. ${SINDICO}`, {
        parameters: [pId('id', 'Id do tipo de taxa.', TIPO_TAXA)],
        responses: { 200: resposta('Taxa desativada.', { sucesso: true, tipo: { ...EX_TIPO, ativo: false } }), 401: E401, 403: E403, 404: E404('Tipo de taxa') },
      }),
    },
    '/api/financeiro/fracoes': {
      get: op('Frações ideais', 'Frações das unidades', `Cada unidade com área e milésimos, e um resumo dizendo se a soma fecha em 1000. ${GESTAO}`, {
        responses: {
          200: resposta('Frações.', { sucesso: true, itens: [EX_FRACAO], resumo: { unidades: 5, comFracao: 4, faltando: 1, soma: 1000, somaEsperada: 1000, completo: false } }),
          401: E401, 403: E403, 503: falha('O portaria não respondeu com as unidades.', 'Não foi possível obter as unidades.'),
        },
      }),
    },
    '/api/financeiro/fracoes/{unidadeId}': {
      put: op('Frações ideais', 'Definir fração de uma unidade', `Milésimos da unidade (inteiro maior que zero). ${SINDICO}`, {
        parameters: [pId('unidadeId', 'Id da unidade (portaria).', UNIDADE_101)],
        requestBody: corpo('Milésimos.', { milesimos: 174 }, { type: 'object', required: ['milesimos'], properties: { milesimos: { type: 'integer', minimum: 1 } } }),
        responses: { 200: resposta('Fração gravada.', { sucesso: true, fracao: { condominioId: COND, unidadeId: UNIDADE_101, milesimos: 174 } }), 400: E400('Informe os milésimos (inteiro maior que zero).'), 401: E401, 403: E403 },
      }),
      delete: op('Frações ideais', 'Remover fração de uma unidade', `A unidade volta a ficar sem fração. ${SINDICO}`, {
        parameters: [pId('unidadeId', 'Id da unidade (portaria).', UNIDADE_101)],
        responses: { 200: resposta('Fração removida.', { sucesso: true }), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/fracoes/propor-por-area': {
      get: op('Frações ideais', 'Propor frações pela área', `Calcula milésimos proporcionais à área de cada unidade, somando 1000. Só propõe: para gravar, use \`aplicar-lote\`. ${GESTAO}`, {
        responses: { 200: resposta('Proposta.', { sucesso: true, proposta: [{ ...EX_FRACAO, milesimos: 146 }], soma: 1000 }), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/fracoes/aplicar-lote': {
      post: op('Frações ideais', 'Gravar várias frações', `Grava a lista inteira de uma vez, numa transação. ${SINDICO}`, {
        requestBody: corpo('Frações a gravar.', { fracoes: [{ unidadeId: UNIDADE_101, milesimos: 146 }, { unidadeId: '35d0a934-bd1f-4ccb-ab21-4bca248f8f12', milesimos: 146 }] }, {
          type: 'object', required: ['fracoes'],
          properties: { fracoes: { type: 'array', items: { type: 'object', properties: { unidadeId: { type: 'string' }, milesimos: { type: 'integer' } } } } },
        }),
        responses: { 200: resposta('Frações gravadas.', { sucesso: true, aplicadas: 2 }), 400: E400('Nenhuma fração enviada.'), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/gateway/status': {
      get: op('Gateway de pagamento', 'Situação do gateway', `Se a chave do Asaas está configurada e se aponta para o sandbox. Mostra só os 6 últimos caracteres da chave. ${GESTAO}`, {
        responses: { 200: resposta('Situação.', { sucesso: true, configurado: true, sandbox: true, baseUrl: 'https://api-sandbox.asaas.com/v3', chaveFinal: 'QzZDEz' }), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/gateway/teste': {
      post: op('Gateway de pagamento', 'Emitir cobrança de teste', `Emite uma cobrança real no **sandbox** do Asaas, para conferir a integração. Recusa fora do sandbox. Valor mínimo R$ 5,00. ${GESTAO}`, {
        requestBody: corpo('Forma e valor.', { forma: 'PIX', valor: '350,00' }, {
          type: 'object', properties: { forma: { type: 'string', enum: ['PIX', 'BOLETO'] }, valor: { type: 'string' } },
        }),
        responses: {
          201: resposta('Cobrança emitida.', {
            sucesso: true,
            cobranca: { id: 'pay_8h2k4m6n9p1q3r5s', forma: 'PIX', status: 'PENDING', valorCentavos: 35000, valorFormatado: 'R$ 350,00', vencimento: '2026-10-27', linkPagamento: 'https://sandbox.asaas.com/i/8h2k4m6n9p1q3r5s' },
            pix: { payload: PIX, imagem: 'iVBORw0KGgoAAAANSUhEUgAAAcIAAAHC...' },
          }),
          400: E400('Informe um valor válido de no mínimo R$ 5,00.'),
          401: E401,
          403: falha('Fora do sandbox, ou perfil sem acesso.', 'O teste de cobrança só roda contra o sandbox do gateway.'),
          502: falha('O Asaas recusou.', 'Gateway: invalid_customer'),
          503: falha('Sem chave do Asaas.', 'Gateway sem credencial. Defina ASAAS_API_KEY.'),
        },
      }),
    },
    '/api/financeiro/gateway/teste/{id}': {
      get: op('Gateway de pagamento', 'Consultar cobrança de teste', `Situação de uma cobrança no Asaas. ${GESTAO}`, {
        parameters: [pId('id', 'Id da cobrança no Asaas.', 'pay_8h2k4m6n9p1q3r5s')],
        responses: { 200: resposta('Situação.', { sucesso: true, cobranca: { id: 'pay_8h2k4m6n9p1q3r5s', status: 'PENDING', deletada: false, pagoEm: null } }), 401: E401, 403: E403, 502: falha('O Asaas recusou.', 'Cobrança não encontrada.') },
      }),
      delete: op('Gateway de pagamento', 'Cancelar cobrança de teste', `Cancela a cobrança no Asaas. ${GESTAO}`, {
        parameters: [pId('id', 'Id da cobrança no Asaas.', 'pay_8h2k4m6n9p1q3r5s')],
        responses: { 200: resposta('Cancelada.', { sucesso: true }), 401: E401, 403: E403, 502: falha('O Asaas recusou.', 'Cobrança não encontrada.') },
      }),
    },
    '/api/financeiro/contas-consumo': {
      get: op('Contas de consumo', 'Listar contas de consumo', `Contas do condomínio, filtráveis pela competência. ${GESTAO}`, {
        parameters: [pQuery('competencia', 'Competência AAAA-MM.', '2026-09')],
        responses: { 200: resposta('Contas.', { sucesso: true, total: 1, itens: [EX_CONTA] }), 401: E401, 403: E403 },
      }),
      post: op('Contas de consumo', 'Lançar conta de consumo', `A conta entra no próximo fechamento, ou é rateada já com \`/ratear\`. ${SINDICO}`, {
        requestBody: corpo('Nova conta.', { tipo: 'AGUA', descricao: 'Conta de água do prédio', competencia: '2026-09', valor: '2000,00', modoRateio: 'FIXO_POR_UNIDADE', vencimento: '2026-09-30' }, {
          type: 'object', required: ['tipo', 'competencia', 'valor', 'modoRateio'],
          properties: {
            tipo: { type: 'string', enum: ['AGUA', 'LUZ', 'GAS', 'INTERNET', 'OUTRO'] }, descricao: { type: 'string' }, competencia: { type: 'string', description: 'AAAA-MM' },
            valor: { type: 'string' }, modoRateio: { type: 'string', enum: ['FRACAO_IDEAL', 'FIXO_POR_UNIDADE'] }, vencimento: { type: 'string', format: 'date' }, comprovanteUrl: { type: 'string' },
          },
        }),
        responses: { 201: resposta('Conta lançada.', { sucesso: true, conta: EX_CONTA }), 400: E400('Tipo inválido. Use: AGUA, LUZ, GAS, INTERNET, OUTRO.'), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/contas-consumo/{id}': {
      put: op('Contas de consumo', 'Atualizar conta de consumo', `Só enquanto a conta não foi rateada. ${SINDICO}`, {
        parameters: [pId('id', 'Id da conta.', CONTA)],
        requestBody: corpo('Campos a alterar.', { valor: '2150,00', descricao: 'Conta de água do prédio (corrigida)' }, { type: 'object' }),
        responses: { 200: resposta('Conta atualizada.', { sucesso: true, conta: { ...EX_CONTA, valorTotalCentavos: 215000 } }), 401: E401, 403: E403, 404: E404('Conta'), 409: falha('Já rateada.', 'Conta já rateada não pode ser alterada.') },
      }),
      delete: op('Contas de consumo', 'Cancelar conta de consumo', `Marca a conta como cancelada. ${SINDICO}`, {
        parameters: [pId('id', 'Id da conta.', CONTA)],
        responses: { 200: resposta('Conta cancelada.', { sucesso: true, conta: { ...EX_CONTA, status: 'CANCELADA' } }), 401: E401, 403: E403, 404: E404('Conta') },
      }),
    },
    '/api/financeiro/contas-consumo/{id}/ratear': {
      post: op('Contas de consumo', 'Ratear conta de consumo', `Distribui o valor nas faturas abertas da competência, pelo modo da conta. Sem fatura aberta, a conta entra no próximo fechamento. ${SINDICO}`, {
        parameters: [pId('id', 'Id da conta.', CONTA)],
        'x-sem-corpo': true,
        responses: { 200: resposta('Conta rateada.', { sucesso: true, ok: true, contaId: CONTA, unidadesRateadas: 5 }), 401: E401, 403: E403, 404: E404('Conta') },
      }),
    },
    '/api/financeiro/faturas': {
      get: op('Faturas', 'Minhas faturas', `Faturas da unidade de quem chama. ${UNIDADE}`, {
        responses: { 200: resposta('Faturas.', { sucesso: true, total: 1, faturas: [EX_FATURA] }), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/faturas/{id}': {
      get: op('Faturas', 'Detalhe da fatura', `A fatura, os itens que a compõem e a cobrança emitida, se houver. ${UNIDADE}`, {
        parameters: [pId('id', 'Id da fatura.', FATURA)],
        responses: {
          200: resposta('Fatura.', {
            sucesso: true, fatura: EX_FATURA,
            itens: [
              { id: '7b4aa2cd-74b1-4617-8561-cfc3219242df', tipo: 'TAXA', descricao: 'Fundo de reserva', valorCentavos: 5000, origemId: null },
              { id: 'ca9b5fd0-f948-4b4c-90d8-2f7cc2457426', tipo: 'CONTA_CONSUMO', descricao: 'AGUA: Conta de água do prédio', valorCentavos: 50000, origemId: CONTA },
            ],
            cobranca: EX_COBRANCA,
          }),
          401: E401, 403: E403, 404: E404('Fatura'),
        },
      }),
    },
    '/api/financeiro/gastos': {
      get: op('Faturas', 'Meus gastos', `Itens das faturas da unidade, um a um, para os gráficos de gasto. Filtrável por ano. ${UNIDADE}`, {
        parameters: [pQuery('ano', 'Ano das faturas.', 2026, { type: 'integer' })],
        responses: {
          200: resposta('Itens.', {
            sucesso: true, total: 1,
            itens: [{ id: 'ca9b5fd0-f948-4b4c-90d8-2f7cc2457426', tipo: 'CONTA_CONSUMO', descricao: 'AGUA: Conta de água do prédio', valorCentavos: 50000, faturaId: FATURA, competencia: '2026-09-01T03:00:00.000Z', faturaStatus: 'EM_ATRASO', vencimento: '2026-09-10T03:00:00.000Z' }],
          }),
          401: E401, 403: E403,
        },
      }),
    },
    '/api/financeiro/faturas/{id}/pagar': {
      post: op('Faturas', 'Gerar cobrança da fatura', `Emite a cobrança no Asaas (PIX ou boleto). Se já existe uma da mesma forma, devolve a existente com 200. Só fatura ABERTA ou EM_ATRASO. ${UNIDADE}`, {
        parameters: [pId('id', 'Id da fatura.', FATURA)],
        requestBody: corpo('Forma de pagamento (padrão PIX).', { forma: 'PIX' }, { type: 'object', properties: { forma: { type: 'string', enum: ['PIX', 'BOLETO'] } } }),
        responses: {
          200: resposta('Cobrança já existente.', { sucesso: true, cobranca: EX_COBRANCA }),
          201: resposta('Cobrança emitida.', { sucesso: true, cobranca: EX_COBRANCA }),
          400: E400('Forma inválida. Use PIX ou BOLETO.'), 401: E401, 403: E403, 404: E404('Fatura'),
          409: falha('Fatura já paga ou cancelada.', 'Fatura com status PAGA não pode ser paga.'),
        },
      }),
    },
    '/api/financeiro/admin/faturas': {
      get: op('Faturas', 'Faturas do condomínio', `Todas as faturas, filtráveis por competência, status e unidade. ${GESTAO}`, {
        parameters: [
          pQuery('competencia', 'Competência AAAA-MM.', '2026-09'),
          pQuery('status', 'Status da fatura.', 'EM_ATRASO', { type: 'string', enum: ['ABERTA', 'PAGA', 'EM_ATRASO', 'CANCELADA'] }),
          pQuery('unidadeId', 'Id da unidade.', UNIDADE_101),
        ],
        responses: { 200: resposta('Faturas.', { sucesso: true, total: 1, faturas: [EX_FATURA] }), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/admin/faturas/kpis': {
      get: op('Faturas', 'Indicadores da competência', `Total, pagas, abertas e em atraso, agregados em SQL. ${GESTAO}`, {
        parameters: [pQuery('competencia', 'Competência AAAA-MM (obrigatória).', '2026-09')],
        responses: {
          200: resposta('Indicadores.', { sucesso: true, total: 4, totalCentavos: 380000, pagas: 1, pagasCentavos: 82840, abertas: 2, emAtraso: 1 }),
          400: E400('Informe a competência.'), 401: E401, 403: E403,
        },
      }),
    },
    '/api/financeiro/admin/faturas/kpis/por-mes': {
      get: op('Faturas', 'Indicadores mês a mês', `Os mesmos indicadores para cada mês da janela, numa consulta agrupada (função \`kpis_faturas_por_mes\` no banco). Mês sem fatura vem com zeros. Usado pelo painel estratégico do síndico. ${GESTAO}`, {
        parameters: [pQuery('meses', 'Janela: 3, 6 ou 12 (padrão 6).', 3, { type: 'integer', enum: [3, 6, 12] })],
        responses: {
          200: resposta('Série mensal.', {
            sucesso: true,
            meses: [
              { mes: '2026-08', total: 0, totalCentavos: 0, pagas: 0, pagasCentavos: 0, abertas: 0, emAtraso: 0 },
              { mes: '2026-09', total: 4, totalCentavos: 380000, pagas: 3, pagasCentavos: 285000, abertas: 1, emAtraso: 1 },
              { mes: '2026-10', total: 4, totalCentavos: 380000, pagas: 1, pagasCentavos: 82840, abertas: 3, emAtraso: 0 },
            ],
          }),
          400: E400('Use meses=3, 6 ou 12.'), 401: E401, 403: E403,
        },
      }),
    },
    '/api/financeiro/admin/fechamento/preview': {
      get: op('Faturas', 'Prévia do fechamento', `O que o fechamento da competência faria, sem gravar nada: unidades, faturas novas, taxas, contas, multas e estornos. ${GESTAO}`, {
        parameters: [pQuery('competencia', 'Competência AAAA-MM (obrigatória).', '2026-10')],
        responses: {
          200: resposta('Prévia.', { sucesso: true, ok: true, unidades: 5, faturasNovas: 5, faturasExistentes: 0, taxas: 2, contasDeConsumo: 1, multasACobrar: 1, estornosDeMulta: 0, modoRateio: 'FIXO', fracoesCadastradas: 4, totalMilesimos: 1000 }),
          400: E400('Informe a competência.'), 401: E401, 403: E403,
        },
      }),
    },
    '/api/financeiro/fechar-competencia': {
      post: op('Faturas', 'Fechar a competência', `Gera as faturas do mês para todas as unidades, com taxas, contas rateadas e multas vencidas. Idempotente: unidade que já tem fatura é pulada. Cada fatura nova notifica o morador. ${SINDICO}`, {
        requestBody: corpo('Competência a fechar.', { competencia: '2026-10' }, { type: 'object', required: ['competencia'], properties: { competencia: { type: 'string', description: 'AAAA-MM' } } }),
        responses: {
          200: resposta('Fechamento feito.', { sucesso: true, ok: true, competencia: '2026-10', unidades: 5, faturasCriadas: 5, faturasExistentes: 0, erros: 0, detalhe: [{ unidadeId: UNIDADE_101, status: 'ok', faturaId: FATURA }] }),
          400: E400('Competência inválida. Use AAAA-MM.'), 401: E401, 403: E403,
        },
      }),
    },
    '/api/financeiro/admin/faturas/{id}/baixa-manual': {
      patch: op('Faturas', 'Dar baixa manual', `Marca a fatura como paga fora do gateway (dinheiro, transferência). Sem corpo, usa a data de agora e o valor da fatura. ${SINDICO}`, {
        parameters: [pId('id', 'Id da fatura.', FATURA)],
        requestBody: corpo('Data e valor pago (opcionais).', { pagoEm: '2026-10-08T14:30:00.000Z', valorCentavos: 82840 }, {
          type: 'object', properties: { pagoEm: { type: 'string', format: 'date-time' }, valorCentavos: { type: 'integer' } },
        }),
        responses: {
          200: resposta('Baixa registrada.', { sucesso: true, fatura: { ...EX_FATURA, status: 'PAGA', pagoEm: '2026-10-08T14:30:00.000Z', valorPagoCentavos: 82840, formaBaixa: 'MANUAL' } }),
          401: E401, 403: E403, 404: E404('Fatura'), 409: falha('Fatura já paga ou cancelada.', 'Fatura com status PAGA não aceita baixa.'),
        },
      }),
    },
    '/api/financeiro/admin/faturas/{id}/estornar': {
      post: op('Estornos', 'Estornar fatura paga', `Pede ao Asaas a devolução integral do pagamento da fatura. Só fatura PAGA pelo gateway e por PIX: o Asaas não estorna boleto, e a baixa manual não passou por ele. A fatura continua PAGA até o gateway confirmar; aí vira ESTORNADA, sai dos totais, libera a competência para reemissão, e o morador recebe \`PAGAMENTO_ESTORNADO\`. Responde 202 enquanto o pedido está em aberto (a conta pode exigir autorização da ação no painel do Asaas). ${SINDICO}`, {
        parameters: [pId('id', 'Id da fatura.', FATURA)],
        requestBody: MOTIVO,
        responses: {
          200: resposta('Estorno concluído na hora.', { sucesso: true, estorno: { ...EX_ESTORNO, status: 'CONCLUIDO', concluidoEm: '2026-10-28T13:10:02.000Z' } }),
          202: resposta('Pedido aceito pelo gateway, aguardando a devolução.', { sucesso: true, estorno: EX_ESTORNO }),
          400: E400('Explique o motivo do estorno (mínimo de 5 caracteres).'), 401: E401, 403: E403, 404: E404('Fatura'),
          409: SO_PIX, 502: E502,
        },
      }),
    },
    '/api/financeiro/admin/estornos': {
      get: op('Estornos', 'Estornos do condomínio', `Pedidos de estorno das faturas, com a competência e a unidade. Antes de responder, pergunta ao Asaas pelos pedidos em aberto, o que conclui os que o webhook não alcançou. ${GESTAO}`, {
        responses: { 200: resposta('Estornos.', { sucesso: true, total: 1, estornos: [{ ...EX_ESTORNO, competencia: '2026-09-01T03:00:00.000Z', unidadeId: UNIDADE_101, faturaStatus: 'PAGA' }] }), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/plataforma/assinaturas': {
      post: op('Cobrança recorrente da plataforma', 'Ativar cobrança recorrente', 'Cria no Asaas uma assinatura mensal para o condomínio pagar a mensalidade do Mora. Plano e valor vêm da assinatura vigente no plan-service; o pagador é o condomínio, pelo CNPJ cadastrado no auth-api. O Asaas gera a primeira cobrança no `proximoVencimento` e as seguintes todo mês, sozinho. Uma ativa por condomínio. Perfil: ADMIN_GERAL.', {
        requestBody: corpo('Condomínio, forma de pagamento e primeiro vencimento.', { condominioId: COND, forma: 'BOLETO', proximoVencimento: '2026-11-05' }, {
          type: 'object', required: ['condominioId'],
          properties: {
            condominioId: { type: 'string' }, forma: { type: 'string', enum: ['BOLETO', 'PIX'], description: 'Padrão BOLETO.' },
            proximoVencimento: { type: 'string', format: 'date', description: 'Hoje ou depois. Padrão: hoje.' },
          },
        }),
        responses: {
          201: resposta('Assinatura criada, com a primeira cobrança.', { sucesso: true, assinatura: EX_ASSINATURA_PLAT }),
          400: E400('Cadastre o CNPJ do condomínio: o gateway exige o documento do pagador.'), 401: E401, 403: E403,
          404: falha('Condomínio inexistente.', 'Condomínio não encontrado.'),
          409: falha('Já existe uma ativa, ou o condomínio não tem plano.', 'Este condomínio já tem cobrança recorrente ativa. Cancele a atual para trocar.'),
          502: falha('O gateway recusou.', 'O gateway recusou a assinatura: ...'),
        },
      }),
      get: op('Cobrança recorrente da plataforma', 'Assinaturas da plataforma', 'Todas as cobranças recorrentes, ativas primeiro, cada uma com as cobranças que o Asaas já gerou (conciliadas com o gateway antes de responder). Perfil: ADMIN_GERAL.', {
        responses: { 200: resposta('Assinaturas.', { sucesso: true, total: 1, assinaturas: [EX_ASSINATURA_PLAT] }), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/plataforma/assinaturas/{id}/cancelar': {
      patch: op('Cobrança recorrente da plataforma', 'Cancelar cobrança recorrente', 'Remove a assinatura no Asaas, que apaga as cobranças ainda não pagas; aqui elas viram CANCELADA. As pagas ficam no histórico. Perfil: ADMIN_GERAL.', {
        parameters: [pId('id', 'Id da assinatura.', ASSINATURA)],
        'x-sem-corpo': true,
        responses: {
          200: resposta('Cancelada.', { sucesso: true, assinatura: { ...EX_ASSINATURA_PLAT, status: 'CANCELADA', canceladaEm: '2026-12-01T10:00:00.000Z', pagamentos: [{ ...EX_PAGAMENTO_PLAT, status: 'CANCELADA' }] } }),
          401: E401, 403: E403, 404: falha('Assinatura inexistente.', 'Assinatura não encontrada.'), 409: falha('Já cancelada.', 'Esta assinatura já está cancelada.'),
        },
      }),
    },
    '/api/financeiro/plataforma/pagamentos/{id}/estornar': {
      post: op('Cobrança recorrente da plataforma', 'Estornar mensalidade', 'Devolve ao condomínio uma mensalidade recebida, pelo mesmo fluxo do estorno de fatura: só PIX, assíncrono, 202 enquanto em aberto. Perfil: ADMIN_GERAL.', {
        parameters: [pId('id', 'Id do pagamento da assinatura.', PAGAMENTO_PLAT)],
        requestBody: MOTIVO,
        responses: {
          200: resposta('Estorno concluído na hora.', { sucesso: true, estorno: { ...EX_ESTORNO, faturaId: null, pagamentoPlataformaId: PAGAMENTO_PLAT, valorCentavos: 100000, status: 'CONCLUIDO', concluidoEm: '2026-10-28T13:10:02.000Z' } }),
          202: resposta('Pedido aceito, aguardando a devolução.', { sucesso: true, estorno: { ...EX_ESTORNO, faturaId: null, pagamentoPlataformaId: PAGAMENTO_PLAT, valorCentavos: 100000 } }),
          400: E400('Explique o motivo do estorno (mínimo de 5 caracteres).'), 401: E401, 403: E403,
          404: falha('Pagamento inexistente.', 'Pagamento não encontrado.'), 409: SO_PIX, 502: E502,
        },
      }),
    },
    '/api/financeiro/plataforma/minha-assinatura': {
      get: op('Cobrança recorrente da plataforma', 'Mensalidade do meu condomínio', 'A cobrança recorrente do condomínio de quem chama (a ativa, ou a última), com as cobranças geradas e o link de pagamento de cada uma (boleto e PIX na mesma página do Asaas). `assinatura` vem nula se o Mora ainda não cobra este condomínio. Perfil: ADMIN_SINDICO.', {
        responses: { 200: resposta('Assinatura do condomínio.', { sucesso: true, assinatura: EX_ASSINATURA_PLAT }), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/admin/taxa-plataforma': {
      get: op('Faturas', 'Taxa da plataforma por condomínio', 'Taxa da plataforma Mora lançada nas faturas do mês, por condomínio: total e quanto já foi pago. Agregado em SQL. Perfil: ADMIN_GERAL, que informa o condomínio de escopo em `condominioId`.', {
        parameters: [
          { ...pQuery('mes', 'Mês AAAA-MM (obrigatório).', '2026-09'), required: true },
          { ...pQuery('condominioId', 'Condomínio de escopo (obrigatório para o Admin Geral).', COND), required: true },
        ],
        responses: {
          200: resposta('Taxa por condomínio.', { sucesso: true, competencia: '2026-09-01', condominios: [{ condominioId: COND, faturas: '4', totalCentavos: '2000', pagoCentavos: '500' }] }),
          400: E400('Informe o mês (YYYY-MM).'), 401: E401, 403: E403,
        },
      }),
    },
    '/api/financeiro/webhooks': {
      post: op('Webhooks', 'Evento de pagamento do Asaas', 'Chamado pelo Asaas quando um pagamento muda (confirmado, recebido, vencido). Autenticado pelo cabeçalho `asaas-webhook-token` (`ASAAS_WEBHOOK_TOKEN`), não por JWT; sem o token configurado, a verificação fica desligada. Idempotente: o mesmo evento entregue duas vezes é ignorado.', {
        security: [],
        parameters: [{ name: 'asaas-webhook-token', in: 'header', required: false, description: 'Token configurado no painel de webhooks do Asaas.', schema: { type: 'string' }, example: '********' }],
        requestBody: corpo('Evento no formato do Asaas.', {
          id: 'evt_05b708f961d739ea7eba7e4db318f621&368604920', event: 'PAYMENT_RECEIVED',
          payment: { id: 'pay_c6gm14r22ejzs4nc', status: 'RECEIVED', value: 828.4, paymentDate: '2026-10-08', billingType: 'PIX', externalReference: FATURA },
        }, { type: 'object', required: ['id', 'event'] }),
        responses: {
          200: resposta('Evento processado (ou ignorado por já ter sido).', { sucesso: true, processado: true }),
          401: falha('Token do webhook ausente ou errado.', 'Assinatura inválida.'),
        },
      }),
    },
    '/api/financeiro/admin/multas': {
      get: op('Multas', 'Multas do condomínio', `Filtráveis por status e unidade, com prazo de recurso calculado. ${GESTAO}`, {
        parameters: [
          pQuery('status', 'Status da multa.', 'APLICADA', { type: 'string', enum: ['APLICADA', 'EM_RECURSO', 'CANCELADA'] }),
          pQuery('unidadeId', 'Id da unidade.', UNIDADE_101),
        ],
        responses: { 200: resposta('Multas.', { sucesso: true, total: 1, multas: [EX_MULTA] }), 401: E401, 403: E403 },
      }),
      post: op('Multas', 'Aplicar multa', `Multa uma unidade (e, opcionalmente, um morador dela). A data da infração não pode estar no futuro. O responsável recebe a notificação \`MULTA_APLICADA\` no sino. A multa entra na fatura depois do prazo de recurso. ${SINDICO}`, {
        requestBody: corpo('Multa.', { unidadeId: UNIDADE_101, usuarioId: 4, valor: 150.0, motivo: 'Barulho após as 22h em dia de semana', dataInfracao: '2026-10-12' }, {
          type: 'object', required: ['unidadeId', 'valor', 'motivo', 'dataInfracao'],
          properties: {
            unidadeId: { type: 'string' }, usuarioId: { type: 'integer', description: 'Morador da unidade (opcional).' },
            valor: { type: 'number', description: 'Em reais; ou envie valorCentavos.' }, motivo: { type: 'string', minLength: 5 }, dataInfracao: { type: 'string', format: 'date' },
          },
        }),
        responses: {
          201: resposta('Multa aplicada.', { sucesso: true, multa: EX_MULTA }),
          400: E400('A data da infração não pode estar no futuro.'), 401: E401, 403: E403, 404: E404('Unidade'),
          503: falha('O portaria não confirmou a unidade.', 'Não foi possível confirmar a unidade. Tente novamente.'),
        },
      }),
    },
    '/api/financeiro/admin/multas/por-mes': {
      get: op('Multas', 'Multas por mês', `Aplicadas, canceladas e valor por mês, agregados em SQL no fuso de São Paulo. Usado pelo painel estratégico. ${GESTAO}`, {
        parameters: [pQuery('meses', 'Janela: 3, 6 ou 12 (padrão 6).', 6, { type: 'integer', enum: [3, 6, 12] })],
        responses: {
          200: resposta('Série mensal.', { sucesso: true, meses: [{ mes: '2026-09', aplicadas: 2, canceladas: 1, valorCentavos: '15000' }, { mes: '2026-10', aplicadas: 1, canceladas: 0, valorCentavos: '15000' }] }),
          400: E400('Use meses=3, 6 ou 12.'), 401: E401, 403: E403,
        },
      }),
    },
    '/api/financeiro/admin/multas/{id}/julgar': {
      patch: op('Multas', 'Julgar recurso', `Aceito, a multa é cancelada; recusado, volta a valer. Só multa EM_RECURSO. O morador recebe \`MULTA_JULGADA\`. ${SINDICO}`, {
        parameters: [pId('id', 'Id da multa.', MULTA)],
        requestBody: corpo('Decisão.', { aceito: false, justificativa: 'Registro da portaria confirma o barulho no horário.' }, {
          type: 'object', required: ['justificativa'],
          properties: { aceito: { type: 'boolean' }, decisao: { type: 'string', enum: ['ACEITO', 'RECUSADO'], description: 'Alternativa a `aceito`.' }, justificativa: { type: 'string', minLength: 5 } },
        }),
        responses: {
          200: resposta('Recurso julgado.', { sucesso: true, multa: { ...EX_MULTA, julgadoPorId: 2, julgadoEm: '2026-10-20T10:00:00.000Z', julgamentoJustificativa: 'Registro da portaria confirma o barulho no horário.' } }),
          400: E400('Informe a decisão: aceito ou recusado.'), 401: E401, 403: E403, 404: E404('Multa'), 409: falha('Multa sem recurso.', 'Só é possível julgar multa em recurso.'),
        },
      }),
    },
    '/api/financeiro/admin/multas/{id}/cancelar': {
      patch: op('Multas', 'Cancelar multa', `Se a multa já foi faturada, o próximo fechamento lança um estorno do mesmo valor. ${SINDICO}`, {
        parameters: [pId('id', 'Id da multa.', MULTA)],
        requestBody: corpo('Justificativa.', { justificativa: 'Multa aplicada na unidade errada.' }, { type: 'object', required: ['justificativa'], properties: { justificativa: { type: 'string', minLength: 5 } } }),
        responses: {
          200: resposta('Multa cancelada.', { sucesso: true, multa: { ...EX_MULTA, status: 'CANCELADA', estornoNoProximoFechamento: false } }),
          400: E400('Explique o motivo do cancelamento.'), 401: E401, 403: E403, 404: E404('Multa'), 409: falha('Já cancelada.', 'Esta multa já está cancelada.'),
        },
      }),
    },
    '/api/financeiro/multas/minhas': {
      get: op('Multas', 'Minhas multas', `Multas da unidade de quem chama, com prazo e se ainda cabe recurso. ${UNIDADE}`, {
        responses: { 200: resposta('Multas.', { sucesso: true, total: 1, multas: [EX_MULTA] }), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/multas/{id}/recurso': {
      post: op('Multas', 'Recorrer de multa', `Um recurso por multa, dentro do prazo, com pelo menos 10 caracteres. A multa passa a EM_RECURSO e não é cobrada até o julgamento. ${UNIDADE}`, {
        parameters: [pId('id', 'Id da multa.', MULTA)],
        requestBody: corpo('Texto do recurso.', { texto: 'Estávamos viajando nesse fim de semana; o apartamento estava vazio.' }, { type: 'object', required: ['texto'], properties: { texto: { type: 'string', minLength: 10 } } }),
        responses: {
          200: resposta('Recurso registrado.', { sucesso: true, multa: { ...EX_MULTA, status: 'EM_RECURSO', recursoTexto: 'Estávamos viajando nesse fim de semana; o apartamento estava vazio.', recursoEm: '2026-10-14T09:12:00.000Z', podeRecorrer: false } }),
          400: E400('Explique o recurso em pelo menos 10 caracteres.'), 401: E401, 403: E403, 404: E404('Multa'), 409: falha('Já recorrida ou fora do prazo.', 'Esta multa já teve recurso.'),
        },
      }),
    },
    '/api/financeiro/admin/contratos': {
      get: op('Contratos de locação', 'Contratos do condomínio', `Contratos ativos e encerrados. ${GESTAO}`, {
        responses: { 200: resposta('Contratos.', { sucesso: true, total: 1, contratos: [EX_CONTRATO] }), 401: E401, 403: E403 },
      }),
      post: op('Contratos de locação', 'Registrar contrato', `O síndico registra o contrato de qualquer unidade. \`responsavelTaxas\` decide quem recebe a fatura. O banco recusa (409) um segundo contrato vigente na unidade e um período que cruze o de outro contrato dela, mesmo encerrado (restrição de exclusão \`ex_contrato_periodo_unico\`). ${SINDICO}`, {
        requestBody: corpo('Contrato.', { unidadeId: UNIDADE_101, proprietarioUsuarioId: 6, inquilinoUsuarioId: 4, inicio: '2026-03-01', fim: '2027-02-28', valorAluguel: '2200,00', responsavelTaxas: 'INQUILINO' }, {
          type: 'object', required: ['unidadeId', 'inicio'],
          properties: {
            unidadeId: { type: 'string' }, proprietarioUsuarioId: { type: 'integer' }, inquilinoUsuarioId: { type: 'integer' },
            inicio: { type: 'string', format: 'date' }, fim: { type: 'string', format: 'date' }, valorAluguel: { type: 'string' },
            responsavelTaxas: { type: 'string', enum: ['PROPRIETARIO', 'INQUILINO'] },
          },
        }),
        responses: {
          201: resposta('Contrato registrado.', { sucesso: true, contrato: EX_CONTRATO }), 400: E400('Informe a data de início (AAAA-MM-DD).'), 401: E401, 403: E403,
          409: falha('Contrato vigente ou período sobreposto.', 'O período informado cruza o de outro contrato desta unidade. Ajuste o início ou o fim.'),
        },
      }),
    },
    '/api/financeiro/admin/contratos/{id}/encerrar': {
      patch: op('Contratos de locação', 'Encerrar contrato', `As próximas faturas voltam para o proprietário. ${SINDICO}`, {
        parameters: [pId('id', 'Id do contrato.', CONTRATO)],
        'x-sem-corpo': true,
        responses: { 200: resposta('Contrato encerrado.', { sucesso: true, contrato: { ...EX_CONTRATO, ativo: false } }), 401: E401, 403: E403, 404: E404('Contrato'), 409: falha('Já encerrado.', 'Este contrato já está encerrado.') },
      }),
    },
    '/api/financeiro/contratos/meus': {
      get: op('Contratos de locação', 'Contratos da minha unidade', `Para o inquilino e o proprietário verem o contrato da unidade. ${UNIDADE}`, {
        responses: { 200: resposta('Contratos.', { sucesso: true, total: 1, contratos: [EX_CONTRATO] }), 401: E401, 403: E403 },
      }),
      post: op('Contratos de locação', 'Registrar contrato da minha unidade', 'O proprietário registra o contrato do próprio apartamento: unidade e proprietário saem do escopo dele, nunca do corpo. Perfil: DONO_ALUGUEL.', {
        requestBody: corpo('Contrato.', { inquilinoUsuarioId: 4, inicio: '2026-03-01', fim: '2027-02-28', valorAluguel: '2200,00', responsavelTaxas: 'INQUILINO' }, {
          type: 'object', required: ['inicio'],
          properties: { inquilinoUsuarioId: { type: 'integer' }, inicio: { type: 'string', format: 'date' }, fim: { type: 'string', format: 'date' }, valorAluguel: { type: 'string' }, responsavelTaxas: { type: 'string', enum: ['PROPRIETARIO', 'INQUILINO'] } },
        }),
        responses: { 201: resposta('Contrato registrado.', { sucesso: true, contrato: EX_CONTRATO }), 400: E400('O fim do contrato não pode ser antes do início.'), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/contratos/{id}/encerrar': {
      patch: op('Contratos de locação', 'Encerrar contrato da minha unidade', 'O proprietário encerra o contrato do próprio apartamento. Perfil: DONO_ALUGUEL.', {
        parameters: [pId('id', 'Id do contrato.', CONTRATO)],
        'x-sem-corpo': true,
        responses: { 200: resposta('Contrato encerrado.', { sucesso: true, contrato: { ...EX_CONTRATO, ativo: false } }), 401: E401, 403: E403, 404: E404('Contrato'), 409: falha('Já encerrado.', 'Este contrato já está encerrado.') },
      }),
    },
    '/api/financeiro/admin/prestacao/{competencia}': {
      get: op('Prestação de contas', 'Prestação da competência', `Receitas das taxas, lançamentos e totais por categoria. Em RASCUNHO até ser publicada. ${GESTAO}`, {
        parameters: [competenciaPath],
        responses: { 200: resposta('Prestação.', { sucesso: true, prestacao: EX_PRESTACAO }), 400: E400('Competência inválida. Use AAAA-MM.'), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/admin/prestacao/{competencia}/lancamentos': {
      post: op('Prestação de contas', 'Lançar receita ou despesa', `Só enquanto a competência não foi publicada. ${SINDICO}`, {
        parameters: [competenciaPath],
        requestBody: corpo('Lançamento.', { tipo: 'DESPESA', categoria: 'Manutenção', descricao: 'Troca da bomba da caixa d\'água', valor: '1850,00', dataLancamento: '2026-09-18' }, {
          type: 'object', required: ['tipo', 'categoria', 'descricao', 'valor', 'dataLancamento'],
          properties: {
            tipo: { type: 'string', enum: ['RECEITA', 'DESPESA'] }, categoria: { type: 'string', maxLength: 80 }, descricao: { type: 'string', maxLength: 300 },
            valor: { type: 'string' }, dataLancamento: { type: 'string', format: 'date' }, comprovanteUrl: { type: 'string' },
          },
        }),
        responses: { 201: resposta('Lançado.', { sucesso: true, lancamento: EX_LANCAMENTO }), 400: E400('Informe se é RECEITA ou DESPESA.'), 401: E401, 403: E403, 409: falha('Competência publicada.', 'Esta competência já foi publicada.') },
      }),
    },
    '/api/financeiro/admin/prestacao/lancamentos/{id}': {
      delete: op('Prestação de contas', 'Excluir lançamento', `Só enquanto a competência não foi publicada. ${SINDICO}`, {
        parameters: [pId('id', 'Id do lançamento.', LANCAMENTO)],
        responses: { 200: resposta('Excluído.', { sucesso: true, id: LANCAMENTO }), 401: E401, 403: E403, 404: E404('Lançamento'), 409: falha('Competência publicada.', 'A competência deste lançamento já foi publicada.') },
      }),
    },
    '/api/financeiro/admin/prestacao/{competencia}/publicar': {
      post: op('Prestação de contas', 'Publicar prestação', `Congela a competência e a torna visível aos moradores. ${SINDICO}`, {
        parameters: [competenciaPath],
        'x-sem-corpo': true,
        responses: { 200: resposta('Publicada.', { sucesso: true, prestacao: { ...EX_PRESTACAO, status: 'PUBLICADA', publicadoEm: '2026-10-05T12:00:00.000Z', publicadoPorId: 2 } }), 401: E401, 403: E403, 409: falha('Já publicada.', 'Esta competência já foi publicada.') },
      }),
    },
    '/api/financeiro/prestacao': {
      get: op('Prestação de contas', 'Prestações publicadas', `Competências que o síndico publicou, com os totais. ${UNIDADE}`, {
        responses: { 200: resposta('Competências.', { sucesso: true, total: 1, competencias: [{ competencia: '2026-09', publicadoEm: '2026-10-05T12:00:00.000Z', receitasCentavos: 331360, despesasCentavos: 185000, saldoCentavos: 146360 }] }), 401: E401, 403: E403 },
      }),
    },
    '/api/financeiro/prestacao/{competencia}': {
      get: op('Prestação de contas', 'Prestação publicada', `O detalhe de uma competência publicada. Rascunho não aparece para o morador. ${UNIDADE}`, {
        parameters: [competenciaPath],
        responses: { 200: resposta('Prestação.', { sucesso: true, prestacao: { ...EX_PRESTACAO, status: 'PUBLICADA', publicadoEm: '2026-10-05T12:00:00.000Z', publicadoPorId: 2 } }), 401: E401, 403: E403, 404: E404('Prestação publicada') },
      }),
    },
    '/api/financeiro/docs/openapi.json': {
      get: op('Documentação', 'Esta especificação', 'A especificação OpenAPI do financeiro, em JSON. Pública.', {
        security: [],
        responses: { 200: resposta('Especificação OpenAPI.', { openapi: '3.0.3', info: { title: 'financeiro', version: '1.0' } }) },
      }),
    },
    '/health': {
      get: op('Saúde', 'Saúde do serviço', 'Banco e gateway. Usada pelo Consul. Fora do gateway: responde só na porta do serviço.', {
        security: [],
        servers: [{ url: 'http://localhost:3004', description: 'Porta do serviço' }],
        responses: { 200: resposta('No ar.', { status: 'ok', servico: 'financeiro', banco: 'ok', gateway: 'configurado' }) },
      }),
    },
  },
};
