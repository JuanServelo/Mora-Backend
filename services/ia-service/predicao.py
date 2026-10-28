"""Pontuação de uma unidade: carrega o artefato, monta as features, explica.

Núcleo compartilhado entre a API e o roteiro de demonstração, para que os dois
mostrem exatamente o mesmo número.
"""

from __future__ import annotations

import functools
import pathlib

import joblib
import numpy as np
import pandas as pd

import features
import treino

AQUI = pathlib.Path(__file__).parent


class SemModelo(RuntimeError):
    pass


@functools.lru_cache(maxsize=1)
def carregar() -> dict:
    caminho = AQUI / "artefatos" / "modelo.joblib"
    if not caminho.exists():
        raise SemModelo("modelo não treinado. Rode: python gerador.py && python treino.py")
    artefato = joblib.load(caminho)

    faturas = pd.read_csv(AQUI / "dados" / "faturas.csv")
    contratos = pd.read_csv(AQUI / "dados" / "contratos.csv")
    dados = features.preparar(faturas, contratos)

    comps = np.sort(pd.to_datetime(faturas["competencia"]).unique())
    # A competência a prever é a seguinte à última emitida.
    proxima = (pd.Timestamp(comps[-1]) + pd.DateOffset(months=1)).to_datetime64()

    return {
        "artefato": artefato,
        "dados": dados,
        "competencia_alvo": proxima,
        "unidades": list(dados["faturas"].keys()),
    }


def pontuar(unidade_id: str, competencia: np.datetime64 | None = None) -> dict:
    """Score da unidade. `sem_previsao` quando não há histórico suficiente."""
    ctx = carregar()
    corte = competencia if competencia is not None else ctx["competencia_alvo"]

    f = features.montar_features(ctx["dados"], unidade_id, corte)
    if f is None:
        # Unidade nova, ou que acabou de trocar de inquilino. O sistema diz que
        # não sabe em vez de herdar o comportamento de quem se mudou.
        return {
            "unidade_id": unidade_id,
            "competencia": str(pd.Timestamp(corte).date()),
            "sem_previsao": True,
            "motivo": f"histórico insuficiente (mínimo de {features.MIN_HISTORICO} faturas vencidas)",
        }

    artefato = ctx["artefato"]
    x = np.asarray([f[k] for k in artefato["features"]], dtype="float64")
    xs = artefato["escalador"].transform(x.reshape(1, -1))
    score = float(artefato["modelo"].predict_proba(xs)[0, 1])
    contribs = treino.explicar(x, artefato)

    return {
        "unidade_id": unidade_id,
        "competencia": str(pd.Timestamp(corte).date()),
        "sem_previsao": False,
        "score": round(score, 4),
        "classe": "RISCO_ALTO" if score >= artefato["limiar"] else "RISCO_BAIXO",
        "limiar": artefato["limiar"],
        "contribuicoes": contribs,
        "features": {k: round(float(v), 4) for k, v in f.items()},
    }


def painel(limite: int = 40) -> list[dict]:
    """Unidades ordenadas por risco — é o que o síndico olharia primeiro."""
    ctx = carregar()
    linhas = []
    for unidade in ctx["unidades"]:
        r = pontuar(unidade)
        if r["sem_previsao"]:
            continue
        linhas.append(
            {
                "unidade_id": unidade,
                "score": r["score"],
                "classe": r["classe"],
                "alugada": bool(r["features"]["alugada"]),
                "atrasos_12m": r["features"]["atrasos_12m"],
                "principal_fator": r["contribuicoes"][0]["feature"],
            }
        )
    linhas.sort(key=lambda d: -d["score"])
    return linhas[:limite]
