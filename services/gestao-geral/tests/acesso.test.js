// Quem pode abrir os painéis do síndico. Nenhum caso chega às outras fontes:
// a recusa acontece antes de qualquer chamada de rede.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import jwt from 'jsonwebtoken';
import request from 'supertest';

process.env.JWT_SECRET = 'segredo-de-teste';
const { app } = await import('../app.js');

const token = (perfil, extra = {}) => jwt.sign(
  { id: 1, perfil, tokenVersion: 0, condominioId: 'cond-a', ...extra },
  process.env.JWT_SECRET,
);
const get = (caminho, cabecalho) => {
  const r = request(app).get(`/api/gestao${caminho}`);
  return cabecalho ? r.set('Authorization', cabecalho) : r;
};

for (const caminho of ['/painel/operacional', '/painel/estrategico']) {
  test(`${caminho}: sem token → 401`, async () => {
    assert.equal((await get(caminho)).status, 401);
  });

  test(`${caminho}: token de outro segredo → 401`, async () => {
    const falso = jwt.sign({ id: 1, perfil: 'ADMIN_SINDICO', condominioId: 'cond-a' }, 'outro');
    assert.equal((await get(caminho, `Bearer ${falso}`)).status, 401);
  });

  for (const perfil of ['MORADOR', 'PORTEIRO', 'DONO_ALUGUEL', 'ADMIN_GERAL']) {
    test(`${caminho}: ${perfil} → 403`, async () => {
      const r = await get(caminho, `Bearer ${token(perfil)}`);
      assert.equal(r.status, 403);
      assert.equal(r.body.sucesso, false);
    });
  }

  test(`${caminho}: síndico sem condomínio → 400`, async () => {
    const r = await get(caminho, `Bearer ${token('ADMIN_SINDICO', { condominioId: null })}`);
    assert.equal(r.status, 400);
  });
}

test('janela fora de 3, 6 ou 12 meses → 400', async () => {
  const r = await get('/painel/estrategico?meses=24', `Bearer ${token('ADMIN_SINDICO')}`);
  assert.equal(r.status, 400);
});
