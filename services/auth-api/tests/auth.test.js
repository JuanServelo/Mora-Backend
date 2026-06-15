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
  test('CPM pode cadastrar Administrator', () => {
    expect(podeCadastrarPerfil(PERFIS.CONTRACTING_PROPERTY_MANAGER, PERFIS.ADMINISTRATOR)).toBe(true);
  });

  test('CPM não pode cadastrar Síndico Contratante', () => {
    expect(podeCadastrarPerfil(PERFIS.CONTRACTING_PROPERTY_MANAGER, PERFIS.CONTRACTING_SYNDIC)).toBe(false);
  });

  test('Lessee não pode cadastrar Lessee', () => {
    expect(podeCadastrarPerfil(PERFIS.LESSEE, PERFIS.LESSEE)).toBe(false);
  });
});

describe('redirectPorPerfil', () => {
  test('porteiro vai para portaria', () => {
    expect(redirectPorPerfil(PERFIS.DOORMAN)).toBe('/portaria');
  });

  test('morador vai para inicio', () => {
    expect(redirectPorPerfil(PERFIS.RESIDENT_OWNER)).toBe('/inicio');
  });
});
