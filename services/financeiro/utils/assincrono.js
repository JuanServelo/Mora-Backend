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
