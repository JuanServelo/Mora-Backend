import express from 'express';
import authMiddleware from '../middleware/auth.js';
import { PERFIS } from '../constants/perfis.js';
import {
  estatisticasCondominios,
  estatisticasUsuarios,
  estatisticasOcorrencias,
  estatisticasDoCondominio,
  condominiosDoFiltro,
} from '../services/estatisticasService.js';

const router = express.Router();

router.use(authMiddleware);

/** Números da plataforma inteira: só quem opera a plataforma vê. */
function adminGeralMiddleware(req, res, next) {
  if (req.userPerfil !== PERFIS.ADMIN_GERAL) {
    return res.status(403).json({
      sucesso: false,
      mensagem: 'Apenas o Admin Geral pode consultar dados da plataforma.',
    });
  }
  next();
}

/**
 * Números da plataforma, com filtros aplicados aqui — não no frontend.
 *
 * `meses` recorta a janela das séries; `status` restringe a quais condomínios
 * tudo se refere, inclusive as contagens de usuário e ocorrência. Filtrar aqui
 * evita mandar a base inteira para a tela peneirar, que era o que aconteceria
 * com dezenas de clientes.
 */
router.get('/plataforma', adminGeralMiddleware, async (req, res) => {
  try {
    const { meses, status } = req.query;
    // Resolvido uma vez e reaproveitado: as três agregações precisam do mesmo
    // recorte, e repetir a consulta em cada uma seria desperdício.
    const condominioIds = await condominiosDoFiltro(status);

    const [condominios, usuarios, ocorrencias] = await Promise.all([
      estatisticasCondominios({ meses, status }),
      estatisticasUsuarios({ condominioIds }),
      estatisticasOcorrencias({ condominioIds }),
    ]);
    res.json({
      sucesso: true,
      filtros: { meses: condominios.meses, status: status || 'todos' },
      condominios,
      usuarios,
      ocorrencias,
    });
  } catch (err) {
    console.error('Erro ao montar estatísticas da plataforma:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao carregar estatísticas' });
  }
});

/**
 * Resumo de um condomínio. Aberto ao Admin Geral e a quem pertence ao próprio
 * condomínio — um síndico não precisa ver os números de outro cliente.
 */
router.get('/condominios/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const ehAdminGeral = req.userPerfil === PERFIS.ADMIN_GERAL;

    if (!ehAdminGeral && req.user.condominioId !== id) {
      return res.status(403).json({ sucesso: false, mensagem: 'Acesso negado' });
    }

    res.json({ sucesso: true, resumo: await estatisticasDoCondominio(id) });
  } catch (err) {
    console.error('Erro ao montar resumo do condomínio:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao carregar resumo' });
  }
});

export default router;
