export function validarSenha(senha) {
  const erros = [];
  if (!senha || senha.length < 8) {
    erros.push('A senha precisa ter ao menos 8 caracteres');
  }
  if (!senha || !/\d/.test(senha)) {
    erros.push('Inclua ao menos 1 número');
  }
  if (!senha || !/[A-Z]/.test(senha)) {
    erros.push('Inclua ao menos 1 letra maiúscula');
  }
  return erros;
}

export function senhaAtendeRequisitos(senha) {
  return validarSenha(senha).length === 0;
}
