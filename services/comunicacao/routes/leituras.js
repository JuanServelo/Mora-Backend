import express from 'express';
import path from 'path';
import crypto from 'crypto';
import sharp from 'sharp';
import { autenticar, exigirPerfis, PERFIS_COMUNICAVEIS, PERFIS_GESTAO } from '../middleware/auth.js';
import { resolverEscopo, exigirEscrita } from '../middleware/escopo.js';
import { upload, DIRETORIO_AVISOS, MIMES_ACEITOS, LIMITE_BYTES } from '../config/upload.js';
import * as servico from '../services/leiturasService.js';

const router = express.Router();

router.use(autenticar, exigirPerfis(...PERFIS_COMUNICAVEIS), resolverEscopo);

/** Avisos ativos do condomínio, marcados com o que este usuário já leu. */
router.get('/avisos', async (req, res) => {
  const resultado = await servico.meusAvisos(req.escopo);
  if (!resultado.sucesso) return res.status(resultado.status).json(resultado);
  res.json(resultado);
});

/**
 * Confirma a leitura.
 *
 * O `exigirEscrita` deixa o Admin Geral de fora de propósito: ele não é
 * destinatário de aviso de condomínio nenhum, e uma confirmação dele
 * distorceria o relatório do síndico.
 */
router.post('/avisos/:avisoId/leitura', exigirEscrita, async (req, res) => {
  const resultado = await servico.confirmar(req.escopo, req.params.avisoId);
  if (!resultado.sucesso) return res.status(resultado.status).json(resultado);
  res.status(201).json(resultado);
});

/** Panorama da gestão: avisos ativos e quantos confirmaram cada um. */
router.get('/avisos/leituras', exigirPerfis(...PERFIS_GESTAO), async (req, res) => {
  const resultado = await servico.panorama(req.escopo);
  if (!resultado.sucesso) return res.status(resultado.status).json(resultado);
  res.json(resultado);
});

/**
 * Recebe a imagem de um aviso e devolve a URL para gravar junto dele.
 *
 * O arquivo fica aqui; a referência (`imagemUrl`) vai para o aviso, que mora no
 * portaria. O portaria não tem nenhuma infraestrutura de upload — nem uma linha
 * de `MultipartFile` —, e construí-la em Java do zero custaria bem mais que
 * reusar aqui o padrão que o auth-api já provou com as fotos de perfil.
 *
 * O upload é separado de salvar o aviso de propósito: quem cria escolhe a
 * imagem antes de existir um aviso a que ela pertença.
 */
router.post(
  '/avisos/imagem',
  exigirPerfis(...PERFIS_GESTAO),
  exigirEscrita,
  upload.single('imagem'),
  async (req, res) => {
    if (!req.file) {
      return res.status(400).json({ sucesso: false, mensagem: 'Nenhuma imagem enviada.' });
    }

    if (!MIMES_ACEITOS.includes(req.file.mimetype?.toLowerCase())) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Formato não aceito. Envie JPG, PNG ou WebP.',
      });
    }

    // Nome sorteado, extensão fixa: o nome que o cliente mandou não chega ao
    // disco. Caminho enviado como nome de arquivo é a forma clássica de
    // escrever fora do diretório pretendido.
    const nome = `aviso-${Date.now()}-${crypto.randomBytes(6).toString('hex')}.jpg`;

    try {
      await sharp(req.file.buffer)
        // Reprocessar é o que garante que o que foi gravado é imagem de verdade:
        // o sharp recusa o que não souber decodificar, e o arquivo que sai é
        // gerado por ele, não os bytes recebidos.
        .rotate()
        .resize(1280, 1280, { fit: 'inside', withoutEnlargement: true })
        .jpeg({ quality: 82 })
        .toFile(path.join(DIRETORIO_AVISOS, nome));
    } catch {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Não foi possível ler esta imagem. Tente outro arquivo.',
      });
    }

    res.status(201).json({ sucesso: true, url: `/uploads/avisos/${nome}` });
  },
);

/** Quem leu e quem falta ler um aviso. */
router.get('/avisos/:avisoId/leituras', exigirPerfis(...PERFIS_GESTAO), async (req, res) => {
  const resultado = await servico.relatorio(req.escopo, req.params.avisoId, req.authorization);
  if (!resultado.sucesso) return res.status(resultado.status).json(resultado);
  res.json(resultado);
});

/**
 * Tratador de erro do router — no fim, e não no meio.
 *
 * Em Express, middleware de erro só alcança o que foi registrado ANTES dele.
 * Posto entre as rotas, deixaria de fora tudo que viesse depois.
 */
router.use((err, _req, res, next) => {
  if (err?.code === 'LIMIT_FILE_SIZE') {
    return res.status(413).json({
      sucesso: false,
      mensagem: `A imagem passa de ${Math.round(LIMITE_BYTES / 1024 / 1024)} MB.`,
    });
  }
  next(err);
});

export default router;
