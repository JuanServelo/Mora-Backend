"""Lembrete preventivo: o terceiro uso do LLM, e o que fecha o fluxo.

    previsão (quem deve atrasar)  →  contrato (quem paga)  →  LEMBRETE (o que dizer)

Prever o risco não muda nada sozinho. O que muda é avisar a pessoa certa antes do
vencimento, num tom que ela leia como cortesia e não como cobrança. O Gemini
escreve; o código decide se o texto pode sair.

**O lembrete nunca revela a previsão.** O destinatário não sabe que foi
classificado, e não deve saber por esta mensagem: "nosso sistema calculou 93% de
chance de você atrasar" é constrangedor, abre discussão sobre o modelo e expõe
um dado interno. Por isso a entrada do LLM não leva score, fatores nem
histórico — só o que um lembrete comum diria — e `verificar()` recusa o texto se
algum desses assuntos aparecer mesmo assim. Recusado, sai o modelo fixo abaixo.

O envio ao Mora usa a rota interna do comunicacao-service, a mesma do
financeiro: o lembrete cai no sino do morador. Como os dados deste serviço são
sintéticos, o destinatário real da demonstração vem de configuração
(`IA_DEMO_USUARIO_ID`), e a resposta diz isso.
"""

from __future__ import annotations

import os
import pathlib
import re
import unicodedata
from datetime import date

import httpx

import gemini

INSTRUCAO = """\
Você escreve lembretes de pagamento da taxa condominial para moradores de um
condomínio. Escreva em português do Brasil, em tom cordial e respeitoso.

Regras obrigatórias:
- Cite a data de vencimento exatamente como recebida (DD/MM/AAAA).
- Trate a pessoa pelo primeiro nome.
- Diga que o boleto e o PIX estão disponíveis no aplicativo Mora.
- Termine oferecendo ajuda da administração caso haja alguma dificuldade.
- NÃO mencione risco, probabilidade, previsão, análise, sistema, inteligência
  artificial, histórico, atrasos anteriores, multas, juros, dívida ou
  inadimplência. NÃO use percentuais.
- Título com até 60 caracteres; mensagem com até 400 caracteres.
"""

SCHEMA = {
    "type": "OBJECT",
    "properties": {
        "titulo": {"type": "STRING"},
        "mensagem": {"type": "STRING"},
    },
    "required": ["titulo", "mensagem"],
    "propertyOrdering": ["titulo", "mensagem"],
}

# Comparados sem acento e em minúsculas. Raiz da palavra, para pegar as
# variações ("atrasou", "atrasos", "inadimplente").
PROIBIDOS = (
    "risco", "probabil", "previs", "previst", "analise", "algoritmo", "sistema",
    "inteligencia", "modelo", "historico", "atras", "multa", "juros", "divida",
    "inadimpl", "score", "classific",
)

TITULO_MAX = 60
MENSAGEM_MAX = 400


def _sem_acento(texto: str) -> str:
    return "".join(
        c for c in unicodedata.normalize("NFD", texto.lower()) if unicodedata.category(c) != "Mn"
    )


def data_br(iso: str) -> str:
    return date.fromisoformat(str(iso)[:10]).strftime("%d/%m/%Y")


def primeiro_nome(nome: str | None) -> str:
    return (nome or "").strip().split(" ")[0].title() or "morador(a)"


def verificar(titulo: str, mensagem: str, vencimento_br: str, nome: str | None = None) -> dict:
    """O texto pode sair? Lista cada problema em vez de só dizer não."""
    problemas = []
    # Lembrete com o nome errado é pior que nenhum. Pega também a resposta
    # gravada de outra pessoa reaproveitada no modo offline.
    if nome and _sem_acento(primeiro_nome(nome)) not in _sem_acento(mensagem):
        problemas.append(f"não trata {primeiro_nome(nome)} pelo nome")
    texto = _sem_acento(f"{titulo} {mensagem}")
    for termo in PROIBIDOS:
        if re.search(rf"\b{termo}", texto):
            problemas.append(f"menciona '{termo}'")
    if "%" in mensagem or "%" in titulo:
        problemas.append("usa percentual")
    if vencimento_br not in mensagem:
        problemas.append(f"não cita o vencimento {vencimento_br}")
    if len(titulo) > TITULO_MAX:
        problemas.append(f"título com {len(titulo)} caracteres")
    if len(mensagem) > MENSAGEM_MAX:
        problemas.append(f"mensagem com {len(mensagem)} caracteres")
    return {"aprovado": not problemas, "problemas": problemas}


def fixo(nome: str, vencimento_br: str, competencia_br: str) -> dict:
    """O lembrete sem LLM. Sai quando o gerado é recusado ou a rede falha."""
    return {
        "titulo": "Lembrete: taxa condominial",
        "mensagem": (
            f"Olá, {primeiro_nome(nome)}! A taxa condominial de {competencia_br} vence em "
            f"{vencimento_br}. O boleto e o PIX já estão disponíveis no aplicativo Mora. "
            "Se precisar de alguma ajuda, fale com a administração."
        ),
    }


def gerar(nome: str, papel: str, unidade: str, competencia: str, vencimento: str) -> dict:
    """Rascunho do lembrete, com a verificação anexada.

    `papel` (INQUILINO ou PROPRIETÁRIO) entra para o texto acertar o
    tratamento — o proprietário que não mora lá é avisado de outro jeito —, mas
    nada sobre o risco entra.
    """
    vencimento_br = data_br(vencimento)
    competencia_br = date.fromisoformat(str(competencia)[:10]).strftime("%m/%Y")
    reserva = fixo(nome, vencimento_br, competencia_br)

    entrada = (
        f"Destinatário: {nome} ({papel.lower()} da unidade {unidade}).\n"
        f"Taxa condominial da competência {competencia_br}, vencimento em {vencimento_br}."
    )
    try:
        bruto = gemini.gerar_json(INSTRUCAO, entrada, SCHEMA, rotulo="lembrete")
    except gemini.ErroGemini as e:
        return {"fonte": "fixo", "motivo": str(e), **reserva, "verificacao": None}

    titulo = str(bruto.get("titulo", "")).strip()
    mensagem = str(bruto.get("mensagem", "")).strip()
    verificacao = verificar(titulo, mensagem, vencimento_br, nome)
    if not verificacao["aprovado"]:
        # O modelo foi além do que podia dizer. O texto dele é descartado
        # inteiro: corrigir uma frase deixaria passar a próxima.
        return {
            "fonte": "fixo",
            "motivo": "o texto gerado foi recusado: " + "; ".join(verificacao["problemas"]),
            **reserva,
            "verificacao": verificacao,
            "recusado": {"titulo": titulo, "mensagem": mensagem},
        }
    return {"fonte": "gemini", "motivo": None, "titulo": titulo, "mensagem": mensagem,
            "verificacao": verificacao}


def vencimento_da_competencia(unidade_id: str, competencia: str) -> str:
    """Dia de vencimento habitual da unidade, aplicado à competência prevista."""
    import pandas as pd  # só aqui: o resto do módulo não precisa

    faturas = pd.read_csv(pathlib.Path(__file__).parent / "dados" / "faturas.csv")
    dias = pd.to_datetime(faturas.loc[faturas["unidade_id"] == unidade_id, "vencimento"]).dt.day
    dia = int(dias.mode().iloc[0]) if not dias.empty else 10
    comp = date.fromisoformat(str(competencia)[:10])
    return date(comp.year, comp.month, dia).isoformat()


def enviar(titulo: str, mensagem: str, chave_evento: str) -> dict:
    """Publica no sino do Mora pela rota interna do comunicacao-service.

    Nunca lança: o rascunho continua útil mesmo que o envio falhe, e a resposta
    diz o que aconteceu.
    """
    url = os.environ.get("COMUNICACAO_URL", "http://localhost:8087/api/comunicacao").rstrip("/")
    token = os.environ.get("SERVICO_TOKEN", "").strip()
    usuario = os.environ.get("IA_DEMO_USUARIO_ID", "").strip()
    condominio = os.environ.get("IA_DEMO_CONDOMINIO_ID", "").strip()
    if not (token and usuario and condominio):
        return {
            "enviado": False,
            "motivo": "envio não configurado: defina SERVICO_TOKEN, IA_DEMO_USUARIO_ID e "
                      "IA_DEMO_CONDOMINIO_ID no .env",
        }
    try:
        r = httpx.post(
            f"{url}/interno/notificacoes",
            headers={"X-Servico-Token": token},
            json={
                "condominioId": condominio,
                "usuarioId": usuario,
                "origem": "ia-service",
                "tipo": "SISTEMA",
                "titulo": titulo,
                "mensagem": mensagem,
                "dados": {"evento": chave_evento},
                # O mesmo lembrete não chega duas vezes, ainda que o botão seja
                # apertado de novo.
                "chaveUnica": f"ia:LEMBRETE:{chave_evento}",
            },
            timeout=10,
        )
    except httpx.HTTPError as e:
        return {"enviado": False, "motivo": f"comunicacao-service inacessível: {e}"}
    if r.status_code not in (200, 201):
        return {"enviado": False, "motivo": f"comunicacao-service devolveu {r.status_code}"}
    corpo = r.json()
    return {
        "enviado": True,
        "repetido": bool(corpo.get("repetida")),
        "notificacao_id": (corpo.get("notificacao") or {}).get("id"),
        "destino": {"usuario_id": usuario, "condominio_id": condominio,
                    "observacao": "dados sintéticos: na demonstração o lembrete vai ao usuário configurado"},
    }
