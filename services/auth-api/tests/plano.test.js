import { vi, afterEach, test, expect } from 'vitest';
import { exigirModulo } from '../middleware/plano.js';

afterEach(() => vi.restoreAllMocks());

test('consulta o módulo com o token e só libera resposta 204', async () => {
  for (const [status, esperado] of [[204, null], [401, 401], [403, 403], [500, 503]]) {
    const consulta = vi.spyOn(globalThis, 'fetch').mockResolvedValue({ status });
    const res = { status: vi.fn().mockReturnThis(), json: vi.fn() };
    const next = vi.fn();
    await exigirModulo('reclamacoes')({ headers: { authorization: 'Bearer token' } }, res, next);
    expect(consulta).toHaveBeenCalledWith(expect.stringContaining('/api/plans/access/reclamacoes'),
      expect.objectContaining({ headers: { Authorization: 'Bearer token' } }));
    if (esperado === null) expect(next).toHaveBeenCalledOnce();
    else { expect(next).not.toHaveBeenCalled(); expect(res.status).toHaveBeenCalledWith(esperado); }
    vi.restoreAllMocks();
  }
});

test('indisponibilidade e ausência de token bloqueiam', async () => {
  const consulta = vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('offline'));
  const next = vi.fn();
  const res = { status: vi.fn().mockReturnThis(), json: vi.fn() };
  await exigirModulo('portaria')({ headers: {} }, res, next);
  expect(res.status).toHaveBeenLastCalledWith(401);
  expect(consulta).not.toHaveBeenCalled();
  await exigirModulo('portaria')({ headers: { authorization: 'Bearer token' } }, res, next);
  expect(res.status).toHaveBeenLastCalledWith(503);
  expect(next).not.toHaveBeenCalled();
});
