export function calcularIdade(dataNascimento) {
  if (!dataNascimento) return null;
  const nasc = new Date(dataNascimento);
  if (Number.isNaN(nasc.getTime())) return null;
  const hoje = new Date();
  let idade = hoje.getFullYear() - nasc.getFullYear();
  const mes = hoje.getMonth() - nasc.getMonth();
  if (mes < 0 || (mes === 0 && hoje.getDate() < nasc.getDate())) {
    idade -= 1;
  }
  return idade;
}

export function temIdadeMinima(dataNascimento, minimo = 16) {
  const idade = calcularIdade(dataNascimento);
  if (idade === null) return false;
  return idade >= minimo;
}

export function ehMenorDe16(dataNascimento) {
  const idade = calcularIdade(dataNascimento);
  if (idade === null) return false;
  return idade < 16;
}
