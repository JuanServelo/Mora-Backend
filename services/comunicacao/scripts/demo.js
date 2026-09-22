// Passeio pelo fluxo do serviço, em português e com os dados reais do seed.
//
//   node scripts/demo.js
//
// Diferente do testar-local.js, que confere códigos HTTP: aqui o objetivo é
// ver o que o usuário veria. Gasta 2 logins — o limitador do auth-api é de 10
// por 15 minutos, e o balde é compartilhado.
import 'dotenv/config';

const AUTH = process.env.AUTH_API_URL || 'http://localhost:3001';
const COM = `http://localhost:${process.env.PORT || 3003}`;
const SENHA = process.env.SENHA_TESTE || 'Mora@2024';

const linha = (t = '') => console.log(t);
const titulo = (t) => linha(`\n\x1b[1m${t}\x1b[0m\n${'─'.repeat(t.length)}`);

async function entrar(email) {
  const r = await fetch(`${AUTH}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, senha: SENHA }),
  });
  if (r.status === 429) {
    linha('\nO auth-api está limitando as tentativas de login.');
    linha('Reinicie: docker compose -f docker/docker-compose.yml restart auth-api\n');
    process.exit(1);
  }
  const c = await r.json().catch(() => ({}));
  if (!c.token) {
    linha(`\nNão consegui entrar como ${email}. Rodou scripts/seed-dev.mjs?\n`);
    process.exit(1);
  }
  return c.token;
}

const api = async (metodo, caminho, token, corpo) => {
  const r = await fetch(`${COM}${caminho}`, {
    method: metodo,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(corpo !== undefined ? { body: JSON.stringify(corpo) } : {}),
  });
  return { status: r.status, dados: await r.json().catch(() => null) };
};

const hora = (iso) => (iso ? new Date(iso).toLocaleTimeString('pt-BR') : '—');

/* ------------------------------------------------------------------------ */

const morador = await entrar('morador1.verde@mora.test');
const sindico = await entrar('sindico.verde@mora.test');

titulo('1. O morador abre uma conversa sem saber quem é o síndico');

const aberta = await api('POST', '/api/comunicacao/conversas', morador, {
  assunto: 'Infiltração na garagem',
  corpo: 'A vaga 12 está com poça d\'água desde a chuva de sábado. Já são quatro dias.',
});

const conversaId = aberta.dados.conversa.id;
linha(`  conversa #${conversaId} — ${aberta.dados.conversa.assunto}`);
linha(`  tipo: ${aberta.dados.conversa.tipo}  (ele não escolheu destinatário nenhum)`);

titulo('2. Sem ninguém encaminhar, a conversa já aparece para o síndico');

const doSindico = await api('GET', '/api/comunicacao/conversas', sindico);
for (const c of doSindico.dados.conversas.slice(0, 3)) {
  const marca = c.naoLidas > 0 ? `\x1b[33m● ${c.naoLidas} nova(s)\x1b[0m` : '  lida';
  linha(`  ${marca}  #${c.id} ${c.assunto}`);
  // Sem resumo quando a única mensagem foi removida: a consulta só olha
  // mensagem visível. A tela mostra a conversa, sem prévia.
  linha(c.resumo ? `            "${c.resumo}"` : '            (sem mensagem visível)');
}

titulo('3. O síndico responde');

await api('POST', `/api/comunicacao/conversas/${conversaId}/mensagens`, sindico, {
  corpo: 'Bom dia. Acionei a empresa de impermeabilização, eles vão amanhã de manhã.',
});

const detalhe = await api('GET', `/api/comunicacao/conversas/${conversaId}`, morador);
for (const m of detalhe.dados.mensagens) {
  const quem = m.autorPerfil === 'ADMIN_SINDICO' ? 'síndico ' : 'morador ';
  linha(`  [${hora(m.criadaEm)}] ${quem} ${m.corpo}`);
}

titulo('4. A caixa de entrada do morador');

const caixa = await api('GET', '/api/comunicacao/notificacoes', morador);
for (const n of caixa.dados.notificacoes.slice(0, 5)) {
  const marca = n.lidaEm ? ' ' : '\x1b[33m●\x1b[0m';
  linha(`  ${marca} [${n.origem}] ${n.titulo}`);
  linha(`      ${n.mensagem}`);
}
linha(`\n  não lidas: ${caixa.dados.naoLidas}`);

titulo('5. O financeiro publica sem usuário logado');

const chave = `demo:fatura:${Date.now()}`;
const publicar = () =>
  fetch(`${COM}/api/comunicacao/interno/notificacoes`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Servico-Token': process.env.SERVICO_TOKEN ?? '',
    },
    body: JSON.stringify({
      condominioId: 'cond-parque-verde',
      usuarioId: detalhe.dados.participantes[0].usuarioId,
      origem: 'financeiro',
      tipo: 'FATURA_EMITIDA',
      titulo: 'Fatura de setembro disponível',
      mensagem: 'R$ 487,30 · vence em 10/10. Boleto e PIX na tela de faturas.',
      dados: { competencia: '2026-09' },
      chaveUnica: chave,
    }),
  });

const p1 = await publicar();
linha(`  1ª publicação: HTTP ${p1.status}  (notificação criada)`);
const p2 = await publicar();
const corpo2 = await p2.json();
linha(`  2ª publicação: HTTP ${p2.status}  repetida=${corpo2.repetida}  (o job rodou de novo, e ninguém recebeu duas vezes)`);

titulo('6. Confirmação de leitura de aviso');

const avisos = await api('GET', '/api/comunicacao/avisos', morador);
for (const a of avisos.dados.avisos ?? []) {
  linha(`  ${a.lido ? '\x1b[32m✓ lido\x1b[0m ' : '\x1b[33m○ pendente\x1b[0m'}  ${a.titulo}`);
}

const alvo = avisos.dados.avisos?.[0];
if (alvo) {
  const rel = await api('GET', `/api/comunicacao/avisos/${alvo.id}/leituras`, sindico);
  linha(`\n  Relatório do síndico — "${rel.dados.aviso.titulo}"`);
  linha(`  ${rel.dados.confirmados} de ${rel.dados.total} confirmaram\n`);
  for (const d of rel.dados.destinatarios) {
    const marca = d.lido ? '\x1b[32m✓\x1b[0m' : '\x1b[33m○\x1b[0m';
    linha(`    ${marca} ${(d.nome ?? '').padEnd(28)} ${d.perfil.padEnd(14)} ${d.lido ? hora(d.lidoEm) : ''}`);
  }
}

titulo('7. O que o morador NÃO consegue');

const tentativas = [
  ['abrir conversa direta com outro morador',
    await api('POST', '/api/comunicacao/conversas', morador,
      { tipo: 'DIRETA', destinatarioId: 2, assunto: 'oi', corpo: 'oi' })],
  ['listar os usuários do condomínio',
    await api('GET', '/api/comunicacao/contatos', morador)],
  ['ver quem leu o aviso',
    alvo ? await api('GET', `/api/comunicacao/avisos/${alvo.id}/leituras`, morador) : { status: '—' }],
  ['encerrar a própria conversa',
    await api('PATCH', `/api/comunicacao/conversas/${conversaId}/encerrar`, morador)],
];

for (const [o_que, r] of tentativas) {
  linha(`  ${String(r.status).padEnd(4)} ${o_que}`);
  if (r.dados?.mensagem) linha(`       "${r.dados.mensagem}"`);
}

linha('\nPronto. A conversa #' + conversaId + ' ficou aberta para você mexer.\n');
