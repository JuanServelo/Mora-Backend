/**
 * Regras de negócio compartilhadas entre model, rotas e middleware.
 *
 * As regras de força de senha vivem em utils/passwordValidation.js — este
 * arquivo cuida das janelas de validade dos tokens.
 */

/** Janela de validade do token de recuperação de senha. */
export const RESET_TOKEN_TTL_MS = 60 * 60 * 1000; // 1 hora

/**
 * Código de troca do OAuth: curto, pois é consumido imediatamente pelo
 * frontend logo após o redirect do Google.
 */
export const OAUTH_CODE_TTL_MS = 2 * 60 * 1000; // 2 minutos

/** Validade do JWT de acesso. */
export const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '7d';

export const ehProducao = () => process.env.NODE_ENV === 'production';
