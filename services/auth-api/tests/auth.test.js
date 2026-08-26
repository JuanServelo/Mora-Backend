import { validarSenha, senhaAtendeRequisitos } from '../utils/passwordValidation.js';
import { gerarCodigoConvite, normalizarCodigo } from '../utils/inviteCode.js';
import { podeCadastrarPerfil, PERFIS } from '../constants/perfis.js';
import { redirectPorPerfil } from '../utils/redirectPorPerfil.js';

describe('passwordValidation', () => {
  test('rejeita senha curta', () => {
    expect(validarSenha('Ab1')).toContain('A senha precisa ter ao menos 8 caracteres');
  });

  test('rejeita senha sem número', () => {
    expect(validarSenha('Abcdefgh')).toContain('Inclua ao menos 1 número');
  });

  test('rejeita senha sem maiúscula', () => {
    expect(validarSenha('abcdefgh1')).toContain('Inclua ao menos 1 letra maiúscula');
  });

  test('aceita senha válida', () => {
    expect(senhaAtendeRequisitos('Senha123')).toBe(true);
  });
});

describe('inviteCode', () => {
  test('normaliza código para uppercase', () => {
    expect(normalizarCodigo(' ab12cd ')).toBe('AB12CD');
  });

  test('gera código alfanumérico', () => {
    const codigo = gerarCodigoConvite(8);
    expect(codigo).toHaveLength(8);
    expect(codigo).toMatch(/^[A-Z0-9]+$/);
  });
});

describe('perfis', () => {
  test('Admin Geral pode cadastrar Admin Síndico', () => {
    expect(podeCadastrarPerfil(PERFIS.ADMIN_GERAL, PERFIS.ADMIN_SINDICO)).toBe(true);
  });

  test('Morador não pode cadastrar outro Morador', () => {
    expect(podeCadastrarPerfil(PERFIS.MORADOR, PERFIS.MORADOR)).toBe(false);
  });

  test('são exatamente 6 perfis', () => {
    expect(Object.keys(PERFIS)).toHaveLength(6);
  });
});

describe('redirectPorPerfil', () => {
  test('porteiro vai para portaria', () => {
    expect(redirectPorPerfil(PERFIS.PORTEIRO)).toBe('/portaria');
  });

  test('morador vai para inicio', () => {
    expect(redirectPorPerfil(PERFIS.MORADOR)).toBe('/inicio');
  });
});
