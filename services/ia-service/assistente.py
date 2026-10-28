"""Assistente do síndico: perguntas em português, respondidas com tool calling.

O quarto uso do LLM, e o único em que ele decide o que fazer. Nos outros três o
serviço monta a entrada e o modelo só escreve; aqui o síndico pergunta "quais
unidades alugadas preocupam?" e o Gemini escolhe quais funções chamar, com que
argumentos, e em que ordem.

O modelo não toca os dados. As funções abaixo são o único caminho até eles, e
são as mesmas que o painel usa: o número que o assistente cita é o número da
tela. Duas guardas:

- **Ética:** nenhuma ferramenta devolve valor de aluguel. O que o modelo não
  recebe ele não usa, nem para "explicar" um risco.
- **Fidelidade:** todo percentual da resposta precisa existir no que as funções
  devolveram. Um número que o modelo inventou ou recalculou derruba a resposta,
  e o síndico recebe os dados consultados em vez do texto.
"""

from __future__ import annotations

import functools
import json
import pathlib
import re

import pandas as pd

import explicacao as mod_explicacao
import gemini
import predicao

AQUI = pathlib.Path(__file__).parent

INSTRUCAO = """\
Você é o assistente do síndico no Mora, um sistema de gestão de condomínio.
Responda em português do Brasil, de forma curta e direta.

- Use SOMENTE os dados que as funções devolverem. Não estime, não arredonde de
  outro jeito e não calcule percentuais novos: cite os valores como vieram.
- Para perguntas sobre risco de atraso, unidades, contratos ou o modelo, chame
  as funções antes de responder. Se nenhuma função cobre a pergunta, diga que
  não tem essa informação.
- Identifique unidades pelo rótulo (por exemplo A-302 ou U-017).
- Os riscos são estimativas de um modelo estatístico sobre dados sintéticos:
  não trate nenhuma unidade como culpada.
"""

# Declarações no subconjunto de OpenAPI que a API aceita.
FERRAMENTAS = [
    {
        "name": "resumo_do_condominio",
        "description": "Visão geral da previsão para a próxima competência: quantas unidades "
                       "têm previsão, quantas estão em risco alto, e a divisão entre alugadas e próprias.",
        "parameters": {"type": "OBJECT", "properties": {}},
    },
    {
        "name": "unidades_em_risco",
        "description": "Unidades ordenadas da de maior para a de menor risco de atraso na próxima competência.",
        "parameters": {
            "type": "OBJECT",
            "properties": {
                "limite": {"type": "INTEGER", "description": "Quantas unidades devolver (1 a 20)."},
                "ocupacao": {"type": "STRING", "enum": ["todas", "alugadas", "proprias"],
                             "description": "Filtra por tipo de ocupação."},
                "somente_risco_alto": {"type": "BOOLEAN", "description": "Só as classificadas como risco alto."},
            },
        },
    },
    {
        "name": "risco_da_unidade",
        "description": "Risco de atraso de uma unidade, com os fatores que mais pesaram e o histórico.",
        "parameters": {
            "type": "OBJECT",
            "properties": {"unidade": {"type": "STRING", "description": "Rótulo da unidade, ex.: A-302 ou U-017."}},
            "required": ["unidade"],
        },
    },
    {
        "name": "contrato_da_unidade",
        "description": "Contrato de locação vigente da unidade: se é alugada, desde quando, até quando, "
                       "e quem responde pelas taxas condominiais (inquilino ou proprietário).",
        "parameters": {
            "type": "OBJECT",
            "properties": {"unidade": {"type": "STRING", "description": "Rótulo da unidade."}},
            "required": ["unidade"],
        },
    },
    {
        "name": "desempenho_do_modelo",
        "description": "Qualidade do modelo de previsão: AUC, precisão e recall, e a auditoria de viés "
                       "entre unidades alugadas e próprias.",
        "parameters": {"type": "OBJECT", "properties": {}},
    },
]


# ── Rótulos legíveis ──────────────────────────────────────────────────────


@functools.lru_cache(maxsize=1)
def _rotulos() -> tuple[dict, dict]:
    """UUID ↔ rótulo. As seis unidades dos contratos de demonstração mantêm o
    bloco e o apartamento do índice; as demais, sintéticas, viram U-001 em
    diante, numa ordem fixa — o mesmo rótulo em toda execução."""
    por_uuid: dict[str, str] = {}
    indice = AQUI / "fixtures" / "contratos" / "indice.json"
    if indice.exists():
        for chave, v in json.loads(indice.read_text(encoding="utf-8")).items():
            por_uuid[v["unidade_id"]] = chave
    n = 0
    for uid in sorted(predicao.carregar()["unidades"]):
        if uid not in por_uuid:
            n += 1
            por_uuid[uid] = f"U-{n:03d}"
    return por_uuid, {r: u for u, r in por_uuid.items()}


def rotulo(uid: str) -> str:
    return _rotulos()[0].get(uid, uid)


def _uuid(rotulo_ou_uuid: str) -> str:
    alvo = str(rotulo_ou_uuid).strip().upper()
    por_uuid, por_rotulo = _rotulos()
    if alvo in por_rotulo:
        return por_rotulo[alvo]
    if rotulo_ou_uuid in por_uuid:
        return rotulo_ou_uuid
    raise ValueError(f"unidade '{rotulo_ou_uuid}' não encontrada; use um rótulo como A-302 ou U-017")


# ── Ferramentas ───────────────────────────────────────────────────────────


def _pct(x: float) -> str:
    return f"{x:.0%}"


def resumo_do_condominio() -> dict:
    linhas = predicao.painel(limite=10_000)
    altos = [l for l in linhas if l["classe"] == "RISCO_ALTO"]
    return {
        "competencia": str(predicao.carregar()["competencia_alvo"])[:10],
        "unidades_com_previsao": len(linhas),
        "unidades_sem_previsao": len(predicao.carregar()["unidades"]) - len(linhas),
        "em_risco_alto": len(altos),
        "em_risco_alto_alugadas": sum(1 for l in altos if l["alugada"]),
        "em_risco_alto_proprias": sum(1 for l in altos if not l["alugada"]),
    }


def unidades_em_risco(limite: int = 10, ocupacao: str = "todas", somente_risco_alto: bool = False) -> dict:
    limite = max(1, min(int(limite or 10), 20))
    linhas = predicao.painel(limite=10_000)
    if ocupacao == "alugadas":
        linhas = [l for l in linhas if l["alugada"]]
    elif ocupacao == "proprias":
        linhas = [l for l in linhas if not l["alugada"]]
    if somente_risco_alto:
        linhas = [l for l in linhas if l["classe"] == "RISCO_ALTO"]
    return {
        "total_no_filtro": len(linhas),
        "unidades": [
            {
                "unidade": rotulo(l["unidade_id"]),
                "risco": _pct(l["score"]),
                "classe": l["classe"],
                "ocupacao": "alugada" if l["alugada"] else "própria",
                "atrasos_12m": int(l["atrasos_12m"]),
                "principal_fator": mod_explicacao.ROTULOS.get(l["principal_fator"], l["principal_fator"]),
            }
            for l in linhas[:limite]
        ],
    }


def risco_da_unidade(unidade: str) -> dict:
    r = predicao.pontuar(_uuid(unidade))
    if r["sem_previsao"]:
        return {"unidade": unidade.upper(), "sem_previsao": True, "motivo": r["motivo"]}
    f = r["features"]
    return {
        "unidade": rotulo(r["unidade_id"]),
        "competencia": r["competencia"],
        "risco": _pct(r["score"]),
        "classe": r["classe"],
        "fatores_que_mais_pesaram": [mod_explicacao.descrever(c) for c in r["contribuicoes"][:4]],
        "historico": {
            "faturas_analisadas": int(f["meses_historico"]),
            "atrasos_12m": int(f["atrasos_12m"]),
            "pagas_em_dia_12m": _pct(f["taxa_em_dia_12m"]),
            "media_dias_atraso": round(f["media_dias_atraso"]),
            "multas_12m": int(f["multas_12m"]),
        },
    }


@functools.lru_cache(maxsize=1)
def _contratos() -> pd.DataFrame:
    return pd.read_csv(AQUI / "dados" / "contratos.csv")


def contrato_da_unidade(unidade: str) -> dict:
    uid = _uuid(unidade)
    c = _contratos()
    vigente = c[(c["unidade_id"] == uid) & (c["ativo"])]
    if vigente.empty:
        return {"unidade": rotulo(uid), "alugada": False,
                "responsavel_taxas": "PROPRIETARIO", "observacao": "sem contrato de locação vigente"}
    v = vigente.iloc[0]
    # O valor do aluguel fica de fora de propósito: é proxy socioeconômico, e o
    # que o modelo não recebe ele não usa para justificar um risco.
    return {
        "unidade": rotulo(uid),
        "alugada": True,
        "inicio": str(v["inicio"]),
        "fim": None if pd.isna(v["fim"]) else str(v["fim"]),
        "responsavel_taxas": v["responsavel_taxas"],
    }


def desempenho_do_modelo() -> dict:
    m = json.loads((AQUI / "artefatos" / "metricas.json").read_text(encoding="utf-8"))
    publicado = m["modelos"][m["modelo_publicado"]]
    vies = m["auditoria_vies_por_ocupacao"]
    return {
        "modelo": m["modelo_publicado"],
        "auc_roc": publicado["auc_roc"],
        "precisao": _pct(publicado["precision"]),
        "recall": _pct(publicado["recall"]),
        "auc_alugadas": vies["alugadas"]["auc_roc"],
        "auc_proprias": vies["proprias"]["auc_roc"],
        "validacao": "temporal: treino nas competências antigas, teste nas recentes",
        "dados": "sintéticos; o modelo demonstra o método, sem validade preditiva no mundo real",
    }


EXECUTORES = {
    "resumo_do_condominio": resumo_do_condominio,
    "unidades_em_risco": unidades_em_risco,
    "risco_da_unidade": risco_da_unidade,
    "contrato_da_unidade": contrato_da_unidade,
    "desempenho_do_modelo": desempenho_do_modelo,
}


def executar(nome: str, args: dict) -> dict:
    if nome not in EXECUTORES:
        return {"erro": f"função desconhecida: {nome}"}
    try:
        return EXECUTORES[nome](**args)
    except (ValueError, TypeError) as e:
        # Vira dado para o modelo, que pode corrigir o argumento e tentar de novo.
        return {"erro": str(e)}


# ── Fidelidade ────────────────────────────────────────────────────────────

_PERCENTUAL = re.compile(r"(\d{1,3}(?:[.,]\d+)?)\s*%")


def _percentuais(texto: str) -> list[float]:
    return [float(m.replace(",", ".")) for m in _PERCENTUAL.findall(texto)]


def verificar(resposta: str, chamadas: list[dict]) -> dict:
    """Todo percentual da resposta aparece no que as funções devolveram?

    Tolerância de 1 ponto, pela escrita ("74%" para 0,7412). Frações dos
    resultados (0,7517 de AUC) contam como 75%.
    """
    consultados: list[float] = []
    for ch in chamadas:
        bruto = json.dumps(ch["resultado"], ensure_ascii=False)
        consultados += _percentuais(bruto)
        consultados += [float(x) * 100 for x in re.findall(r"(?<![\d.])0\.\d+", bruto)]
    citados = _percentuais(resposta)
    sem_origem = [p for p in citados if not any(abs(p - q) <= 1.0 for q in consultados)]
    return {"fiel": not sem_origem, "percentuais_citados": citados, "sem_origem": sem_origem}


def _dados_consultados(chamadas: list[dict]) -> str:
    linhas = ["Não consegui confirmar a resposta escrita; estes são os dados consultados:"]
    for ch in chamadas:
        linhas.append(f"- {ch['ferramenta']}: {json.dumps(ch['resultado'], ensure_ascii=False)}")
    return "\n".join(linhas)


def perguntar(pergunta: str) -> dict:
    pergunta = (pergunta or "").strip()
    if not pergunta:
        return {"fonte": "nenhuma", "resposta": "Faça uma pergunta.", "chamadas": []}
    try:
        r = gemini.conversar(INSTRUCAO, pergunta, FERRAMENTAS, executar)
    except gemini.ErroGemini as e:
        return {"fonte": "indisponivel", "motivo": str(e), "chamadas": [],
                "resposta": "O assistente está indisponível agora. O painel de risco continua funcionando."}

    verificacao = verificar(r["resposta"], r["chamadas"])
    if not verificacao["fiel"]:
        return {"fonte": "dados", "motivo": "a resposta citou números que as funções não devolveram",
                "resposta": _dados_consultados(r["chamadas"]), "recusada": r["resposta"],
                "chamadas": r["chamadas"], "modelo": r["modelo"], "verificacao": verificacao}
    return {"fonte": "gemini", "motivo": None, "resposta": r["resposta"], "chamadas": r["chamadas"],
            "modelo": r["modelo"], "verificacao": verificacao}
