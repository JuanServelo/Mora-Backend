/**
 * Verificação de autorização do comunicacao-service, contra a stack real.
 *
 * Não substitui teste unitário: exercita o serviço como um cliente o faria,
 * pelo HTTP, com token emitido pelo auth-api de verdade. É o que confirma que
 * a checagem está no caminho da requisição, e não só no arquivo.
 *
 *   node verificar-autorizacao.mjs
 *
 * Depende do seed: `node services/auth-api/scripts/seed-condominios-teste.js`.
 */

const AUTH = process.env.AUTH_API_URL || 'http://localhost:3001';
const COMUNICACAO = process.env.COMUNICACAO_URL || 'http://localhost:8094';
const SENHA = 'Teste1234';

let ok = 0;
let falhou = 0;

function checar(nome, condicao, detalhe = '') {
  if (condicao) {
    ok += 1;
    console.log(`  \x1b[32mok\x1b[0m   ${nome}`);
  } else {
    falhou += 1;
    console.log(`  \x1b[31mFALHA\x1b[0m ${nome}${detalhe ? ` — ${detalhe}` : ''}`);
  }
}

function secao(titulo) {
  console.log(`\n\x1b[1m${titulo}\x1b[0m`);
}

async function login(email) {
  const r = await fetch(`${AUTH}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, senha: SENHA }),
  });
  if (!r.ok) return null;
  const corpo = await r.json();
  return corpo.token ?? null;
}

/**
 * Uma fatia dos usuários do seed nasce inativa ou pendente, de propósito, para
 * os indicadores não darem 100%. Por isso a busca por índice, em vez de um
 * e-mail fixo: o primeiro que logar serve.
 */
async function primeiroQueLoga(condominio, indices) {
  for (const i of indices) {
    const email = `${condominio}.${i}@qa.mora.local`;
    const token = await login(email);
    if (token) return { email, token };
  }
  return null;
}

async function chamar(metodo, caminho, token, corpo) {
  const r = await fetch(`${COMUNICACAO}${caminho}`, {
    method: metodo,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(corpo ? { 'Content-Type': 'application/json' } : {}),
    },
    ...(corpo ? { body: JSON.stringify(corpo) } : {}),
  });
  let dados = null;
  try { dados = await r.json(); } catch { /* 204 e afins */ }
  return { status: r.status, dados };
}

const AVISO = {
  titulo: 'Teste de autorização',
  mensagem: 'Gerado pelo verificar-autorizacao.mjs',
  dataInicio: new Date().toISOString().slice(0, 10),
  dataFim: new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10),
  publicoAlvo: 'TODOS',
};

async function main() {
  console.log(`\n\x1b[1mAutorização do comunicacao-service\x1b[0m\n`);

  // ── quem vamos usar ──────────────────────────────────────────────────────
  // No seed, os índices 0..1 de cada condomínio são síndicos e 6..101 moradores.
  const sindicoA = await primeiroQueLoga('vila-serena', [0, 1]);
  const sindicoB = await primeiroQueLoga('parque-atlantico', [0, 1]);
  const morador = await primeiroQueLoga('vila-serena', [6, 7, 8, 9, 10, 11, 12]);
  const porteiro = await primeiroQueLoga('vila-serena', [2, 3, 4, 5]);

  for (const [papel, u] of Object.entries({ sindicoA, sindicoB, morador, porteiro })) {
    if (!u) {
      console.error(`\x1b[31mNão consegui logar como ${papel}.\x1b[0m `
        + 'Rode o seed: node services/auth-api/scripts/seed-condominios-teste.js');
      process.exit(1);
    }
  }
  console.log(`  síndico A: ${sindicoA.email}`);
  console.log(`  síndico B: ${sindicoB.email}`);
  console.log(`  morador:   ${morador.email}`);
  console.log(`  porteiro:  ${porteiro.email}`);

  // ── 1. sem token ─────────────────────────────────────────────────────────
  secao('Sem token');
  const semToken = await chamar('GET', '/avisos/ativos', null);
  checar('GET /avisos/ativos → 401', semToken.status === 401, `respondeu ${semToken.status}`);

  // ── 2. escrever é só do síndico ──────────────────────────────────────────
  secao('Quem pode publicar');
  const criaSindico = await chamar('POST', '/avisos', sindicoA.token, AVISO);
  checar('síndico cria aviso → 201', criaSindico.status === 201, `respondeu ${criaSindico.status}`);
  const avisoId = criaSindico.dados?.id;

  const criaMorador = await chamar('POST', '/avisos', morador.token, AVISO);
  checar('morador cria aviso → 403', criaMorador.status === 403, `respondeu ${criaMorador.status}`);

  const criaPorteiro = await chamar('POST', '/avisos', porteiro.token, AVISO);
  checar('porteiro cria aviso → 403', criaPorteiro.status === 403, `respondeu ${criaPorteiro.status}`);

  if (!avisoId) {
    console.error('\n\x1b[31mSem aviso criado, o resto não tem o que exercitar.\x1b[0m');
    process.exit(1);
  }

  // ── 3. isolamento entre condomínios ──────────────────────────────────────
  secao('Isolamento entre condomínios');
  const leAlheio = await chamar('GET', `/avisos/${avisoId}`, sindicoB.token);
  checar('síndico de outro condomínio lê o aviso → 404',
    leAlheio.status === 404, `respondeu ${leAlheio.status}`);

  const editaAlheio = await chamar('PUT', `/avisos/${avisoId}`, sindicoB.token,
    { ...AVISO, titulo: 'Invadido' });
  checar('síndico de outro condomínio edita → 404',
    editaAlheio.status === 404, `respondeu ${editaAlheio.status}`);

  const apagaAlheio = await chamar('DELETE', `/avisos/${avisoId}`, sindicoB.token);
  checar('síndico de outro condomínio exclui → 404',
    apagaAlheio.status === 404, `respondeu ${apagaAlheio.status}`);

  const confirmaAlheio = await chamar('POST', `/avisos/${avisoId}/lido`, sindicoB.token);
  checar('confirma leitura de aviso alheio → 404',
    confirmaAlheio.status === 404, `respondeu ${confirmaAlheio.status}`);

  // ── 4. rascunho não vaza ─────────────────────────────────────────────────
  secao('Rascunho');
  const rascunhoMorador = await chamar('GET', '/avisos', morador.token);
  checar('morador lista não publicados → 403',
    rascunhoMorador.status === 403, `respondeu ${rascunhoMorador.status}`);

  const rascunhoSindico = await chamar('GET', '/avisos', sindicoA.token);
  checar('síndico lista não publicados → 200',
    rascunhoSindico.status === 200, `respondeu ${rascunhoSindico.status}`);

  // ── 5. notificação forjada ───────────────────────────────────────────────
  secao('Notificação');
  const params = new URLSearchParams({
    destinatarioId: '999999',
    condominioId: 'qa-vila-serena',
    tipo: 'SISTEMA',
    titulo: 'Forjada',
  });
  const notifMorador = await chamar('POST', `/notificacoes/admin?${params}`, morador.token, 'texto');
  checar('morador publica notificação para outro → 403',
    notifMorador.status === 403, `respondeu ${notifMorador.status}`);

  // ── 6. o morador enxerga o que é dele ────────────────────────────────────
  secao('Leitura normal');
  const ativos = await chamar('GET', '/avisos/ativos', morador.token);
  checar('morador lista avisos ativos → 200', ativos.status === 200, `respondeu ${ativos.status}`);
  checar('a lista traz a marca de leitura',
    Array.isArray(ativos.dados) && (ativos.dados.length === 0 || 'lido' in ativos.dados[0]));

  const resumo = await chamar('GET', '/notificacoes/resumo', morador.token);
  checar('resumo do sino → 200 com as duas contagens',
    resumo.status === 200 && resumo.dados
      && 'naoLidas' in resumo.dados && 'conversasNaoLidas' in resumo.dados,
    `respondeu ${resumo.status}`);

  // ── 7. confirmar leitura, de ponta a ponta ───────────────────────────────
  // O caminho que o defeito de tipo derrubava: aqui o id do usuário sai do
  // token e vai para o banco. Enquanto a coluna era UUID, isto respondia 500
  // com `Invalid UUID string: 32` — o id de usuário do auth-api é inteiro.
  secao('Confirmação de leitura');

  await chamar('PATCH', `/avisos/${avisoId}/publicar`, sindicoA.token);

  const confirma = await chamar('POST', `/avisos/${avisoId}/lido`, morador.token);
  checar('morador confirma leitura → 200', confirma.status === 200, `respondeu ${confirma.status}`);

  const comLeitura = await chamar('GET', '/avisos/ativos', morador.token);
  const esse = (comLeitura.dados ?? []).find((a) => a.id === avisoId);
  checar('o aviso aparece marcado como lido', esse?.lido === true);

  // Confirmar duas vezes é a mesma confirmação: a data que vale é a da
  // primeira. Devolver a segunda como nova faria o relatório mostrar o morador
  // lendo o aviso toda vez que abrisse a tela.
  const primeiraData = esse?.lidoEm;
  await chamar('POST', `/avisos/${avisoId}/lido`, morador.token);
  const depois = await chamar('GET', '/avisos/ativos', morador.token);
  const mesmo = (depois.dados ?? []).find((a) => a.id === avisoId);
  checar('confirmar de novo não muda a data',
    primeiraData != null && mesmo?.lidoEm === primeiraData,
    `${primeiraData} -> ${mesmo?.lidoEm}`);

  const relatorio = await chamar('GET', `/avisos/${avisoId}/leituras`, sindicoA.token);
  checar('relatório conta uma leitura',
    relatorio.status === 200 && relatorio.dados?.totalLeituras === 1,
    `respondeu ${relatorio.status} com ${relatorio.dados?.totalLeituras}`);

  const relatorioMorador = await chamar('GET', `/avisos/${avisoId}/leituras`, morador.token);
  checar('morador não vê o relatório → 403',
    relatorioMorador.status === 403, `respondeu ${relatorioMorador.status}`);

  // ── limpeza ──────────────────────────────────────────────────────────────
  const limpou = await chamar('DELETE', `/avisos/${avisoId}`, sindicoA.token);
  checar('síndico dono exclui o próprio aviso → 204',
    limpou.status === 204, `respondeu ${limpou.status}`);

  console.log(`\n\x1b[1m${ok} ok, ${falhou} falha(s)\x1b[0m\n`);
  process.exit(falhou > 0 ? 1 : 0);
}

main().catch((e) => {
  console.error('\n\x1b[31mErro:\x1b[0m', e.message);
  process.exit(1);
});
