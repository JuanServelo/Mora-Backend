/**
 * Roda a collection no Newman e apaga o que o teste criou.
 *
 * Os dois segredos que o teste precisa não ficam em arquivo nenhum desta pasta:
 *
 * - a senha das contas de teste vem de `SENHA_SEED`, ou do padrão definido em
 *   `scripts/seed-dev.mjs` — a mesma que o seed usou;
 * - o `SERVICO_TOKEN` (rota interna do comunicacao) vem do `docker/.env`.
 *
 * O relatório HTML esconde o corpo e a resposta dos logins e os cabeçalhos com
 * token, para poder ser compartilhado.
 *
 * Pré-requisitos: a stack no ar e o seed aplicado (`npm run seed` na raiz).
 */
import { readFileSync, existsSync, mkdirSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import newman from 'newman';

const aqui = (p) => new URL(p, import.meta.url);

function senhaDoSeed() {
  if (process.env.SENHA_SEED) return process.env.SENHA_SEED;
  const fonte = readFileSync(aqui('../scripts/seed-dev.mjs'), 'utf8');
  const achou = fonte.match(/SENHA_SEED \|\| '([^']+)'/);
  if (!achou) throw new Error('Não achei a senha padrão em scripts/seed-dev.mjs; defina SENHA_SEED.');
  return achou[1];
}

function doDockerEnv(chave) {
  const arquivo = aqui('../docker/.env');
  if (!existsSync(arquivo)) return process.env[chave] ?? '';
  const linha = readFileSync(arquivo, 'utf8').split(/\r?\n/).find((l) => l.startsWith(`${chave}=`));
  return linha ? linha.slice(chave.length + 1).trim() : (process.env[chave] ?? '');
}

const servicoToken = doDockerEnv('SERVICO_TOKEN');
if (!servicoToken) {
  console.error('SERVICO_TOKEN vazio no docker/.env — os testes da rota interna dependem dele.');
  process.exit(1);
}

const colecao = JSON.parse(readFileSync(aqui('./mora.postman_collection.json'), 'utf8'));
const nomesDeLogin = colecao.item.flatMap((p) => p.item).map((r) => r.name).filter((n) => n.startsWith('Login'));

mkdirSync(aqui('./relatorio/'), { recursive: true });

// O que o teste criou, lido das respostas: as variáveis da collection não
// voltam no resumo do Newman.
const criados = {};
const guardar = {
  'Síndico aplica multa ao morador': (r) => { criados.multaId = String(r.multa?.id ?? ''); },
  'Upload de imagem PNG': (r) => { criados.imagemUrl = r.url; },
};

newman.run({
  collection: colecao,
  environment: JSON.parse(readFileSync(aqui('./local.postman_environment.json'), 'utf8')),
  envVar: [
    { key: 'senha', value: senhaDoSeed() },
    { key: 'servicoToken', value: servicoToken },
  ],
  workingDir: new URL('.', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'),
  reporters: ['cli', 'htmlextra'],
  reporter: {
    htmlextra: {
      export: './relatorio/relatorio-newman.html',
      title: 'Mora — teste de API',
      browserTitle: 'Mora — teste de API',
      hideRequestBody: nomesDeLogin,
      hideResponseBody: nomesDeLogin,
      skipHeaders: 'Authorization,X-Servico-Token',
      skipEnvironmentVars: true,
      skipGlobalVars: true,
    },
  },
}, (erro, resumo) => {
  if (erro) {
    console.error('Falha ao rodar o Newman:', erro.message);
    process.exit(1);
  }
  limpar(criados);
  const falhas = resumo.run.failures.length;
  console.log(falhas ? `\n${falhas} asserção(ões) falharam.` : '\nTodas as asserções passaram.');
  console.log('Relatório: tests-api/relatorio/relatorio-newman.html');
  process.exit(falhas ? 1 : 0);
}).on('request', (erro, args) => {
  const ler = guardar[args.item?.name];
  if (erro || !ler || !args.response) return;
  try { ler(JSON.parse(args.response.stream.toString())); } catch { /* resposta sem JSON: nada a guardar */ }
});

/** Apaga o que o teste criou e não tem rota de exclusão. Tudo leva `[newman]`. */
function limpar(vars) {
  const psql = (banco, sql) => execFileSync('docker',
    ['exec', 'postgres', 'psql', '-q', '-U', 'admin', '-d', banco, '-v', 'ON_ERROR_STOP=1', '-c', sql],
    { stdio: ['ignore', 'pipe', 'pipe'] });

  // O id vai para dentro do SQL; só passa se tiver o formato esperado.
  const multaId = /^[\w-]+$/.test(vars.multaId ?? '') ? vars.multaId : null;
  const tarefas = [
    ['multas', () => psql('mora_financeiro', "DELETE FROM multas WHERE motivo LIKE '[newman]%'")],
    ['notificações', () => psql('mora', `DELETE FROM notificacoes WHERE titulo LIKE '[newman]%'${
      multaId ? ` OR chave_unica = 'financeiro:MULTA_APLICADA:${multaId}'` : ''}`)],
    ['avisos que sobraram', () => psql('mora',
      "DELETE FROM aviso_leituras WHERE aviso_id IN (SELECT id FROM avisos WHERE titulo LIKE '[newman]%'); "
      + "DELETE FROM avisos WHERE titulo LIKE '[newman]%'")],
    ['ocorrências', () => psql('auth_db', "DELETE FROM reclamacoes WHERE description LIKE '[newman]%'")],
  ];
  const imagem = (vars.imagemUrl ?? '').match(/^\/uploads\/avisos\/([0-9a-f-]{36}\.(png|jpg))$/);
  if (imagem) {
    tarefas.push(['imagem', () => execFileSync('docker',
      ['exec', 'comunicacao-service', 'rm', '-f', `/app/uploads/avisos/${imagem[1]}`])]);
  }

  console.log('\nLimpeza:');
  for (const [nome, fazer] of tarefas) {
    try {
      fazer();
      console.log(`  ✔ ${nome}`);
    } catch (e) {
      console.log(`  ✘ ${nome}: ${String(e.stderr ?? e.message).trim()}`);
    }
  }
}
