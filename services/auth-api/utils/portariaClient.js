const getPortariaUrl = () =>
  process.env.PORTARIA_SERVICE_URL || 'http://localhost:8090';

/**
 * Valida se unidade existe no portaria-service.
 * Retorna true se indisponível (modo degradado) para não bloquear dev local.
 */
export async function validarUnidadeExiste(unidadeId) {
  if (!unidadeId) return false;
  try {
    const res = await fetch(`${getPortariaUrl()}/apartamentos/${unidadeId}`, {
      signal: AbortSignal.timeout(3000),
    });
    return res.ok;
  } catch {
    console.warn('[portariaClient] Serviço indisponível — pulando validação de unidade');
    return true;
  }
}

/**
 * Consulta a jornada do funcionário antes de registrar a entrada (RF-09, RN-04).
 *
 * A regra de turno vive no portaria-service; aqui só perguntamos. O token do
 * porteiro é repassado para o outro serviço resolver o condomínio sozinho.
 *
 * Falha aberta de propósito: se o portaria-service estiver fora do ar, a
 * entrada é permitida. Trancar a portaria por indisponibilidade de um serviço
 * de apoio seria pior que deixar passar — e a saída nunca é bloqueada mesmo.
 */
export async function avaliarEntradaFuncionario(userId, token) {
  try {
    const res = await fetch(
      `${getPortariaUrl()}/jornadas/${encodeURIComponent(userId)}/avaliar-entrada`,
      {
        headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
        signal: AbortSignal.timeout(3000),
      },
    );
    if (!res.ok) {
      console.warn(`[portariaClient] avaliar-entrada respondeu HTTP ${res.status}`);
      return { permitido: true, indisponivel: true };
    }
    return await res.json();
  } catch (e) {
    console.warn('[portariaClient] Jornada indisponível — modo degradado:', e.message);
    return { permitido: true, indisponivel: true };
  }
}
