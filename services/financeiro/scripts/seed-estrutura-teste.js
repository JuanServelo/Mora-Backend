// Cria blocos e apartamentos de teste no portaria-service.
//
// Por que isto existe aqui: o seed de condomínios do auth-api cria usuários,
// mas não cria a estrutura física, e sem apartamento não há a quem cobrar. As
// telas de fração ideal e o fechamento do mês ficam sem nada para exercitar.
//
//   node scripts/seed-estrutura-teste.js [condominioId]
//
// É idempotente pelo nome do bloco: rodar duas vezes não duplica.
import 'dotenv/config';

const PORTARIA = process.env.PORTARIA_SERVICE_URL || 'http://localhost:8090';
const AUTH = process.env.AUTH_API_URL || 'http://localhost:3001';

const CONDOMINIO = process.argv[2] || 'qa-vila-serena';
const EMAIL_SINDICO = process.env.EMAIL_SEED || `${CONDOMINIO.replace('qa-', '')}.0@qa.mora.local`;
const SENHA = process.env.SENHA_TESTE || 'Teste1234';

// Áreas propositalmente diferentes: com todas iguais, o rateio proporcional
// daria o mesmo resultado do fixo, e não provaria nada.
const APARTAMENTOS = [
  { numero: '101', andar: 1, quartos: 2, area: 62.5 },
  { numero: '102', andar: 1, quartos: 2, area: 62.5 },
  { numero: '201', andar: 2, quartos: 3, area: 88.0 },
  { numero: '202', andar: 2, quartos: 2, area: 62.5 },
  { numero: '301', andar: 3, quartos: 3, area: 88.0 },
  { numero: '302', andar: 3, quartos: 2, area: 62.5 },
  { numero: '401', andar: 4, quartos: 4, area: 145.0 },
];

async function entrar() {
  const r = await fetch(`${AUTH}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: EMAIL_SINDICO, senha: SENHA }),
  });
  const corpo = await r.json().catch(() => ({}));
  if (!corpo.token) {
    console.error(`Não foi possível entrar como ${EMAIL_SINDICO}.`);
    console.error('Rode antes: node ../auth-api/scripts/seed-condominios-teste.js');
    process.exit(1);
  }
  return corpo.token;
}

const cabecalhos = (token) => ({
  'Content-Type': 'application/json',
  Authorization: `Bearer ${token}`,
});

async function garantirBloco(token) {
  const nome = 'Bloco A';

  const existentes = await fetch(
    `${PORTARIA}/blocos?condominioId=${encodeURIComponent(CONDOMINIO)}`,
    { headers: cabecalhos(token) },
  ).then((r) => r.json()).catch(() => []);

  const achado = (existentes || []).find((b) => b.nome === nome);
  if (achado) {
    console.log(`bloco já existe: ${nome} (${achado.id})`);
    return achado.id;
  }

  const r = await fetch(`${PORTARIA}/blocos/cadastrar`, {
    method: 'POST',
    headers: cabecalhos(token),
    body: JSON.stringify({
      nome,
      condominioId: CONDOMINIO,
      descricao: 'Bloco de teste para o financeiro',
      andares: 4,
      apartamentosPorAndar: 2,
      ativo: true,
    }),
  });

  if (!r.ok) {
    console.error(`falhou ao criar o bloco: HTTP ${r.status}`, await r.text());
    process.exit(1);
  }
  const bloco = await r.json();
  console.log(`bloco criado: ${nome} (${bloco.id})`);
  return bloco.id;
}

async function garantirApartamentos(token, blocoId) {
  const existentes = await fetch(`${PORTARIA}/apartamentos/bloco/${blocoId}`, {
    headers: cabecalhos(token),
  }).then((r) => r.json()).catch(() => []);

  const jaTem = new Set((existentes || []).map((a) => a.numero));
  let criados = 0;

  for (const ap of APARTAMENTOS) {
    if (jaTem.has(ap.numero)) continue;
    const r = await fetch(`${PORTARIA}/apartamentos/cadastrar`, {
      method: 'POST',
      headers: cabecalhos(token),
      body: JSON.stringify({
        numero: ap.numero,
        andar: ap.andar,
        blocoId,
        quartos: ap.quartos,
        areaMxComTotal: ap.area,
      }),
    });
    if (r.ok) {
      criados++;
    } else {
      console.warn(`  apartamento ${ap.numero}: HTTP ${r.status} ${await r.text()}`);
    }
  }

  console.log(`apartamentos: ${criados} criados, ${jaTem.size} já existiam`);
  const total = APARTAMENTOS.reduce((a, ap) => a + ap.area, 0);
  console.log(`área total: ${total} m² — a proposta por área usa isto`);
}

const token = await entrar();
const blocoId = await garantirBloco(token);
await garantirApartamentos(token, blocoId);
console.log(`\npronto. condomínio: ${CONDOMINIO}`);
