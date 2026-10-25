/**
 * Encaminha ao tratador de erro do Express o erro de um handler `async`.
 *
 * O Express 4 não espera a Promise: se o handler rejeitar — banco fora do ar,
 * constraint violada —, a requisição fica pendurada sem resposta, e a rejeição
 * sem tratamento derruba o processo nas versões atuais do Node. Com isto, o
 * erro chega ao `app.use((err, ...))` e vira um 500 com mensagem genérica.
 */
export const assincrono = (fn) => (req, res, next) =>
  Promise.resolve(fn(req, res, next)).catch(next);

/** Embrulha todas as funções exportadas por um controller. */
export function embrulhar(controller) {
  return Object.fromEntries(
    Object.entries(controller).map(([nome, fn]) => [nome, typeof fn === 'function' ? assincrono(fn) : fn]),
  );
}

/**
 * Protege todas as rotas de um router de uma vez.
 *
 * Para os arquivos de rota que definem o handler direto na rota, sem
 * controller — onde `embrulhar` não alcança. Percorre o que já foi registrado e
 * troca cada handler pela versão que encaminha a rejeição ao `next`.
 *
 * Handler de erro (4 argumentos) fica como está: o Express o reconhece pela
 * aridade, e embrulhá-lo o transformaria num handler comum.
 */
export function protegerRotas(router) {
  for (const camada of router.stack) {
    if (!camada.route) continue;
    for (const etapa of camada.route.stack) {
      const original = etapa.handle;
      if (original.length >= 4 || original.__protegido) continue;
      const protegido = assincrono(original);
      protegido.__protegido = true;
      etapa.handle = protegido;
    }
  }
  return router;
}
