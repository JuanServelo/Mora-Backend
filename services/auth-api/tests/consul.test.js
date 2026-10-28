import { describe, it, expect } from 'vitest';
import { regraDoGateway } from '../config/consul.js';

describe('regra do gateway', () => {
  it('reserva /api/portaria ao serviço Java e anuncia o controle de acesso separadamente', () => {
    expect(regraDoGateway()).not.toContain('/api/portaria');
    expect(regraDoGateway()).toContain('PathPrefix(`/api/acessos`)');
  });

  it('anuncia os prefixos do auth-api', () => {
    const regra = regraDoGateway();
    for (const p of ['/api/auth', '/api/interno', '/api/estatisticas', '/uploads/avatars']) {
      expect(regra).toContain(`PathPrefix(\`${p}\`)`);
    }
  });
});
