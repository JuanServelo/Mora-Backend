import { SERVICOS } from '../config/servicos.js';
import { buscar } from '../utils/httpClient.js';

/**
 * Assinatura vigente do condomínio no plan-service: o plano e a mensalidade
 * que a cobrança recorrente vai cobrar. 404 é "sem plano", não falha.
 */
export async function assinaturaVigente(condominioId, authorization) {
  return buscar(
    `${SERVICOS.plan}/api/assinaturas/condominio/${encodeURIComponent(condominioId)}`,
    authorization,
  );
}
