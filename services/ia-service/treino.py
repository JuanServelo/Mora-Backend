"""Treino com validação temporal e auditoria de viés.

Duas decisões que a proposta já havia assumido e que aqui se cumprem:

**Validação temporal, não split aleatório.** Treina nas competências antigas,
testa nas recentes. Split aleatório vaza futuro para o treino e infla a
métrica — é o erro que a banca sabe perguntar. O `StandardScaler` é ajustado
só no treino, pela mesma razão.

**A Regressão Logística é o modelo publicado**, e o Gradient Boosting entra
como comparação de métrica. O motivo está na proposta: "não adianta ganhar 3
pontos de AUC e perder a capacidade de explicar a decisão a um morador". Num
modelo aditivo a contribuição de cada feature é **exata** —
`coef_i × valor_padronizado_i`, em log-odds — e a soma das contribuições mais o
intercepto dá exatamente o logit do score. Dá para verificar na frente da banca
(ver `explicar()`), o que uma aproximação de modelo de árvore não permite.

As métricas saem separadas para unidades próprias e alugadas. Se o modelo erra
mais com inquilinos, isso tem de aparecer, não ser diluído na média — é
auditoria de viés de verdade, não parágrafo no documento.
"""

from __future__ import annotations

import json
import pathlib

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.inspection import permutation_importance
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.preprocessing import StandardScaler

import features
from features import FEATURES

AQUI = pathlib.Path(__file__).parent
# Proporção das competências usada para treino; o resto é o futuro.
FRACAO_TREINO = 0.75


def _metricas(y, p, limiar: float = 0.5) -> dict:
    pred = (p >= limiar).astype(int)
    tn, fp, fn, tp = confusion_matrix(y, pred, labels=[0, 1]).ravel()
    return {
        "auc_roc": round(float(roc_auc_score(y, p)), 4),
        "precision": round(float(precision_score(y, pred, zero_division=0)), 4),
        "recall": round(float(recall_score(y, pred, zero_division=0)), 4),
        "f1": round(float(f1_score(y, pred, zero_division=0)), 4),
        "matriz_confusao": {
            "verdadeiro_negativo": int(tn),
            "falso_positivo": int(fp),
            "falso_negativo": int(fn),
            "verdadeiro_positivo": int(tp),
        },
        "n": int(len(y)),
        "taxa_positivos": round(float(y.mean()), 4),
    }


def treinar() -> dict:
    faturas = pd.read_csv(AQUI / "dados" / "faturas.csv")
    contratos = pd.read_csv(AQUI / "dados" / "contratos.csv")

    X, y, meta = features.montar_matriz(faturas, contratos)
    if X.size == 0:
        raise SystemExit("matriz vazia — rode `python gerador.py` primeiro")

    # --- corte temporal ---------------------------------------------------
    comps = np.sort(meta["competencia"].unique())
    corte = comps[int(len(comps) * FRACAO_TREINO)]
    treino = (meta["competencia"] < corte).to_numpy()
    teste = ~treino

    X_tr, y_tr = X[treino], y[treino]
    X_te, y_te = X[teste], y[teste]

    escalador = StandardScaler().fit(X_tr)  # fit SÓ no treino
    X_tr_s, X_te_s = escalador.transform(X_tr), escalador.transform(X_te)

    # --- modelos ----------------------------------------------------------
    logreg = LogisticRegression(max_iter=2000, class_weight="balanced").fit(X_tr_s, y_tr)
    gb = GradientBoostingClassifier(random_state=42).fit(X_tr_s, y_tr)

    p_lr = logreg.predict_proba(X_te_s)[:, 1]
    p_gb = gb.predict_proba(X_te_s)[:, 1]

    # --- auditoria de viés por tipo de ocupação ---------------------------
    alugada = meta.loc[teste, "alugada"].to_numpy() == 1.0
    segmentado = {}
    for nome, mascara in (("alugadas", alugada), ("proprias", ~alugada)):
        if mascara.sum() > 10 and len(np.unique(y_te[mascara])) > 1:
            segmentado[nome] = _metricas(y_te[mascara], p_lr[mascara])

    # --- importância global (no teste, não no treino) ---------------------
    imp = permutation_importance(
        logreg, X_te_s, y_te, n_repeats=10, random_state=42, scoring="roc_auc"
    )
    importancia = sorted(
        ({"feature": f, "importancia": round(float(v), 5)} for f, v in zip(FEATURES, imp.importances_mean)),
        key=lambda d: -d["importancia"],
    )

    relatorio = {
        "competencias_treino": [str(pd.Timestamp(c).date()) for c in comps if c < corte],
        "competencias_teste": [str(pd.Timestamp(c).date()) for c in comps if c >= corte],
        "n_treino": int(treino.sum()),
        "n_teste": int(teste.sum()),
        "features": FEATURES,
        "features_excluidas_por_etica": features.FEATURES_PROIBIDAS,
        "modelo_publicado": "regressao_logistica",
        "modelos": {
            "regressao_logistica": _metricas(y_te, p_lr),
            "gradient_boosting": _metricas(y_te, p_gb),
        },
        "auditoria_vies_por_ocupacao": segmentado,
        "importancia_permutacao": importancia,
    }

    (AQUI / "artefatos").mkdir(exist_ok=True)
    joblib.dump(
        {
            "modelo": logreg,
            "escalador": escalador,
            "features": FEATURES,
            "limiar": 0.5,
        },
        AQUI / "artefatos" / "modelo.joblib",
    )
    (AQUI / "artefatos" / "metricas.json").write_text(
        json.dumps(relatorio, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    return relatorio


def explicar(x: np.ndarray, artefato: dict) -> list[dict]:
    """Contribuições exatas, em log-odds, da predição de um único caso.

    `soma(contribuicoes) + intercepto == logit(p)`. É a decomposição que um
    modelo aditivo dá de graça e que uma aproximação de árvore não garante.
    """
    modelo, escalador = artefato["modelo"], artefato["escalador"]
    xs = escalador.transform(x.reshape(1, -1))[0]
    coef = modelo.coef_[0]
    contribs = coef * xs
    return sorted(
        (
            {
                "feature": f,
                "valor": round(float(v), 4),
                "contribuicao_log_odds": round(float(c), 4),
            }
            for f, v, c in zip(artefato["features"], x, contribs)
        ),
        key=lambda d: -abs(d["contribuicao_log_odds"]),
    )


if __name__ == "__main__":
    r = treinar()
    print(f"\ntreino: {r['n_treino']} linhas · teste: {r['n_teste']} linhas")
    print(f"competências de teste: {r['competencias_teste'][0]} .. {r['competencias_teste'][-1]}\n")
    for nome, m in r["modelos"].items():
        marca = "  <- publicado" if nome == r["modelo_publicado"] else ""
        print(
            f"{nome:<22} AUC {m['auc_roc']:.3f}  precision {m['precision']:.3f}  "
            f"recall {m['recall']:.3f}  F1 {m['f1']:.3f}{marca}"
        )
    print("\nauditoria de viés (modelo publicado):")
    for seg, m in r["auditoria_vies_por_ocupacao"].items():
        print(f"  {seg:<10} AUC {m['auc_roc']:.3f}  n={m['n']}  positivos {m['taxa_positivos']:.1%}")
    print("\ntop features (importância por permutação, no teste):")
    for d in r["importancia_permutacao"][:6]:
        print(f"  {d['feature']:<28} {d['importancia']:+.4f}")
