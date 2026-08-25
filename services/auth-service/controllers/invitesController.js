import jwt from 'jsonwebtoken';
import { validarConvite, ativarConta } from '../services/inviteService.js';
import { validarSenha } from '../utils/passwordValidation.js';
import { usuarioPublico, validarCpf, validarTelefone } from '../utils/usuarioPublico.js';
import { redirectPorPerfil } from '../utils/redirectPorPerfil.js';

const getJwtSecret = () => process.env.JWT_SECRET;

export const signToken = (userId, perfil, tokenVersion = 0, email = undefined) =>
  jwt.sign({ id: userId, perfil, tokenVersion, ...(email != null && { email }) }, getJwtSecret(), { expiresIn: '7d' });

function validarCamposObrigatorios(body, campos) {
  const erros = {};
  for (const campo of campos) {
    const valor = body[campo];
    if (valor === undefined || valor === null || String(valor).trim() === '') {
      erros[campo] = 'Este campo é obrigatório.';
    }
  }
  return erros;
}

export const validateInvite = async (req, res) => {
  try {
    const { codigo } = req.body;
    if (!codigo || !String(codigo).trim()) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Este campo é obrigatório.',
        erros: { codigo: 'Este campo é obrigatório.' },
      });
    }

    const resultado = await validarConvite(codigo);
    if (!resultado.sucesso) {
      return res.status(400).json(resultado);
    }

    res.json(resultado);
  } catch (err) {
    console.error('Erro validate convite:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao validar código' });
  }
};

export const activateAccount = async (req, res) => {
  try {
    const { codigo, nome, email, telefone, cpf, senha, confirmacaoSenha } = req.body;

    const erros = validarCamposObrigatorios(
      { codigo, nome, email, telefone, cpf, senha, confirmacaoSenha },
      ['codigo', 'nome', 'email', 'telefone', 'cpf', 'senha', 'confirmacaoSenha'],
    );

    if (Object.keys(erros).length > 0) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Este campo é obrigatório.',
        erros,
      });
    }

    if (senha !== confirmacaoSenha) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'As senhas não coincidem.',
        erros: { senha: 'As senhas não coincidem.', confirmacaoSenha: 'As senhas não coincidem.' },
      });
    }

    const errosSenha = validarSenha(senha);
    if (errosSenha.length > 0) {
      return res.status(400).json({
        sucesso: false,
        mensagem: errosSenha[0],
        errosSenha,
      });
    }

    if (!validarCpf(cpf)) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'CPF inválido. Verifique o número informado.',
        erros: { cpf: 'CPF inválido.' },
      });
    }

    if (!validarTelefone(telefone)) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Telefone inválido. Informe um número com DDD (10 ou 11 dígitos).',
        erros: { telefone: 'Telefone inválido.' },
      });
    }

    const resultado = await ativarConta(
      { codigo, nome, email, telefone, cpf, senha },
      signToken,
      usuarioPublico,
      redirectPorPerfil,
    );

    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }

    res.status(201).json(resultado);
  } catch (err) {
    console.error('Erro activate:', err);
    res.status(500).json({ sucesso: false, mensagem: err.message || 'Erro ao ativar conta' });
  }
};
