import request from 'supertest';
import app from '../app.js';
import User from '../models/User.js';

/**
 * O limitador de login conta por IP **e** e-mail.
 *
 * O defeito que isto cobre: contado só por IP, dez senhas erradas de uma pessoa
 * bloqueavam o login de todo mundo que chegasse pelo mesmo endereço — no Docker
 * Desktop, todos os clientes.
 *
 * Sem banco: `findOne` devolve nulo, que é o caminho de e-mail ou senha errados.
 * O limitador guarda as contagens em memória por toda a vida do `app`, então
 * cada teste usa e-mails próprios para não herdar tentativas do anterior.
 */
beforeEach(() => {
  vi.spyOn(User, 'scope').mockReturnValue({ findOne: async () => null });
});

afterEach(() => {
  vi.restoreAllMocks();
});

const login = (email) =>
  request(app).post('/api/auth/login').send({ email, senha: 'senha-errada' });

describe('limitador de login', () => {
  test('a 11ª tentativa do mesmo e-mail é barrada', async () => {
    for (let i = 0; i < 10; i++) {
      const r = await login('ana@limite.test');
      expect(r.status).not.toBe(429);
    }
    const r = await login('ana@limite.test');
    expect(r.status).toBe(429);
  });

  test('esgotar as tentativas de um e-mail não bloqueia outro do mesmo IP', async () => {
    for (let i = 0; i < 11; i++) await login('bruno@limite.test');

    const outro = await login('carla@limite.test');
    expect(outro.status).not.toBe(429);
  });

  test('o e-mail conta igual com maiúsculas e espaços', async () => {
    for (let i = 0; i < 10; i++) await login('davi@limite.test');

    const r = await login('  DAVI@limite.test ');
    expect(r.status).toBe(429);
  });
});
