export function normalizarCep(cep) {
  if (cep == null || cep === '') return null;
  const digits = String(cep).replace(/\D/g, '').slice(0, 8);
  if (digits.length !== 8) return null;
  return `${digits.slice(0, 5)}-${digits.slice(5)}`;
}

export function montarEndereco({
  logradouro,
  numero,
  complemento,
  bairro,
  cidade,
  uf,
  cep,
} = {}) {
  const partes = [];

  if (logradouro) {
    let linha = logradouro;
    if (numero) linha += `, ${numero}`;
    if (complemento) linha += ` — ${complemento}`;
    partes.push(linha);
  }

  if (bairro) partes.push(bairro);

  const cidadeUf = [cidade, uf].filter(Boolean).join(' — ');
  if (cidadeUf) partes.push(cidadeUf);

  const cepFmt = normalizarCep(cep);
  if (cepFmt) partes.push(`CEP ${cepFmt}`);

  return partes.length > 0 ? partes.join(' · ') : null;
}

export function extrairDadosEndereco(body = {}) {
  const cep = body.cep !== undefined ? normalizarCep(body.cep) : undefined;
  const campos = ['logradouro', 'numero', 'complemento', 'bairro', 'cidade', 'uf'];
  const dados = {};

  if (cep !== undefined) dados.cep = cep;
  for (const campo of campos) {
    if (body[campo] !== undefined) {
      const valor = typeof body[campo] === 'string' ? body[campo].trim() : body[campo];
      dados[campo] = valor || null;
    }
  }

  return dados;
}

export function aplicarEnderecoCondominio(cond, body) {
  const dados = extrairDadosEndereco(body);
  Object.assign(cond, dados);
  cond.endereco = montarEndereco(cond);
}
