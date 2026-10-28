/** The plan-service verifies the JWT and resolves the tenant from its signed claims. */
export function exigirModulo(modulo) {
  return async (req, res, next) => {
    const authorization = req.headers.authorization;
    if (!authorization?.startsWith('Bearer ')) {
      return res.status(401).json({ sucesso: false, mensagem: 'Token não fornecido.' });
    }
    try {
      const base = (process.env.PLAN_SERVICE_URL || 'http://localhost:8093').replace(/\/+$/, '');
      const response = await fetch(`${base}/api/plans/access/${modulo}`, {
        headers: { Authorization: authorization }, signal: AbortSignal.timeout(3000), redirect: 'error',
      });
      if (response.status === 204) return next();
      return res.status([401, 403].includes(response.status) ? response.status : 503)
        .json({ sucesso: false, mensagem: 'Acesso ao módulo não autorizado.' });
    } catch {
      return res.status(503).json({ sucesso: false, mensagem: 'Não foi possível verificar o plano.' });
    }
  };
}
