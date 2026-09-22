// Move as notificações já gravadas na tabela local para o comunicacao-service.
//
//   node scripts/migrar-notificacoes.js          confere e mostra o que faria
//   node scripts/migrar-notificacoes.js --gravar  publica de verdade
//
// A escrita nova já vai direto para o comunicacao. Isto é só o histórico: sem
// ele, o morador abriria a tela depois da virada e veria a caixa vazia, como se
// as faturas anteriores nunca tivessem sido avisadas.
//
// É idempotente — cada registro vai com a chave `financeiro:legado:<id>`, e o
// comunicacao recusa a segunda entrada da mesma chave. Rodar duas vezes não
// duplica nada.
import 'dotenv/config';
import { pool } from '../config/database.js';
import { SERVICOS, TOKEN_SERVICO } from '../config/servicos.js';

const gravar = process.argv.includes('--gravar');

if (!TOKEN_SERVICO) {
  console.error('SERVICO_TOKEN não definido — sem ele o comunicacao recusa a publicação.');
  process.exit(1);
}

const { rows } = await pool.query(
  `SELECT id, usuario_id AS "usuarioId", condominio_id AS "condominioId",
          tipo, titulo, mensagem, lida, metadados, criado_em AS "criadoEm"
     FROM notificacoes
    ORDER BY criado_em ASC`,
);

console.log(`\n${rows.length} notificação(ões) na tabela local do financeiro.`);

if (rows.length === 0) {
  await pool.end();
  process.exit(0);
}

const semCondominio = rows.filter((n) => !n.condominioId);
if (semCondominio.length) {
  // `condominio_id` é nullable aqui e obrigatório lá. Sem ele não dá para
  // publicar, e inventar um valor colocaria a notificação no condomínio errado.
  console.log(`  ${semCondominio.length} sem condominio_id — não podem ser migradas.`);
}

const migraveis = rows.filter((n) => n.condominioId);
console.log(`  ${migraveis.length} migráveis.`);

if (!gravar) {
  console.log('\nPrévia (use --gravar para publicar):\n');
  for (const n of migraveis.slice(0, 10)) {
    console.log(`  #${n.id} usuário ${n.usuarioId} · ${n.tipo} · ${n.lida ? 'lida' : 'não lida'}`);
    console.log(`      ${n.titulo}`);
  }
  if (migraveis.length > 10) console.log(`  … e mais ${migraveis.length - 10}`);
  console.log();
  await pool.end();
  process.exit(0);
}

let publicadas = 0;
let repetidas = 0;
let falhas = 0;

for (const n of migraveis) {
  try {
    const r = await fetch(`${SERVICOS.comunicacao}/api/comunicacao/interno/notificacoes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Servico-Token': TOKEN_SERVICO },
      body: JSON.stringify({
        condominioId: n.condominioId,
        usuarioId: n.usuarioId,
        origem: 'financeiro',
        tipo: n.tipo,
        titulo: n.titulo,
        mensagem: n.mensagem ?? n.titulo,
        dados: { ...(n.metadados ?? {}), migradoDe: n.id },
        chaveUnica: `financeiro:legado:${n.id}`,
        // Preserva quando a notificação aconteceu e se já tinha sido lida. Sem
        // isso o histórico inteiro reapareceria como "agora mesmo" e não lido —
        // o morador abriria a tela com as faturas antigas todas pedindo atenção.
        criadoEm: n.criadoEm,
        lidaEm: n.lida ? n.criadoEm : null,
      }),
      signal: AbortSignal.timeout(5000),
    });

    const corpo = await r.json().catch(() => ({}));

    if (r.status === 201) publicadas += 1;
    else if (r.status === 200 && corpo.repetida) repetidas += 1;
    else {
      falhas += 1;
      console.log(`  falhou #${n.id}: HTTP ${r.status} ${corpo.mensagem ?? ''}`);
    }
  } catch (err) {
    falhas += 1;
    console.log(`  falhou #${n.id}: ${err.message}`);
  }
}

console.log(`\n${publicadas} publicadas · ${repetidas} já existiam · ${falhas} falharam`);

await pool.end();
