"""Prova de que o futuro não entra nas features.

O argumento não é "tomamos cuidado". É um teste: monta as features de uma
unidade num corte, depois **apaga da base tudo que vence a partir desse corte**
e monta de novo. Se qualquer feature tivesse lido o futuro, os dois vetores
seriam diferentes.

Cobre as duas armadilhas descritas em features.py — a média calculada sobre a
série inteira, e a leitura de `status`/`pago_em` como estão hoje em vez de como
estavam no corte.
"""

from __future__ import annotations

import pathlib
import sys

import numpy as np
import pandas as pd
import pytest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))

import features  # noqa: E402

AQUI = pathlib.Path(__file__).resolve().parents[1]


@pytest.fixture(scope="module")
def base():
    f = AQUI / "dados" / "faturas.csv"
    c = AQUI / "dados" / "contratos.csv"
    if not f.exists():
        pytest.skip("dados não gerados; rode `python gerador.py`")
    return pd.read_csv(f), pd.read_csv(c)


def test_truncar_o_futuro_nao_muda_as_features(base):
    faturas, contratos = base
    comps = np.sort(pd.to_datetime(faturas["competencia"]).unique())
    corte = comps[len(comps) // 2]

    unidades = faturas["unidade_id"].drop_duplicates().head(25).tolist()

    completo = features.preparar(faturas, contratos)

    # A mesma base sem nada que vença no corte ou depois dele.
    venc = pd.to_datetime(faturas["vencimento"])
    truncado = features.preparar(faturas[venc < corte].copy(), contratos)

    comparadas = 0
    for unidade in unidades:
        a = features.montar_features(completo, unidade, corte)
        b = features.montar_features(truncado, unidade, corte)
        assert (a is None) == (b is None), f"{unidade}: disponibilidade divergiu"
        if a is None:
            continue
        for k in features.FEATURES:
            assert a[k] == pytest.approx(b[k], abs=1e-9), (
                f"vazamento em '{k}' na unidade {unidade}: "
                f"com futuro {a[k]}, sem futuro {b[k]}"
            )
        comparadas += 1

    assert comparadas >= 5, "amostra insuficiente para a prova valer"


def test_pagamento_posterior_ao_corte_conta_como_em_aberto():
    """A armadilha sutil, isolada num caso mínimo.

    Uma fatura vencida antes do corte e paga depois dele estava **em aberto** no
    corte. Ler `pago_em` como está hoje faria o modelo saber que ela seria paga.
    """
    faturas = pd.DataFrame(
        [
            # vence em janeiro, paga em março: no corte de fevereiro, em aberto
            {"unidade_id": "u1", "competencia": "2026-01-01", "vencimento": "2026-01-10",
             "valor_centavos": 38000, "status": "PAGA", "pago_em": "2026-03-05",
             "valor_pago_centavos": 38000, "forma_baixa": "GATEWAY", "tem_multa": False},
            {"unidade_id": "u1", "competencia": "2025-12-01", "vencimento": "2025-12-10",
             "valor_centavos": 38000, "status": "PAGA", "pago_em": "2025-12-09",
             "valor_pago_centavos": 38000, "forma_baixa": "GATEWAY", "tem_multa": False},
            {"unidade_id": "u1", "competencia": "2025-11-01", "vencimento": "2025-11-10",
             "valor_centavos": 38000, "status": "PAGA", "pago_em": "2025-11-08",
             "valor_pago_centavos": 38000, "forma_baixa": "GATEWAY", "tem_multa": False},
            {"unidade_id": "u1", "competencia": "2025-10-01", "vencimento": "2025-10-10",
             "valor_centavos": 38000, "status": "PAGA", "pago_em": "2025-10-10",
             "valor_pago_centavos": 38000, "forma_baixa": "GATEWAY", "tem_multa": False},
            {"unidade_id": "u1", "competencia": "2025-09-01", "vencimento": "2025-09-10",
             "valor_centavos": 38000, "status": "PAGA", "pago_em": "2025-09-09",
             "valor_pago_centavos": 38000, "forma_baixa": "GATEWAY", "tem_multa": False},
            {"unidade_id": "u1", "competencia": "2025-08-01", "vencimento": "2025-08-10",
             "valor_centavos": 38000, "status": "PAGA", "pago_em": "2025-08-07",
             "valor_pago_centavos": 38000, "forma_baixa": "GATEWAY", "tem_multa": False},
        ]
    )
    dados = features.preparar(faturas, pd.DataFrame(columns=["unidade_id", "inicio", "fim",
                                                             "responsavel_taxas"]))
    f = features.montar_features(dados, "u1", np.datetime64("2026-02-01"))
    assert f is not None
    # No corte de fevereiro, a de janeiro estava vencida e não paga.
    assert f["abertas_vencidas_no_corte"] == 1.0
    assert f["atrasos_3m"] >= 1.0
    # E a média de dias de atraso ignora a de janeiro, que ainda não tinha
    # desfecho: as demais foram pagas em dia ou adiantado.
    assert f["media_dias_atraso"] == 0.0


def test_unidade_sem_historico_nao_recebe_previsao(base):
    faturas, contratos = base
    dados = features.preparar(faturas, contratos)
    primeira = np.sort(pd.to_datetime(faturas["competencia"]).unique())[0]
    unidade = faturas["unidade_id"].iloc[0]
    # No primeiro mês da série não há passado algum.
    assert features.montar_features(dados, unidade, primeira) is None
    # Unidade inexistente também não.
    assert features.montar_features(dados, "nao-existe", primeira) is None
