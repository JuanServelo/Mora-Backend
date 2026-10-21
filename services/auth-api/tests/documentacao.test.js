// A especificação OpenAPI cobre toda rota do Express, com sumário, descrição
// e exemplos. Rota nova sem documentação quebra aqui.
import { describe, it, expect } from 'vitest';
import request from 'supertest';
import app from '../app.js';
import { rotasDe } from './helpers/rotas.js';
import { problemasDeDocumentacao } from './helpers/documentacao.js';
import { openapi } from '../docs/openapi.js';

describe('documentação OpenAPI', () => {
  it('toda rota está documentada, com sumário, descrição e exemplos', () => {
    expect(problemasDeDocumentacao(rotasDe(app), openapi)).toEqual([]);
  });

  it('a especificação é servida sem token', async () => {
    const r = await request(app).get('/api/auth/docs/openapi.json');
    expect(r.status).toBe(200);
    expect(r.body.info.title).toBe('auth-api');
  });
});
