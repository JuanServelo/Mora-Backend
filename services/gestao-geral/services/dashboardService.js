import * as auth from '../clients/authClient.js';
import { estruturaDoCondominio } from '../clients/portariaClient.js';
import { assinaturaDoCondominio } from '../clients/planClient.js';

/**
 * Compõe os painéis a partir das fontes disponíveis.
 *
 * O contrato para o frontend é único e estável: cada bloco vem de um serviço
 * diferente, e `fontesIndisponiveis` diz o que não pôde ser carregado, para a
 * tela mostrar um traço naquele ponto em vez de quebrar inteira.
 */

/** Sem a fonte principal não há painel: erro claro em vez de tela vazia. */
function falhaDaFontePrincipal(resultado, oQue) {
  return {
    erro: true,
    status: resultado.status === 403 ? 403 : 503,
    mensagem: resultado.status === 403
      ? `Sem permissão para consultar ${oQue}.`
      : 'Serviço de identidade indisponível. Tente novamente em instantes.',
  };
}

export async function montarDashboard(authorization) {
  const fontesIndisponiveis = [];

  const plataforma = await auth.estatisticasPlataforma(authorization);
  if (!plataforma.ok) return falhaDaFontePrincipal(plataforma, 'dados da plataforma');

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
  const fontesIndisponiveis = [];

  const resumo = await auth.resumoCondominio(id, authorization);
  if (!resumo.ok) return falhaDaFontePrincipal(resumo, 'este condomínio');

  // Estrutura e assinatura são acessórias: se caírem, o resumo ainda serve.
  const [estrutura, assinatura] = await Promise.all([
    estruturaDoCondominio(id, authorization),
    assinaturaDoCondominio(id, authorization),
  ]);

  if (!estrutura.ok) fontesIndisponiveis.push('portaria-service');
  if (!assinatura.ok) fontesIndisponiveis.push('plan-service');

  return {
    sucesso: true,
    geradoEm: new Date().toISOString(),
    resumo: resumo.dados.resumo,
    estrutura: estrutura.ok ? estrutura.dados : null,
    assinatura: assinatura.ok ? assinatura.dados : null,
    fontesIndisponiveis,
  };
}

/**
 * Painel do síndico: os números do condomínio dele.
 *
 * Traz o que hoje tem fonte real — usuários, convites, ocorrências, estrutura
 * física e plano contratado. Os indicadores financeiros entram quando o
 * financeiro-service existir; até lá o bloco vem nulo, e a tela sabe disso.
 */
export async function montarPainelCondominio(id, authorization) {
  const base = await montarResumoCondominio(id, authorization);
  if (base.erro) return base;

  const { resumo, estrutura, assinatura } = base;

  // Ocupação: quantos apartamentos têm ao menos um morador vinculado.
  const totalApartamentos = estrutura?.apartamentos ?? null;
  const moradores = resumo.usuarios.porPerfil?.MORADOR ?? 0;
  const ocupacao = totalApartamentos
    ? Math.min(100, Math.round((moradores / totalApartamentos) * 100))
    : null;

  return {
    sucesso: true,
    geradoEm: base.geradoEm,
    condominioId: id,
    indicadores: {
      usuariosAtivos: resumo.usuarios.ativos,
      usuariosTotal: resumo.usuarios.total,
      convitesPendentes: resumo.convitesPendentes,
      ocorrenciasAbertas: resumo.ocorrencias,
      apartamentos: totalApartamentos,
      blocos: estrutura?.blocos ?? null,
      areasComuns: estrutura?.areasComuns ?? null,
      taxaOcupacao: ocupacao,
    },
    usuariosPorPerfil: resumo.usuarios.porPerfil,
    usuariosPorStatus: resumo.usuarios.porStatus,
    plano: assinatura
      ? {
        nome: assinatura.planNome,
        mensalidade: assinatura.mensalidade,
        status: assinatura.status,
        vigente: assinatura.vigente,
        limiteUsuarios: assinatura.maxUsuariosPorCondominio,
        modulos: assinatura.modulosAtivos,
      }
      : null,
    // Reservado para quando o financeiro-service existir.
    financeiro: null,
    fontesIndisponiveis: base.fontesIndisponiveis,
  };
}
