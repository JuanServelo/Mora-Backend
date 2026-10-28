/**
 * Especificação OpenAPI do auth-api.
 *
 * Escrita à mão. As permissões descritas em cada rota são as do inventário de
 * contrato (`tests/contrato/inventario.js`), que os testes conferem contra o
 * comportamento real; os exemplos partem de respostas da stack de
 * desenvolvimento com as contas do seed (`scripts/seed-dev.mjs`). Nenhum
 * exemplo traz senha ou token real: senha aparece como `********` e o JWT,
 * truncado.
 *
 * `tests/documentacao.test.js` compara esta especificação com as rotas do
 * Express: rota nova sem documentação quebra os testes.
 */

// ── Ajudantes ───────────────────────────────────────────────────────────────

const json = (exemplo) => ({ 'application/json': { example: exemplo } });
const resposta = (description, exemplo) => ({ description, content: json(exemplo) });
const falha = (description, mensagem) => resposta(description, { sucesso: false, mensagem });
const corpo = (description, exemplo, schema = { type: 'object' }) => ({
  required: true, description, content: { 'application/json': { schema, example: exemplo } },
});
const op = (tag, summary, description, extra = {}) => ({ tags: [tag], summary, description, ...extra });
const pId = (nome, description, example, schema = { type: 'string' }) => ({ name: nome, in: 'path', required: true, description, schema, example });
const pQuery = (nome, description, example, schema = { type: 'string' }) => ({ name: nome, in: 'query', required: false, description, schema, example });
const publica = { security: [] };

const E401 = falha('Sem token, token inválido, expirado ou revogado (logout, troca de senha, conta desativada).', 'Token inválido ou expirado');
const E403 = falha('O perfil de quem chama não tem acesso.', 'Acesso negado — apenas administradores');
const E404 = (oque) => falha(`${oque} não encontrado(a), ou de outro condomínio.`, `${oque} não encontrado.`);
const E400 = (mensagem) => falha('Dado inválido.', mensagem);
const E429 = falha('Tentativas demais: limite por IP e por e-mail.', 'Muitas tentativas. Tente novamente em alguns minutos.');

const negaGestao = 'Perfis: ADMIN_GERAL e ADMIN_SINDICO (este, só no próprio condomínio).';
const soAdminGeral = 'Perfil: ADMIN_GERAL.';

// ── Exemplos (seed: Residencial Parque Verde) ───────────────────────────────

const COND = 'cond-parque-verde';
const UNIDADE_101 = 'e064d8a3-2835-4bdf-8e9f-06fa3a108e6d';
const JWT = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6Miwic3ViIjoiMiIsInBlcmZpbCI6IkFETUlOX1NJTkRJQ08i...';

const USUARIO = (extra = {}) => ({
  id: 4, nome: 'Ana Souza', email: 'morador1.verde@mora.test', telefone: '(41) 99876-1001', cpf: '100.000.010-90',
  fotoUrl: '/uploads/avatars/avatar-4-1789421836000.jpg', provider: 'local', perfil: 'MORADOR', status: 'active',
  condominioId: COND, unidadeId: UNIDADE_101, responsavelFinanceiro: true, semAcessoSistema: false,
  entradaPermitida: false, dataNascimento: '1990-05-15', createdAt: '2026-09-09T20:46:25.970Z', activatedAt: '2026-09-09T20:46:25.970Z',
  ...extra,
});
const SINDICO = USUARIO({
  id: 2, nome: 'Síndico Residencial Parque Verde', email: 'sindico.verde@mora.test', telefone: null, cpf: '100.000.001-08',
  fotoUrl: null, perfil: 'ADMIN_SINDICO', unidadeId: null, responsavelFinanceiro: false,
});
const CONDOMINIO = {
  id: COND, nome: 'Residencial Parque Verde', cnpj: '12.345.678/0001-99', endereco: 'Rua das Flores, 100 — Batel, Curitiba/PR',
  telefone: '(41) 3321-5500', email: 'contato.verde@mora.test', status: 'active', criadoPorId: 1,
  createdAt: '2026-09-09T23:46:25.970Z', updatedAt: '2026-09-11T01:42:35.354Z',
};
const CONVITE = {
  id: 58, email: 'joana.ribeiro@mora.test', perfil: 'MORADOR', codigo: 'MORA-7K3P-Q9XD', expiresAt: '2026-10-27T18:00:00.000Z',
};
const LOGIN = { sucesso: true, mensagem: 'Login realizado com sucesso', token: JWT, usuario: SINDICO, redirectPath: '/adm' };
const OCUPANTE = {
  id: 9, nome: 'Rita Convidada', email: 'convidado.verde@mora.test', cpf: '100.000.030-34', perfil: 'CONVIDADO', status: 'active',
  responsavelFinanceiro: false, semAcessoSistema: true, dataNascimento: '1990-05-15', cadastradoPorId: 4, tipo: 'usuario',
};
const REGISTRO = {
  id: 19, usuarioId: 4, entradaEm: '2026-10-07T22:33:26.471Z', nome: 'Ana Souza', perfil: 'Morador', unidadeId: UNIDADE_101,
  turnoPrevisto: null, liberacaoExcepcional: false, registradoPorNome: 'Porteiro Residencial Parque Verde', saidaEm: null,
};

// ── Especificação ───────────────────────────────────────────────────────────

export const openapi = {
  openapi: '3.0.3',
  info: {
    title: 'auth-api',
    version: '1.0',
    description: [
      'Autenticação e cadastro: login por e-mail e senha ou Google, convites, usuários, ocupantes das unidades,',
      'condomínios, perfis e registro de entrada e saída na portaria (RF-01 a RF-03, RF-07). As ocorrências são do ocorrencias-service.',
      '',
      '- **Responsável:** João Victor',
      '- **Banco:** `auth_db`',
      '- **Pelo gateway:** `/api/auth`, `/api/users`, `/api/user-management`, `/api/invites`, `/api/condominios`,',
      '  `/api/perfis`, `/api/estatisticas`. As rotas `/api/portaria` deste serviço respondem',
      '  só na porta dele (3001): no gateway, esse prefixo é do portaria-service.',
      '- **Autenticação:** é este serviço que emite o JWT. Faça `POST /api/auth/login` com uma conta do seed e',
      '  cole o `token` no botão **Authorize**. O token vale para todos os serviços.',
      '- **Cadastro:** só por convite. Não existe auto-cadastro.',
      '',
      'Perfis: ADMIN_GERAL, ADMIN_SINDICO, PORTEIRO, MORADOR, DONO_ALUGUEL, CONVIDADO e TERCEIRO.',
    ].join('\n'),
  },
  servers: [{ url: '/', description: 'Gateway (Traefik)' }],
  tags: [
    { name: 'Autenticação', description: 'Login, sessão, perfil próprio, senha e login com Google.' },
    { name: 'Convites', description: 'Validação e ativação do convite pelo convidado. Públicas.' },
    { name: 'Usuários', description: 'Cadastro e manutenção de usuários pela gestão.' },
    { name: 'Gestão de usuários', description: 'Convites, desativação e vínculo com unidade, no escopo de quem chama.' },
    { name: 'Ocupantes da unidade', description: 'Inquilino, moradores e convidados de uma unidade (RF-07), e a transferência da responsabilidade financeira.' },
    { name: 'Condomínios', description: 'Cadastro dos condomínios clientes da plataforma.' },
    { name: 'Perfis', description: 'O que cada perfil pode fazer e quem ele pode cadastrar.' },
    { name: 'Portaria', description: 'Entrada e saída de pessoas cadastradas. Fora do gateway (o prefixo /api/portaria lá é do portaria-service).' },
    { name: 'Estatísticas', description: 'Números da plataforma e de um condomínio, agregados em SQL.' },
    { name: 'Documentação', description: 'Esta especificação.' },
    { name: 'Saúde', description: 'Verificação usada pelo Consul.' },
  ],
  components: {
    securitySchemes: {
      bearerAuth: { type: 'http', scheme: 'bearer', bearerFormat: 'JWT', description: 'Token devolvido por POST /api/auth/login.' },
    },
  },
  security: [{ bearerAuth: [] }],
  paths: {
    // ── Autenticação ────────────────────────────────────────────────────────
    '/api/auth/login': {
      post: op('Autenticação', 'Entrar', 'Troca e-mail e senha por um JWT, que vale para todos os serviços. Limite de tentativas por IP e por e-mail. Pública.', {
        ...publica,
        requestBody: corpo('Credenciais.', { email: 'sindico.verde@mora.test', senha: '********' }, {
          type: 'object', required: ['email', 'senha'], properties: { email: { type: 'string', format: 'email' }, senha: { type: 'string', format: 'password' } },
        }),
        responses: {
          200: resposta('Login aceito.', LOGIN),
          400: E400('Email e senha são obrigatórios'),
          401: falha('E-mail ou senha errados, conta desativada, cadastro não concluído ou perfil sem acesso ao sistema (convidado).', 'E-mail ou senha inválidos.'),
          429: E429,
        },
      }),
    },
    '/api/auth/logout': {
      post: op('Autenticação', 'Sair', 'Revoga todos os tokens do usuário (incrementa o `tokenVersion`): o token atual e os de outros aparelhos param de valer. Qualquer perfil.', {
        'x-sem-corpo': true,
        responses: { 200: resposta('Sessão encerrada.', { sucesso: true, mensagem: 'Sessão encerrada' }), 401: E401 },
      }),
    },
    '/api/auth/me': {
      get: op('Autenticação', 'Meus dados', 'O usuário do token. Qualquer perfil.', {
        responses: { 200: resposta('Usuário.', { sucesso: true, usuario: SINDICO }), 401: E401 },
      }),
      put: op('Autenticação', 'Atualizar meus dados', 'Nome, e-mail, telefone e senha. Trocar a senha exige `senhaAtual` e encerra as outras sessões; a resposta traz um token novo. Qualquer perfil.', {
        requestBody: corpo('Campos a alterar.', { nome: 'Ana Souza', telefone: '(41) 99876-1001' }, {
          type: 'object',
          properties: { nome: { type: 'string' }, email: { type: 'string', format: 'email' }, telefone: { type: 'string' }, senha: { type: 'string', format: 'password' }, senhaAtual: { type: 'string', format: 'password' } },
        }),
        responses: {
          200: resposta('Dados atualizados.', { sucesso: true, mensagem: 'Dados atualizados com sucesso.', usuario: USUARIO() }),
          400: E400('Informe a senha atual para trocar a senha.'), 401: E401,
        },
      }),
    },
    '/api/auth/me/foto': {
      post: op('Autenticação', 'Enviar foto de perfil', 'Imagem JPG ou PNG de até 5 MB, no campo `foto`. É redimensionada e gravada no volume do serviço. Qualquer perfil.', {
        requestBody: {
          required: true,
          description: 'Arquivo de imagem.',
          content: { 'multipart/form-data': { schema: { type: 'object', required: ['foto'], properties: { foto: { type: 'string', format: 'binary' } } }, example: { foto: '(arquivo avatar.jpg)' } } },
        },
        responses: {
          200: resposta('Foto atualizada.', { sucesso: true, mensagem: 'Foto atualizada com sucesso.', usuario: USUARIO() }),
          400: E400('Formato não permitido. Envie uma imagem JPG ou PNG.'), 401: E401,
        },
      }),
    },
    '/api/auth/register': {
      post: op('Autenticação', 'Auto-cadastro (desativado)', 'Desativado: o cadastro é só por convite (`/api/invites/activate`). Sempre responde 403. Mantido para quem ainda chama o caminho antigo receber uma mensagem clara.', {
        ...publica,
        deprecated: true,
        'x-sem-corpo': true,
        responses: { 403: falha('Sempre.', 'Cadastro disponível apenas via código de convite. Acesse a página de ativação.') },
      }),
    },
    '/api/auth/forgot-password': {
      post: op('Autenticação', 'Esqueci a senha', 'Envia por e-mail um link de redefinição, se o e-mail existir. A resposta é a mesma nos dois casos, para não revelar quem tem conta. Pública.', {
        ...publica,
        requestBody: corpo('E-mail da conta.', { email: 'morador1.verde@mora.test' }, { type: 'object', required: ['email'], properties: { email: { type: 'string', format: 'email' } } }),
        responses: { 200: resposta('Pedido recebido.', { sucesso: true, mensagem: 'Se esse email estiver cadastrado, você receberá um link em breve.' }), 429: E429 },
      }),
    },
    '/api/auth/reset-password/{token}': {
      post: op('Autenticação', 'Redefinir senha', 'Grava a senha nova a partir do link do e-mail. O token do link vale uma vez e expira. Encerra todas as sessões. Pública.', {
        ...publica,
        parameters: [pId('token', 'Token do link enviado por e-mail.', '3f9a1c7e2b8d4f6a0c5e9b1d7a3f8c2e')],
        requestBody: corpo('Senha nova.', { senha: '********' }, { type: 'object', required: ['senha'], properties: { senha: { type: 'string', format: 'password', minLength: 8 } } }),
        responses: {
          200: resposta('Senha redefinida.', { sucesso: true, mensagem: 'Senha redefinida com sucesso. Faça login novamente.' }),
          400: E400('Link inválido ou expirado.'), 429: E429,
        },
      }),
    },
    '/api/auth/google/disponivel': {
      get: op('Autenticação', 'Login com Google disponível?', 'Diz se o login com Google está configurado, para a tela mostrar ou esconder o botão. Pública.', {
        ...publica,
        responses: { 200: resposta('Disponibilidade.', { disponivel: true }) },
      }),
    },
    '/api/auth/google': {
      get: op('Autenticação', 'Iniciar login com Google', 'Redireciona para a tela de consentimento do Google. Abra no navegador, não pelo Try it out. Pública.', {
        ...publica,
        responses: { 302: { description: 'Redireciona para accounts.google.com (ou para /login?erro=oauth_nao_configurado).' } },
      }),
    },
    '/api/auth/google/callback': {
      get: op('Autenticação', 'Retorno do Google', 'Para onde o Google devolve o usuário. Redireciona ao front com um código de uso único (`/auth/callback?code=...`), trocado por token em `/api/auth/oauth/exchange`. Pública.', {
        ...publica,
        responses: { 302: { description: 'Redireciona ao front: /auth/callback?code=... ou /login?erro=....' } },
      }),
    },
    '/api/auth/oauth/exchange': {
      post: op('Autenticação', 'Trocar código do Google por token', 'O código do callback vale uma vez e por pouco tempo. Assim o JWT nunca passa pela URL. Pública.', {
        ...publica,
        requestBody: corpo('Código do callback.', { code: '8c1e5f9a2d7b4e0c6a3f1b8d5e2c9a7f' }, { type: 'object', required: ['code'], properties: { code: { type: 'string' } } }),
        responses: { 200: resposta('Login aceito.', LOGIN), 400: E400('Código inválido ou expirado.'), 429: E429 },
      }),
    },
    // ── Convites ────────────────────────────────────────────────────────────
    '/api/invites/validate': {
      post: op('Convites', 'Validar convite', 'Confere o código e devolve os dados pré-cadastrados, para a tela de ativação já vir preenchida. Pública.', {
        ...publica,
        requestBody: corpo('Código do convite.', { codigo: 'MORA-7K3P-Q9XD' }, { type: 'object', required: ['codigo'], properties: { codigo: { type: 'string' } } }),
        responses: {
          200: resposta('Convite válido.', { sucesso: true, convite: { email: 'joana.ribeiro@mora.test', perfil: 'MORADOR', nomePrecadastro: 'Joana Ribeiro', cpfPrecadastro: '100.000.040-06', unidadeId: UNIDADE_101 } }),
          400: E400('Código de convite inválido ou expirado.'), 429: E429,
        },
      }),
    },
    '/api/invites/activate': {
      post: op('Convites', 'Ativar conta pelo convite', 'Cria a conta a partir do convite e já devolve o token. O e-mail precisa ser o do convite; CPF válido; maior de idade. Pública.', {
        ...publica,
        requestBody: corpo('Dados da conta.', {
          codigo: 'MORA-7K3P-Q9XD', nome: 'Joana Ribeiro', email: 'joana.ribeiro@mora.test', telefone: '(41) 99812-3344',
          cpf: '100.000.040-06', dataNascimento: '1994-03-22', senha: '********', confirmacaoSenha: '********',
        }, {
          type: 'object', required: ['codigo', 'nome', 'email', 'cpf', 'dataNascimento', 'senha', 'confirmacaoSenha'],
          properties: {
            codigo: { type: 'string' }, nome: { type: 'string' }, email: { type: 'string', format: 'email' }, telefone: { type: 'string' },
            cpf: { type: 'string' }, dataNascimento: { type: 'string', format: 'date' }, senha: { type: 'string', format: 'password' }, confirmacaoSenha: { type: 'string', format: 'password' },
          },
        }),
        responses: {
          201: resposta('Conta ativada.', { ...LOGIN, mensagem: 'Conta ativada com sucesso.', usuario: USUARIO({ id: 41, nome: 'Joana Ribeiro', email: 'joana.ribeiro@mora.test', cpf: '100.000.040-06' }), redirectPath: '/inicio' }),
          400: E400('As senhas não conferem.'), 429: E429,
        },
      }),
    },
    // ── Usuários ────────────────────────────────────────────────────────────
    '/api/users': {
      get: op('Usuários', 'Listar usuários', 'Usuários do condomínio de quem chama; o Admin Geral pode filtrar por `condominioId`. Perfis: ADMIN_GERAL, ADMIN_SINDICO, MORADOR e DONO_ALUGUEL (estes dois, só os da própria unidade).', {
        parameters: [pQuery('condominioId', 'Só para o Admin Geral.', COND)],
        responses: { 200: resposta('Usuários.', { sucesso: true, usuarios: [USUARIO()] }), 401: E401, 403: E403 },
      }),
      post: op('Usuários', 'Convidar usuário', 'Emite um convite por e-mail para o perfil escolhido. Cada perfil só convida os perfis que pode cadastrar (ver `/api/perfis/info`). Morador e inquilino exigem unidade, nome e CPF pré-cadastrados.', {
        requestBody: corpo('Convite.', { email: 'joana.ribeiro@mora.test', perfil: 'MORADOR', unidadeId: UNIDADE_101, nomePrecadastro: 'Joana Ribeiro', cpfPrecadastro: '100.000.040-06' }, {
          type: 'object', required: ['email', 'perfil'],
          properties: { email: { type: 'string', format: 'email' }, perfil: { type: 'string' }, unidadeId: { type: 'string' }, nomePrecadastro: { type: 'string' }, cpfPrecadastro: { type: 'string' } },
        }),
        responses: {
          201: resposta('Convite emitido.', { sucesso: true, mensagem: 'Convite enviado com sucesso.', emailEnviado: true, convite: CONVITE }),
          400: E400('Já existe um usuário cadastrado com este e-mail.'), 401: E401, 403: E403,
        },
      }),
    },
    '/api/users/{id}': {
      put: op('Usuários', 'Atualizar usuário', `Nome, e-mail e senha de outro usuário. ${negaGestao}`, {
        parameters: [pId('id', 'Id do usuário.', 4, { type: 'integer' })],
        requestBody: corpo('Campos a alterar.', { nome: 'Ana Souza Martins', email: 'morador1.verde@mora.test' }, {
          type: 'object', properties: { nome: { type: 'string' }, email: { type: 'string', format: 'email' }, senha: { type: 'string', format: 'password' } },
        }),
        responses: { 200: resposta('Usuário atualizado.', { sucesso: true, usuario: USUARIO({ nome: 'Ana Souza Martins' }) }), 401: E401, 403: E403, 404: E404('Usuário') },
      }),
      delete: op('Usuários', 'Desativar usuário', 'Desativa (não apaga). Quem foi cadastrado por ele (convidados, ocupantes) é desativado junto. Perfis: ADMIN_GERAL, ADMIN_SINDICO, MORADOR e DONO_ALUGUEL, cada um no próprio escopo.', {
        parameters: [pId('id', 'Id do usuário.', 9, { type: 'integer' })],
        responses: { 200: resposta('Usuário desativado.', { sucesso: true, mensagem: 'Usuário desativado com sucesso.' }), 401: E401, 403: E403, 404: E404('Usuário') },
      }),
    },
    // ── Gestão de usuários ──────────────────────────────────────────────────
    '/api/user-management/users': {
      get: op('Gestão de usuários', 'Usuários e convites do escopo', 'Usuários que quem chama pode gerir e os convites pendentes. Perfis: ADMIN_GERAL, ADMIN_SINDICO, MORADOR e DONO_ALUGUEL, cada um no próprio escopo.', {
        parameters: [pQuery('condominioId', 'Só para o Admin Geral.', COND)],
        responses: {
          200: resposta('Usuários e convites.', {
            sucesso: true, usuarios: [USUARIO()],
            convitesPendentes: [{ id: 58, email: 'joana.ribeiro@mora.test', perfil: 'MORADOR', unidadeId: UNIDADE_101, status: 'pending', expiresAt: '2026-10-27T18:00:00.000Z', createdAt: '2026-10-20T18:00:00.000Z', expirado: false }],
          }),
          401: E401, 403: E403,
        },
      }),
    },
    '/api/user-management/invites': {
      post: op('Gestão de usuários', 'Emitir convite', 'Mesmo convite de `POST /api/users`, aceitando `condominioId` para o Admin Geral convidar o síndico de um condomínio novo. Perfis: ADMIN_GERAL, ADMIN_SINDICO, MORADOR e DONO_ALUGUEL.', {
        requestBody: corpo('Convite.', { email: 'joana.ribeiro@mora.test', perfil: 'MORADOR', unidadeId: UNIDADE_101, nomePrecadastro: 'Joana Ribeiro', cpfPrecadastro: '100.000.040-06' }, {
          type: 'object', required: ['email', 'perfil'],
          properties: { email: { type: 'string', format: 'email' }, perfil: { type: 'string' }, unidadeId: { type: 'string' }, nomePrecadastro: { type: 'string' }, cpfPrecadastro: { type: 'string' }, condominioId: { type: 'string', description: 'Só para o Admin Geral.' } },
        }),
        responses: {
          201: resposta('Convite emitido.', { sucesso: true, mensagem: 'Convite enviado com sucesso.', emailEnviado: true, convite: CONVITE }),
          400: E400('Unidade é obrigatória para este perfil.'), 401: E401, 403: E403,
        },
      }),
    },
    '/api/user-management/invites/{id}/resend': {
      post: op('Gestão de usuários', 'Reenviar convite', 'Gera um código novo, com prazo renovado, e reenvia o e-mail. Convite já usado não pode ser reenviado. Perfis: ADMIN_GERAL, ADMIN_SINDICO, MORADOR e DONO_ALUGUEL.', {
        parameters: [pId('id', 'Id do convite.', 58, { type: 'integer' })],
        'x-sem-corpo': true,
        responses: {
          200: resposta('Convite reenviado.', { sucesso: true, mensagem: 'Convite reenviado com sucesso.', emailEnviado: true, avisoEmail: null, convite: { ...CONVITE, codigo: 'MORA-2V8N-L4TR' } }),
          400: E400('Este código de convite já foi utilizado.'), 401: E401, 403: E403, 404: E404('Convite'),
        },
      }),
    },
    '/api/user-management/users/{id}/deactivate': {
      patch: op('Gestão de usuários', 'Desativar usuário', 'Desativa o usuário e, em cascata, quem ele cadastrou. Responsável financeiro não pode ser removido sem transferir antes. Perfis: ADMIN_GERAL, ADMIN_SINDICO, MORADOR e DONO_ALUGUEL.', {
        parameters: [pId('id', 'Id do usuário.', 9, { type: 'integer' })],
        'x-sem-corpo': true,
        responses: {
          200: resposta('Usuário desativado.', { sucesso: true, mensagem: 'Usuário desativado. 1 usuários vinculados também foram desativados.' }),
          400: E400('Transfira a responsabilidade financeira antes de remover este morador.'), 401: E401, 403: E403, 404: E404('Usuário'),
        },
      }),
    },
    '/api/user-management/users/{id}/unit': {
      patch: op('Gestão de usuários', 'Vincular usuário a uma unidade', `${negaGestao}`, {
        parameters: [pId('id', 'Id do usuário.', 5, { type: 'integer' })],
        requestBody: corpo('Unidade.', { unidadeId: '35d0a934-bd1f-4ccb-ab21-4bca248f8f12' }, { type: 'object', required: ['unidadeId'], properties: { unidadeId: { type: 'string' } } }),
        responses: { 200: resposta('Vinculado.', { sucesso: true, mensagem: 'Usuário vinculado à unidade.', usuario: USUARIO({ id: 5, nome: 'Bruno Lima', unidadeId: '35d0a934-bd1f-4ccb-ab21-4bca248f8f12' }) }), 401: E401, 403: E403, 404: E404('Usuário') },
      }),
      delete: op('Gestão de usuários', 'Remover vínculo com a unidade', `${negaGestao}`, {
        parameters: [pId('id', 'Id do usuário.', 5, { type: 'integer' })],
        responses: { 200: resposta('Vínculo removido.', { sucesso: true, mensagem: 'Vínculo com unidade removido.', usuario: USUARIO({ id: 5, nome: 'Bruno Lima', unidadeId: null }) }), 401: E401, 403: E403, 404: E404('Usuário') },
      }),
    },
    // ── Ocupantes da unidade ────────────────────────────────────────────────
    '/api/user-management/units/{unidadeId}/occupants': {
      get: op('Ocupantes da unidade', 'Ocupantes da unidade', 'Moradores, inquilino e convidados da unidade, quem é o responsável financeiro e os convites pendentes. Perfis: ADMIN_GERAL, ADMIN_SINDICO, DONO_ALUGUEL e o responsável da unidade.', {
        parameters: [pId('unidadeId', 'Id da unidade (portaria).', UNIDADE_101)],
        responses: {
          200: resposta('Ocupantes.', { sucesso: true, responsavelFinanceiroId: 4, ocupantes: [OCUPANTE], convitesPendentes: [] }),
          401: E401, 403: E403,
        },
      }),
    },
    '/api/user-management/units/{unidadeId}/occupants/lessee': {
      post: op('Ocupantes da unidade', 'Cadastrar inquilino responsável', 'Convida o inquilino, que passa a ser o responsável financeiro da unidade. Perfis: proprietário (DONO_ALUGUEL) e gestão.', {
        parameters: [pId('unidadeId', 'Id da unidade (portaria).', UNIDADE_101)],
        requestBody: corpo('Inquilino.', { nome: 'Joana Ribeiro', email: 'joana.ribeiro@mora.test', cpf: '100.000.040-06', dataNascimento: '1994-03-22' }, {
          type: 'object', required: ['nome', 'email', 'cpf'],
          properties: { nome: { type: 'string' }, email: { type: 'string', format: 'email' }, cpf: { type: 'string' }, dataNascimento: { type: 'string', format: 'date' } },
        }),
        responses: {
          201: resposta('Convite emitido.', { sucesso: true, mensagem: 'Morador responsável cadastrado com sucesso.', emailEnviado: true, convite: { ...CONVITE, perfil: 'MORADOR' } }),
          400: E400('CPF inválido. Verifique o número informado.'), 401: E401, 403: E403,
        },
      }),
    },
    '/api/user-management/units/{unidadeId}/occupants/occupant': {
      post: op('Ocupantes da unidade', 'Cadastrar morador', 'Convida um morador da unidade (16 anos ou mais). Perfis: responsável da unidade e gestão.', {
        parameters: [pId('unidadeId', 'Id da unidade (portaria).', UNIDADE_101)],
        requestBody: corpo('Morador.', { nome: 'Lucas Souza', email: 'lucas.souza@mora.test', cpf: '100.000.041-89', dataNascimento: '2006-08-02' }, {
          type: 'object', required: ['nome', 'email', 'cpf', 'dataNascimento'],
          properties: { nome: { type: 'string' }, email: { type: 'string', format: 'email' }, cpf: { type: 'string' }, dataNascimento: { type: 'string', format: 'date' } },
        }),
        responses: {
          201: resposta('Convite emitido.', { sucesso: true, mensagem: 'Morador cadastrado com sucesso.', emailEnviado: true, convite: { ...CONVITE, email: 'lucas.souza@mora.test' } }),
          400: E400('O morador precisa ter pelo menos 16 anos.'), 401: E401, 403: falha('Só o responsável cadastra moradores.', 'Somente Resident Owner ou Lessee podem cadastrar Occupants.'),
        },
      }),
    },
    '/api/user-management/units/{unidadeId}/occupants/guest': {
      post: op('Ocupantes da unidade', 'Cadastrar convidado', 'Cadastra um convidado da unidade, sem acesso ao sistema: ele existe para a portaria liberar a entrada. Perfis: responsável da unidade e gestão.', {
        parameters: [pId('unidadeId', 'Id da unidade (portaria).', UNIDADE_101)],
        requestBody: corpo('Convidado.', { nome: 'Rita Convidada', cpf: '100.000.030-34', dataNascimento: '1990-05-15' }, {
          type: 'object', required: ['nome', 'cpf'], properties: { nome: { type: 'string' }, cpf: { type: 'string' }, dataNascimento: { type: 'string', format: 'date' } },
        }),
        responses: {
          201: resposta('Convidado cadastrado.', { sucesso: true, mensagem: 'Convidado cadastrado com sucesso.', usuario: OCUPANTE }),
          400: E400('CPF inválido. Verifique o número informado.'), 401: E401, 403: E403,
        },
      }),
    },
    '/api/user-management/units/{unidadeId}/occupants/{userId}': {
      delete: op('Ocupantes da unidade', 'Remover ocupante', 'Remove o vínculo do ocupante com a unidade e o desativa. O responsável financeiro não sai sem transferir antes. Perfis: responsável da unidade e gestão.', {
        parameters: [pId('unidadeId', 'Id da unidade (portaria).', UNIDADE_101), pId('userId', 'Id do ocupante.', 9, { type: 'integer' })],
        responses: { 200: resposta('Removido.', { sucesso: true, mensagem: 'Vínculo removido. O usuário foi desativado.' }), 401: E401, 403: E403, 404: E404('Ocupante') },
      }),
    },
    '/api/user-management/units/{unidadeId}/residents': {
      get: op('Ocupantes da unidade', 'Moradores da unidade', 'Todos os moradores ativos da unidade. Perfis: ADMIN_GERAL, ADMIN_SINDICO e DONO_ALUGUEL.', {
        parameters: [pId('unidadeId', 'Id da unidade (portaria).', UNIDADE_101)],
        responses: { 200: resposta('Moradores.', { sucesso: true, moradores: [USUARIO()] }), 401: E401, 403: E403 },
      }),
    },
    '/api/user-management/units/{unidadeId}/transfer-eligibility': {
      get: op('Ocupantes da unidade', 'Pode transferir a responsabilidade financeira?', 'Só há transferência para um inquilino já cadastrado na unidade. Perfis: proprietário e gestão.', {
        parameters: [pId('unidadeId', 'Id da unidade (portaria).', UNIDADE_101)],
        responses: { 200: resposta('Elegibilidade.', { sucesso: true, elegivel: true, podeTransferir: true, lesseeId: 41 }), 401: E401, 403: E403 },
      }),
    },
    '/api/user-management/units/{unidadeId}/transfer-financial-responsibility': {
      post: op('Ocupantes da unidade', 'Transferir a responsabilidade financeira', 'Passa a responsabilidade para o inquilino: as próximas faturas vão para ele, e o proprietário vira DONO_ALUGUEL. Os convidados do proprietário são desativados. Perfis: proprietário e gestão.', {
        parameters: [pId('unidadeId', 'Id da unidade (portaria).', UNIDADE_101)],
        'x-sem-corpo': true,
        responses: {
          200: resposta('Transferida.', { sucesso: true, mensagem: 'Responsabilidade financeira transferida. Seu perfil foi atualizado para Dono Aluguel.', desativados: 1 }),
          400: E400('Cadastre um inquilino antes de transferir.'), 401: E401, 403: E403,
        },
      }),
    },
    // ── Condomínios ─────────────────────────────────────────────────────────
    '/api/condominios': {
      get: op('Condomínios', 'Listar condomínios', 'O Admin Geral vê todos (filtrável por `status`); os demais, só o próprio. Qualquer perfil.', {
        parameters: [pQuery('status', 'Status do condomínio.', 'active', { type: 'string', enum: ['active', 'inactive'] })],
        responses: { 200: resposta('Condomínios.', { sucesso: true, condominios: [CONDOMINIO] }), 401: E401 },
      }),
      post: op('Condomínios', 'Cadastrar condomínio', `O \`id\` é o identificador legível usado em toda a plataforma. ${soAdminGeral}`, {
        requestBody: corpo('Condomínio.', { id: 'cond-jardim-sol', nome: 'Condomínio Jardim do Sol', cnpj: '23.456.789/0001-10', endereco: 'Av. Sete de Setembro, 2400 — Centro, Curitiba/PR', telefone: '(41) 3322-7700', email: 'contato.jardimsol@mora.test' }, {
          type: 'object', required: ['id', 'nome'],
          properties: { id: { type: 'string' }, nome: { type: 'string' }, cnpj: { type: 'string' }, endereco: { type: 'string' }, telefone: { type: 'string' }, email: { type: 'string', format: 'email' } },
        }),
        responses: {
          201: resposta('Cadastrado.', { sucesso: true, condominio: { ...CONDOMINIO, id: 'cond-jardim-sol', nome: 'Condomínio Jardim do Sol', cnpj: '23.456.789/0001-10' } }),
          400: E400('Já existe um condomínio com este ID.'), 401: E401, 403: E403,
        },
      }),
    },
    '/api/condominios/{id}': {
      get: op('Condomínios', 'Detalhe do condomínio', 'Qualquer perfil, no próprio condomínio; o Admin Geral, em qualquer um.', {
        parameters: [pId('id', 'Id do condomínio.', COND)],
        responses: { 200: resposta('Condomínio.', { sucesso: true, condominio: CONDOMINIO }), 401: E401, 404: E404('Condomínio') },
      }),
      put: op('Condomínios', 'Atualizar condomínio', `Dados cadastrais. ${negaGestao}`, {
        parameters: [pId('id', 'Id do condomínio.', COND)],
        requestBody: corpo('Campos a alterar.', { telefone: '(41) 3321-5501', email: 'administracao.verde@mora.test' }, {
          type: 'object', properties: { nome: { type: 'string' }, cnpj: { type: 'string' }, endereco: { type: 'string' }, telefone: { type: 'string' }, email: { type: 'string', format: 'email' } },
        }),
        responses: { 200: resposta('Atualizado.', { sucesso: true, condominio: { ...CONDOMINIO, telefone: '(41) 3321-5501' } }), 401: E401, 403: E403, 404: E404('Condomínio') },
      }),
    },
    '/api/condominios/{id}/users': {
      get: op('Condomínios', 'Usuários do condomínio', `${negaGestao}`, {
        parameters: [pId('id', 'Id do condomínio.', COND)],
        responses: { 200: resposta('Condomínio e usuários.', { sucesso: true, condominio: CONDOMINIO, usuarios: [SINDICO, USUARIO()] }), 401: E401, 403: E403, 404: E404('Condomínio') },
      }),
    },
    '/api/condominios/{id}/activate': {
      patch: op('Condomínios', 'Reativar condomínio', soAdminGeral, {
        parameters: [pId('id', 'Id do condomínio.', COND)],
        'x-sem-corpo': true,
        responses: { 200: resposta('Reativado.', { sucesso: true, mensagem: 'Condomínio reativado.' }), 401: E401, 403: E403, 404: E404('Condomínio') },
      }),
    },
    '/api/condominios/{id}/deactivate': {
      patch: op('Condomínios', 'Desativar condomínio', `Os usuários dele deixam de conseguir entrar. ${soAdminGeral}`, {
        parameters: [pId('id', 'Id do condomínio.', COND)],
        'x-sem-corpo': true,
        responses: { 200: resposta('Desativado.', { sucesso: true, mensagem: 'Condomínio desativado.' }), 401: E401, 403: E403, 404: E404('Condomínio') },
      }),
    },
    '/api/condominios/{id}/assign-user': {
      patch: op('Condomínios', 'Vincular usuário ao condomínio', `${negaGestao}`, {
        parameters: [pId('id', 'Id do condomínio.', COND)],
        requestBody: corpo('Usuário.', { userId: 2 }, { type: 'object', required: ['userId'], properties: { userId: { type: 'integer' } } }),
        responses: { 200: resposta('Vinculado.', { sucesso: true, mensagem: 'Usuário vinculado ao condomínio.', usuario: SINDICO }), 401: E401, 403: E403, 404: E404('Usuário') },
      }),
    },
    // ── Perfis ──────────────────────────────────────────────────────────────
    '/api/perfis/info': {
      get: op('Perfis', 'Perfis e permissões', 'Cada perfil com descrição, permissões e quem ele pode cadastrar, agrupados por nível. Perfis: ADMIN_GERAL, ADMIN_SINDICO e DONO_ALUGUEL.', {
        responses: {
          200: resposta('Perfis.', {
            sucesso: true, total: 7,
            categorias: [{
              categoria: 'plataforma', label: 'Nível Plataforma', descricao: 'Gerenciam múltiplos condomínios',
              perfis: [{ chave: 'ADMIN_GERAL', label: 'Admin Geral', categoria: 'plataforma', descricao: 'Responsável pela plataforma. Cadastra e configura condomínios e empresas.', permissoes: ['Cadastrar e configurar condomínios e empresas', 'Cadastrar síndicos em qualquer condomínio'], podeCadastrar: ['ADMIN_GERAL', 'ADMIN_SINDICO'], podeCadastrarLabels: ['Admin Geral', 'Admin Síndico'] }],
            }],
          }),
          401: E401, 403: E403,
        },
      }),
    },
    '/api/perfis/info/{perfil}': {
      get: op('Perfis', 'Um perfil', 'Descrição, permissões e quem o perfil pode cadastrar. Perfis: ADMIN_GERAL, ADMIN_SINDICO e DONO_ALUGUEL.', {
        parameters: [pId('perfil', 'Chave do perfil.', 'ADMIN_SINDICO', { type: 'string', enum: ['ADMIN_GERAL', 'ADMIN_SINDICO', 'PORTEIRO', 'MORADOR', 'DONO_ALUGUEL', 'CONVIDADO', 'TERCEIRO'] })],
        responses: {
          200: resposta('Perfil.', { sucesso: true, perfil: { chave: 'ADMIN_SINDICO', label: 'Admin Síndico', categoria: 'condominio', descricao: 'Administra um condomínio.', permissoes: ['Cadastrar porteiros e moradores', 'Publicar comunicados'], podeCadastrar: ['PORTEIRO', 'MORADOR', 'DONO_ALUGUEL'], podeCadastrarLabels: ['Porteiro', 'Morador', 'Dono Aluguel'] } }),
          401: E401, 403: E403, 404: falha('Perfil inexistente.', 'Perfil não encontrado.'),
        },
      }),
    },
    // ── Portaria ────────────────────────────────────────────────────────────
    ...Object.fromEntries([
      ['/api/portaria/guests', 'Convidados do condomínio', 'Convidados cadastrados, com a situação de acesso (DENTRO/FORA) e o último registro.', { sucesso: true, guests: [{ ...OCUPANTE, statusAcesso: 'FORA', ultimoRegistroEm: null }] }],
      ['/api/portaria/residentes', 'Moradores do condomínio', 'Moradores com a situação de acesso (DENTRO/FORA) e o último registro.', { sucesso: true, residentes: [{ ...USUARIO(), statusAcesso: 'DENTRO', ultimoRegistroEm: '2026-10-07T22:33:26.471Z' }] }],
      ['/api/portaria/dentro', 'Quem está dentro', 'Entradas sem saída registrada.', { sucesso: true, dentro: [{ entradaId: 19, usuarioId: 4, entradaEm: '2026-10-07T22:33:26.471Z', nome: 'Ana Souza', perfil: 'Morador', unidadeId: UNIDADE_101, turnoPrevisto: null, liberacaoExcepcional: false, registradoPorNome: 'Porteiro Residencial Parque Verde' }] }],
      ['/api/portaria/funcionarios', 'Funcionários do condomínio', 'Porteiros e síndicos ativos, com id em texto (o portaria-service compara com o `sub` do JWT).', { sucesso: true, funcionarios: [{ id: '3', nome: 'Porteiro Residencial Parque Verde', email: 'porteiro.verde@mora.test', perfil: 'PORTEIRO' }] }],
      ['/api/portaria/usuarios-condominio', 'Usuários do condomínio', 'Todos os usuários ativos do condomínio, para a busca da portaria.', { sucesso: true, usuarios: [USUARIO()] }],
      ['/api/portaria/historico-acesso', 'Histórico de acesso', 'Entradas e saídas do condomínio, mais recentes primeiro.', { sucesso: true, registros: [REGISTRO] }],
    ].map(([caminho, summary, description, exemplo]) => [caminho, {
      get: op('Portaria', summary, `${description} Perfis: ADMIN_GERAL, ADMIN_SINDICO e PORTEIRO (MORADOR e CONVIDADO recebem 403).`, {
        servers: [{ url: 'http://localhost:3001', description: 'Porta do auth-api' }],
        responses: { 200: resposta('Lista.', exemplo), 401: E401, 403: E403 },
      }),
    }])),
    '/api/portaria/meus-guests': {
      get: op('Portaria', 'Meus convidados', 'Convidados da unidade de quem chama. Perfis: ADMIN_SINDICO, MORADOR e DONO_ALUGUEL.', {
        servers: [{ url: 'http://localhost:3001', description: 'Porta do auth-api' }],
        responses: { 200: resposta('Convidados.', { sucesso: true, guests: [{ ...OCUPANTE, statusAcesso: 'FORA', ultimoRegistroEm: null }] }), 401: E401, 403: E403 },
      }),
    },
    '/api/portaria/pessoas-unidade': {
      get: op('Portaria', 'Pessoas de uma unidade', 'Quem mora na unidade, para a portaria avisar de uma entrega ou visita. Morador vê a própria; portaria e gestão escolhem em `unidadeId`. Todos os perfis menos CONVIDADO.', {
        servers: [{ url: 'http://localhost:3001', description: 'Porta do auth-api' }],
        parameters: [pQuery('unidadeId', 'Só para portaria e gestão.', UNIDADE_101)],
        responses: { 200: resposta('Pessoas.', { sucesso: true, pessoas: [{ id: '4', nome: 'Ana Souza', email: 'morador1.verde@mora.test', perfil: 'MORADOR' }] }), 401: E401, 403: E403 },
      }),
    },
    '/api/portaria/historico/{userId}': {
      get: op('Portaria', 'Histórico de uma pessoa', 'Entradas e saídas de um usuário. Perfis: ADMIN_GERAL, ADMIN_SINDICO e PORTEIRO.', {
        servers: [{ url: 'http://localhost:3001', description: 'Porta do auth-api' }],
        parameters: [pId('userId', 'Id do usuário.', 4, { type: 'integer' })],
        responses: { 200: resposta('Registros.', { sucesso: true, registros: [REGISTRO] }), 401: E401, 403: E403, 404: E404('Usuário') },
      }),
    },
    '/api/portaria/entrada/{userId}': {
      post: op('Portaria', 'Registrar entrada', 'Registra a entrada de uma pessoa cadastrada. Para funcionário, consulta o turno no portaria-service; fora do turno, só com liberação excepcional, que vem sinalizada em `alerta`. Perfis: ADMIN_GERAL, ADMIN_SINDICO e PORTEIRO.', {
        servers: [{ url: 'http://localhost:3001', description: 'Porta do auth-api' }],
        parameters: [pId('userId', 'Id de quem entra.', 4, { type: 'integer' })],
        'x-sem-corpo': true,
        responses: {
          200: resposta('Entrada registrada.', { sucesso: true, mensagem: 'Entrada de Ana Souza registrada.' }),
          400: falha('Funcionário fora do turno (regra do portaria-service).', 'Entrada fora do turno.'),
          401: E401,
          403: falha('Convidado não autorizado pelo responsável, ou usuário de outro condomínio.', 'Entrada não permitida. O responsável da unidade não autorizou a entrada deste convidado.'),
          404: falha('Usuário inexistente ou inativo.', 'Usuário não encontrado ou inativo.'),
        },
      }),
    },
    '/api/portaria/saida/{userId}': {
      post: op('Portaria', 'Registrar saída', 'Fecha a entrada em aberto da pessoa. Perfis: ADMIN_GERAL, ADMIN_SINDICO e PORTEIRO.', {
        servers: [{ url: 'http://localhost:3001', description: 'Porta do auth-api' }],
        parameters: [pId('userId', 'Id de quem sai.', 4, { type: 'integer' })],
        'x-sem-corpo': true,
        responses: {
          200: resposta('Saída registrada.', { sucesso: true, mensagem: 'Saída de Ana Souza registrada.' }),
          401: E401,
          403: falha('Usuário de outro condomínio.', 'Usuário não pertence a este condomínio.'),
          404: falha('Usuário inexistente ou inativo.', 'Usuário não encontrado ou inativo.'),
        },
      }),
    },
    '/api/portaria/guests/{guestId}/permissao': {
      patch: op('Portaria', 'Autorizar ou bloquear convidado', 'Liga ou desliga a entrada do convidado; sem `permitir`, inverte o estado atual. Perfis: responsável da unidade e gestão.', {
        servers: [{ url: 'http://localhost:3001', description: 'Porta do auth-api' }],
        parameters: [pId('guestId', 'Id do convidado.', 9, { type: 'integer' })],
        requestBody: corpo('Autorização.', { permitir: true }, { type: 'object', properties: { permitir: { type: 'boolean' } } }),
        responses: { 200: resposta('Atualizado.', { sucesso: true, mensagem: 'Entrada de Rita Convidada autorizada.', entradaPermitida: true }), 401: E401, 403: E403, 404: E404('Convidado') },
      }),
    },
    // ── Estatísticas ────────────────────────────────────────────────────────
    '/api/estatisticas/plataforma': {
      get: op('Estatísticas', 'Números da plataforma', `Condomínios e usuários de toda a plataforma, com os filtros aplicados na consulta. Devolve também os ids dos condomínios que passaram no filtro, para o gestao-geral pedir as ocorrências ao ocorrencias-service com o mesmo recorte. ${soAdminGeral}`, {
        parameters: [
          pQuery('meses', 'Janela das séries, de 1 a 36 (padrão 12).', 12, { type: 'integer' }),
          pQuery('status', 'Restringe aos condomínios com este status.', 'todos', { type: 'string', enum: ['todos', 'active', 'inactive'] }),
        ],
        responses: {
          200: resposta('Números.', {
            sucesso: true, filtros: { meses: 12, status: 'todos' },
            condominios: { total: 10, ativos: 10, inativos: 0, novosUltimos30: 0, criadosPorMes: [{ mes: '2025-11', total: 0 }, { mes: '2025-12', total: 1 }], meses: 12 },
            usuarios: { total: 388, ativos: 321, porPerfil: { ADMIN_GERAL: 1, ADMIN_SINDICO: 11, MORADOR: 293 }, porStatus: { active: 321, inactive: 24, pending_activation: 43 }, porCondominio: [{ condominioId: 'qa-vila-serena', nome: 'Residencial Vila Serena', total: 122 }], convitesPendentes: 0 },
            condominioIds: null,
          }),
          401: E401, 403: falha('Não é Admin Geral.', 'Apenas o Admin Geral pode consultar dados da plataforma.'),
        },
      }),
    },
    '/api/estatisticas/condominios/{id}': {
      get: op('Estatísticas', 'Resumo de um condomínio', 'Usuários por perfil e status e convites pendentes. Perfis: ADMIN_GERAL e quem pertence ao condomínio.', {
        parameters: [pId('id', 'Id do condomínio.', COND)],
        responses: {
          200: resposta('Resumo.', { sucesso: true, resumo: { condominioId: COND, usuarios: { total: 8, ativos: 8, porPerfil: { ADMIN_SINDICO: 1, PORTEIRO: 1, MORADOR: 4, DONO_ALUGUEL: 1, CONVIDADO: 1 }, porStatus: { active: 8 } }, convitesPendentes: 0 } }),
          401: E401, 403: falha('Condomínio de outro cliente.', 'Acesso negado'),
        },
      }),
    },
    // ── Documentação e saúde ────────────────────────────────────────────────
    '/api/auth/docs/openapi.json': {
      get: op('Documentação', 'Esta especificação', 'A especificação OpenAPI do auth-api, em JSON. Pública.', {
        ...publica,
        responses: { 200: resposta('Especificação OpenAPI.', { openapi: '3.0.3', info: { title: 'auth-api', version: '1.0' } }) },
      }),
    },
    '/api/health': {
      get: op('Saúde', 'Saúde do serviço', 'Usada pelo Consul. Pública.', {
        ...publica,
        servers: [{ url: 'http://localhost:3001', description: 'Porta do auth-api' }],
        responses: { 200: resposta('No ar.', { status: 'ok', message: 'Auth API funcionando' }) },
      }),
    },
  },
};
