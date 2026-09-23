import jwt from 'jsonwebtoken';
import { PERFIS, STATUS_USUARIO } from '../../constants/perfis.js';

/**
 * Um usuário com a superfície que o `authMiddleware` e as rotas consultam.
 *
 * Simular o model, em vez de subir Postgres, é o que permite os testes de
 * autorização rodarem em toda execução — inclusive em CI sem serviço nenhum de
 * pé. O que está sendo verificado aqui é a decisão de acesso, e ela acontece
 * antes de qualquer consulta: quem decide é o perfil no token, não o dado.
 *
 * O limite é conhecido e deliberado: isto **não** verifica isolamento por
 * condomínio nem regra de negócio, que dependem do que está gravado. Esses
 * casos ficam nos testes de integração, contra a stack real.
 */
export function usuarioFalso({
  id = 1,
  perfil = PERFIS.MORADOR,
  condominioId = 'cond-teste',
  unidadeId = null,
  responsavelFinanceiro = false,
  status = STATUS_USUARIO.ACTIVE,
  tokenVersion = 0,
} = {}) {
  return {
    id,
    nome: 'Usuário de Teste',
    email: 'teste@mora.local',
    perfil,
    condominioId,
    unidadeId,
    responsavelFinanceiro,
    status,
    tokenVersion,
    getPerfilEfetivo: () => perfil,
    // Rotas que escrevem chamam estes; sem eles o teste falharia por
    // `is not a function` e leríamos isso como falha de autorização.
    save: async () => {},
    update: async () => {},
    revogarTokens: async () => {},
    toJSON() {
      return { ...this };
    },
  };
}

/** Token válido para o usuário dado — mesmas claims que o serviço emite. */
export function tokenPara(usuario) {
  return jwt.sign(
    {
      id: usuario.id,
      perfil: usuario.perfil,
      tokenVersion: usuario.tokenVersion ?? 0,
      email: usuario.email,
      ...(usuario.condominioId != null && { condominioId: usuario.condominioId }),
      ...(usuario.unidadeId != null && { unidadeId: usuario.unidadeId }),
      nome: usuario.nome,
    },
    process.env.JWT_SECRET,
    { expiresIn: '1h' },
  );
}

/** O par pronto: usuário simulado e o token que o representa. */
export function comoPerfil(perfil, extras = {}) {
  const usuario = usuarioFalso({ perfil, ...extras });
  return { usuario, token: tokenPara(usuario) };
}
