import passport from 'passport';
import { Strategy as GoogleStrategy } from 'passport-google-oauth20';
import User from '../models/User.js';
import { STATUS_USUARIO } from '../constants/perfis.js';
import { googleOAuthConfigurado } from '../utils/oauthConfig.js';

/**
 * Resolve o usuário do perfil Google, nesta ordem:
 *   1. já vinculado por googleId
 *   2. conta local ativa com o mesmo email → vincula
 *   3. recusa
 *
 * O login social NÃO cria conta: o cadastro no Mora é sempre por convite
 * (RF-3), então uma conta sem convite prévio não deve nascer por aqui.
 */
const resolverUsuarioGoogle = async (profile) => {
  const porGoogleId = await User.findOne({ where: { googleId: profile.id } });
  if (porGoogleId) return porGoogleId;

  const email = profile.emails?.[0]?.value;
  if (!email) throw new Error('Email não fornecido pelo Google');

  const porEmail = await User.findOne({ where: { email: email.toLowerCase().trim() } });
  if (!porEmail) {
    throw new Error('Conta não encontrada. Ative via convite.');
  }

  if (porEmail.status === STATUS_USUARIO.INACTIVE) {
    throw new Error('Conta desativada');
  }
  if (porEmail.status === STATUS_USUARIO.PENDING_ACTIVATION) {
    throw new Error('Cadastro pendente');
  }

  porEmail.googleId = profile.id;
  porEmail.provider = 'google';
  await porEmail.save({ validate: false });
  return porEmail;
};

export default function setupPassport() {
  if (!googleOAuthConfigurado()) {
    console.warn('GOOGLE_CLIENT_ID/SECRET não configurados — OAuth Google desativado');
    return;
  }

  passport.use(new GoogleStrategy(
    {
      clientID: process.env.GOOGLE_CLIENT_ID,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET,
      callbackURL: process.env.GOOGLE_CALLBACK_URL || 'http://localhost:3001/api/auth/google/callback',
      // Parâmetro `state` assinado na sessão: barra CSRF no fluxo OAuth.
      state: true,
    },
    async (accessToken, refreshToken, profile, done) => {
      try {
        done(null, await resolverUsuarioGoogle(profile));
      } catch (err) {
        done(err, null);
      }
    },
  ));

  console.log('Google OAuth configurado');
}
