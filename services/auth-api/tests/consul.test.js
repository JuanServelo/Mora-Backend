import { describe, it, expect } from 'vitest';
import { regraDoGateway } from '../config/consul.js';

describe('regra do gateway', () => {
  it('não anuncia /api/portaria, que no Traefik é do portaria-service', () => {
    expect(regraDoGateway()).not.toContain('/api/portaria');
  });

  it('anuncia os prefixos do auth-api', () => {
    const regra = regraDoGateway();
    for (const p of ['/api/auth', '/api/estatisticas', '/uploads/avatars']) {
      expect(regra).toContain(`PathPrefix(\`${p}\`)`);
    }
  });
});
