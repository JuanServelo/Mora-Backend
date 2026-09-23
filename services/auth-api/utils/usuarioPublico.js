export function validarCpf(cpf) {
  const digits = normalizarCpf(cpf);
  if (!digits || digits.length !== 11) return false;
  if (/^(\d)\1{10}$/.test(digits)) return false;

  let sum = 0;
  for (let i = 0; i < 9; i += 1) sum += parseInt(digits[i], 10) * (10 - i);
  let d1 = (sum * 10) % 11;
  if (d1 >= 10) d1 = 0;
  if (d1 !== parseInt(digits[9], 10)) return false;

  sum = 0;
  for (let i = 0; i < 10; i += 1) sum += parseInt(digits[i], 10) * (11 - i);
  let d2 = (sum * 10) % 11;
  if (d2 >= 10) d2 = 0;
  return d2 === parseInt(digits[10], 10);
}

export function validarTelefone(telefone) {
  if (!telefone) return false;
  const digits = String(telefone).replace(/\D/g, '');
  return digits.length === 10 || digits.length === 11;
}

/** Campos do usuário expostos nas APIs (sem senha / tokens). */
export function usuarioPublico(usuario) {
  if (!usuario) return null;
  const perfil = usuario.getPerfilEfetivo?.() ?? usuario.perfil ?? 'MORADOR';
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
    responsavelFinanceiro: usuario.responsavelFinanceiro ?? false,
    semAcessoSistema: usuario.semAcessoSistema ?? false,
    entradaPermitida: usuario.entradaPermitida ?? false,
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
