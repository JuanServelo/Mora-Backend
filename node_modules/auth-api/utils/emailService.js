import nodemailer from 'nodemailer';

export async function enviarEmailReset(destinatario, token) {
  const link = `${process.env.FRONTEND_URL}/reset-password?token=${token}`;

  const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
      user: process.env.MAIL_USER,
      pass: process.env.MAIL_PASS,
    },
  });

  console.log(`[Email] Enviando para ${destinatario} via ${process.env.MAIL_USER}`);
  const info = await transporter.sendMail({
    from: process.env.MAIL_FROM || process.env.MAIL_USER,
    to: destinatario,
    subject: 'Recuperação de senha — Mora',
    html: `
      <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto;">
        <h2>Recuperação de senha</h2>
        <p>Recebemos uma solicitação para redefinir a senha da sua conta.</p>
        <p>Clique no botão abaixo para criar uma nova senha. O link expira em <strong>1 hora</strong>.</p>
        <a href="${link}" style="
          display: inline-block;
          margin: 16px 0;
          padding: 12px 24px;
          background: #4f46e5;
          color: #fff;
          text-decoration: none;
          border-radius: 6px;
          font-weight: bold;
        ">Redefinir senha</a>
        <p style="color: #666; font-size: 13px;">
          Se você não solicitou isso, pode ignorar este email com segurança.
        </p>
        <hr style="margin-top: 32px; border: none; border-top: 1px solid #eee;" />
        <p style="color: #aaa; font-size: 12px;">Mora · este email foi enviado automaticamente</p>
      </div>
    `,
  });
  console.log(`[Email] Enviado! MessageId: ${info.messageId}`);
}
