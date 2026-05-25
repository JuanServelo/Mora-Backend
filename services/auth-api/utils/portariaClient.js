const getPortariaUrl = () =>
  process.env.PORTARIA_SERVICE_URL || 'http://localhost:8090';

/**
 * Valida se unidade existe no portaria-service.
 * Retorna true se indisponível (modo degradado) para não bloquear dev local.
 */
export async function validarUnidadeExiste(unidadeId) {
  if (!unidadeId) return false;
  try {
    const res = await fetch(`${getPortariaUrl()}/api/apartamentos/${unidadeId}`, {
      signal: AbortSignal.timeout(3000),
    });
    return res.ok;
  } catch {
    console.warn('[portariaClient] Serviço indisponível — pulando validação de unidade');
    return true;
  }
}
