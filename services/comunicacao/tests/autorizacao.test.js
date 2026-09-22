// Testes das decisões que são tomadas ANTES de tocar no banco: validação de
// entrada e recusa por perfil. Rodam sem Postgres no ar.
//
//   npm test
//
// O que depende de banco fica em scripts/testar-local.js, contra a stack real.
import test from 'node:test';
import assert from 'node:assert/strict';

process.env.JWT_SECRET ??= 'segredo-de-teste-com-tamanho-suficiente';
process.env.POSTGRES_PASSWORD ??= 'nao-usada-nestes-testes';

const { abrir, responder } = await import('../services/conversasService.js');
const { publicar } = await import('../services/notificacoesService.js');
const { confirmar } = await import('../services/leiturasService.js');
const { resolverEscopo, exigirEscrita } = await import('../middleware/escopo.js');

const morador = {
  condominioId: 'cond-a',
  usuarioId: 10,
  perfil: 'MORADOR',
  ehGestao: false,
  somenteLeitura: false,
};

const sindico = { ...morador, usuarioId: 2, perfil: 'ADMIN_SINDICO', ehGestao: true };

/* --------------------------------------------------------- conversas --- */

test('morador não abre conversa direta com outro morador', async () => {
  const r = await abrir(morador, {
    tipo: 'DIRETA',
    destinatarioId: 11,
    assunto: 'oi',
    corpo: 'tudo bem?',
  });

  assert.equal(r.sucesso, false);
  assert.equal(r.status, 403);
});

test('conversa do morador nasce endereçada à administração', async () => {
  // Sem `tipo` no corpo, o padrão depende de quem pede. É o que permite ao
  // morador abrir conversa sem conhecer nenhum usuário do condomínio.
  const r = await abrir(morador, { assunto: '', corpo: 'vazamento no 3º andar' });

  // Para no assunto vazio, e não no 403 de conversa direta: o tipo assumido
  // foi ADMINISTRACAO.
  assert.equal(r.status, 400);
  assert.match(r.mensagem, /assunto/i);
});

test('mensagem vazia é recusada antes de abrir transação', async () => {
  const r = await abrir(sindico, { tipo: 'ADMINISTRACAO', assunto: 'Aviso', corpo: '   ' });
  assert.equal(r.status, 400);
});

test('mensagem acima do limite é recusada', async () => {
  const r = await responder(morador, 1, 'x'.repeat(4001));
  assert.equal(r.status, 400);
  assert.match(r.mensagem, /4000/);
});

test('identificador de conversa não numérico não vira consulta', async () => {
  const r = await responder(morador, 'nao-e-numero', 'oi');
  assert.equal(r.status, 400);
});

/* ------------------------------------------------------ notificações --- */

test('notificação exige condomínio, destinatário e origem conhecida', async () => {
  const base = {
    condominioId: 'cond-a',
    usuarioId: 10,
    origem: 'financeiro',
    tipo: 'FATURA_EMITIDA',
    titulo: 'Fatura de setembro',
    mensagem: 'Vence dia 10.',
  };

  for (const [campo, valor] of [
    ['condominioId', undefined],
    ['usuarioId', 'dez'],
    ['origem', 'servico-que-nao-existe'],
    ['titulo', '  '],
    ['mensagem', ''],
  ]) {
    const r = await publicar({ ...base, [campo]: valor });
    assert.equal(r.sucesso, false, `${campo} deveria ser recusado`);
    assert.equal(r.status, 400);
  }
});

test('dados precisa ser objeto, não lista', async () => {
  const r = await publicar({
    condominioId: 'cond-a',
    usuarioId: 10,
    origem: 'portaria',
    tipo: 'ENTREGA',
    titulo: 'Encomenda',
    mensagem: 'Retire na portaria.',
    dados: ['nao', 'deveria'],
  });
  assert.equal(r.status, 400);
});

/* ---------------------------------------------------------- leituras --- */

test('aviso com id fora do formato UUID nem chega ao portaria', async () => {
  // `avisos.id` é UUID no portaria. Sem esta guarda o Postgres responderia com
  // erro de sintaxe de tipo, que vira 500 em vez de 400.
  const r = await confirmar(morador, '42');
  assert.equal(r.status, 400);
});

/* ------------------------------------------------------------ escopo --- */

test('condomínio vem do token, e o da query é ignorado para quem não é admin geral', () => {
  const req = {
    perfil: 'ADMIN_SINDICO',
    claims: { id: 2, condominioId: 'cond-a' },
    params: {},
    query: { condominioId: 'cond-b' },
  };

  resolverEscopo(req, resposta(), () => {});
  assert.equal(req.escopo.condominioId, 'cond-a');
});

test('admin geral escolhe o condomínio, mas entra somente leitura', () => {
  const req = {
    perfil: 'ADMIN_GERAL',
    claims: { id: 1, condominioId: null },
    params: {},
    query: { condominioId: 'cond-b' },
  };

  resolverEscopo(req, resposta(), () => {});
  assert.equal(req.escopo.condominioId, 'cond-b');
  assert.equal(req.escopo.somenteLeitura, true);

  // E somente leitura barra escrita: o Admin Geral não fala em nome do
  // condomínio nem confirma leitura de aviso que não é destinado a ele.
  const res = resposta();
  let seguiu = false;
  exigirEscrita(req, res, () => { seguiu = true; });
  assert.equal(seguiu, false);
  assert.equal(res.codigo, 403);
});

test('usuário sem condomínio é recusado com 400', () => {
  const req = { perfil: 'MORADOR', claims: { id: 5, condominioId: null }, params: {}, query: {} };
  const res = resposta();
  resolverEscopo(req, res, () => assert.fail('não deveria seguir'));
  assert.equal(res.codigo, 400);
});

function resposta() {
  return {
    codigo: null,
    corpo: null,
    status(c) { this.codigo = c; return this; },
    json(b) { this.corpo = b; return this; },
  };
}
