const PLACEHOLDERS = [
  'seu-google',
  'your-google',
  'client-id',
  'client-secret',
  'xxxx',
  'troque',
  'example',
];

function isPlaceholder(valor) {
  if (!valor || !String(valor).trim()) return true;
  const lower = String(valor).toLowerCase();
  return PLACEHOLDERS.some((p) => lower.includes(p));
}

export function googleOAuthConfigurado() {
  return !isPlaceholder(process.env.GOOGLE_CLIENT_ID)
    && !isPlaceholder(process.env.GOOGLE_CLIENT_SECRET);
}
