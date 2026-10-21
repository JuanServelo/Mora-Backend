/** Fail closed: no subscription, expired subscription or unavailable plan-service never grants access. */
export async function exigirPlano(req, res, next) {
  const authorization = req.headers.authorization;
  if (!authorization?.startsWith('Bearer ')) {
    return res.status(401).json({ sucesso: false, mensagem: 'Token não fornecido.' });
  }
  try {
    const base = (process.env.PLAN_SERVICE_URL || 'http://localhost:8093').replace(/\/+$/, '');
    const response = await fetch(`${base}/api/plans/access/financeiro`, {
      headers: { Authorization: authorization },
      signal: AbortSignal.timeout(3000),
      redirect: 'error',
    });
    if (response.status === 204) return next();
    const status = [401, 403].includes(response.status) ? response.status : 503;
    return res.status(status).json({ sucesso: false, mensagem: 'Acesso ao módulo financeiro não autorizado.' });
  } catch {
    return res.status(503).json({ sucesso: false, mensagem: 'Não foi possível verificar o plano.' });
  }
}
