/** Campos do usuário expostos nas APIs (sem senha / tokens). */
export function usuarioPublico(usuario) {
  if (!usuario) return null;
  return {
    id: usuario.id,
    nome: usuario.nome,
    email: usuario.email,
    provider: usuario.provider,
    role: usuario.role,
    bloco: usuario.bloco ?? null,
    apartamento: usuario.apartamento ?? null,
    vaga: usuario.vaga ?? null,
    ...(usuario.createdAt && { createdAt: usuario.createdAt }),
  };
}

/** Normaliza string opcional: vazio vira null para o banco. */
export function normalizarVinculo(valor) {
  if (valor === undefined) return undefined;
  if (valor === null) return null;
  const s = String(valor).trim();
  return s === "" ? null : s;
}
