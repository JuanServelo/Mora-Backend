// Verificação de ponta a ponta do comunicacao-service contra a stack real.
//
//   node scripts/testar-local.js
//
// Espera a stack no ar (docker compose up -d) e os usuários de teste criados
// por scripts/seed-dev.mjs. O que não depende de banco fica em npm test.
import 'dotenv/config';

const AUTH = process.env.AUTH_API_URL || 'http://localhost:3001';
const COM = `http://localhost:${process.env.PORT || 3003}`;
const SENHA = process.env.SENHA_TESTE || 'Mora@2024';

let passou = 0;
let falhou = 0;

function conferir(nome, esperado, obtido, detalhe = '') {
  const ok = esperado === obtido;
  ok ? passou++ : falhou++;
  const marca = ok ? 'ok    ' : 'FALHOU';
  const cmp = ok ? `${obtido}` : `esperava ${esperado}, veio ${obtido}`;
  console.log(`${marca} ${nome.padEnd(54)} ${cmp}${detalhe ? ' — ' + detalhe : ''}`);
}

async function entrar(email, senha = SENHA) {
  const r = await fetch(`${AUTH}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, senha }),
  });

  // O limitador do auth-api é de 10 tentativas por 15 minutos e, sem
  // `trust proxy`, o balde é compartilhado por todo mundo. Rodar este script
  // duas vezes seguidas basta para estourar — e o sintoma parece "usuário não
  // existe", que manda investigar o lado errado.
  if (r.status === 429) {
    console.log('\nFALHOU o auth-api está limitando as tentativas de login.');
    console.log('       Espere 15 minutos ou reinicie: docker compose -f docker/docker-compose.yml restart auth-api\n');
    process.exit(1);
  }

  const corpo = await r.json().catch(() => ({}));
  return corpo.token || null;
}

const cabecalhos = (token) => ({
  'Content-Type': 'application/json',
  ...(token ? { Authorization: `Bearer ${token}` } : {}),
});

const pegar = (caminho, token) => fetch(`${COM}${caminho}`, { headers: cabecalhos(token) });

const enviar = (metodo, caminho, token, corpo) =>
  fetch(`${COM}${caminho}`, {
    method: metodo,
    headers: cabecalhos(token),
    ...(corpo !== undefined ? { body: JSON.stringify(corpo) } : {}),
  });

/* ------------------------------------------------------------- saúde --- */

console.log('\n── saúde ────────────────────────────────────────────────────────────');
const saude = await pegar('/health').catch(() => null);
if (!saude) {
  console.log(`FALHOU o serviço não respondeu em ${COM} — ele está no ar?`);
  process.exit(1);
}
const corpoSaude = await saude.json();
conferir('/health responde', 200, saude.status);
conferir('banco conectado', 'ok', corpoSaude.banco);
console.log(`       publicação interna: ${corpoSaude.publicacaoInterna}`);

/* ------------------------------------------------------ autenticação --- */

console.log('\n── autenticação ─────────────────────────────────────────────────────');
conferir('sem token é recusado', 401, (await pegar('/api/comunicacao/notificacoes')).status);
conferir('token inválido é recusado', 401,
  (await pegar('/api/comunicacao/notificacoes', 'nao.e.um.token')).status);

/* ---------------------------------------------------------- conversa --- */

console.log('\n── conversa do morador com a administração ───────────────────────────');

const tokenMorador = await entrar('morador1.verde@mora.test');
const tokenSindico = await entrar('sindico.verde@mora.test');
const tokenOutroSindico = await entrar('sindico.vistamar@mora.test');
const tokenConvidado = await entrar('convidado.verde@mora.test');

if (!tokenMorador || !tokenSindico) {
  console.log('FALHOU login dos usuários de teste — rodou scripts/seed-dev.mjs?');
  process.exit(1);
}

const assunto = `Vazamento no 3º andar ${Date.now()}`;

const criacao = await enviar('POST', '/api/comunicacao/conversas', tokenMorador, {
  assunto,
  corpo: 'Tem água escorrendo pela parede do corredor desde ontem.',
});
const criada = await criacao.json();
conferir('morador abre conversa sem escolher destinatário', 201, criacao.status,
  criada.mensagem ?? '');
conferir('tipo assumido é ADMINISTRACAO', 'ADMINISTRACAO', criada.conversa?.tipo);

const conversaId = criada.conversa?.id;

// A regra que justifica o tipo ADMINISTRACAO existir: o morador não lista os
// usuários do condomínio, então não teria como endereçar a uma pessoa.
const direta = await enviar('POST', '/api/comunicacao/conversas', tokenMorador, {
  tipo: 'DIRETA',
  destinatarioId: 2,
  assunto: 'oi',
  corpo: 'tudo bem?',
});
conferir('morador não abre conversa direta', 403, direta.status);
conferir('morador não lista contatos', 403,
  (await pegar('/api/comunicacao/contatos', tokenMorador)).status);

conferir('convidado não acessa conversas', 403,
  tokenConvidado ? (await pegar('/api/comunicacao/conversas', tokenConvidado)).status : 403,
  tokenConvidado ? '' : 'convidado sem acesso ao sistema, como esperado');

/* ------------------------------------------------- visibilidade da gestão --- */

console.log('\n── visibilidade e isolamento ─────────────────────────────────────────');

const listaSindico = await (await pegar('/api/comunicacao/conversas', tokenSindico)).json();
const vista = listaSindico.conversas?.find((c) => c.id === conversaId);
conferir('síndico vê a conversa sem ter sido adicionado', true, Boolean(vista));
conferir('conversa nova conta como não lida para o síndico', 1, vista?.naoLidas);
conferir('resumo traz o começo da mensagem', true,
  typeof vista?.resumo === 'string' && vista.resumo.startsWith('Tem água'));

if (tokenOutroSindico) {
  const alheia = await pegar(`/api/comunicacao/conversas/${conversaId}`, tokenOutroSindico);
  conferir('síndico de outro condomínio recebe 404', 404, alheia.status);

  const listaAlheia = await (await pegar('/api/comunicacao/conversas', tokenOutroSindico)).json();
  conferir('conversa não aparece na lista do outro condomínio', false,
    Boolean(listaAlheia.conversas?.some((c) => c.id === conversaId)));
}

/* --------------------------------------------------------- resposta --- */

console.log('\n── resposta e notificação ────────────────────────────────────────────');

const resposta = await enviar('POST', `/api/comunicacao/conversas/${conversaId}/mensagens`,
  tokenSindico, { corpo: 'Recebido. O encanador vai hoje à tarde.' });
conferir('síndico responde', 201, resposta.status);

const detalhe = await (await pegar(`/api/comunicacao/conversas/${conversaId}`, tokenMorador)).json();
conferir('conversa tem as duas mensagens', 2, detalhe.mensagens?.length);
conferir('síndico virou participante ao responder', 2, detalhe.participantes?.length);

const caixa = await (await pegar('/api/comunicacao/notificacoes', tokenMorador)).json();
const notificacao = caixa.notificacoes?.find((n) => n.dados?.conversaId === conversaId);
conferir('morador foi notificado da resposta', true, Boolean(notificacao));
conferir('notificação aponta para a conversa', conversaId, notificacao?.dados?.conversaId);

// Detalhar marca como lida; pedir de novo não pode continuar contando.
const listaDepois = await (await pegar('/api/comunicacao/conversas', tokenMorador)).json();
conferir('abrir a conversa zera o não lido do morador', 0,
  listaDepois.conversas?.find((c) => c.id === conversaId)?.naoLidas);

const marcada = await enviar('PATCH', `/api/comunicacao/notificacoes/${notificacao?.id}/lida`, tokenMorador);
conferir('marcar notificação como lida', 200, marcada.status);
conferir('notificação de outro usuário responde 404', 404,
  (await enviar('PATCH', `/api/comunicacao/notificacoes/${notificacao?.id}/lida`, tokenSindico)).status);

/* --------------------------------------------------------- encerrar --- */

console.log('\n── encerramento ──────────────────────────────────────────────────────');
conferir('morador não encerra a conversa', 403,
  (await enviar('PATCH', `/api/comunicacao/conversas/${conversaId}/encerrar`, tokenMorador)).status);
conferir('síndico encerra', 200,
  (await enviar('PATCH', `/api/comunicacao/conversas/${conversaId}/encerrar`, tokenSindico)).status);
conferir('conversa encerrada não aceita mensagem', 409,
  (await enviar('POST', `/api/comunicacao/conversas/${conversaId}/mensagens`, tokenMorador,
    { corpo: 'obrigado' })).status);

/* --------------------------------------------------- rota de serviço --- */

console.log('\n── publicação entre serviços ─────────────────────────────────────────');

const servicoToken = process.env.SERVICO_TOKEN;

if (!servicoToken) {
  console.log('       SERVICO_TOKEN não definido — rota interna deve responder 503');
  conferir('rota interna fechada sem configuração', 503,
    (await enviar('POST', '/api/comunicacao/interno/notificacoes', null, {})).status);
} else {
  const semCredencial = await fetch(`${COM}/api/comunicacao/interno/notificacoes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({}),
  });
  conferir('rota interna recusa sem credencial', 401, semCredencial.status);

  const chave = `fatura:teste:${Date.now()}`;
  const publicar = (corpo) =>
    fetch(`${COM}/api/comunicacao/interno/notificacoes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Servico-Token': servicoToken },
      body: JSON.stringify(corpo),
    });

  const carga = {
    condominioId: 'cond-parque-verde',
    usuarioId: detalhe.participantes?.[0]?.usuarioId,
    origem: 'financeiro',
    tipo: 'FATURA_EMITIDA',
    titulo: 'Fatura de setembro disponível',
    mensagem: 'Vence em 10/10. Boleto e PIX na tela de faturas.',
    dados: { competencia: '2026-09' },
    chaveUnica: chave,
  };

  const primeira = await publicar(carga);
  conferir('financeiro publica notificação', 201, primeira.status);

  // O fechamento pode rodar duas vezes na mesma competência. A segunda não
  // pode virar notificação nova, e também não pode ser erro — erro faria o
  // publicador tentar de novo para sempre.
  const segunda = await publicar(carga);
  const corpoSegunda = await segunda.json();
  conferir('reenvio com a mesma chave não duplica', 200, segunda.status);
  conferir('reenvio é reconhecido como repetido', true, corpoSegunda.repetida === true);

  conferir('origem desconhecida é recusada', 400,
    (await publicar({ ...carga, origem: 'inventado', chaveUnica: `${chave}:x` })).status);
}

/* --------------------------------------------------- leitura de aviso --- */

console.log('\n── confirmação de leitura de aviso ───────────────────────────────────');

conferir('aviso com id inválido é 400', 400,
  (await enviar('POST', '/api/comunicacao/avisos/123/leitura', tokenMorador)).status);

const avisos = await pegar('/api/comunicacao/avisos', tokenMorador);
const corpoAvisos = await avisos.json();

if (avisos.status === 503) {
  console.log('       portaria indisponível — as rotas de aviso ficaram sem verificação');
} else {
  conferir('lista de avisos responde', 200, avisos.status);

  const alvo = corpoAvisos.avisos?.[0];
  if (!alvo) {
    console.log('       nenhum aviso ativo no condomínio — publique um pelo portaria para verificar');
  } else {
    const c1 = await enviar('POST', `/api/comunicacao/avisos/${alvo.id}/leitura`, tokenMorador);
    conferir('morador confirma a leitura', 201, c1.status);

    const c2 = await enviar('POST', `/api/comunicacao/avisos/${alvo.id}/leitura`, tokenMorador);
    const corpo2 = await c2.json();
    const corpo1 = await c1.json();
    conferir('confirmar de novo mantém a primeira data', corpo1.leitura?.confirmadaEm,
      corpo2.leitura?.confirmadaEm);

    const rel = await pegar(`/api/comunicacao/avisos/${alvo.id}/leituras`, tokenSindico);
    const relatorio = await rel.json();
    conferir('síndico lê o relatório', 200, rel.status);
    conferir('relatório sabe o total de destinatários', true,
      typeof relatorio.total === 'number' && relatorio.total > 0,
      `${relatorio.confirmados}/${relatorio.total} confirmaram`);

    conferir('morador não lê o relatório', 403,
      (await pegar(`/api/comunicacao/avisos/${alvo.id}/leituras`, tokenMorador)).status);

    // Não há FK para `avisos` — ela está em outro banco. Quem impede confirmar
    // aviso alheio é a checagem de condomínio contra o portaria, e é só ela.
    const tokenMoradorOutro = await entrar('morador1.vistamar@mora.test');
    if (tokenMoradorOutro) {
      conferir('morador de outro condomínio não confirma este aviso', 404,
        (await enviar('POST', `/api/comunicacao/avisos/${alvo.id}/leitura`, tokenMoradorOutro)).status);
    }

    const panorama = await pegar('/api/comunicacao/avisos/leituras', tokenSindico);
    const corpoPanorama = await panorama.json();
    conferir('panorama da gestão responde', 200, panorama.status);
    conferir('panorama conta as confirmações', true,
      (corpoPanorama.avisos?.find((a) => a.id === alvo.id)?.confirmacoes ?? 0) >= 1);
  }
}

/* ------------------------------------------------ remoção de mensagem --- */

console.log('\n── remoção de mensagem ───────────────────────────────────────────────');
{
  const nova = await enviar('POST', '/api/comunicacao/conversas', tokenMorador, {
    assunto: `Barulho no 2º andar ${Date.now()}`,
    corpo: 'Som alto depois das 23h com frequência.',
  });
  const corpoNova = await nova.json();
  const mensagemId = corpoNova.mensagem?.id;

  conferir('síndico não apaga mensagem do morador', 404,
    (await enviar('DELETE', `/api/comunicacao/mensagens/${mensagemId}`, tokenSindico)).status);
  conferir('autor apaga a própria mensagem', 200,
    (await enviar('DELETE', `/api/comunicacao/mensagens/${mensagemId}`, tokenMorador)).status);

  // A linha continua na conversa, sem o texto: sumir com ela deixaria o
  // histórico incoerente para quem já tinha lido.
  const depois = await (await pegar(`/api/comunicacao/conversas/${corpoNova.conversa.id}`, tokenSindico)).json();
  const apagada = depois.mensagens?.find((m) => m.id === mensagemId);
  conferir('mensagem removida continua na conversa', true, Boolean(apagada));
  conferir('texto da mensagem removida não é devolvido', null, apagada?.corpo);
}

/* ------------------------------------------------------------ resumo --- */

const resumo = await pegar('/api/comunicacao/notificacoes/resumo', tokenMorador);
conferir('resumo do sino responde', 200, resumo.status);

console.log(`\n${passou} passaram, ${falhou} falharam\n`);
process.exit(falhou > 0 ? 1 : 0);
