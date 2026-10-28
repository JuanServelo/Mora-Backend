"""Garantias sobre a explicação — a parte que vai ao morador.

Duas afirmações são feitas em voz alta na apresentação. Elas precisam ser
verdadeiras, e verificadas automaticamente:

1. As contribuições somam exatamente o logit do score. É a propriedade que
   torna a decomposição *exata* num modelo aditivo, e é o argumento para ter
   publicado a Regressão Logística em vez do Gradient Boosting.

2. O LLM não altera o número. Ele traduz; o score é do modelo estatístico.
"""

from __future__ import annotations

import math
import pathlib
import sys

import pytest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))

import explicacao  # noqa: E402

AQUI = pathlib.Path(__file__).resolve().parents[1]


@pytest.fixture(scope="module")
def ambiente():
    if not (AQUI / "artefatos" / "modelo.joblib").exists():
        pytest.skip("modelo não treinado; rode `python gerador.py && python treino.py`")
    import predicao

    return predicao


def test_contribuicoes_somam_o_logit_do_score(ambiente):
    """A decomposição é exata, não aproximada."""
    artefato = ambiente.carregar()["artefato"]
    intercepto = float(artefato["modelo"].intercept_[0])

    for linha in ambiente.painel(8):
        r = ambiente.pontuar(linha["unidade_id"])
        soma = sum(c["contribuicao_log_odds"] for c in r["contribuicoes"])
        logit = math.log(r["score"] / (1 - r["score"]))
        # A folga é o arredondamento aplicado na formatação (4 casas).
        assert soma + intercepto == pytest.approx(logit, abs=2e-3), (
            f"a decomposição não fecha para {linha['unidade_id']}: "
            f"soma+intercepto={soma + intercepto}, logit={logit}"
        )


def test_texto_que_altera_o_score_e_reprovado():
    """A verificação de fidelidade pega o LLM citando outro número."""
    r = explicacao.verificar_fidelidade(0.73, "Risco de 45% para a próxima fatura.", 0.45)
    assert not r["fiel"]


def test_texto_fiel_e_aprovado_em_fracao_e_em_percentual():
    """0,73 e 73 são a mesma afirmação: recusar uma seria inventar um erro."""
    for citado in (0.73, 73):
        r = explicacao.verificar_fidelidade(0.732, "Risco de 73% para a próxima fatura.", citado)
        assert r["fiel"], f"score citado como {citado} deveria ser aceito"


def test_virgula_decimal_nao_confunde_a_verificacao():
    """"91,8%" é 0,918 — a regex ingênua lia 0,08 e reprovava texto correto."""
    r = explicacao.verificar_fidelidade(0.918, "Risco de 91,8% para a próxima fatura.", 0.918)
    assert r["fiel"]
    # 91.8/100 não é exatamente 0.918 em ponto flutuante.
    assert r["percentuais_no_texto"] == pytest.approx([0.918])


def test_explicacao_deterministica_nao_depende_de_rede():
    """Sempre há explicação, mesmo sem LLM nenhum."""
    contribs = [
        {"feature": "taxa_em_dia_12m", "valor": 0.1, "contribuicao_log_odds": 1.2},
        {"feature": "atrasos_3m", "valor": 3.0, "contribuicao_log_odds": 0.4},
        {"feature": "antecedencia_media", "valor": 2.0, "contribuicao_log_odds": -0.3},
    ]
    texto = explicacao.em_portugues_simples(0.84, contribs)
    assert "84%" in texto
    assert "Pesa a favor do risco" in texto
    assert "Pesa contra" in texto
