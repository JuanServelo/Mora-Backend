import { describe, it, expect } from 'vitest';
import { migracoes } from '../migrations/index.js';

/**
 * O nome de cada migração é a chave do histórico em `SequelizeMeta`. Renomear
 * uma que já rodou faz o Umzug achar que ela é nova; reordenar muda a ordem
 * em banco novo. Estes testes não pegam tudo, mas pegam o descuido comum.
 */
describe('migrações do auth_db', () => {
  const nomes = migracoes.map((m) => m.name);
  const posicao = (sufixo) => nomes.findIndex((n) => n.endsWith(sufixo));

  it('são numeradas em sequência, sem repetir', () => {
    nomes.forEach((nome, i) => {
      expect(nome).toMatch(new RegExp(`^${String(i + 1).padStart(3, '0')}-[a-z0-9-]+$`));
    });
    expect(new Set(nomes).size).toBe(nomes.length);
  });

  it('cada uma tem a função que aplica', () => {
    migracoes.forEach((m) => expect(typeof m.up).toBe('function'));
  });

  it('a estrutura inicial vem antes de tudo', () => {
    expect(nomes[0]).toBe('001-estrutura-inicial');
  });

  it('as colunas legadas só caem depois de convertidas', () => {
    const remover = posicao('remover-colunas-legado');
    expect(remover).toBeGreaterThan(posicao('rf03-usuarios-legados'));
    expect(remover).toBeGreaterThan(posicao('perfis-v2'));
  });

  it('o perfil TERCEIRO entra depois do enum da v2 existir', () => {
    expect(posicao('perfil-terceiro')).toBeGreaterThan(posicao('perfis-v2'));
  });
});
