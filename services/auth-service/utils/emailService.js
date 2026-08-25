import nodemailer from 'nodemailer';

const PLACEHOLDERS = [
  'seuemail@gmail.com',
  'sua-app-password-gmail',
  'xxxx',
  'troque',
  'seu-google',
];

function isPlaceholder(valor) {
  if (!valor || !String(valor).trim()) return true;
  const lower = String(valor).toLowerCase();
  return PLACEHOLDERS.some((p) => lower.includes(p));
}

export function emailConfigurado() {
  return !isPlaceholder(process.env.MAIL_USER) && !isPlaceholder(process.env.MAIL_PASS);
}

export function assertEmailConfigurado() {
  if (!emailConfigurado()) {
    throw new Error('Serviço de e-mail não configurado. Defina MAIL_USER e MAIL_PASS no .env.');
  }
}

function createTransporter() {
  assertEmailConfigurado();
  return nodemailer.createTransport({
    service: 'gmail',
    auth: {
      user: process.env.MAIL_USER,
      pass: process.env.MAIL_PASS,
    },
  });
}

function templateEmail({ titulo, corpo, botaoTexto, botaoLink, rodape }) {
  return `
    <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto; color: #1f2937;">
      <div style="padding: 24px 0 16px; border-bottom: 2px solid #4f46e5;">
        <h1 style="margin: 0; font-size: 24px; color: #4f46e5;">Mora</h1>
      </div>
      <div style="padding: 24px 0;">
        <h2 style="margin: 0 0 16px; font-size: 20px;">${titulo}</h2>
        ${corpo}
        ${botaoTexto && botaoLink ? `
        <a href="${botaoLink}" style="
          display: inline-block;
          margin: 20px 0;
          padding: 12px 24px;
          background: #4f46e5;
          color: #fff;
          text-decoration: none;
          border-radius: 6px;
          font-weight: bold;
        ">${botaoTexto}</a>
        ` : ''}
        <p style="color: #666; font-size: 13px; margin-top: 24px;">${rodape}</p>
      </div>
      <div style="border-top: 1px solid #eee; padding-top: 16px; color: #aaa; font-size: 12px;">
        Mora · este e-mail foi enviado automaticamente
      </div>
    </div>
  `;
}

export async function enviarEmailReset(destinatario, token) {
  const link = `${process.env.FRONTEND_URL || 'http://localhost:5173'}/reset-password?token=${token}`;
  const transporter = createTransporter();

  console.log(`[Email] Enviando reset para ${destinatario}`);
  const info = await transporter.sendMail({
    from: process.env.MAIL_FROM || process.env.MAIL_USER,
    to: destinatario,
    subject: 'Recuperação de senha — Mora',
    html: templateEmail({
      titulo: 'Recuperação de senha',
      corpo: `
        <p>Recebemos uma solicitação para redefinir a senha da sua conta.</p>
        <p>Clique no botão abaixo para criar uma nova senha. O link expira em <strong>1 hora</strong>.</p>
      `,
      botaoTexto: 'Redefinir senha',
      botaoLink: link,
      rodape: 'Se você não solicitou isso, pode ignorar este e-mail com segurança.',
    }),
  });
  console.log(`[Email] Reset enviado! MessageId: ${info.messageId}`);
}

export async function enviarEmailConvite(destinatario, codigo, perfil, linkAtivacao) {
  const transporter = createTransporter();

  console.log(`[Email] Enviando convite para ${destinatario}`);
  const info = await transporter.sendMail({
    from: process.env.MAIL_FROM || process.env.MAIL_USER,
    to: destinatario,
    subject: 'Convite de acesso — Mora',
    html: templateEmail({
      titulo: 'Você foi convidado para o Mora',
      corpo: `
        <p>Seu perfil de acesso: <strong>${perfil.replace(/_/g, ' ')}</strong></p>
        <p>Use o código abaixo para ativar sua conta. O código expira em <strong>48 horas</strong>.</p>
        <p style="font-size: 28px; letter-spacing: 4px; font-weight: bold; text-align: center; padding: 16px; background: #f3f4f6; border-radius: 8px;">${codigo}</p>
      `,
      botaoTexto: 'Ativar minha conta',
      botaoLink: linkAtivacao,
      rodape: 'Se você não esperava este convite, pode ignorar este e-mail.',
    }),
  });
  console.log(`[Email] Convite enviado! MessageId: ${info.messageId}`);
}

export const AVISO_EMAIL_FALHOU =
  'Convite criado, mas o e-mail não pôde ser enviado. Verifique a configuração SMTP.';
