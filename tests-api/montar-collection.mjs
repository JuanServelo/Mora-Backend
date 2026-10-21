/**
 * Gera `mora.postman_collection.json` a partir das definições abaixo.
 *
 * A collection é escrita aqui, em JavaScript, e não direto no JSON: no formato
 * do Postman cada script de teste é uma lista de linhas dentro de aspas, o que
 * torna revisar e editar à mão impraticável. Mudou um teste? Edite aqui e rode
 * `npm run montar` (o `npm test` já monta antes de rodar).
 *
 * Ordem importa: as pastas rodam em sequência e cada uma usa o que as
 * anteriores guardaram (tokens, ids). Tudo que é criado leva `[newman]` no
 * texto, para a limpeza do `rodar.mjs` achar.
 */
import { writeFileSync } from 'node:fs';

/** Corpo de uma função como lista de linhas — o formato do script no Postman. */
function linhas(fn) {
  const fonte = fn.toString();
  const seta = fonte.match(/^\(\)\s*=>\s*/);
  // `() => expressão` não tem chaves: o corpo é a própria expressão.
  const corpo = seta && fonte[seta[0].length] !== '{'
    ? `${fonte.slice(seta[0].length)};`
    : fonte.slice(fonte.indexOf('{') + 1, fonte.lastIndexOf('}'));
  const ls = corpo.split('\n');
  while (ls.length && !ls[0].trim()) ls.shift();
  while (ls.length && !ls.at(-1).trim()) ls.pop();
  const recuo = Math.min(...ls.filter((l) => l.trim()).map((l) => l.match(/^ */)[0].length));
  return ls.map((l) => l.slice(recuo));
}
const bearer = (variavel) => [{ key: 'Authorization', value: `Bearer {{${variavel}}}` }];

/** Uma requisição. `testes` e `antes` são funções cujo corpo vira o script. */
function req(nome, metodo, url, { token, corpo, form, testes, antes, headers = [] } = {}) {
  const item = {
    name: nome,
    request: {
      method: metodo,
      header: [...(token ? bearer(token) : []), ...headers],
      url,
    },
    event: [],
  };
  if (corpo !== undefined) {
    item.request.header.push({ key: 'Content-Type', value: 'application/json' });
    item.request.body = {
      mode: 'raw',
      raw: typeof corpo === 'string' ? corpo : JSON.stringify(corpo, null, 2),
      options: { raw: { language: 'json' } },
    };
  }
  if (form) {
    item.request.body = {
      mode: 'formdata',
      formdata: Object.entries(form).map(([key, src]) => ({ key, type: 'file', src })),
    };
  }
  if (antes) item.event.push({ listen: 'prerequest', script: { type: 'text/javascript', exec: linhas(antes) } });
  if (testes) item.event.push({ listen: 'test', script: { type: 'text/javascript', exec: linhas(testes) } });
  return item;
}

const pasta = (name, item, description) => ({ name, description, item });

/** Testes de "status esperado", o caso mais comum. */
const espera = (status, extra = '') => new Function(`
    pm.test('responde ${status}', () => pm.response.to.have.status(${status}));
    ${extra}
`);

const login = (nome, emailVar, prefixo) => req(nome, 'POST', '{{gw}}/api/auth/login', {
  corpo: `{ "email": "{{${emailVar}}}", "senha": "{{senha}}" }`,
  testes: new Function(`
    pm.test('login aceito', () => pm.response.to.have.status(200));
    const r = pm.response.json();
    pm.test('devolve token', () => pm.expect(r.token).to.be.a('string'));
    pm.collectionVariables.set('token${prefixo}', r.token);
    pm.collectionVariables.set('id${prefixo}', String(r.usuario.id));
    pm.collectionVariables.set('condominio${prefixo}', r.usuario.condominioId ?? '');
    pm.collectionVariables.set('unidade${prefixo}', r.usuario.unidadeId ?? '');
  `),
});

// ── 00 Documentação ────────────────────────────────────────────────────────

/**
 * Confere uma especificação OpenAPI servida pelo gateway: toda operação com
 * sumário e descrição, e exemplo em todo corpo e toda resposta de sucesso com
 * conteúdo. O exemplo pode vir do conteúdo ou do esquema (com todos os campos
 * exemplificados, seguindo $ref). Mesma regra dos testes dos serviços Node.
 */
const conferirEspecificacao = (titulo) => new Function(`
    pm.test('responde 200', () => pm.response.to.have.status(200));
    const spec = pm.response.json();
    pm.test('título ${titulo}', () => pm.expect(spec.info.title).to.eql('${titulo}'));
    pm.test('descrição do serviço preenchida', () => pm.expect(spec.info.description).to.be.a('string').and.not.empty);
    const resolver = (s) => { let a = s; for (let i = 0; a && a.$ref && i < 10; i++) a = a.$ref.split('/').slice(1).reduce((o, k) => o && o[k], spec); return a; };
    const temExemplo = (s, d = 0) => {
      s = resolver(s);
      if (!s || d > 6) return false;
      if (s.example !== undefined || s.examples !== undefined || s.enum || s.type === 'boolean') return true;
      if (s.type === 'array') return temExemplo(s.items, d + 1);
      if (s.type === 'string' && s.format === 'binary') return true;
      const props = Object.values(s.properties || {});
      return props.length > 0 && props.every((p) => temExemplo(p, d + 1));
    };
    const conteudoOk = (c) => { const t = Object.values(c || {}); return t.length > 0 && t.every((m) => m.example !== undefined || (m.examples && Object.keys(m.examples).length) || temExemplo(m.schema)); };
    const problemas = [];
    let total = 0;
    for (const [caminho, ops] of Object.entries(spec.paths || {})) {
      for (const metodo of ['get', 'post', 'put', 'patch', 'delete']) {
        const op = ops[metodo];
        if (!op) continue;
        total++;
        const chave = metodo.toUpperCase() + ' ' + caminho;
        if (!op.summary) problemas.push(chave + ': sem summary');
        if (!op.description) problemas.push(chave + ': sem description');
        if (op.requestBody && !conteudoOk(op.requestBody.content)) problemas.push(chave + ': corpo sem exemplo');
        for (const [codigo, r] of Object.entries(op.responses || {})) {
          if (/^[23]/.test(codigo) && r.content && !conteudoOk(r.content)) problemas.push(chave + ': resposta ' + codigo + ' sem exemplo');
        }
      }
    }
    pm.test('documenta ' + total + ' operações', () => pm.expect(total).to.be.above(0));
    pm.test('toda operação com sumário, descrição e exemplos', () => pm.expect(problemas).to.eql([]));
`);

const documentacao = pasta('00 Documentação', [
  req('Portal /docs abre pelo gateway', 'GET', '{{gw}}/docs', {
    testes: () => {
      pm.test('responde 200', () => pm.response.to.have.status(200));
      pm.test('é a página do portal', () => pm.expect(pm.response.text()).to.include('Mora — documentação da API'));
    },
  }),
  req('Especificação do auth-api', 'GET', '{{gw}}/api/auth/docs/openapi.json', { testes: conferirEspecificacao('auth-api') }),
  req('Especificação do portaria', 'GET', '{{gw}}/api/portaria/v3/api-docs', { testes: conferirEspecificacao('portaria-service') }),
  req('Especificação do comunicacao', 'GET', '{{gw}}/api/comunicacao/v3/api-docs', { testes: conferirEspecificacao('comunicacao-service') }),
  req('Especificação do financeiro', 'GET', '{{gw}}/api/financeiro/docs/openapi.json', { testes: conferirEspecificacao('financeiro') }),
  req('Especificação do gestao-geral', 'GET', '{{gw}}/api/gestao/docs/openapi.json', { testes: conferirEspecificacao('gestao-geral') }),
  req('Especificação do plan', 'GET', '{{gw}}/api/plans/v3/api-docs', { testes: conferirEspecificacao('plan-service') }),
], 'O portal e a especificação de cada serviço, pelo gateway. O meeting-service fica de fora até voltar a subir.');

// ── 00 Infraestrutura ──────────────────────────────────────────────────────

const infra = pasta('00 Infraestrutura', [
  req('Consul lista os serviços Node e Java', 'GET', '{{consul}}/v1/catalog/services', {
    testes: () => {
      const s = pm.response.json();
      ['auth-api', 'comunicacao-service', 'financeiro', 'gestao-geral', 'portaria-service']
        .forEach((nome) => pm.test(`${nome} registrado`, () => pm.expect(s).to.have.property(nome)));
      pm.test('auth-api não anuncia /api/portaria (é do portaria-service)', () => {
        pm.expect(s['auth-api'].join(' ')).to.not.include('/api/portaria');
      });
    },
  }),
  req('Swagger do comunicacao responde', 'GET', '{{comunicacao}}/v3/api-docs', {
    testes: () => {
      pm.test('responde 200', () => pm.response.to.have.status(200));
      pm.test('é um documento OpenAPI', () => pm.expect(pm.response.json().openapi).to.match(/^3\./));
    },
  }),
  req('Swagger do portaria responde', 'GET', '{{portaria}}/v3/api-docs', {
    testes: () => {
      pm.test('responde 200', () => pm.response.to.have.status(200));
      pm.test('é um documento OpenAPI', () => pm.expect(pm.response.json().openapi).to.match(/^3\./));
    },
  }),
  req('Portaria continua exigindo token nas rotas', 'GET', '{{portaria}}/entregas/pendentes', { testes: espera(401) }),
], 'Registro no Consul e documentação. Não exige login.');

// ── 01 Autenticação ────────────────────────────────────────────────────────

const autenticacao = pasta('01 Autenticação', [
  req('Prepara datas da execução', 'GET', '{{consul}}/v1/status/leader', {
    antes: () => {
      const hoje = new Date().toLocaleDateString('sv-SE', { timeZone: 'America/Sao_Paulo' });
      const fim = new Date(Date.now() + 7 * 86400000).toLocaleDateString('sv-SE', { timeZone: 'America/Sao_Paulo' });
      pm.collectionVariables.set('hoje', hoje);
      pm.collectionVariables.set('fim', fim);
      pm.collectionVariables.set('mesAtual', hoje.slice(0, 7));
      pm.collectionVariables.set('categoria', `[newman] Teste ${Date.now()}`);
    },
    testes: () => pm.test('datas definidas', () => pm.expect(pm.collectionVariables.get('hoje')).to.match(/^\d{4}-\d{2}-\d{2}$/)),
  }),
  login('Login síndico (Parque Verde)', 'emailSindico', 'Sindico'),
  login('Login morador (Parque Verde)', 'emailMorador', 'Morador'),
  login('Login porteiro (Parque Verde)', 'emailPorteiro', 'Porteiro'),
  login('Login síndico de outro condomínio (Vista Mar)', 'emailSindicoOutro', 'SindicoOutro'),
  req('Login com e-mail inexistente é recusado', 'POST', '{{gw}}/api/auth/login', {
    corpo: '{ "email": "ninguem@mora.test", "senha": "qualquer-coisa" }',
    testes: espera(401, "pm.test('sem token', () => pm.expect(pm.response.json().token).to.be.undefined);"),
  }),
  req('Morador tem unidade (pré-condição da multa)', 'GET', '{{consul}}/v1/status/leader', {
    testes: () => pm.test('unidade do morador conhecida', () => pm.expect(pm.collectionVariables.get('unidadeMorador')).to.not.be.empty),
  }),
], 'Contas do seed (`scripts/seed-dev.mjs`). A senha chega pelo `rodar.mjs`, nunca fica gravada aqui.');

// ── 02 auth-api: ocorrências agregadas ─────────────────────────────────────

const authApi = pasta('02 auth-api — estatísticas de ocorrências', [
  req('Morador abre uma ocorrência', 'POST', '{{gw}}/api/reclamacoes', {
    token: 'tokenMorador',
    corpo: '{ "category": "{{categoria}}", "description": "[newman] Ocorrência criada pelo teste de API" }',
    testes: espera(201),
  }),
  req('Síndico consulta estatísticas (6 meses)', 'GET', '{{gw}}/api/reclamacoes/estatisticas?meses=6', {
    token: 'tokenSindico',
    testes: () => {
      pm.test('responde 200', () => pm.response.to.have.status(200));
      const r = pm.response.json();
      pm.test('janela de 6 meses, terminando no mês atual', () => {
        pm.expect(r.meses).to.have.length(6);
        pm.expect(r.meses[5]).to.eql(pm.collectionVariables.get('mesAtual'));
      });
      pm.test('série mensal sem buracos', () => pm.expect(r.porMes).to.have.length(6));
      pm.test('situação atual conta a ocorrência nova', () => {
        pm.expect(r.atual.pendentes).to.be.at.least(1);
        pm.expect(r.atual.abertas).to.eql(r.atual.pendentes + r.atual.emAnalise);
      });
      pm.test('categoria nova aparece com 1', () => {
        const c = r.porCategoria.find((x) => x.categoria === pm.collectionVariables.get('categoria'));
        pm.expect(c && c.total).to.eql(1);
      });
      pm.test('abertas do mês atual ≥ 1', () => pm.expect(r.porMes[5].abertas).to.be.at.least(1));
    },
  }),
  req('Janela absurda é limitada a 24 meses', 'GET', '{{gw}}/api/reclamacoes/estatisticas?meses=999', {
    token: 'tokenSindico',
    testes: espera(200, "pm.test('24 meses', () => pm.expect(pm.response.json().meses).to.have.length(24));"),
  }),
  req('Síndico de outro condomínio não vê a ocorrência', 'GET', '{{gw}}/api/reclamacoes/estatisticas?meses=1', {
    token: 'tokenSindicoOutro',
    testes: () => {
      pm.test('responde 200', () => pm.response.to.have.status(200));
      pm.test('categoria do teste não aparece', () => {
        const cat = pm.collectionVariables.get('categoria');
        pm.expect(pm.response.json().porCategoria.map((c) => c.categoria)).to.not.include(cat);
      });
    },
  }),
  req('Morador não acessa estatísticas', 'GET', '{{gw}}/api/reclamacoes/estatisticas', { token: 'tokenMorador', testes: espera(403) }),
  req('Sem token é recusado', 'GET', '{{gw}}/api/reclamacoes/estatisticas', { testes: espera(401) }),
  req('Gateway leva /api/portaria ao portaria-service', 'GET', '{{gw}}/api/portaria/entregas/pendentes', {
    token: 'tokenSindico',
    testes: espera(200, "pm.test('resposta do portaria (lista)', () => pm.expect(pm.response.json()).to.be.an('array'));"),
  }),
], 'RNF-20: as contagens do painel saem do banco do auth-api, já recortadas pelo condomínio do token.');

// ── 03 comunicacao ─────────────────────────────────────────────────────────

const aviso = (titulo, publico, variavel) => req(`Síndico publica aviso para ${publico}`, 'POST', '{{gw}}/api/comunicacao/avisos', {
  token: 'tokenSindico',
  corpo: `{ "titulo": "[newman] ${titulo}", "mensagem": "Criado pelo teste de API.", "publicoAlvo": "${publico}",
  "dataInicio": "{{hoje}}", "dataFim": "{{fim}}", "publicado": true }`,
  testes: new Function(`
    pm.test('responde 201', () => pm.response.to.have.status(201));
    const a = pm.response.json();
    pm.test('ficou publicado', () => pm.expect(a.publicado).to.eql(true));
    pm.collectionVariables.set('${variavel}', a.id);
  `),
});

const idsAtivos = `const lista = pm.response.json();
    const ids = lista.map((a) => a.id);
    const func = pm.collectionVariables.get('avisoFuncionarios');
    const mor = pm.collectionVariables.get('avisoMoradores');`;

const comunicacao = pasta('03 comunicacao — avisos, leitura, imagem, rota interna', [
  aviso('Manutenção da guarita', 'FUNCIONARIOS', 'avisoFuncionarios'),
  aviso('Assembleia de outubro', 'MORADORES', 'avisoMoradores'),
  req('Morador só recebe o aviso destinado a ele', 'GET', '{{gw}}/api/comunicacao/avisos/ativos', {
    token: 'tokenMorador',
    testes: new Function(`
    pm.test('responde 200', () => pm.response.to.have.status(200));
    ${idsAtivos}
    pm.test('aviso de MORADORES aparece', () => pm.expect(ids).to.include(mor));
    pm.test('aviso de FUNCIONARIOS não aparece', () => pm.expect(ids).to.not.include(func));
    pm.test('marcado como destinatário', () => pm.expect(lista.find((a) => a.id === mor).destinatario).to.eql(true));
    `),
  }),
  req('Porteiro recebe o de funcionários e não o de moradores', 'GET', '{{gw}}/api/comunicacao/avisos/ativos', {
    token: 'tokenPorteiro',
    testes: new Function(`
    ${idsAtivos}
    pm.test('aviso de FUNCIONARIOS aparece', () => pm.expect(ids).to.include(func));
    pm.test('aviso de MORADORES não aparece', () => pm.expect(ids).to.not.include(mor));
    `),
  }),
  req('Síndico vê os dois, sem ser destinatário', 'GET', '{{gw}}/api/comunicacao/avisos/ativos', {
    token: 'tokenSindico',
    testes: new Function(`
    ${idsAtivos}
    pm.test('vê os dois', () => { pm.expect(ids).to.include(func); pm.expect(ids).to.include(mor); });
    pm.test('destinatario: false nos dois', () => {
      lista.filter((a) => a.id === func || a.id === mor).forEach((a) => pm.expect(a.destinatario).to.eql(false));
    });
    `),
  }),
  req('Morador não confirma leitura de aviso que não é para ele', 'POST', '{{gw}}/api/comunicacao/avisos/{{avisoFuncionarios}}/lido', {
    token: 'tokenMorador', testes: espera(404),
  }),
  req('Morador confirma leitura do aviso dele', 'POST', '{{gw}}/api/comunicacao/avisos/{{avisoMoradores}}/lido', {
    token: 'tokenMorador', testes: espera(200),
  }),
  req('Panorama de leitura conta a confirmação', 'GET', '{{gw}}/api/comunicacao/avisos/leituras', {
    token: 'tokenSindico',
    testes: () => {
      pm.test('responde 200', () => pm.response.to.have.status(200));
      const a = pm.response.json().avisos.find((x) => x.id === pm.collectionVariables.get('avisoMoradores'));
      pm.test('aviso está no panorama', () => pm.expect(a).to.be.an('object'));
      pm.test('1 confirmação', () => pm.expect(a.confirmacoes).to.eql(1));
    },
  }),
  req('Relatório "x de y" do aviso', 'GET', '{{gw}}/api/comunicacao/avisos/{{avisoMoradores}}/leituras', {
    token: 'tokenSindico',
    testes: () => {
      pm.test('responde 200', () => pm.response.to.have.status(200));
      const r = pm.response.json();
      pm.test('total de destinatários vem do auth-api', () => pm.expect(r.total).to.be.a('number').and.at.least(1));
      pm.test('1 confirmado', () => pm.expect(r.confirmados).to.eql(1));
      pm.test('o morador consta como lido', () => {
        const d = r.destinatarios.find((x) => String(x.usuarioId) === pm.collectionVariables.get('idMorador'));
        pm.expect(d && d.lido).to.eql(true);
      });
      pm.test('porteiro não entra num aviso para moradores', () => {
        pm.expect(r.destinatarios.map((x) => String(x.usuarioId))).to.not.include(pm.collectionVariables.get('idPorteiro'));
      });
    },
  }),
  req('Morador não vê o panorama', 'GET', '{{gw}}/api/comunicacao/avisos/leituras', { token: 'tokenMorador', testes: espera(403) }),
  req('Síndico de outro condomínio não vê o relatório', 'GET', '{{gw}}/api/comunicacao/avisos/{{avisoMoradores}}/leituras', {
    token: 'tokenSindicoOutro',
    testes: () => pm.test('recusa (403 ou 404)', () => pm.expect(pm.response.code).to.be.oneOf([403, 404])),
  }),
  req('Upload de imagem PNG', 'POST', '{{gw}}/api/comunicacao/avisos/imagem', {
    token: 'tokenSindico', form: { imagem: 'fixtures/aviso.png' },
    testes: () => {
      pm.test('responde 201', () => pm.response.to.have.status(201));
      const url = pm.response.json().url;
      pm.test('nome sorteado no servidor', () => pm.expect(url).to.match(/^\/uploads\/avisos\/[0-9a-f-]{36}\.(png|jpg)$/));
      pm.collectionVariables.set('imagemUrl', url);
    },
  }),
  req('Imagem enviada é servida', 'GET', '{{gw}}/api/comunicacao{{imagemUrl}}', {
    testes: () => {
      pm.test('responde 200', () => pm.response.to.have.status(200));
      pm.test('é imagem', () => pm.expect(pm.response.headers.get('Content-Type')).to.match(/^image\//));
    },
  }),
  req('Texto renomeado para .png é recusado', 'POST', '{{gw}}/api/comunicacao/avisos/imagem', {
    token: 'tokenSindico', form: { imagem: 'fixtures/falso.png' }, testes: espera(400),
  }),
  req('Morador não envia imagem', 'POST', '{{gw}}/api/comunicacao/avisos/imagem', {
    token: 'tokenMorador', form: { imagem: 'fixtures/aviso.png' }, testes: espera(403),
  }),
  req('Rota interna sem token de serviço → 401', 'POST', '{{comunicacao}}/interno/notificacoes', {
    corpo: { usuarioId: '1', tipo: 'SISTEMA', titulo: '[newman] não deve entrar' }, testes: espera(401),
  }),
  req('Rota interna com token errado → 401', 'POST', '{{comunicacao}}/interno/notificacoes', {
    headers: [{ key: 'X-Servico-Token', value: 'token-errado' }],
    corpo: { usuarioId: '1', tipo: 'SISTEMA', titulo: '[newman] não deve entrar' }, testes: espera(401),
  }),
  req('Rota interna pelo gateway também exige o token', 'POST', '{{gw}}/api/comunicacao/interno/notificacoes', {
    corpo: { usuarioId: '1', tipo: 'SISTEMA', titulo: '[newman] não deve entrar' }, testes: espera(401),
  }),
  req('Rota interna recusa tipo desconhecido', 'POST', '{{comunicacao}}/interno/notificacoes', {
    headers: [{ key: 'X-Servico-Token', value: '{{servicoToken}}' }],
    corpo: '{ "condominioId": "{{condominioMorador}}", "usuarioId": "{{idMorador}}", "origem": "financeiro", "tipo": "INVENTADO", "titulo": "[newman] não deve entrar" }',
    testes: espera(400),
  }),
], 'Público-alvo, confirmação de leitura, upload seguro de imagem e a rota que o financeiro usa para notificar.');

// ── 04 financeiro ──────────────────────────────────────────────────────────

const doMorador = `const itens = pm.response.json().content ?? pm.response.json();
    const multaId = pm.collectionVariables.get('multaId');
    const daMulta = itens.filter((n) => n.tipo === 'MULTA_APLICADA'
      && (String(n.dados?.multaId) === multaId || String(n.referenciaId) === multaId));`;

const financeiro = pasta('04 financeiro — multa vira notificação', [
  req('Síndico aplica multa ao morador', 'POST', '{{gw}}/api/financeiro/admin/multas', {
    token: 'tokenSindico',
    corpo: '{ "unidadeId": "{{unidadeMorador}}", "usuarioId": {{idMorador}}, "valor": 150.00, "motivo": "[newman] Barulho após as 22h", "dataInfracao": "{{hoje}}" }',
    testes: () => {
      pm.test('responde 201', () => pm.response.to.have.status(201));
      const m = pm.response.json().multa;
      pm.test('valor em centavos', () => pm.expect(m.valorCentavos).to.eql(15000));
      pm.collectionVariables.set('multaId', String(m.id));
    },
  }),
  req('A notificação chega no sino do morador', 'GET', '{{gw}}/api/comunicacao/notificacoes', {
    token: 'tokenMorador',
    testes: new Function(`
    pm.test('responde 200', () => pm.response.to.have.status(200));
    ${doMorador}
    pm.test('notificação MULTA_APLICADA da multa', () => pm.expect(daMulta).to.have.length(1));
    pm.test('veio do financeiro', () => pm.expect(daMulta[0].origem).to.eql('financeiro'));
    pm.test('ainda não lida', () => pm.expect(daMulta[0].lida).to.eql(false));
    `),
  }),
  req('Reenvio da mesma notificação não duplica', 'POST', '{{comunicacao}}/interno/notificacoes', {
    headers: [{ key: 'X-Servico-Token', value: '{{servicoToken}}' }],
    corpo: `{ "condominioId": "{{condominioMorador}}", "usuarioId": "{{idMorador}}", "origem": "financeiro",
  "tipo": "MULTA_APLICADA", "titulo": "Multa aplicada à sua unidade", "dados": { "multaId": "{{multaId}}" },
  "chaveUnica": "financeiro:MULTA_APLICADA:{{multaId}}" }`,
    testes: () => {
      pm.test('responde 200 (não 201)', () => pm.response.to.have.status(200));
      pm.test('marcada como repetida', () => pm.expect(pm.response.json().repetida).to.eql(true));
    },
  }),
  req('Continua uma só no sino', 'GET', '{{gw}}/api/comunicacao/notificacoes', {
    token: 'tokenMorador',
    testes: new Function(`
    ${doMorador}
    pm.test('exatamente uma', () => pm.expect(daMulta).to.have.length(1));
    `),
  }),
  req('Multas por mês (agregado no banco)', 'GET', '{{gw}}/api/financeiro/admin/multas/por-mes?meses=6', {
    token: 'tokenSindico',
    testes: () => {
      pm.test('responde 200', () => pm.response.to.have.status(200));
      const atual = pm.response.json().meses.find((m) => m.mes === pm.collectionVariables.get('mesAtual'));
      pm.test('mês atual conta a multa', () => pm.expect(atual && atual.aplicadas).to.be.at.least(1));
    },
  }),
  req('Janela inválida é recusada', 'GET', '{{gw}}/api/financeiro/admin/multas/por-mes?meses=5', { token: 'tokenSindico', testes: espera(400) }),
  req('Morador não vê o agregado', 'GET', '{{gw}}/api/financeiro/admin/multas/por-mes', { token: 'tokenMorador', testes: espera(403) }),
], 'RF-13: a multa aplicada no financeiro chega ao morador pelo comunicacao, uma vez só.');

// ── 05 gestao-geral ────────────────────────────────────────────────────────

const fontesOk = `pm.test('nenhuma das fontes agregadas indisponível', () => {
      const fora = pm.response.json().fontesIndisponiveis;
      ['auth-api', 'comunicacao-service', 'financeiro'].forEach((f) => pm.expect(fora).to.not.include(f));
    });`;

const gestao = pasta('05 gestao-geral — painel do síndico', [
  req('Painel operacional', 'GET', '{{gw}}/api/gestao/painel/operacional', {
    token: 'tokenSindico',
    testes: new Function(`
    pm.test('responde 200', () => pm.response.to.have.status(200));
    const r = pm.response.json();
    ${fontesOk}
    pm.test('ocorrências abertas ≥ 1', () => pm.expect(r.ocorrencias.abertas).to.be.at.least(1));
    pm.test('aviso de moradores com 1 leitura', () => {
      const a = r.avisos.leitura.find((x) => x.id === pm.collectionVariables.get('avisoMoradores'));
      pm.expect(a && a.leituras).to.eql(1);
    });
    `),
  }),
  req('Painel estratégico (6 meses)', 'GET', '{{gw}}/api/gestao/painel/estrategico?meses=6', {
    token: 'tokenSindico',
    testes: new Function(`
    pm.test('responde 200', () => pm.response.to.have.status(200));
    const r = pm.response.json();
    ${fontesOk}
    pm.test('6 meses em todas as séries', () => {
      pm.expect(r.meses).to.have.length(6);
      pm.expect(r.ocorrencias.porMes).to.have.length(6);
      pm.expect(r.multas.porMes).to.have.length(6);
    });
    pm.test('multa do mês atual contada', () => pm.expect(r.multas.porMes[5].aplicadas).to.be.at.least(1));
    pm.test('ocorrência do mês atual contada', () => pm.expect(r.ocorrencias.porMes[5].abertas).to.be.at.least(1));
    `),
  }),
  req('Janela inválida é recusada', 'GET', '{{gw}}/api/gestao/painel/estrategico?meses=5', { token: 'tokenSindico', testes: espera(400) }),
  req('Morador não acessa o painel', 'GET', '{{gw}}/api/gestao/painel/operacional', { token: 'tokenMorador', testes: espera(403) }),
], 'O painel junta as fontes já agregadas; nenhuma delas pode aparecer como indisponível.');

// ── 06 Limpeza ─────────────────────────────────────────────────────────────

const limpeza = pasta('06 Limpeza', [
  req('Exclui o aviso de funcionários', 'DELETE', '{{gw}}/api/comunicacao/avisos/{{avisoFuncionarios}}', { token: 'tokenSindico', testes: espera(204) }),
  req('Exclui o aviso de moradores', 'DELETE', '{{gw}}/api/comunicacao/avisos/{{avisoMoradores}}', { token: 'tokenSindico', testes: espera(204) }),
], 'O que não tem rota de exclusão (multa, ocorrência, notificação, imagem) é apagado pelo `rodar.mjs` depois da execução.');

const colecao = {
  info: {
    name: 'Mora — teste de API',
    description: 'Fluxos ponta a ponta dos serviços. Rode com `npm test` dentro de `tests-api/`.',
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
  },
  item: [documentacao, infra, autenticacao, authApi, comunicacao, financeiro, gestao, limpeza],
  variable: [],
};

writeFileSync(new URL('./mora.postman_collection.json', import.meta.url), `${JSON.stringify(colecao, null, 2)}\n`);
const total = colecao.item.reduce((s, p) => s + p.item.length, 0);
console.log(`collection montada: ${colecao.item.length} pastas, ${total} requisições`);
