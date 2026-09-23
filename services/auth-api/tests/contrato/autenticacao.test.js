import request from 'supertest';
import jwt from 'jsonwebtoken';
import app from '../../app.js';
import { INVENTARIO, PUBLICA, TOKEN, rotasCom, separar, comParametros } from './inventario.js';

/**
 * A porta de entrada de cada rota, exercitada uma a uma.
 *
 * Roda sem banco: o `authMiddleware` responde 401 antes de qualquer consulta,
 * então a recusa é verificável com o Postgres desligado. É o que torna viável
 * cobrir as 61 rotas em toda execução, e não só quando a stack está de pé.
 *
 * Três formas de token inválido, porque falham por caminhos diferentes no
 * `jwt.verify` e já foram confundidas entre si: ausente, malformado e assinado
 * com outro segredo. A terceira é a que pega o erro mais grave — aceitar token
 * que qualquer um consegue forjar.
 */
const chamar = (chave, token) => {
  const { metodo, caminho } = separar(chave);
  const req = request(app)[metodo.toLowerCase()](comParametros(caminho));
  return token ? req.set('Authorization', `Bearer ${token}`).send({}) : req.send({});
};

describe('rotas protegidas recusam quem não se identifica', () => {
  const protegidas = rotasCom(TOKEN);

  test('há rotas protegidas para testar', () => {
    expect(protegidas.length).toBeGreaterThan(0);
  });

  test.each(protegidas)('%s → 401 sem token', async (chave) => {
    const r = await chamar(chave, null);
    expect(r.status).toBe(401);
  });

  test.each(protegidas)('%s → 401 com token malformado', async (chave) => {
    const r = await chamar(chave, 'isto-nao-e-um-jwt');
    expect(r.status).toBe(401);
  });

  test.each(protegidas)('%s → 401 com token assinado por outro segredo', async (chave) => {
    // O caso que importa: um token bem formado, com as claims certas, mas
    // assinado por quem não tem o segredo. Se passar, qualquer pessoa entra
    // como qualquer usuário.
    const forjado = jwt.sign(
      { id: 1, perfil: 'ADMIN_GERAL', tokenVersion: 0 },
      'segredo-que-o-servico-nao-conhece',
      { expiresIn: '1h' },
    );
    const r = await chamar(chave, forjado);
    expect(r.status).toBe(401);
  });

  test.each(protegidas)('%s → 401 com token expirado', async (chave) => {
    const expirado = jwt.sign(
      { id: 1, perfil: 'ADMIN_GERAL', tokenVersion: 0 },
      process.env.JWT_SECRET,
      { expiresIn: '-1h' },
    );
    const r = await chamar(chave, expirado);
    expect(r.status).toBe(401);
  });
});

describe('rotas públicas respondem sem token', () => {
  const publicas = rotasCom(PUBLICA);

  test.each(publicas)('%s não responde 401', async (chave) => {
    const r = await chamar(chave, null);
    // O que se afirma é só que a rota não exige identidade. O corpo vazio leva
    // a 400 na maioria delas, e isso é resposta legítima — quem valida o
    // conteúdo é outro teste.
    expect(r.status).not.toBe(401);
  });
});

describe('cobertura', () => {
  test('toda rota do inventário aparece em algum dos dois grupos', () => {
    const cobertas = [...rotasCom(TOKEN), ...rotasCom(PUBLICA)].sort();
    expect(cobertas).toEqual(Object.keys(INVENTARIO).sort());
  });
});
