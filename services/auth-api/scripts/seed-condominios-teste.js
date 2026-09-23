import dotenv from 'dotenv';
import bcrypt from 'bcryptjs';
import sequelize from '../config/database.js';
import { PERFIS, STATUS_USUARIO, STATUS_CONVITE } from '../constants/perfis.js';

dotenv.config();

/**
 * Dados de teste: 6 condomínios de portes diferentes.
 *
 * Todo registro criado aqui é marcado com o prefixo `qa-` no id do condomínio
 * e no e-mail dos usuários, para que `--limpar` consiga removê-los sem tocar
 * nos dados reais.
 *
 *   node scripts/seed-condominios-teste.js
 *   node scripts/seed-condominios-teste.js --limpar
 */

const PREFIXO = 'qa-';
const SENHA_PADRAO = 'Teste1234';
const DOMINIO = 'qa.mora.local';

/** Porte define quantos usuários e como se distribuem entre os perfis. */
const CONDOMINIOS = [
  {
    id: 'qa-vila-serena',
    nome: 'Residencial Vila Serena',
    porte: 'grande',
    cnpj: '12345678000101',
    cidade: 'Curitiba',
    endereco: 'Av. das Araucárias, 1200 — Ecoville, Curitiba/PR',
    telefone: '4133215500',
    mesesAtras: 11,
    moradores: 96,
    donos: 14,
    porteiros: 4,
    sindicos: 2,
    convidados: 6,
  },
  {
    id: 'qa-parque-atlantico',
    nome: 'Condomínio Parque Atlântico',
    porte: 'grande',
    cnpj: '12345678000102',
    cidade: 'Florianópolis',
    endereco: 'Rua Beira-Mar Norte, 890 — Centro, Florianópolis/SC',
    telefone: '4832214477',
    mesesAtras: 9,
    moradores: 78,
    donos: 11,
    porteiros: 3,
    sindicos: 2,
    convidados: 5,
  },
  {
    id: 'qa-jardim-europa',
    nome: 'Edifício Jardim Europa',
    porte: 'médio',
    cnpj: '12345678000103',
    cidade: 'Curitiba',
    endereco: 'Rua Padre Anchieta, 455 — Bigorrilho, Curitiba/PR',
    telefone: '4133449988',
    mesesAtras: 7,
    moradores: 38,
    donos: 6,
    porteiros: 2,
    sindicos: 1,
    convidados: 3,
  },
  {
    id: 'qa-mirante-sul',
    nome: 'Residencial Mirante Sul',
    porte: 'médio',
    cnpj: '12345678000104',
    cidade: 'Porto Alegre',
    endereco: 'Av. Ipiranga, 3200 — Partenon, Porto Alegre/RS',
    telefone: '5133887766',
    mesesAtras: 5,
    moradores: 32,
    donos: 5,
    porteiros: 2,
    sindicos: 1,
    convidados: 2,
  },
  {
    id: 'qa-alameda-flores',
    nome: 'Condomínio Alameda das Flores',
    porte: 'médio',
    cnpj: '12345678000105',
    cidade: 'Joinville',
    endereco: 'Rua XV de Novembro, 780 — Centro, Joinville/SC',
    telefone: '4734226611',
    mesesAtras: 3,
    moradores: 27,
    donos: 4,
    porteiros: 1,
    sindicos: 1,
    convidados: 2,
  },
  {
    id: 'qa-recanto-pinheiros',
    nome: 'Recanto dos Pinheiros',
    porte: 'pequeno',
    cnpj: '12345678000106',
    cidade: 'Ponta Grossa',
    endereco: 'Rua Balduíno Taques, 145 — Centro, Ponta Grossa/PR',
    telefone: '4232238899',
    mesesAtras: 1,
    moradores: 10,
    donos: 2,
    porteiros: 1,
    sindicos: 1,
    convidados: 1,
  },
];

const NOMES = ['Ana', 'Bruno', 'Carla', 'Diego', 'Elisa', 'Fábio', 'Gabriela', 'Henrique',
  'Isabela', 'João', 'Karina', 'Lucas', 'Mariana', 'Nelson', 'Olívia', 'Paulo',
  'Queila', 'Rafael', 'Sofia', 'Thiago', 'Úrsula', 'Vinícius', 'Yara', 'Zeca'];
const SOBRENOMES = ['Almeida', 'Barbosa', 'Cardoso', 'Duarte', 'Esteves', 'Ferreira',
  'Gonçalves', 'Henriques', 'Ibrahim', 'Jardim', 'Klein', 'Lopes', 'Moraes',
  'Nogueira', 'Oliveira', 'Pereira', 'Ramos', 'Silveira', 'Tavares', 'Vieira'];

const CATEGORIAS = ['Barulho', 'Vazamento', 'Limpeza', 'Animais', 'Vandalismo', 'Outros'];
const STATUS_OCORRENCIA = ['PENDENTE', 'EM_ANALISE', 'RESOLVIDO'];

/** Gerador determinístico: rodar duas vezes produz os mesmos nomes. */
function criarRandom(semente) {
  let s = semente;
  return () => {
    s = (s * 1103515245 + 12345) % 2147483648;
    return s / 2147483648;
  };
}

const dataAtras = (meses, dias = 0) => {
  const d = new Date();
  d.setMonth(d.getMonth() - meses);
  d.setDate(Math.max(1, d.getDate() - dias));
  return d;
};

async function limpar() {
  const t = await sequelize.transaction();
  try {
    const opts = { transaction: t };
    await sequelize.query(
      `DELETE FROM reclamacoes WHERE "condominioId" LIKE '${PREFIXO}%'`, opts);
    await sequelize.query(
      `DELETE FROM invites WHERE "condominioId" LIKE '${PREFIXO}%'`, opts);
    await sequelize.query(
      `DELETE FROM users WHERE "condominioId" LIKE '${PREFIXO}%' OR email LIKE '%@${DOMINIO}'`, opts);
    const [, meta] = await sequelize.query(
      `DELETE FROM condominios WHERE id LIKE '${PREFIXO}%'`, opts);
    await t.commit();
    console.log(`Removidos ${meta?.rowCount ?? 0} condomínios de teste e seus dados.`);
  } catch (err) {
    await t.rollback();
    throw err;
  }
}

async function semear() {
  const senhaHash = await bcrypt.hash(SENHA_PADRAO, 10);
  const t = await sequelize.transaction();

  try {
    const opts = { transaction: t };
    let totalUsuarios = 0;
    let totalConvites = 0;
    let totalOcorrencias = 0;

    for (const [indice, cond] of CONDOMINIOS.entries()) {
      const rnd = criarRandom(indice + 1);
      const criadoEm = dataAtras(cond.mesesAtras, Math.floor(rnd() * 20));

      await sequelize.query(`
        INSERT INTO condominios (id, nome, cnpj, endereco, telefone, email, status, "createdAt", "updatedAt")
        VALUES (:id, :nome, :cnpj, :endereco, :telefone, :email, 'active', :criadoEm, :criadoEm)
        ON CONFLICT (id) DO UPDATE SET
          nome = EXCLUDED.nome, cnpj = EXCLUDED.cnpj, endereco = EXCLUDED.endereco,
          telefone = EXCLUDED.telefone, email = EXCLUDED.email, "createdAt" = EXCLUDED."createdAt"
      `, {
        ...opts,
        replacements: {
          id: cond.id,
          nome: cond.nome,
          cnpj: cond.cnpj,
          endereco: cond.endereco,
          telefone: cond.telefone,
          email: `contato@${cond.id.replace(PREFIXO, '')}.com.br`,
          criadoEm,
        },
      });

      // Usuários, na proporção do porte.
      const perfis = [
        ...Array(cond.sindicos).fill(PERFIS.ADMIN_SINDICO),
        ...Array(cond.porteiros).fill(PERFIS.PORTEIRO),
        ...Array(cond.moradores).fill(PERFIS.MORADOR),
        ...Array(cond.donos).fill(PERFIS.DONO_ALUGUEL),
        ...Array(cond.convidados).fill(PERFIS.CONVIDADO),
      ];

      const idsMoradores = [];

      for (const [i, perfil] of perfis.entries()) {
        const nome = `${NOMES[Math.floor(rnd() * NOMES.length)]} ${SOBRENOMES[Math.floor(rnd() * SOBRENOMES.length)]}`;
        const email = `${cond.id.replace(PREFIXO, '')}.${i}@${DOMINIO}`;
        // Uma fatia pequena fica pendente ou inativa, para os KPIs não serem 100% ativos.
        const sorteio = rnd();
        const status = sorteio > 0.94
          ? STATUS_USUARIO.INACTIVE
          : sorteio > 0.86
            ? STATUS_USUARIO.PENDING_ACTIVATION
            : STATUS_USUARIO.ACTIVE;

        const [linhas] = await sequelize.query(`
          INSERT INTO users (nome, email, senha, perfil, status, "condominioId",
                             "responsavelFinanceiro", "semAcessoSistema", "tokenVersion",
                             "activatedAt", "createdAt", "updatedAt")
          VALUES (:nome, :email, :senha, :perfil, :status, :condominioId,
                  :responsavel, :semAcesso, 0, :criadoEm, :criadoEm, :criadoEm)
          ON CONFLICT (email) DO UPDATE SET
            nome = EXCLUDED.nome, perfil = EXCLUDED.perfil, status = EXCLUDED.status,
            "condominioId" = EXCLUDED."condominioId"
          RETURNING id
        `, {
          ...opts,
          replacements: {
            nome,
            email,
            senha: senhaHash,
            perfil,
            status,
            condominioId: cond.id,
            // Um em cada três moradores responde pela fatura da unidade.
            responsavel: perfil === PERFIS.MORADOR && i % 3 === 0,
            semAcesso: perfil === PERFIS.CONVIDADO,
            criadoEm: dataAtras(cond.mesesAtras, Math.floor(rnd() * 25)),
          },
        });

        if (perfil === PERFIS.MORADOR && linhas?.[0]?.id) idsMoradores.push(linhas[0].id);
        totalUsuarios += 1;
      }

      // Convites pendentes, proporcionais ao porte.
      const convites = cond.porte === 'grande' ? 6 : cond.porte === 'médio' ? 3 : 1;
      const [[sindico]] = await sequelize.query(
        `SELECT id FROM users WHERE "condominioId" = :c AND perfil = :p LIMIT 1`,
        { ...opts, replacements: { c: cond.id, p: PERFIS.ADMIN_SINDICO } },
      );

      for (let i = 0; i < convites; i += 1) {
        const expira = new Date(Date.now() + (24 + i) * 60 * 60 * 1000);
        await sequelize.query(`
          INSERT INTO invites (codigo, email, perfil, "condominioId", "cadastradoPorId",
                               status, "responsavelFinanceiro", "expiresAt", "createdAt", "updatedAt")
          VALUES (:codigo, :email, :perfil, :condominioId, :por, :status, false, :expira, NOW(), NOW())
          ON CONFLICT (codigo) DO NOTHING
        `, {
          ...opts,
          replacements: {
            codigo: `QA${cond.id.slice(3, 7).toUpperCase()}${String(i).padStart(2, '0')}`,
            email: `convite.${i}.${cond.id.replace(PREFIXO, '')}@${DOMINIO}`,
            perfil: PERFIS.MORADOR,
            condominioId: cond.id,
            por: sindico?.id ?? null,
            status: STATUS_CONVITE.PENDING,
            expira,
          },
        });
        totalConvites += 1;
      }

      // Ocorrências abertas e resolvidas, para o KPI e a série do painel.
      const ocorrencias = cond.porte === 'grande' ? 9 : cond.porte === 'médio' ? 5 : 2;
      for (let i = 0; i < ocorrencias && idsMoradores.length; i += 1) {
        const autor = idsMoradores[Math.floor(rnd() * idsMoradores.length)];
        const status = STATUS_OCORRENCIA[Math.floor(rnd() * STATUS_OCORRENCIA.length)];
        await sequelize.query(`
          INSERT INTO reclamacoes ("userId", "protocolNumber", category, description,
                                   status, interactions, "condominioId", "createdAt", "updatedAt")
          VALUES (:autor, :protocolo, :categoria, :descricao, :status, '[]'::json, :cond, :criadoEm, :criadoEm)
          ON CONFLICT ("protocolNumber") DO NOTHING
        `, {
          ...opts,
          replacements: {
            autor,
            protocolo: `QA-${cond.id.slice(3, 9).toUpperCase()}-${String(i).padStart(3, '0')}`,
            categoria: CATEGORIAS[Math.floor(rnd() * CATEGORIAS.length)],
            descricao: 'Registro gerado para testes de painel.',
            status,
            cond: cond.id,
            criadoEm: dataAtras(0, Math.floor(rnd() * 45)),
          },
        });
        totalOcorrencias += 1;
      }

      console.log(
        `  ${cond.porte.padEnd(7)} ${cond.nome.padEnd(32)} `
        + `${String(perfis.length).padStart(3)} usuários · ${convites} convites · ${ocorrencias} ocorrências`,
      );
    }

    await t.commit();
    console.log(`\n${CONDOMINIOS.length} condomínios · ${totalUsuarios} usuários · `
      + `${totalConvites} convites · ${totalOcorrencias} ocorrências`);
    console.log(`Senha de todos os usuários de teste: ${SENHA_PADRAO}`);
  } catch (err) {
    await t.rollback();
    throw err;
  }
}

const limparApenas = process.argv.includes('--limpar');

try {
  await sequelize.authenticate();
  if (limparApenas) {
    await limpar();
  } else {
    // Recria do zero para o script ser reexecutável sem acumular lixo.
    await limpar();
    console.log('Semeando condomínios de teste...\n');
    await semear();
  }
} catch (err) {
  console.error('Falhou:', err.message);
  process.exitCode = 1;
} finally {
  await sequelize.close();
}
