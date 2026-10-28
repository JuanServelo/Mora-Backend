"""Engenharia de features, com a janela temporal fechada à força.

A regra que governa este arquivo: **a predição acontece na emissão da fatura da
competência N**, logo nada com data maior ou igual ao primeiro dia de N pode ser
lido. Toda feature sai de `montar_features(..., corte)`, que começa filtrando a
janela e reconstrói o estado como ele era no corte.

Duas armadilhas de vazamento que isto resolve:

1. A óbvia: calcular médias com `groupby` sobre a série inteira e só depois
   fatiar treino e teste. A média da unidade já conteria os meses de teste.

2. A sutil, e específica deste esquema: uma fatura de 2025-03 paga em 2025-09
   tem hoje `status = 'PAGA'` e `pago_em` no futuro. Ao montar as features para
   a competência alvo 2025-06, ler a coluna como ela está hoje usa informação
   que não existia em junho — no corte, aquela fatura estava em aberto e
   vencida. Por isso **nenhuma feature lê `status`**: coluna mutável é
   vazamento por construção. O estado é recalculado de `pago_em` contra o
   corte.

`testes/test_vazamento.py` prova a ausência de vazamento truncando a série e
exigindo vetores idênticos.
"""

from __future__ import annotations

import numpy as np
import pandas as pd

LIMIAR_ATRASO_DIAS = 5
JANELA_MESES = 12
# Abaixo disto não há previsão: o sistema diz que não sabe em vez de chutar.
# Seis meses, e não três: com histórico curto a estimativa da propensão a
# atrasar é dominada por ruído binomial, e a linha entra no treino atrapalhando
# em vez de ensinar. Unidade nova fica sem score — o que é a resposta honesta.
MIN_HISTORICO = 6

# Fonte única de verdade dos nomes. O treino, o artefato e a API leem daqui —
# nunca de `feature_names_in_` do sklearn.
FEATURES: list[str] = [
    # comportamentais
    # Features colineares foram podadas, não por estética: num modelo linear
    # elas repartem o sinal e uma delas ganha coeficiente de sinal invertido,
    # produzindo explicação contraditória ("mais atrasos reduz o risco"). Uma
    # explicação que se contradiz não explica nada.
    #
    #   atrasos_6m   x atrasos_3m/12m      → correlação alta
    #   atrasos_12m  x taxa_em_dia_12m     → -0,97, são a mesma coisa
    #   desvio_dias  x media_dias_atraso   → +0,62
    #   trocas_resp  x alugada             → +0,92
    #   meses_decorr x alugada             → +0,73
    "atrasos_3m",
    "media_dias_atraso",
    "taxa_em_dia_12m",
    #   tendencia    x atrasos_3m          → supressão: a tendência é uma
    #                                        diferença de taxas e carrega a
    #                                        taxa de 3 meses, que briga com a
    #                                        contagem de atrasos de 3 meses.
    #                                        Mantida a contagem, que explica
    #                                        melhor: "3 atrasos nos últimos 3
    #                                        meses" diz mais que "tendência
    #                                        -0,12".
    "antecedencia_media",
    "abertas_vencidas_no_corte",
    "multas_12m",
    "meses_historico",
    "razao_valor_media_unidade",
    "mes_competencia",
    # contratuais — a contribuição da gestão de locação
    "alugada",
    "responsavel_inquilino",
    "meses_restantes_contrato",
    "troca_recente_3m",
]

# Proibidas por decisão ética, não por limitação técnica: são proxy
# socioeconômico direto. O dado existe no banco; a escolha é não usá-lo.
# Ver testes/test_etica.py, que falha se qualquer uma entrar em FEATURES.
FEATURES_PROIBIDAS: list[str] = [
    "valor_aluguel_centavos",
    "valor_aluguel",
    "razao_taxa_aluguel",
    "aluguel",
]


def _dias(a: pd.Series, b: pd.Series) -> np.ndarray:
    return (a - b).dt.days.to_numpy(dtype="float64")


def preparar(faturas: pd.DataFrame, contratos: pd.DataFrame) -> dict:
    """Converte os CSVs em arrays por unidade, uma vez, para o resto ser rápido."""
    f = faturas.copy()
    for col in ("competencia", "vencimento", "pago_em"):
        f[col] = pd.to_datetime(f[col], errors="coerce")

    c = contratos.copy()
    if len(c):
        for col in ("inicio", "fim"):
            c[col] = pd.to_datetime(c[col], errors="coerce")

    por_unidade: dict[str, dict] = {}
    for unidade, g in f.groupby("unidade_id", sort=False):
        g = g.sort_values("competencia")
        por_unidade[unidade] = {
            "competencia": g["competencia"].to_numpy(),
            "vencimento": g["vencimento"].to_numpy(),
            "pago_em": g["pago_em"].to_numpy(),
            "valor": g["valor_centavos"].to_numpy(dtype="float64"),
            "multa": g["tem_multa"].to_numpy(),
        }

    contratos_por_unidade: dict[str, pd.DataFrame] = {}
    if len(c):
        for unidade, g in c.groupby("unidade_id", sort=False):
            contratos_por_unidade[unidade] = g.sort_values("inicio")

    return {"faturas": por_unidade, "contratos": contratos_por_unidade}


def montar_features(dados: dict, unidade_id: str, corte: np.datetime64) -> dict[str, float] | None:
    """Features da unidade no instante `corte`. None se não houver histórico.

    `corte` é o primeiro dia da competência alvo. Nada com data >= corte entra.
    """
    u = dados["faturas"].get(unidade_id)
    if u is None:
        return None

    corte = np.datetime64(corte)
    inicio_janela = corte - np.timedelta64(JANELA_MESES * 31, "D")

    # Filtro primeiro, sempre. Só faturas já vencidas antes do corte.
    na_janela = (u["vencimento"] < corte) & (u["vencimento"] >= inicio_janela)
    if not na_janela.any():
        return None

    venc = u["vencimento"][na_janela]
    pago = u["pago_em"][na_janela]
    valor = u["valor"][na_janela]
    multa = u["multa"][na_janela]

    # Estado reconstruído no corte: pagamento posterior ao corte não existia.
    pago_no_corte = ~pd.isna(pago) & (pago < corte)

    dias = np.full(venc.shape, np.nan)
    dias[pago_no_corte] = (
        (pago[pago_no_corte] - venc[pago_no_corte]) / np.timedelta64(1, "D")
    ).astype("float64")
    # Não paga no corte: estava em aberto, atrasada desde o vencimento.
    aberta = ~pago_no_corte
    dias[aberta] = ((corte - venc[aberta]) / np.timedelta64(1, "D")).astype("float64")

    n = int(venc.size)
    if n < MIN_HISTORICO:
        return None

    atrasou = dias > LIMIAR_ATRASO_DIAS

    def ultimos(meses: int) -> np.ndarray:
        limite = corte - np.timedelta64(meses * 31, "D")
        return venc >= limite

    m3, m6 = ultimos(3), ultimos(6)
    atrasos_3m = float(atrasou[m3].sum())
    atrasos_6m = float(atrasou[m6].sum())
    atrasos_12m = float(atrasou.sum())

    # Intensidade medida SÓ sobre o que foi efetivamente pago até o corte.
    # Misturar aqui as faturas em aberto há meses — cuja contagem de dias é
    # enorme por construção — afogava o sinal: a média virava função de quantas
    # faturas estavam abertas, não de quão atrasada a unidade costuma pagar.
    # O número de abertas já é uma feature própria.
    dias_pagos = dias[pago_no_corte]
    dias_positivos = dias_pagos[dias_pagos > 0]
    dias_negativos = dias_pagos[dias_pagos <= 0]

    taxa_12m = float((~atrasou).mean())
    taxa_3m = float((~atrasou[m3]).mean()) if m3.any() else taxa_12m

    # --- contratuais ------------------------------------------------------
    cs = dados["contratos"].get(unidade_id)
    alugada = 0.0
    responsavel_inquilino = 0.0
    decorridos = 0.0
    restantes = -1.0  # -1 = sem contrato ou sem data de fim
    trocas = 0.0
    troca_recente = 0.0
    if cs is not None and len(cs):
        iniciados = cs[cs["inicio"] <= corte]
        trocas = float(len(iniciados))
        if len(iniciados):
            vigentes = iniciados[iniciados["fim"].isna() | (iniciados["fim"] >= corte)]
            atual = vigentes.iloc[-1] if len(vigentes) else None
            if atual is not None:
                alugada = 1.0
                responsavel_inquilino = 1.0 if atual["responsavel_taxas"] == "INQUILINO" else 0.0
                decorridos = float((corte - atual["inicio"]) / np.timedelta64(1, "D") / 30.4)
                if not pd.isna(atual["fim"]):
                    restantes = float((atual["fim"] - corte) / np.timedelta64(1, "D") / 30.4)
            ultimo_inicio = iniciados["inicio"].max()
            meses_desde = (corte - ultimo_inicio) / np.timedelta64(1, "D") / 30.4
            troca_recente = 1.0 if meses_desde <= 3 and trocas > 1 else 0.0

    # Valor da fatura alvo: conhecido na emissão, não é o alvo. Usa a última
    # fatura da janela como proxy do valor corrente.
    media_valor = float(valor.mean()) if valor.size else 0.0
    razao_valor = float(valor[-1] / media_valor) if media_valor else 1.0

    return {
        "atrasos_3m": atrasos_3m,
        "atrasos_6m": atrasos_6m,
        "atrasos_12m": atrasos_12m,
        "media_dias_atraso": float(dias_positivos.mean()) if dias_positivos.size else 0.0,
        "desvio_dias_atraso": float(dias_positivos.std()) if dias_positivos.size > 1 else 0.0,
        "taxa_em_dia_12m": taxa_12m,
        "tendencia_3m_vs_12m": taxa_3m - taxa_12m,
        "antecedencia_media": float(-dias_negativos.mean()) if dias_negativos.size else 0.0,
        "abertas_vencidas_no_corte": float(aberta.sum()),
        "multas_12m": float(multa.sum()),
        "meses_historico": float(n),
        "razao_valor_media_unidade": razao_valor,
        "mes_competencia": float(pd.Timestamp(corte).month),
        "alugada": alugada,
        "responsavel_inquilino": responsavel_inquilino,
        "meses_decorridos_contrato": decorridos,
        "meses_restantes_contrato": restantes,
        "trocas_responsavel": trocas,
        "troca_recente_3m": troca_recente,
    }


def rotular(dados: dict, unidade_id: str, competencia: np.datetime64, hoje: np.datetime64) -> int | None:
    """Alvo: a fatura da competência foi paga com mais de 5 dias de atraso?

    A única função autorizada a olhar `pago_em` da competência alvo.
    """
    u = dados["faturas"].get(unidade_id)
    if u is None:
        return None
    alvo = u["competencia"] == np.datetime64(competencia)
    if not alvo.any():
        return None

    venc = u["vencimento"][alvo][0]
    pago = u["pago_em"][alvo][0]

    if pd.isna(pago):
        # Nunca paga: só rotula se já venceu: antes disso o desfecho é desconhecido.
        if venc >= hoje:
            return None
        dias = (hoje - venc) / np.timedelta64(1, "D")
    else:
        dias = (pago - venc) / np.timedelta64(1, "D")
    return int(dias > LIMIAR_ATRASO_DIAS)


def montar_matriz(
    faturas: pd.DataFrame, contratos: pd.DataFrame
) -> tuple[np.ndarray, np.ndarray, pd.DataFrame]:
    """Monta (X, y, meta) para todas as linhas (unidade, competência) válidas."""
    dados = preparar(faturas, contratos)
    comps = np.sort(pd.to_datetime(faturas["competencia"]).unique())
    hoje = comps[-1] + np.timedelta64(31, "D")

    linhas, alvos, meta = [], [], []
    for comp in comps:
        for unidade in dados["faturas"]:
            y = rotular(dados, unidade, comp, hoje)
            if y is None:
                continue
            f = montar_features(dados, unidade, comp)
            if f is None:
                continue
            linhas.append([f[k] for k in FEATURES])
            alvos.append(y)
            meta.append({"unidade_id": unidade, "competencia": pd.Timestamp(comp), "alugada": f["alugada"]})

    X = np.asarray(linhas, dtype="float64")
    y = np.asarray(alvos, dtype="int64")
    return X, y, pd.DataFrame(meta)
