import {
  podeCadastrarPerfil,
  podeGerenciarOcupantes,
  PERFIS,
} from '../constants/perfis.js';
import { MSG_RF07 } from '../constants/occupantMessages.js';
import { calcularIdade, temIdadeMinima, ehMenorDe16 } from '../utils/ageValidation.js';

describe('RF07 — perfis e permissões', () => {
  test('Morador pode cadastrar Convidado', () => {
    expect(podeCadastrarPerfil(PERFIS.MORADOR, PERFIS.CONVIDADO)).toBe(true);
  });

  test('Morador não cadastra outro Morador nem perfis de gestão', () => {
    expect(podeCadastrarPerfil(PERFIS.MORADOR, PERFIS.MORADOR)).toBe(false);
    expect(podeCadastrarPerfil(PERFIS.MORADOR, PERFIS.ADMIN_SINDICO)).toBe(false);
    expect(podeCadastrarPerfil(PERFIS.MORADOR, PERFIS.PORTEIRO)).toBe(false);
  });

  test('Dono Aluguel cadastra o Morador que ocupa o imóvel, e Convidado', () => {
    expect(podeCadastrarPerfil(PERFIS.DONO_ALUGUEL, PERFIS.MORADOR)).toBe(true);
    expect(podeCadastrarPerfil(PERFIS.DONO_ALUGUEL, PERFIS.CONVIDADO)).toBe(true);
    expect(podeCadastrarPerfil(PERFIS.DONO_ALUGUEL, PERFIS.ADMIN_SINDICO)).toBe(false);
  });

  test('Admin Síndico cadastra todos os perfis do condomínio, menos Admin Geral', () => {
    expect(podeCadastrarPerfil(PERFIS.ADMIN_SINDICO, PERFIS.PORTEIRO)).toBe(true);
    expect(podeCadastrarPerfil(PERFIS.ADMIN_SINDICO, PERFIS.MORADOR)).toBe(true);
    expect(podeCadastrarPerfil(PERFIS.ADMIN_SINDICO, PERFIS.DONO_ALUGUEL)).toBe(true);
    expect(podeCadastrarPerfil(PERFIS.ADMIN_SINDICO, PERFIS.ADMIN_GERAL)).toBe(false);
  });

  test('Porteiro e Convidado não cadastram ninguém', () => {
    expect(podeCadastrarPerfil(PERFIS.PORTEIRO, PERFIS.MORADOR)).toBe(false);
    expect(podeCadastrarPerfil(PERFIS.CONVIDADO, PERFIS.CONVIDADO)).toBe(false);
  });

  test('podeGerenciarOcupantes inclui Morador e Dono Aluguel', () => {
    expect(podeGerenciarOcupantes(PERFIS.MORADOR)).toBe(true);
    expect(podeGerenciarOcupantes(PERFIS.DONO_ALUGUEL)).toBe(true);
    expect(podeGerenciarOcupantes(PERFIS.CONVIDADO)).toBe(false);
    expect(podeGerenciarOcupantes(PERFIS.PORTEIRO)).toBe(false);
  });
});

describe('RF07 — validação de idade', () => {
  test('Morador exige idade mínima de 16 anos', () => {
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
  test('mensagens literais', () => {
    expect(MSG_RF07.LESSEE_DUPLICADO).toBe('Esta unidade já possui um morador responsável financeiro.');
    expect(MSG_RF07.OCCUPANT_SUCESSO).toBe('Morador cadastrado com sucesso.');
    expect(MSG_RF07.OCCUPANT_MENOR).toBe(
      'Menores de 16 anos não podem ser cadastrados como Morador. Use o cadastro de Convidado.',
    );
    expect(MSG_RF07.GUEST_SUCESSO).toBe('Convidado cadastrado com sucesso.');
    expect(MSG_RF07.GUEST_SEM_ACESSO).toBe('Convidados não possuem acesso ao sistema.');
    expect(MSG_RF07.TRANSFER_SUCESSO).toBe(
      'Responsabilidade financeira transferida. Seu perfil foi atualizado para Dono Aluguel.',
    );
    expect(MSG_RF07.TRANSFER_SEM_LESSEE).toBe(
      'É necessário ter um morador ativo na unidade para realizar a transferência.',
    );
    expect(MSG_RF07.TRANSFER_SEM_PERMISSAO).toBe(
      'Somente o morador responsável financeiro pode realizar esta transferência.',
    );
    expect(MSG_RF07.TRANSFER_CADASTRE_LESSEE).toBe(
      'Cadastre um morador antes de transferir a responsabilidade financeira.',
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
