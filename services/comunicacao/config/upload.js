import multer from 'multer';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

/**
 * Onde as imagens de aviso são gravadas.
 *
 * Precisa de volume no compose. Sem ele os arquivos ficam na camada gravável do
 * container e somem no primeiro `up --build`, deixando a coluna `imagem_url` do
 * aviso apontando para 404 — exatamente o que aconteceu com as fotos de perfil
 * do auth-api até ganharem `auth_uploads`.
 */
export const DIRETORIO_AVISOS =
  process.env.STORAGE_PATH || path.join(__dirname, '..', 'uploads', 'avisos');

if (!fs.existsSync(DIRETORIO_AVISOS)) {
  fs.mkdirSync(DIRETORIO_AVISOS, { recursive: true });
}

export const LIMITE_BYTES = 5 * 1024 * 1024;

/**
 * Recebe o arquivo em memória, não em disco.
 *
 * O que vai para o disco é a imagem já reprocessada pelo sharp — nunca os bytes
 * que chegaram. Gravar o original primeiro deixaria no servidor um arquivo que
 * ninguém validou, com a extensão que o cliente escolheu.
 */
export const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: LIMITE_BYTES, files: 1 },
});

/** Formatos aceitos. Lista fechada: o resto o sharp nem tenta decodificar. */
export const MIMES_ACEITOS = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
