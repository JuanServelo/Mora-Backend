import { describe, it, expect, vi, afterEach } from 'vitest';
import request from 'supertest';

afterEach(() => { vi.unstubAllEnvs(); vi.restoreAllMocks(); vi.resetModules(); });

describe('consulta interna pelo gateway', () => {
  it('exige a credencial de serviço e não aceita um JWT no lugar dela', async () => {
    vi.stubEnv('SERVICO_TOKEN', 'credencial-local-de-teste');
    vi.resetModules();
    const { default: app } = await import('../app.js');
    expect((await request(app).get('/api/interno/sindicos')).status).toBe(401);
    expect((await request(app).get('/api/interno/sindicos').set('Authorization', 'Bearer qualquer-jwt')).status).toBe(401);
    expect((await request(app).get('/api/interno/sindicos').set('X-Servico-Token', 'credencial-local-de-teste')).status).toBe(400);
  });
});
