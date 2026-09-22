import { SERVICOS } from '../config/servicos.js';
import { buscar } from '../utils/httpClient.js';

/**
 * Busca um aviso no portaria-service.
 *
 * Os avisos não foram movidos para cá: eles já funcionam onde estão, e mover
 * esquema e código seria trabalho sem entrega nova. O que faltava era o
 * registro de leitura, e é só isso que nasce neste serviço.
 *
 * Como não há FK entre bancos, esta consulta *é* a integridade referencial:
 * sem ela, qualquer UUID seria aceito como aviso lido.
 */
export async function buscarAviso(avisoId) {
  // A rota do portaria não pede token — é um defeito conhecido, registrado em
  // docs/PENDENCIAS.md. Quando ela passar a exigir, o Bearer entra aqui e
  // nada mais muda.
  const r = await buscar(`${SERVICOS.portaria}/avisos/${encodeURIComponent(avisoId)}`);
  if (!r.ok) return { ok: false, status: r.status ?? null, erro: r.erro };
  return { ok: true, aviso: r.dados };
}

/**
 * Apartamentos do condomínio, para traduzir `unidadeId` em algo legível.
 *
 * O auth-api guarda só o UUID da unidade no usuário; o nome do bloco e o número
 * do apartamento vivem no portaria. Um UUID na tela não ajuda ninguém a saber
 * de qual apartamento veio o chamado.
 */
export async function listarApartamentos(condominioId) {
  const url = `${SERVICOS.portaria}/apartamentos?condominioId=${encodeURIComponent(condominioId)}`;
  const r = await buscar(url);
  if (!r.ok) return null;
  return Array.isArray(r.dados) ? r.dados : [];
}

/** Avisos ativos de um condomínio, para cruzar com o que o morador já leu. */
export async function listarAvisosAtivos(condominioId) {
  const url = `${SERVICOS.portaria}/avisos/ativos?condominioId=${encodeURIComponent(condominioId)}`;
  const r = await buscar(url);
  if (!r.ok) return null;
  return Array.isArray(r.dados) ? r.dados : [];
}
