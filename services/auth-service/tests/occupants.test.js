import {
  podeCadastrarPerfil,
  podeGerenciarOcupantes,
  PERFIS,
} from '../constants/perfis.js';
import { MSG_RF07 } from '../constants/occupantMessages.js';
import { calcularIdade, temIdadeMinima, ehMenorDe16 } from '../utils/ageValidation.js';

describe('RF07 — perfis e permissões', () => {
  test('Resident Owner pode cadastrar Lessee, Occupant e Guest', () => {
    expect(podeCadastrarPerfil(PERFIS.RESIDENT_OWNER, PERFIS.LESSEE)).toBe(true);
    expect(podeCadastrarPerfil(PERFIS.RESIDENT_OWNER, PERFIS.OCCUPANT)).toBe(true);
    expect(podeCadastrarPerfil(PERFIS.RESIDENT_OWNER, PERFIS.GUEST)).toBe(true);
  });

  test('Absent Owner pode cadastrar apenas Lessee', () => {
    expect(podeCadastrarPerfil(PERFIS.ABSENT_OWNER, PERFIS.LESSEE)).toBe(true);
    expect(podeCadastrarPerfil(PERFIS.ABSENT_OWNER, PERFIS.OCCUPANT)).toBe(false);
    expect(podeCadastrarPerfil(PERFIS.ABSENT_OWNER, PERFIS.GUEST)).toBe(false);
  });

  test('Lessee pode cadastrar Occupant e Guest', () => {
    expect(podeCadastrarPerfil(PERFIS.LESSEE, PERFIS.OCCUPANT)).toBe(true);
    expect(podeCadastrarPerfil(PERFIS.LESSEE, PERFIS.GUEST)).toBe(true);
    expect(podeCadastrarPerfil(PERFIS.LESSEE, PERFIS.LESSEE)).toBe(false);
  });

  test('podeGerenciarOcupantes inclui RO, AO e Lessee', () => {
    expect(podeGerenciarOcupantes(PERFIS.RESIDENT_OWNER)).toBe(true);
    expect(podeGerenciarOcupantes(PERFIS.ABSENT_OWNER)).toBe(true);
    expect(podeGerenciarOcupantes(PERFIS.LESSEE)).toBe(true);
    expect(podeGerenciarOcupantes(PERFIS.OCCUPANT)).toBe(false);
    expect(podeGerenciarOcupantes(PERFIS.ADMINISTRATOR)).toBe(false);
  });
});

describe('RF07 — validação de idade', () => {
  test('Occupant exige idade mínima de 16 anos', () => {
    const hoje = new Date();
    const menor = new Date(hoje.getFullYear() - 15, hoje.getMonth(), hoje.getDate());
    const maior = new Date(hoje.getFullYear() - 20, hoje.getMonth(), hoje.getDate());

    expect(temIdadeMinima(menor.toISOString().slice(0, 10), 16)).toBe(false);
    expect(temIdadeMinima(maior.toISOString().slice(0, 10), 16)).toBe(true);
    expect(ehMenorDe16(menor.toISOString().slice(0, 10))).toBe(true);
  });

  test('calcularIdade retorna null para data inválida', () => {
    expect(calcularIdade('invalido')).toBeNull();
    expect(calcularIdade(null)).toBeNull();
  });
});

describe('RF07 — mensagens de aceitação', () => {
  test('mensagens literais da documentação', () => {
    expect(MSG_RF07.LESSEE_DUPLICADO).toBe('Você já possui um Lessee vinculado a esta unidade.');
    expect(MSG_RF07.OCCUPANT_SUCESSO).toBe('Occupant cadastrado com sucesso.');
    expect(MSG_RF07.OCCUPANT_MENOR).toBe(
      'Menores de 16 anos não podem ser cadastrados como Occupant. Use o cadastro de Guest.',
    );
    expect(MSG_RF07.GUEST_SUCESSO).toBe('Guest cadastrado com sucesso.');
    expect(MSG_RF07.GUEST_SEM_ACESSO).toBe('Guests não possuem acesso ao sistema.');
    expect(MSG_RF07.TRANSFER_SUCESSO).toBe(
      'Responsabilidade financeira transferida. Seu perfil foi atualizado para Absent Owner.',
    );
    expect(MSG_RF07.TRANSFER_SEM_LESSEE).toBe(
      'É necessário ter um Lessee ativo na unidade para realizar a transferência.',
    );
    expect(MSG_RF07.TRANSFER_SEM_PERMISSAO).toBe(
      'Somente o Resident Owner responsável financeiro pode realizar esta transferência.',
    );
    expect(MSG_RF07.TRANSFER_CADASTRE_LESSEE).toBe(
      'Cadastre um Lessee antes de transferir a responsabilidade financeira.',
    );
    expect(MSG_RF07.VINCULO_REMOVIDO).toBe('Vínculo removido com sucesso.');
    expect(MSG_RF07.NENHUM_OCUPANTE).toBe('Nenhum ocupante vinculado a esta unidade.');
    expect(MSG_RF07.SEM_PERMISSAO_CADASTRO).toBe(
      'Você não tem permissão para cadastrar dependentes nesta unidade.',
    );
    expect(MSG_RF07.CAMPO_OBRIGATORIO).toBe('Este campo é obrigatório.');
    expect(MSG_RF07.REMOVER_FINANCEIRO).toBe(
      'Transfira a responsabilidade financeira antes de remover este vínculo.',
    );
  });
});
