/**
 * Seed de desenvolvimento — limpa e recria a base com 3 condomínios completos.
 *
 * Cada condomínio sai com um usuário de cada perfil, 4 apartamentos com áreas
 * diferentes, frações somando 1000, taxas cadastradas e assinatura ativa.
 *
 *   node scripts/seed-dev.mjs            # limpa e recria
 *   node scripts/seed-dev.mjs --manter   # só cria, sem limpar
 *
 * Precisa da stack no ar: postgres, auth-api, portaria-service, financeiro.
 *
 * A conexão vem do ambiente, não fica fixa no arquivo: a porta publicada muda
 * de máquina para máquina (5433 aqui, porque a 5432 costuma estar ocupada por
 * uma instalação nativa do PostgreSQL).
 */

import pg from 'pg';
import bcrypt from 'bcryptjs';

const PG = {
  host: process.env.POSTGRES_HOST || 'localhost',
  port: Number(process.env.POSTGRES_PORT || 5433),
  user: process.env.POSTGRES_USER || 'admin',
  password: process.env.POSTGRES_PASSWORD,
};

const AUTH_API = process.env.AUTH_API_URL || 'http://localhost:3001';
const PORTARIA_API = process.env.PORTARIA_SERVICE_URL || 'http://localhost:8090';
const FINANCEIRO_API = process.env.FINANCEIRO_API_URL || 'http://localhost:3004';

const SENHA = process.env.SENHA_SEED || 'Mora@2024';
const LIMPAR = !process.argv.includes('--manter');

if (!PG.password) {
  console.error('Defina POSTGRES_PASSWORD (a mesma do docker/.env).');
  process.exit(1);
}

/* ─── condomínios ───────────────────────────────────────────────────────── */

// Áreas propositalmente diferentes: com todas iguais o rateio proporcional
// daria o mesmo resultado do fixo, e não provaria nada.
const CONDOMINIOS = [
  {
    id: 'cond-parque-verde',
    nome: 'Residencial Parque Verde',
    slug: 'verde',
    cnpj: '12.345.678/0001-99',
    endereco: 'Rua das Flores, 100 — Batel, Curitiba/PR',
    telefone: '(41) 3321-5500',
    modo: 'FRACAO_IDEAL',
    aptos: [
      { numero: '101', andar: 1, quartos: 2, area: 62.5 },
      { numero: '102', andar: 1, quartos: 2, area: 62.5 },
      { numero: '201', andar: 2, quartos: 3, area: 88.0 },
      { numero: '202', andar: 2, quartos: 4, area: 145.0 },
    ],
  },
  {
    id: 'cond-vista-mar',
    nome: 'Edifício Vista Mar',
    slug: 'vistamar',
    cnpj: '98.765.432/0001-10',
    endereco: 'Av. Beira-Mar, 2200 — Centro, Florianópolis/SC',
    telefone: '(48) 3222-8080',
    modo: 'FIXO',
    aptos: [
      { numero: '301', andar: 3, quartos: 2, area: 70.0 },
      { numero: '302', andar: 3, quartos: 2, area: 70.0 },
      { numero: '401', andar: 4, quartos: 3, area: 95.0 },
      { numero: '402', andar: 4, quartos: 3, area: 95.0 },
    ],
  },
  {
    id: 'cond-jardim-sol',
    nome: 'Condomínio Jardim do Sol',
    slug: 'jardimsol',
    cnpj: '11.222.333/0001-44',
    endereco: 'Rua do Sol, 45 — Savassi, Belo Horizonte/MG',
    telefone: '(31) 3444-1200',
    modo: 'FRACAO_IDEAL',
    aptos: [
      { numero: '11', andar: 1, quartos: 1, area: 45.0 },
      { numero: '12', andar: 1, quartos: 2, area: 58.0 },
      { numero: '21', andar: 2, quartos: 2, area: 58.0 },
      { numero: '22', andar: 2, quartos: 3, area: 110.0 },
    ],
  },
];

/* ─── helpers ───────────────────────────────────────────────────────────── */

const log = (marca, msg) => console.log(`${marca}  ${msg}`);

/**
 * CPF fictício com dígitos verificadores corretos.
 *
 * `000.000.000-00` e afins são recusados por qualquer validação séria, e o
 * gateway de pagamento é uma delas. Estes são inventados, mas passam no
 * cálculo — que é o que um ambiente de teste precisa.
 */
function cpfValido(base9) {
  const dv = (nums) => {
    const peso = nums.length + 1;
    const soma = nums.reduce((a, n, i) => a + n * (peso - i), 0);
    const r = (soma * 10) % 11;
    return r === 10 ? 0 : r;
  };
  const n = String(base9).padStart(9, '0').split('').map(Number);
  const d1 = dv(n);
  const d2 = dv([...n, d1]);
  const s = n.join('') + d1 + d2;
  return `${s.slice(0, 3)}.${s.slice(3, 6)}.${s.slice(6, 9)}-${s.slice(9)}`;
}

/** Distribui `total` entre pesos, dando a sobra à maior parte. */
function ratear(total, pesos) {
  const soma = pesos.reduce((a, p) => a + p.peso, 0);
  const out = new Map();
  let distribuido = 0;
  for (const { chave, peso } of pesos) {
    const parcela = Math.floor((total * peso) / soma);
    out.set(chave, parcela);
    distribuido += parcela;
  }
  const sobra = total - distribuido;
  if (sobra !== 0) {
    const maior = pesos.reduce((a, b) => (b.peso > a.peso ? b : a));
    out.set(maior.chave, out.get(maior.chave) + sobra);
  }
  return out;
}

async function api(url, opts = {}) {
  const { headers, body, ...rest } = opts;
  const r = await fetch(url, {
    ...rest,
    headers: { 'Content-Type': 'application/json', ...(headers || {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const json = await r.json().catch(() => ({}));
  if (!r.ok) {
    throw new Error(`${opts.method || 'GET'} ${url} → ${r.status}: ${json.mensagem || json.message || JSON.stringify(json)}`);
  }
  return json;
}

const bearer = (t) => ({ Authorization: `Bearer ${t}` });
const conectar = (database) => new pg.Pool({ ...PG, database });

/* ─── 1. limpeza ────────────────────────────────────────────────────────── */

async function limpar() {
  log('🧹', 'Limpando auth_db, mora e mora_financeiro...');

  const alvos = {
    auth_db: ['users', 'invites', 'reclamacoes', 'condominios', 'registros_acesso'],
    mora: ['apartamentos', 'areas_comuns', 'blocos', 'carros', 'chaves', 'visitantes',
      'vagas_estacionamento', 'turno_saidas', 'entregas', 'moradores', 'funcionarios',
      'turno_entradas', 'turnos', 'avisos', 'veiculos', 'artigos_conhecimento'],
    mora_financeiro: ['fatura_itens', 'faturas', 'multas', 'cobrancas', 'webhook_eventos',
      'contas_consumo', 'contratos_locacao', 'lancamentos', 'prestacao_contas',
      'notificacoes', 'unidades_fracao', 'regras_taxa', 'tipos_taxa'],
  };

  for (const [db, tabelas] of Object.entries(alvos)) {
    const pool = conectar(db);
    // `pgmigrations` fica de fora de propósito: zerá-la faria o node-pg-migrate
    // tentar recriar tabelas que já existem.
    const existentes = (await pool.query(
      `SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename = ANY($1)`,
      [tabelas],
    )).rows.map((r) => `"${r.tablename}"`);

    if (existentes.length) {
      await pool.query(`TRUNCATE ${existentes.join(', ')} RESTART IDENTITY CASCADE`);
      log('  ✓', `${db}: ${existentes.length} tabelas zeradas`);
    }
    await pool.end();
  }

  // As assinaturas dos condomínios que vão sumir ficariam órfãs.
  const plan = conectar('mora_plan');
  const { rowCount } = await plan.query('DELETE FROM tb_assinaturas');
  await plan.end();
  log('  ✓', `mora_plan: ${rowCount} assinatura(s) removida(s) — os planos foram mantidos`);
}

/* ─── 2. usuários e condomínios ─────────────────────────────────────────── */

/** Um de cada perfil, mais moradores até fechar quatro por condomínio. */
function elencoDe(cond, i) {
  const dom = 'mora.test';
  const s = cond.slug;
  const nomes = ['Ana Souza', 'Bruno Lima', 'Carla Dias', 'Diego Rocha'];
  const base = 100000000 + i * 1000;

  const moradores = cond.aptos.map((apto, j) => ({
    nome: nomes[j],
    email: `morador${j + 1}.${s}@${dom}`,
    perfil: 'MORADOR',
    apto: apto.numero,
    // Cada morador responde pela própria unidade — é quem recebe a fatura.
    responsavelFinanceiro: true,
    cpf: cpfValido(base + 10 + j),
  }));

  return [
    { nome: `Síndico ${cond.nome}`, email: `sindico.${s}@${dom}`, perfil: 'ADMIN_SINDICO', cpf: cpfValido(base + 1) },
    { nome: `Porteiro ${cond.nome}`, email: `porteiro.${s}@${dom}`, perfil: 'PORTEIRO', cpf: cpfValido(base + 2) },
    ...moradores,
    {
      nome: 'Paulo Proprietário',
      email: `dono.${s}@${dom}`,
      perfil: 'DONO_ALUGUEL',
      apto: cond.aptos[3].numero,
      // Proprietário que aluga: quem paga a taxa é o morador, não ele.
      responsavelFinanceiro: false,
      cpf: cpfValido(base + 20),
    },
    {
      nome: 'Rita Convidada',
      email: `convidado.${s}@${dom}`,
      perfil: 'CONVIDADO',
      apto: cond.aptos[0].numero,
      // CONVIDADO não entra no sistema — existe para o controle de acesso.
      semAcesso: true,
      cpf: cpfValido(base + 30),
    },
  ];
}

async function criarUsuarios() {
  log('🔑', 'Criando usuários...');
  const pool = conectar('auth_db');
  const hash = await bcrypt.hash(SENHA, 10);
  const agora = new Date().toISOString();
  const ids = {};

  const inserir = async (u, condominioId) => {
    const { rows } = await pool.query(
      `INSERT INTO users
         (nome, email, senha, perfil, status, "condominioId", cpf, telefone,
          "dataNascimento", "responsavelFinanceiro", "semAcessoSistema",
          "tokenVersion", "createdAt", "updatedAt", "activatedAt")
       VALUES ($1,$2,$3,$4,'active',$5,$6,$7,$8,$9,$10,0,$11,$11,$11)
       RETURNING id`,
      [u.nome, u.email, hash, u.perfil, condominioId, u.cpf, u.telefone ?? null,
        u.dataNascimento ?? '1990-05-15', u.responsavelFinanceiro ?? false,
        u.semAcesso ?? false, agora],
    );
    ids[u.email] = rows[0].id;
    return rows[0].id;
  };

  // O Admin Geral opera a plataforma: condomínio nulo de propósito.
  const adminGeral = await inserir(
    { nome: 'Admin Geral Mora', email: 'admin@mora.test', perfil: 'ADMIN_GERAL', cpf: cpfValido(999888777) },
    null,
  );
  log('  ✓', `ADMIN_GERAL     admin@mora.test (id=${adminGeral})`);

  for (const [i, cond] of CONDOMINIOS.entries()) {
    await pool.query(
      `INSERT INTO condominios (id, nome, cnpj, endereco, telefone, email, status,
                                "criadoPorId", "createdAt", "updatedAt")
       VALUES ($1,$2,$3,$4,$5,$6,'active',$7,$8,$8)`,
      [cond.id, cond.nome, cond.cnpj, cond.endereco, cond.telefone,
        `contato.${cond.slug}@mora.test`, adminGeral, agora],
    );

    for (const u of elencoDe(cond, i)) await inserir(u, cond.id);
    log('  ✓', `${cond.nome}: 8 usuários`);
  }

  await pool.end();
  return ids;
}

/* ─── 3. estrutura física ───────────────────────────────────────────────── */

async function criarEstrutura(cond, token) {
  const bloco = await api(`${PORTARIA_API}/blocos/cadastrar`, {
    method: 'POST',
    headers: bearer(token),
    body: {
      nome: 'Bloco A',
      descricao: `Bloco principal — ${cond.nome}`,
      condominioId: cond.id,
      // Derivado dos apartamentos: o portaria recusa apartamento em andar acima
      // do que o bloco declara, e fixar 2 aqui quebrava os prédios mais altos.
      andares: Math.max(...cond.aptos.map((a) => a.andar)),
      apartamentosPorAndar: Math.max(
        ...Object.values(cond.aptos.reduce((acc, a) => {
          acc[a.andar] = (acc[a.andar] || 0) + 1;
          return acc;
        }, {})),
      ),
      ativo: true,
    },
  });

  const unidades = {};
  for (const a of cond.aptos) {
    const r = await api(`${PORTARIA_API}/apartamentos/cadastrar`, {
      method: 'POST',
      headers: bearer(token),
      body: { numero: a.numero, andar: a.andar, blocoId: bloco.id, quartos: a.quartos, areaMxComTotal: a.area },
    });
    unidades[a.numero] = r.id;
  }
  log('  ✓', `${cond.nome}: Bloco A + ${cond.aptos.length} apartamentos`);
  return unidades;
}

async function vincularUnidades(cond, i, unidades, ids) {
  const pool = conectar('auth_db');
  const agora = new Date().toISOString();
  let n = 0;
  for (const u of elencoDe(cond, i)) {
    if (!u.apto || !unidades[u.apto]) continue;
    await pool.query('UPDATE users SET "unidadeId"=$1, "updatedAt"=$2 WHERE id=$3',
      [unidades[u.apto], agora, ids[u.email]]);
    n++;
  }
  await pool.end();
  log('  ✓', `${cond.nome}: ${n} vínculos com unidade`);
}

/* ─── 4. financeiro ─────────────────────────────────────────────────────── */

async function configurarFinanceiro(cond, token, unidades) {
  const base = `${FINANCEIRO_API}/api/financeiro`;

  await api(`${base}/config`, {
    method: 'PUT',
    headers: bearer(token),
    body: { modo: cond.modo, diaFechamento: 25, diaVencimento: 10, diasRecursoMulta: 15 },
  });

  // Frações proporcionais à área, somando exatamente 1000 milésimos.
  const pesos = cond.aptos.map((a) => ({ chave: unidades[a.numero], peso: a.area }));
  const parcelas = ratear(1000, pesos);
  await api(`${base}/fracoes/aplicar-lote`, {
    method: 'POST',
    headers: bearer(token),
    body: { fracoes: [...parcelas].map(([unidadeId, milesimos]) => ({ unidadeId, milesimos })) },
  });

  // No modo proporcional o valor cadastrado é o total do condomínio; no fixo é
  // por unidade. Cadastrar sem olhar o modo cobraria errado.
  const total = cond.modo === 'FRACAO_IDEAL';
  await api(`${base}/tipos-taxa`, {
    method: 'POST',
    headers: bearer(token),
    body: {
      nome: 'Taxa condominial',
      descricao: 'Manutenção, limpeza e serviços',
      valor: total ? '1400,00' : '350,00',
      baseCalculo: total ? 'TOTAL_CONDOMINIO' : 'POR_UNIDADE',
      periodicidade: 'MENSAL',
    },
  });
  await api(`${base}/tipos-taxa`, {
    method: 'POST',
    headers: bearer(token),
    body: {
      nome: 'Fundo de reserva',
      descricao: 'Reserva para obras e emergências',
      valor: '50,00',
      baseCalculo: 'POR_UNIDADE',
      periodicidade: 'MENSAL',
    },
  });

  log('  ✓', `${cond.nome}: modo ${cond.modo}, frações somando 1000, 2 taxas`);
}

/* ─── 5. assinatura ─────────────────────────────────────────────────────── */

async function criarAssinaturas() {
  const pool = conectar('mora_plan');
  const { rows: planos } = await pool.query('SELECT id, name FROM tb_plans ORDER BY id');
  if (!planos.length) {
    log('  ⚠', 'Nenhum plano cadastrado em tb_plans — assinaturas não criadas');
    await pool.end();
    return;
  }

  // Datas de início escalonadas e planos diferentes de propósito: com todas
  // assinando no mesmo dia e no mesmo plano, o gráfico de evolução da receita
  // vira uma linha reta e o de receita por plano, uma fatia só.
  const escalonado = [
    { mesesAtras: 8, plano: planos[0] },
    { mesesAtras: 5, plano: planos[planos.length - 1] },
    { mesesAtras: 2, plano: planos[0] },
  ];

  for (const [i, cond] of CONDOMINIOS.entries()) {
    const { mesesAtras, plano } = escalonado[i % escalonado.length];
    const d = new Date();
    d.setMonth(d.getMonth() - mesesAtras);
    d.setDate(1);
    const inicio = d.toISOString().slice(0, 10);

    await pool.query(
      `INSERT INTO tb_assinaturas (condominio_id, plan_id, status, vigencia_inicio, observacao, created_at, updated_at)
       VALUES ($1,$2,'ATIVA',$3,'Assinatura de desenvolvimento', now(), now())`,
      [cond.id, plano.id, inicio],
    );
    log('  ✓', `${cond.nome}: plano "${plano.name}" desde ${inicio}`);
  }
  await pool.end();
}

/* ─── main ──────────────────────────────────────────────────────────────── */

async function main() {
  console.log('\n═══ MORA — seed de desenvolvimento ═══\n');

  if (LIMPAR) {
    await limpar();
    console.log('');
  }

  const ids = await criarUsuarios();
  console.log('');

  log('🏗', 'Criando estrutura e financeiro...');
  for (const [i, cond] of CONDOMINIOS.entries()) {
    const { token } = await api(`${AUTH_API}/api/auth/login`, {
      method: 'POST',
      body: { email: `sindico.${cond.slug}@mora.test`, senha: SENHA },
    });
    const unidades = await criarEstrutura(cond, token);
    await vincularUnidades(cond, i, unidades, ids);
    await configurarFinanceiro(cond, token, unidades);
  }

  console.log('');
  log('📄', 'Criando assinaturas...');
  await criarAssinaturas();

  console.log(`\n═══ pronto ═══\n\nSenha de todos: ${SENHA}\n`);
  console.log('  admin@mora.test                  ADMIN_GERAL (plataforma)\n');
  for (const cond of CONDOMINIOS) {
    console.log(`  ${cond.nome}  [${cond.id}]  ·  rateio ${cond.modo}`);
    console.log(`    sindico.${cond.slug}@mora.test`.padEnd(42) + 'ADMIN_SINDICO');
    console.log(`    porteiro.${cond.slug}@mora.test`.padEnd(42) + 'PORTEIRO');
    cond.aptos.forEach((a, j) =>
      console.log(`    morador${j + 1}.${cond.slug}@mora.test`.padEnd(42) + `MORADOR — apto ${a.numero}`));
    console.log(`    dono.${cond.slug}@mora.test`.padEnd(42) + `DONO_ALUGUEL — apto ${cond.aptos[3].numero}`);
    console.log(`    convidado.${cond.slug}@mora.test`.padEnd(42) + 'CONVIDADO (sem acesso ao sistema)');
    console.log('');
  }
}

main().catch((e) => {
  console.error('\n❌', e.message);
  process.exit(1);
});
