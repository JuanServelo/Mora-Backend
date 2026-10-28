/**
 * Portal de documentação: o sumário dos serviços e o Swagger UI com um seletor.
 *
 * A lista de serviços é a única coisa escrita aqui. O número de rotas e a
 * situação de cada um são lidos da especificação ao abrir a página, então o
 * sumário não envelhece: rota nova aparece na contagem, serviço fora do ar
 * aparece como fora do ar.
 */
const SERVICOS = [
  {
    nome: 'auth-api', url: '/api/auth/docs/openapi.json',
    faz: 'Login (e-mail e Google), usuários, convites, condomínios e perfis',
    responsavel: 'João Victor', banco: 'auth_db',
  },
  {
    nome: 'portaria-service', url: '/api/portaria/v3/api-docs',
    faz: 'Estrutura física, moradores, funcionários, visitantes, entregas, reservas e vagas',
    responsavel: 'Juan Servelo', banco: 'mora',
  },
  {
    nome: 'comunicacao-service', url: '/api/comunicacao/v3/api-docs',
    faz: 'Avisos, base de conhecimento, conversas e notificações',
    responsavel: 'João Victor', banco: 'mora',
  },
  {
    nome: 'ocorrencias-service', url: '/api/ocorrencias/v3/api-docs',
    faz: 'Ocorrências dos moradores, ordens de serviço e fornecedores; notificações pela fila (RabbitMQ)',
    responsavel: 'Ray Govaski', banco: 'mora_ocorrencias',
  },
  {
    nome: 'financeiro', url: '/api/financeiro/docs/openapi.json',
    faz: 'Taxas, rateio, faturas, multas, contratos, prestação de contas e cobrança via Asaas',
    responsavel: 'João Victor', banco: 'mora_financeiro',
  },
  {
    nome: 'gestao-geral', url: '/api/gestao/docs/openapi.json',
    faz: 'Painéis da plataforma e do síndico, agregando os outros serviços; preferências dos painéis',
    responsavel: 'João Victor', banco: 'mora_gestao',
  },
  {
    nome: 'plan-service', url: '/api/plans/v3/api-docs',
    faz: 'Planos comerciais e assinaturas dos condomínios',
    responsavel: 'Luana Akemi', banco: 'mora_plan',
  },
  {
    nome: 'meeting-service', url: '/api/meetings/v3/api-docs',
    faz: 'Assembleias, atas e votações, com sala no Google Meet',
    responsavel: 'Luana Akemi', banco: 'mora_meeting',
  },
];

const METODOS = ['get', 'post', 'put', 'patch', 'delete'];

const contarRotas = (spec) => Object.values(spec.paths ?? {})
  .reduce((total, ops) => total + METODOS.filter((m) => ops[m]).length, 0);

function celula(texto, classe) {
  const td = document.createElement('td');
  td.textContent = texto;
  if (classe) td.className = classe;
  return td;
}

async function montarSumario() {
  const corpo = document.getElementById('linhas-sumario');
  for (const s of SERVICOS) {
    const tr = document.createElement('tr');
    const nome = document.createElement('td');
    const link = document.createElement('a');
    link.href = '#';
    link.textContent = s.nome;
    link.addEventListener('click', (e) => {
      e.preventDefault();
      // Troca a especificação aberta no Swagger UI, como o seletor faz.
      window.ui.specActions.updateUrl(s.url);
      window.ui.specActions.download(s.url);
      document.getElementById('swagger-ui').scrollIntoView({ behavior: 'smooth' });
    });
    nome.appendChild(link);
    const rotas = celula('…');
    const situacao = celula('verificando…');
    tr.append(nome, celula(s.faz), celula(s.responsavel), celula(s.banco), rotas, situacao);
    corpo.appendChild(tr);

    try {
      const r = await fetch(s.url);
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      rotas.textContent = String(contarRotas(await r.json()));
      situacao.textContent = 'no ar';
      situacao.className = 'ok';
    } catch (e) {
      rotas.textContent = '—';
      situacao.textContent = `fora do ar (${e.message})`;
      situacao.className = 'fora';
    }
  }
}

window.ui = SwaggerUIBundle({
  dom_id: '#swagger-ui',
  urls: SERVICOS.map((s) => ({ url: s.url, name: s.nome })),
  'urls.primaryName': 'auth-api',
  presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
  layout: 'StandaloneLayout',
  deepLinking: true,
  docExpansion: 'none',
  persistAuthorization: true,
  displayRequestDuration: true,
  tryItOutEnabled: false,
  // Sem o validador público: ele chamaria um serviço externo a cada página.
  validatorUrl: null,
});

montarSumario();
