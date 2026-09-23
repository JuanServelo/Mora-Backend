// Verificação de ponta a ponta do que a fase 1 entrega: saúde do serviço e as
// regras de autorização, com usuários reais do seed.
//
//   node scripts/testar-local.js
//
// Espera a stack no ar (docker compose up -d) e os usuários de teste criados
// por scripts/seed-dev.mjs.
import 'dotenv/config';

const AUTH = process.env.AUTH_API_URL || 'http://localhost:3001';
const FIN = `http://localhost:${process.env.PORT || 3004}`;
const GATEWAY = 'http://localhost:8087';
const SENHA = process.env.SENHA_TESTE || 'Mora@2024';

let passou = 0;
let falhou = 0;

function conferir(nome, esperado, obtido, detalhe = '') {
  const ok = esperado === obtido;
  ok ? passou++ : falhou++;
  const marca = ok ? 'ok    ' : 'FALHOU';
  const cmp = ok ? `${obtido}` : `esperava ${esperado}, veio ${obtido}`;
  console.log(`${marca} ${nome.padEnd(52)} ${cmp}${detalhe ? ' — ' + detalhe : ''}`);
}

async function entrar(email, senha = SENHA) {
  const r = await fetch(`${AUTH}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, senha }),
  });
  const corpo = await r.json().catch(() => ({}));
  return corpo.token || null;
}

const pegar = (url, token) =>
  fetch(url, { headers: token ? { Authorization: `Bearer ${token}` } : {} });

console.log('\n── saúde ────────────────────────────────────────────────────────────');
const saude = await pegar(`${FIN}/health`);
const corpoSaude = await saude.json();
conferir('/health responde', 200, saude.status);
conferir('banco conectado', 'ok', corpoSaude.banco);
console.log(`       gateway: ${corpoSaude.gateway}`);

console.log('\n── autenticação ─────────────────────────────────────────────────────');
conferir('sem token é recusado', 401, (await pegar(`${FIN}/api/financeiro/contexto`)).status);
conferir('token inválido é recusado', 401,
  (await pegar(`${FIN}/api/financeiro/contexto`, 'nao.e.um.token')).status);

console.log('\n── escopo por perfil ────────────────────────────────────────────────');
const tokenAdmin = await entrar('admin@mora.test');
if (!tokenAdmin) {
  console.log('FALHOU login do admin geral — o usuário de teste existe?');
  falhou++;
} else {
  conferir('admin geral sem condominioId pede o parâmetro', 400,
    (await pegar(`${FIN}/api/financeiro/contexto`, tokenAdmin)).status);

  const r = await pegar(`${FIN}/api/financeiro/contexto?condominioId=cond-parque-verde`, tokenAdmin);
  const c = await r.json();
  conferir('admin geral escolhe o condomínio', 200, r.status);
  conferir('admin geral entra como somente leitura', true, c.escopo?.somenteLeitura === true);
}

const tokenSindico = await entrar('sindico.verde@mora.test');
if (!tokenSindico) {
  console.log('FALHOU login do síndico — rodou scripts/seed-dev.mjs?');
  falhou++;
} else {
  // O teste que importa: pedir outro condomínio não pode devolver outro condomínio.
  const r = await pegar(`${FIN}/api/financeiro/contexto?condominioId=cond-vista-mar`, tokenSindico);
  const c = await r.json();
  conferir('síndico pedindo condomínio alheio recebe o próprio',
    'cond-parque-verde', c.escopo?.condominioId);
  conferir('síndico pode escrever', false, c.escopo?.somenteLeitura);
}

const tokenMorador = await entrar('morador1.verde@mora.test');
if (tokenMorador) {
  const r = await pegar(`${FIN}/api/financeiro/contexto`, tokenMorador);
  const c = await r.json();
  // O JWT não carrega unidadeId — ele é resolvido no auth-api. Se essa resolução
  // falhasse em silêncio, "sem unidade" viraria "sem filtro" e o morador
  // enxergaria as faturas do prédio inteiro.
  conferir('morador tem unidade resolvida', true, Boolean(c.escopo?.unidadeId));
  conferir('morador fica no próprio condomínio',
    'cond-parque-verde', c.escopo?.condominioId);
  conferir('morador é responsável financeiro', true, c.escopo?.responsavelFinanceiro);
}

// CONVIDADO não entra no sistema: o perfil existe para o controle de acesso
// físico, e não deve render token.
const tokenConvidado = await entrar('convidado.verde@mora.test');
conferir('convidado não recebe token', true, tokenConvidado === null);

console.log('\n── pelo gateway (Traefik :8087) ─────────────────────────────────────');
conferir('rota /api/financeiro chega ao serviço', 401,
  (await pegar(`${GATEWAY}/api/financeiro/contexto`)).status);

console.log(`\n${passou} passaram, ${falhou} falharam\n`);
process.exit(falhou ? 1 : 0);
