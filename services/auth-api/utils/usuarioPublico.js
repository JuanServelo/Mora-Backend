/** Campos do usuário expostos nas APIs (sem senha / tokens). */
export function usuarioPublico(usuario) {
  if (!usuario) return null;
  const perfil = usuario.getPerfilEfetivo?.() ?? usuario.perfil ?? 'RESIDENT_OWNER';
  return {
    id: usuario.id,
    nome: usuario.nome,
    email: usuario.email,
    telefone: usuario.telefone ?? null,
    cpf: usuario.cpf ?? null,
    fotoUrl: usuario.fotoUrl ?? null,
    provider: usuario.provider,
    perfil,
    status: usuario.status,
    condominioId: usuario.condominioId ?? null,
    unidadeId: usuario.unidadeId ?? null,
    bloco: usuario.bloco ?? null,
    apartamento: usuario.apartamento ?? null,
    vaga: usuario.vaga ?? null,
    responsavelFinanceiro: usuario.responsavelFinanceiro ?? false,
    semAcessoSistema: usuario.semAcessoSistema ?? false,
    dataNascimento: usuario.dataNascimento ?? null,
    ...(usuario.createdAt && { createdAt: usuario.createdAt }),
    ...(usuario.activatedAt && { activatedAt: usuario.activatedAt }),
  };
}

/** Normaliza string opcional: vazio vira null para o banco. */
export function normalizarVinculo(valor) {
  if (valor === undefined) return undefined;
  if (valor === null) return null;
  const s = String(valor).trim();
  return s === '' ? null : s;
}

export function normalizarCpf(cpf) {
  if (!cpf) return null;
  return String(cpf).replace(/\D/g, '');
}

export function formatarCpf(cpf) {
  const digits = normalizarCpf(cpf);
  if (!digits || digits.length !== 11) return cpf;
  return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`;
}
