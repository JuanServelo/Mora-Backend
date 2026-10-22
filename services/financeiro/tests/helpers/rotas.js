/**
 * As rotas que o app realmente registrou, lidas do próprio Express.
 *
 * Uma lista escrita à mão envelhece em silêncio: alguém acrescenta um endpoint,
 * ninguém lembra de acrescentar o teste, e a suíte continua verde afirmando uma
 * cobertura que já não existe. Perguntar ao router elimina essa classe inteira
 * de defeito — rota nova sem entrada no inventário **quebra o teste**.
 */

/** Converte o regexp interno de um `app.use(prefixo, router)` de volta em texto. */
function prefixoDe(camada) {
  if (camada.path) return camada.path;

  const fonte = camada.regexp?.source;
  if (!fonte) return '';

  // Express guarda `/api/auth` como /^\/api\/auth\/?(?=\/|$)/i
  const limpo = fonte
    .replace('^', '')
    .replace('\\/?(?=\\/|$)', '')
    .replace(/\\\//g, '/')
    .replace(/\$$/, '');

  return limpo === '/' ? '' : limpo;
}

/**
 * Todas as rotas do app, como `{ metodo, caminho }`.
 *
 * Percorre a pilha de camadas recursivamente: `app.use(prefixo, router)` vira
 * uma camada sem rota própria, cujo `handle` é outra pilha.
 */
export function rotasDe(app, base = '', pilha = app?._router?.stack) {
  const encontradas = [];

  for (const camada of pilha ?? []) {
    if (camada.route) {
      const caminho = `${base}${camada.route.path}`.replace(/\/$/, '') || '/';
      for (const [metodo, ativo] of Object.entries(camada.route.methods)) {
        if (ativo && metodo !== '_all') {
          encontradas.push({ metodo: metodo.toUpperCase(), caminho });
        }
      }
      continue;
    }

    if (camada.name === 'router' && camada.handle?.stack) {
      encontradas.push(...rotasDe(app, base + prefixoDe(camada), camada.handle.stack));
    }
  }

  return encontradas;
}

/** `GET /api/auth/me` — a forma usada como chave no inventário. */
export function chaveDaRota({ metodo, caminho }) {
  return `${metodo} ${caminho}`;
}

/** Todas as rotas do app, já como chaves, ordenadas. */
export function chavesDe(app) {
  return rotasDe(app).map(chaveDaRota).sort();
}
