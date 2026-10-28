"""Tradução do score para português, a partir das contribuições do modelo.

Este é o uso que amarra ML, explicabilidade e LLM numa coisa só. O modelo
devolve `0.78` e uma lista de contribuições em log-odds. Número não comunica
nada a um síndico — muito menos a um morador que queira contestar.

O LLM **traduz, nunca recalcula**. Ele recebe o score já decidido e os fatores
já medidos, e escreve o parágrafo. A instrução proíbe alterar o número, e
`verificar_fidelidade()` confere que o texto cita o mesmo score que o modelo
produziu — porque uma proibição que ninguém verifica é só um pedido.

Também existe `em_portugues_simples()`, que monta a frase sem chamar o LLM. Não
é fallback decorativo: é o que garante que a explicação exista mesmo sem rede,
e deixa claro na defesa qual parte é determinística e qual é gerada.
"""

from __future__ import annotations

import re

import gemini

INSTRUCAO = """Você explica a um síndico de condomínio, em português do Brasil, por que o \
sistema atribuiu determinado risco de atraso a uma unidade.

Regras rígidas:
- A PRIMEIRA frase deve declarar o risco usando EXATAMENTE o percentual recebido em `score_percentual`, escrito com o símbolo %. Exemplo: "Risco de 92% para a próxima fatura."
- NÃO altere, arredonde para outro valor, nem recalcule o score.
- Ao citar percentuais dos fatores, deixe claro que são do histórico, não o risco.
- Use SOMENTE os fatores fornecidos. Não invente causa, histórico ou motivo pessoal.
- Fatores com contribuição positiva aumentam o risco; negativa, reduzem.
- Duas a quatro frases, tom profissional e concreto, sem jargão estatístico \
(nada de "log-odds", "coeficiente", "AUC").
- Não sugira cobrança, restrição de acesso ou qualquer medida punitiva.
- Não faça juízo sobre a pessoa: fale do histórico de pagamento da unidade."""

SCHEMA = {
    "type": "object",
    "properties": {
        "explicacao": {"type": "string"},
        "fatores_citados": {"type": "array", "items": {"type": "string"}},
        "score_citado": {"type": "number"},
    },
    "required": ["explicacao", "fatores_citados", "score_citado"],
}

ROTULOS = {
    "atrasos_3m": "atrasos nos últimos 3 meses",
    "atrasos_12m": "atrasos nos últimos 12 meses",
    "media_dias_atraso": "média de dias de atraso",
    "desvio_dias_atraso": "irregularidade no atraso",
    "taxa_em_dia_12m": "proporção de faturas pagas em dia",
    "tendencia_3m_vs_12m": "tendência recente comparada ao histórico",
    "antecedencia_media": "antecedência média do pagamento",
    "abertas_vencidas_no_corte": "faturas vencidas em aberto",
    "multas_12m": "multas no período",
    "meses_historico": "meses de histórico disponíveis",
    "razao_valor_media_unidade": "valor da fatura frente à média da unidade",
    "mes_competencia": "mês da competência",
    "alugada": "unidade alugada",
    "responsavel_inquilino": "taxa paga pelo inquilino",
    "meses_decorridos_contrato": "meses decorridos do contrato",
    "meses_restantes_contrato": "meses restantes de contrato",
    "trocas_responsavel": "trocas de responsável na unidade",
    "troca_recente_3m": "troca de responsável recente",
}


def _principais(contribuicoes: list[dict], n: int = 5) -> list[dict]:
    return [
        {
            "fator": descrever(c),
            "valor": c["valor"],
            "efeito": "aumenta o risco" if c["contribuicao_log_odds"] > 0 else "reduz o risco",
            "peso": round(abs(c["contribuicao_log_odds"]), 3),
        }
        for c in contribuicoes[:n]
    ]


def descrever(c: dict) -> str:
    """Fator com o valor observado, para a frase comunicar a direção.

    "proporção de faturas pagas em dia" não diz se o risco sobe ou desce;
    "apenas 8% das faturas pagas em dia" diz.
    """
    f, v = c["feature"], c["valor"]
    if f == "taxa_em_dia_12m":
        return f"{v:.0%} das faturas pagas em dia no último ano"
    if f in ("atrasos_3m", "atrasos_12m"):
        janela = "3 meses" if f.endswith("3m") else "12 meses"
        return f"{v:.0f} atraso(s) nos últimos {janela}"
    if f == "media_dias_atraso":
        return f"atraso médio de {v:.0f} dias"
    if f == "abertas_vencidas_no_corte":
        return f"{v:.0f} fatura(s) vencida(s) em aberto"
    if f == "multas_12m":
        return f"{v:.0f} multa(s) no período"
    if f == "alugada":
        return "unidade alugada" if v else "unidade ocupada pelo proprietário"
    if f == "responsavel_inquilino":
        return "taxa sob responsabilidade do inquilino" if v else "taxa sob responsabilidade do proprietário"
    if f == "troca_recente_3m" and v:
        return "troca de responsável nos últimos 3 meses"
    if f == "tendencia_3m_vs_12m":
        return "melhora recente no comportamento" if v > 0 else "piora recente no comportamento"
    return ROTULOS.get(f, f)


def em_portugues_simples(score: float, contribuicoes: list[dict]) -> str:
    """Explicação determinística, sem LLM. Sempre disponível."""
    sobe = [c for c in contribuicoes[:6] if c["contribuicao_log_odds"] > 0][:2]
    desce = [c for c in contribuicoes[:6] if c["contribuicao_log_odds"] < 0][:2]
    partes = [f"Risco estimado de {score:.0%} para a próxima fatura."]
    if sobe:
        partes.append("Pesa a favor do risco: " + "; ".join(descrever(c) for c in sobe) + ".")
    if desce:
        partes.append("Pesa contra: " + "; ".join(descrever(c) for c in desce) + ".")
    return " ".join(partes)


def verificar_fidelidade(score: float, texto: str, score_citado: float) -> dict:
    """O texto cita o mesmo score que o modelo produziu?

    Verifica o campo estruturado e também os números escritos no corpo do
    texto: o modelo pode respeitar o campo e escrever outro valor na frase.
    """
    tolerancia = 0.011  # 1 ponto percentual

    # O modelo devolve o score ora como fração (0.737), ora como percentual
    # (74). As duas formas são a mesma afirmação; recusar uma delas seria
    # acusá-lo de um erro de unidade que ele não cometeu.
    citado = float(score_citado)
    if citado > 1.0:
        citado /= 100.0
    campo_ok = abs(citado - score) <= tolerancia

    # Aceita vírgula decimal: "91,8%" tem de virar 0,918, não 0,08. A regex
    # ingênua \d{1,3}% capturava só o "8" depois da vírgula e reprovava um
    # texto correto — a verificação acusava o modelo do erro dela mesma.
    percentuais = [
        float(m.replace(",", ".")) / 100
        for m in re.findall(r"(\d{1,3}(?:[.,]\d+)?)\s*%", texto)
    ]
    texto_ok = (not percentuais) or any(abs(p - score) <= tolerancia for p in percentuais)

    return {
        "fiel": bool(campo_ok and texto_ok),
        "score_modelo": round(score, 4),
        "score_citado": round(citado, 4),
        "percentuais_no_texto": percentuais,
    }


def explicar(score: float, contribuicoes: list[dict], meses_historico: float | None = None) -> dict:
    """Parágrafo gerado pelo LLM, com a verificação de fidelidade anexada."""
    fatores = _principais(contribuicoes)
    entrada = {
        "score": round(float(score), 4),
        "score_percentual": f"{score:.0%}",
        "fatores": fatores,
    }
    if meses_historico is not None:
        entrada["meses_de_historico"] = meses_historico

    determinística = em_portugues_simples(score, contribuicoes)

    try:
        bruto = gemini.gerar_json(
            instrucao=INSTRUCAO,
            entrada=__import__("json").dumps(entrada, ensure_ascii=False, indent=2),
            schema=SCHEMA,
            rotulo="explicacao",
        )
    except gemini.ErroGemini as e:
        return {
            "fonte": "deterministica",
            "motivo": str(e),
            "explicacao": determinística,
            "fatores": fatores,
            "verificacao": None,
        }

    verificacao = verificar_fidelidade(score, bruto["explicacao"], bruto["score_citado"])
    if not verificacao["fiel"]:
        # O modelo mexeu no número. A explicação gerada é descartada — o score
        # é do modelo estatístico, e o LLM não tem autoridade sobre ele.
        return {
            "fonte": "deterministica",
            "motivo": "o texto gerado citou um score diferente do calculado",
            "explicacao": determinística,
            "fatores": fatores,
            "verificacao": verificacao,
        }

    return {
        "fonte": "gemini",
        "motivo": None,
        "explicacao": bruto["explicacao"],
        "fatores": fatores,
        "verificacao": verificacao,
    }
