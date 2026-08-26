import * as auth from '../clients/authClient.js';

/**
 * Compõe o painel do Admin Geral a partir das fontes disponíveis.
 *
 * O contrato para o frontend é único e estável: quando outras fontes entrarem
 * (estrutura física do portaria, assinaturas do plan-service), elas viram
 * blocos novos aqui, sem que a tela precise saber de onde vem cada número.
 */
export async function montarDashboard(authorization) {
  const fontesIndisponiveis = [];

  const plataforma = await auth.estatisticasPlataforma(authorization);

  if (!plataforma.ok) {
    // Sem a fonte principal não há painel: melhor um erro claro que uma tela vazia.
    return {
      erro: true,
      status: plataforma.status === 403 ? 403 : 503,
      mensagem:
        plataforma.status === 403
          ? 'Sem permissão para consultar dados da plataforma.'
          : 'Serviço de identidade indisponível. Tente novamente em instantes.',
    };
  }

  const { condominios, usuarios, ocorrencias } = plataforma.dados;

  return {
    sucesso: true,
    geradoEm: new Date().toISOString(),
    condominios,
    usuarios,
    ocorrencias,
    fontesIndisponiveis,
  };
}

export async function montarResumoCondominio(id, authorization) {
  const resumo = await auth.resumoCondominio(id, authorization);

  if (!resumo.ok) {
    return {
      erro: true,
      status: resumo.status === 403 ? 403 : 503,
      mensagem:
        resumo.status === 403
          ? 'Sem permissão para consultar este condomínio.'
          : 'Não foi possível carregar o resumo do condomínio.',
    };
  }

  return {
    sucesso: true,
    geradoEm: new Date().toISOString(),
    resumo: resumo.dados.resumo,
    fontesIndisponiveis: [],
  };
}
