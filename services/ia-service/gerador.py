"""Gerador do histórico sintético de faturas e contratos de locação.

Espelha o esquema real de `mora_financeiro` — `faturas` e `contratos_locacao`,
definidos em services/financeiro/migrations/ — para que as features construídas
sobre este CSV valham sobre o banco de verdade sem reescrita.

Por que sintético: o Mora está em desenvolvimento e não tem 12 meses de
histórico de pagamento. A limitação é declarada: um modelo treinado aqui NÃO
tem validade preditiva no mundo real. O que se demonstra é o método.

O desenho do ruído é deliberado. Perfil discreto ("pontual", "caloteiro") é
separável demais: o modelo recupera o rótulo e o AUC vai a 0,97, que é a
primeira coisa que uma banca ataca — com razão. Aqui a propensão a atrasar é
uma variável latente contínua, o atraso de cada mês é um sorteio de Bernoulli,
e 10% das faturas levam um choque sem relação com o histórico. Esse choque é o
teto natural de acerto, e é também um argumento ético pronto: desemprego e
doença não são previsíveis a partir de boleto pago.
"""

from __future__ import annotations

import uuid
from datetime import date, timedelta

import numpy as np
import pandas as pd

# ---------------------------------------------------------------------------
# Parâmetros. NIVEL_RUIDO é a única alavanca global: subir aproxima o modelo do
# acaso, descer deixa o sinal mais fácil. Alvo de AUC honesto: 0,72 a 0,82.
# ---------------------------------------------------------------------------
SEED = 42
NIVEL_RUIDO = 1.0

CONDOMINIO_ID = "cond-parque-verde"
N_UNIDADES = 120
N_COMPETENCIAS = 36
PRIMEIRA_COMPETENCIA = "2023-11"

# Define o alvo: atraso acima disto conta como inadimplência prevista.
LIMIAR_ATRASO_DIAS = 5
# Dia do vencimento — a mesma convenção de scripts/seed-historico.js.
DIA_VENCIMENTO = 10

# Propensão latente a atrasar ALÉM do limiar, por unidade. Beta largo de
# propósito: os perfis se sobrepõem em vez de formar classes separáveis.
#
# A propensão governa o alvo diretamente, em um sorteio só. Empilhar dois
# sorteios — "atrasou?" e depois "quantos dias?" — somava duas camadas de
# ruído e apagava a heterogeneidade entre unidades: o histórico deixava de
# predizer o futuro e o AUC caía para 0,54, ou seja, moeda ao ar.
#
# Os parâmetros do Beta foram escolhidos pelo TETO TEÓRICO que implicam, não
# por tentativa. Dado que o alvo é Bernoulli(propensão), existe um AUC máximo
# alcançável mesmo por um modelo que conhecesse a propensão exata; e o
# estimador realista — a taxa de atraso dos 12 meses anteriores — fica um pouco
# abaixo dele. Medido por simulação:
#
#   Beta(0.95, 3.0) → teto 0,72 · janela de 12 meses 0,68
#   Beta(0.50, 1.3) → teto 0,80 · janela de 12 meses 0,77
#
# Com o primeiro, o gerador era o limitador: nenhuma engenharia de feature
# passaria de 0,72. O segundo põe o teto em 0,80 e deixa o alvo de 0,72–0,82
# ao alcance, que é a faixa que a proposta chamou de honesta. Dispersão alta
# com média baixa significa perfis sobrepostos e bem separados nas pontas — é
# o que se vê num condomínio real.
BETA_PROPENSAO = (0.5, 1.3)
# Atraso leve, abaixo do limiar: existe para o histórico não ser binário.
PROB_ATRASO_LEVE = 0.18
# Magnitude do atraso grave: LIMIAR + LogNormal. Não decide o rótulo — a
# propensão já decidiu — mas a média POR UNIDADE acompanha a propensão:
# quem atrasa mais também atrasa por mais tempo. Sem esse acoplamento a
# severidade não carregava informação nenhuma, e o modelo atribuía
# coeficiente negativo a "atraso médio alto", o que é indefensável numa
# explicação. O modelo estava certo; o gerador é que era irreal.
MU_MAGNITUDE = 1.0
SIGMA_MAGNITUDE = 0.9
ACOPLA_MAGNITUDE = 1.6

# Choque idiossincrático: atraso sem relação com o histórico da unidade.
# É o teto natural de acerto, e o argumento ético de que desemprego e doença
# não se preveem a partir de boleto pago.
PROB_CHOQUE = 0.07
# Mudança de regime no meio da série — é o que dá valor à feature de tendência.
PROB_DERIVA = 0.15
# Inversão de rótulo: erro de medição do mundo real.
RUIDO_ROTULO = 0.04
# Fatura que simplesmente não foi paga até hoje.
PROB_EM_ABERTO = 0.07

# Unidades novas: entram no condomínio no fim da série e não acumulam
# histórico. Existem para o modelo ter de recusar a previsão — sem elas, o
# caminho de "sem histórico suficiente" nunca é exercitado, e a regra vira
# código morto que ninguém testou.
PROP_UNIDADES_NOVAS = 0.05

# Locação
PROP_ALUGADAS = 0.45
PROB_TROCA_INQUILINO = 0.35
ALUGUEL_MIN_CENTAVOS = 120_000
ALUGUEL_MAX_CENTAVOS = 450_000

TAXA_BASE_CENTAVOS = 38_000
TAXA_DESVIO_CENTAVOS = 9_000


def competencias(primeira: str, n: int) -> list[date]:
    """n competências consecutivas, sempre no dia 1 (CHECK da tabela faturas)."""
    ano, mes = (int(p) for p in primeira.split("-"))
    saida = []
    for _ in range(n):
        saida.append(date(ano, mes, 1))
        mes += 1
        if mes == 13:
            ano, mes = ano + 1, 1
    return saida


def _soma_meses(d: date, meses: int) -> date:
    ano, mes = d.year, d.month + meses
    ano += (mes - 1) // 12
    mes = (mes - 1) % 12 + 1
    return date(ano, mes, 1)


def gerar(seed: int = SEED, nivel_ruido: float = NIVEL_RUIDO) -> tuple[pd.DataFrame, pd.DataFrame]:
    """Devolve (faturas, contratos) já no formato das tabelas reais."""
    rng = np.random.default_rng(seed)
    comps = competencias(PRIMEIRA_COMPETENCIA, N_COMPETENCIAS)
    hoje = _soma_meses(comps[-1], 1)  # a série termina no mês passado

    unidades = [str(uuid.UUID(bytes=rng.bytes(16))) for _ in range(N_UNIDADES)]

    # --- propensão latente, contínua e sobreposta -------------------------
    propensao = rng.beta(BETA_PROPENSAO[0], BETA_PROPENSAO[1], N_UNIDADES)
    # Severidade típica da unidade, acoplada à propensão.
    mu_magnitude = (
        MU_MAGNITUDE
        + ACOPLA_MAGNITUDE * (propensao - propensao.mean())
        + rng.normal(0, 0.25, N_UNIDADES)
    )
    # Deriva: parte das unidades muda de comportamento no meio da série.
    tem_deriva = rng.random(N_UNIDADES) < PROB_DERIVA
    deriva_inicio = rng.integers(N_COMPETENCIAS // 3, N_COMPETENCIAS, N_UNIDADES)
    deriva_delta = rng.normal(0, 0.28, N_UNIDADES)

    # Quais unidades só passam a existir perto do fim da série.
    nova = rng.random(N_UNIDADES) < PROP_UNIDADES_NOVAS
    primeira_competencia = np.zeros(N_UNIDADES, dtype=int)
    primeira_competencia[nova] = N_COMPETENCIAS - rng.integers(
        2, 5, int(nova.sum())
    )

    taxa_unidade = np.clip(
        rng.normal(TAXA_BASE_CENTAVOS, TAXA_DESVIO_CENTAVOS, N_UNIDADES), 15_000, None
    ).astype(np.int64)

    # --- contratos de locação --------------------------------------------
    contratos: list[dict] = []
    alugada = rng.random(N_UNIDADES) < PROP_ALUGADAS
    for i, unidade in enumerate(unidades):
        proprietario = 1000 + i
        if not alugada[i]:
            continue
        inicio = comps[int(rng.integers(0, max(1, N_COMPETENCIAS // 2)))]
        inquilino = 5000 + i * 2
        troca = rng.random() < PROB_TROCA_INQUILINO
        fim = _soma_meses(inicio, int(rng.integers(10, 18))) if troca else None
        contratos.append(
            {
                "id": str(uuid.UUID(bytes=rng.bytes(16))),
                "condominio_id": CONDOMINIO_ID,
                "unidade_id": unidade,
                "proprietario_usuario_id": proprietario,
                "inquilino_usuario_id": inquilino,
                "inicio": inicio.isoformat(),
                "fim": fim.isoformat() if fim else None,
                # Está no esquema real e é gerado aqui de propósito: o dado
                # existe, e a decisão de NÃO usá-lo como feature é ética, não
                # técnica. Ver testes/test_etica.py.
                "valor_aluguel_centavos": int(
                    rng.integers(ALUGUEL_MIN_CENTAVOS, ALUGUEL_MAX_CENTAVOS)
                ),
                "responsavel_taxas": "INQUILINO" if rng.random() < 0.75 else "PROPRIETARIO",
                "ativo": not troca,
            }
        )
        if troca:
            # Sucessão: entra outro inquilino, e o responsável pode mudar.
            contratos.append(
                {
                    "id": str(uuid.UUID(bytes=rng.bytes(16))),
                    "condominio_id": CONDOMINIO_ID,
                    "unidade_id": unidade,
                    "proprietario_usuario_id": proprietario,
                    "inquilino_usuario_id": inquilino + 1,
                    "inicio": _soma_meses(fim, 1).isoformat(),
                    "fim": None,
                    "valor_aluguel_centavos": int(
                        rng.integers(ALUGUEL_MIN_CENTAVOS, ALUGUEL_MAX_CENTAVOS)
                    ),
                    "responsavel_taxas": "INQUILINO" if rng.random() < 0.6 else "PROPRIETARIO",
                    "ativo": True,
                }
            )

    df_contratos = pd.DataFrame(contratos)

    # --- faturas ----------------------------------------------------------
    linhas: list[dict] = []
    for i, unidade in enumerate(unidades):
        for k, comp in enumerate(comps):
            if k < primeira_competencia[i]:
                continue  # a unidade ainda não existia
            p = propensao[i]
            if tem_deriva[i] and k >= deriva_inicio[i]:
                p = float(np.clip(p + deriva_delta[i], 0.02, 0.95))

            vencimento = date(comp.year, comp.month, DIA_VENCIMENTO)
            valor = int(taxa_unidade[i] + rng.normal(0, 2_500))
            valor = max(valor, 10_000)

            choque = rng.random() < PROB_CHOQUE * nivel_ruido
            sorteio = rng.random()
            if sorteio < p or choque:
                # Atraso grave: acima do limiar. É este o evento que o modelo
                # tenta prever.
                extra = float(rng.lognormal(mu_magnitude[i], SIGMA_MAGNITUDE * nivel_ruido))
                dias = min(LIMIAR_ATRASO_DIAS + 1.0 + extra, 150.0)
            elif sorteio < p + PROB_ATRASO_LEVE:
                # Atrasou, mas dentro da tolerância: não é alvo.
                dias = float(rng.integers(1, LIMIAR_ATRASO_DIAS + 1))
            else:
                # Pagou em dia ou adiantado.
                dias = -float(rng.integers(0, 6))

            em_aberto = rng.random() < PROB_EM_ABERTO and dias > 0
            if em_aberto:
                status = "EM_ATRASO" if vencimento < hoje else "ABERTA"
                pago_em = None
                valor_pago = None
                forma = None
            else:
                status = "PAGA"
                pago_em = vencimento + timedelta(days=int(round(dias)))
                valor_pago = valor
                forma = "GATEWAY" if rng.random() < 0.8 else "MANUAL"

            linhas.append(
                {
                    "id": str(uuid.UUID(bytes=rng.bytes(16))),
                    "condominio_id": CONDOMINIO_ID,
                    "unidade_id": unidade,
                    "competencia": comp.isoformat(),
                    "vencimento": vencimento.isoformat(),
                    "valor_centavos": valor,
                    "status": status,
                    "pago_em": pago_em.isoformat() if pago_em else None,
                    "valor_pago_centavos": valor_pago,
                    "forma_baixa": forma,
                    # Multa acompanha o atraso: é o que a convenção do
                    # condomínio prevê. Gerá-la com probabilidade fixa fazia a
                    # feature não informar nada.
                    "tem_multa": bool(
                        rng.random() < (0.72 if dias > LIMIAR_ATRASO_DIAS else 0.03)
                    ),
                }
            )

    df_faturas = pd.DataFrame(linhas)

    # Ruído de rótulo: a realidade também erra de medição. Aplicado deslocando
    # o pagamento em torno do limiar, não mexendo num alvo pré-calculado — o
    # alvo é derivado depois, em features.rotular().
    pagas = df_faturas.index[df_faturas["status"] == "PAGA"]
    sorteadas = rng.choice(pagas, size=int(len(pagas) * RUIDO_ROTULO * nivel_ruido), replace=False)
    for idx in sorteadas:
        venc = date.fromisoformat(df_faturas.at[idx, "vencimento"])
        atual = (date.fromisoformat(df_faturas.at[idx, "pago_em"]) - venc).days
        novo = LIMIAR_ATRASO_DIAS + 1 if atual <= LIMIAR_ATRASO_DIAS else LIMIAR_ATRASO_DIAS - 1
        df_faturas.at[idx, "pago_em"] = (venc + timedelta(days=novo)).isoformat()

    return df_faturas, df_contratos


if __name__ == "__main__":
    import pathlib

    faturas, contratos = gerar()
    destino = pathlib.Path(__file__).parent / "dados"
    destino.mkdir(exist_ok=True)
    faturas.to_csv(destino / "faturas.csv", index=False)
    contratos.to_csv(destino / "contratos.csv", index=False)

    atraso = (
        pd.to_datetime(faturas["pago_em"]) - pd.to_datetime(faturas["vencimento"])
    ).dt.days
    taxa = float((atraso > LIMIAR_ATRASO_DIAS).mean())

    print(f"faturas          {len(faturas):>6}")
    print(f"contratos        {len(contratos):>6}")
    print(f"unidades         {faturas['unidade_id'].nunique():>6}")
    print(f"competências     {faturas['competencia'].nunique():>6}")
    print(f"em aberto        {(faturas['status'] != 'PAGA').sum():>6}")
    print(f"taxa de atraso   {taxa:>6.1%}  (alvo 20–30%)")
