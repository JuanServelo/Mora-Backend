import { validarSenha, senhaAtendeRequisitos } from '../utils/passwordValidation.js';
import { gerarCodigoConvite, normalizarCodigo } from '../utils/inviteCode.js';
import {
  podeCadastrarPerfil,
  PERFIS,
  PERFIS_SEM_ACESSO,
  PERFIS_EXIGEM_UNIDADE,
} from '../constants/perfis.js';
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

  // Contar não bastava: trocar um perfil por outro mantinha o número e passava
  // despercebido. Afirmar o conjunto faz qualquer entrada ou saída aparecer
  // com nome — foi assim que `TERCEIRO` apareceu, vindo da branch de ajustes.
  test('o conjunto de perfis é exatamente este', () => {
    expect(Object.keys(PERFIS)).toEqual([
      'ADMIN_GERAL',
      'ADMIN_SINDICO',
      'PORTEIRO',
      'MORADOR',
      'DONO_ALUGUEL',
      'CONVIDADO',
      'TERCEIRO',
    ]);
  });

  test('perfis sem acesso ao sistema não entram na camada de unidade', () => {
    // `TERCEIRO` é de condomínio, não de unidade: um terceirizado atende o
    // prédio, não um apartamento. Exigir `unidadeId` dele deixaria o cadastro
    // impossível de concluir.
    expect(PERFIS_SEM_ACESSO).toContain(PERFIS.TERCEIRO);
    expect(PERFIS_EXIGEM_UNIDADE).not.toContain(PERFIS.TERCEIRO);
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
