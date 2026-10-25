/**
 * Confere a especificação OpenAPI contra as rotas que o Express registrou.
 *
 * O critério é o que a documentação promete: toda rota tem sumário, descrição
 * e exemplo — do corpo que recebe e de cada resposta de sucesso. Uma
 * especificação escrita à mão envelhece em silêncio; comparada com o router,
 * rota nova sem documentação vira teste vermelho.
 *
 * Um POST/PUT/PATCH sem corpo declara `'x-sem-corpo': true` na operação, para
 * a ausência de `requestBody` ser uma decisão escrita, e não um esquecimento.
 * Rota desativada que só recusa é marcada `deprecated` e fica sem resposta de
 * sucesso.
 */

/** `/condominios/:id` do Express para `/condominios/{id}` do OpenAPI. */
export const paraOpenApi = (caminho) => caminho.replace(/:(\w+)/g, '{$1}');

const METODOS = ['get', 'post', 'put', 'patch', 'delete'];
const COM_CORPO = ['POST', 'PUT', 'PATCH'];

/** Todo tipo de conteúdo traz um exemplo. */
const temExemplo = (content) => {
  const tipos = Object.values(content ?? {});
  return tipos.length > 0 && tipos.every((m) => m.example !== undefined
    || (m.examples && Object.keys(m.examples).length > 0));
};

/** Lista o que falta na documentação; vazio quando está completa. */
export function problemasDeDocumentacao(rotas, spec) {
  const problemas = [];
  const documentadas = new Set();

  for (const { metodo, caminho } of rotas) {
    const chave = `${metodo} ${caminho}`;
    const op = spec.paths?.[paraOpenApi(caminho)]?.[metodo.toLowerCase()];
    if (!op) {
      problemas.push(`${chave}: rota sem documentação`);
      continue;
    }
    documentadas.add(`${metodo.toLowerCase()} ${paraOpenApi(caminho)}`);

    if (!op.summary?.trim()) problemas.push(`${chave}: sem summary`);
    if (!op.description?.trim()) problemas.push(`${chave}: sem description`);
    if (!op.tags?.length) problemas.push(`${chave}: sem tag`);

    if (op.requestBody) {
      if (!temExemplo(op.requestBody.content)) problemas.push(`${chave}: corpo sem exemplo`);
    } else if (COM_CORPO.includes(metodo) && !op['x-sem-corpo']) {
      problemas.push(`${chave}: ${metodo} sem requestBody (ou 'x-sem-corpo': true)`);
    }

    // 3xx conta como sucesso (o login com Google responde com redirecionamento);
    // operação `deprecated` pode não ter caso de sucesso nenhum.
    const sucesso = Object.entries(op.responses ?? {}).filter(([codigo]) => /^[23]/.test(codigo));
    if (!sucesso.length && !op.deprecated) problemas.push(`${chave}: sem resposta de sucesso`);
    for (const [codigo, r] of sucesso) {
      if (!r.description?.trim()) problemas.push(`${chave}: resposta ${codigo} sem descrição`);
      // 204 e afins não têm corpo; quando há corpo, há exemplo.
      if (r.content && !temExemplo(r.content)) problemas.push(`${chave}: resposta ${codigo} sem exemplo`);
    }
  }

  // O caminho inverso: documentação de rota que não existe mais engana quem lê.
  for (const [caminho, ops] of Object.entries(spec.paths ?? {})) {
    for (const metodo of METODOS) {
      if (ops[metodo] && !documentadas.has(`${metodo} ${caminho}`)) {
        problemas.push(`${metodo.toUpperCase()} ${caminho}: documentada, mas a rota não existe`);
      }
    }
  }

  return problemas;
}
