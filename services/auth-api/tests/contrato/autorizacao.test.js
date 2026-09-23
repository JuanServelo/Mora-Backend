import { vi, afterEach } from 'vitest';
import request from 'supertest';
import app from '../../app.js';
import User from '../../models/User.js';
import { INVENTARIO, rotasComBarreiraDePerfil, separar, comParametros } from './inventario.js';
import { comoPerfil } from '../helpers/usuarioFalso.js';
import { PERFIS, STATUS_USUARIO } from '../../constants/perfis.js';

/**
 * A barreira de perfil de cada rota, exercitada perfil a perfil.
 *
 * Roda sem Postgres porque o `findByPk` do model é espionado — e não porque o
 * model inteiro foi substituído. A diferença custou uma hora: um objeto no
 * lugar do model derruba `models/index.js`, que chama `User.hasMany(...)` na
 * importação e recusa qualquer coisa que não seja uma subclasse de verdade.
 * Espionar um método deixa o Sequelize intacto e não toca no banco — definir
 * model não abre conexão.
 *
 * **O que isto não cobre, e não tem como cobrir aqui:** as rotas que recortam
 * o resultado pelo condomínio ou pela unidade de quem pediu. Essa decisão mora
 * no handler e depende do que está gravado; sem banco, ela responde 500. Por
 * isso `nega: []` significa "a decisão não é de perfil", nunca "liberado".
 */
const comoUsuario = (usuario) => vi.spyOn(User, 'findByPk').mockResolvedValue(usuario);

/**
 * Os perfis exercitados contra cada rota.
 *
 * `TERCEIRO` e `DONO_ALUGUEL` ficam de fora aqui por um motivo: nenhum dos dois
 * aparece em barreira de middleware — `TERCEIRO` não acessa o sistema, e
 * `DONO_ALUGUEL` acompanha `MORADOR` em toda regra de perfil. Incluí-los
 * dobraria o tempo da suíte sem afirmar nada que já não esteja afirmado.
 */
const PERFIS_SONDADOS = [
  PERFIS.ADMIN_GERAL,
  PERFIS.ADMIN_SINDICO,
  PERFIS.PORTEIRO,
  PERFIS.MORADOR,
  PERFIS.CONVIDADO,
];

afterEach(() => {
  vi.restoreAllMocks();
});

describe('barreira de perfil', () => {
  const comBarreira = rotasComBarreiraDePerfil();

  test('há rotas com barreira de perfil para testar', () => {
    expect(comBarreira.length).toBeGreaterThan(0);
  });

  test.each(comBarreira)('%s recusa os perfis declarados', async (chave, negados) => {
    const { metodo, caminho } = separar(chave);

    for (const perfil of negados) {
      const { usuario, token } = comoPerfil(perfil, { unidadeId: 'uni-1' });
      comoUsuario(usuario);

      const r = await request(app)[metodo.toLowerCase()](comParametros(caminho))
        .set('Authorization', `Bearer ${token}`)
        .send({});

      expect(
        r.status,
        `${chave} deveria recusar ${perfil} com 403, e respondeu ${r.status}`,
      ).toBe(403);

      vi.restoreAllMocks();
    }
  });

  test.each(comBarreira)('%s deixa passar quem não está na lista', async (chave, negados) => {
    const { metodo, caminho } = separar(chave);
    const permitidos = PERFIS_SONDADOS.filter((p) => !negados.includes(p));

    for (const perfil of permitidos) {
      const { usuario, token } = comoPerfil(perfil, { unidadeId: 'uni-1' });
      comoUsuario(usuario);

      const r = await request(app)[metodo.toLowerCase()](comParametros(caminho))
        .set('Authorization', `Bearer ${token}`)
        .send({});

      // Passar da barreira é o que se afirma — não que a chamada dê certo. Sem
      // banco, a maioria responde 500, e isso é o esperado: o handler foi
      // alcançado. O que não pode acontecer é 403.
      expect(
        r.status,
        `${chave} não deveria recusar ${perfil}, e respondeu 403`,
      ).not.toBe(403);

      vi.restoreAllMocks();
    }
  });
});

describe('conta desativada não entra, com token válido', () => {
  // O token continua assinado e no prazo: quem o revoga é o estado da conta.
  // Sem esta checagem, desativar um usuário não o expulsaria até o token vencer.
  const protegidas = Object.entries(INVENTARIO)
    .filter(([, v]) => v.acesso === 'token')
    .map(([k]) => k);

  test.each(protegidas)('%s → 401 para conta INACTIVE', async (chave) => {
    const { metodo, caminho } = separar(chave);
    const { usuario, token } = comoPerfil(PERFIS.ADMIN_GERAL, { status: STATUS_USUARIO.INACTIVE });
    comoUsuario(usuario);

    const r = await request(app)[metodo.toLowerCase()](comParametros(caminho))
      .set('Authorization', `Bearer ${token}`)
      .send({});

    expect(r.status).toBe(401);
  });
});

describe('token de versão antiga não entra', () => {
  // `revogarTokens()` incrementa `tokenVersion` no usuário. Um token emitido
  // antes disso continua válido para o `jwt.verify` — é a comparação de versão
  // que o derruba. É o que faz o logout encerrar a sessão de verdade, em todo
  // lugar, e não só apagar o token do navegador que pediu.
  const protegidas = Object.entries(INVENTARIO)
    .filter(([, v]) => v.acesso === 'token')
    .map(([k]) => k);

  test.each(protegidas)('%s → 401 quando tokenVersion ficou para trás', async (chave) => {
    const { metodo, caminho } = separar(chave);
    const { usuario, token } = comoPerfil(PERFIS.ADMIN_GERAL);
    comoUsuario({ ...usuario, tokenVersion: usuario.tokenVersion + 1 });

    const r = await request(app)[metodo.toLowerCase()](comParametros(caminho))
      .set('Authorization', `Bearer ${token}`)
      .send({});

    expect(r.status).toBe(401);
  });
});
