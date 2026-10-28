"""A restrição ética como código verificável, não como promessa de documento.

A proposta declara que o modelo não usa indicador socioeconômico: o valor do
aluguel está no banco (`contratos_locacao.valor_aluguel_centavos`), é fácil de
ler e provavelmente melhoraria a métrica — e fica fora de propósito. Usá-lo
seria treinar um modelo que prevê atraso a partir de quanto a pessoa pode pagar
de moradia, que é classificação de renda com outro nome.

Documento não impede ninguém de acrescentar a coluna em uma sexta-feira à
tarde. Estes testes impedem: quebram a build.
"""

from __future__ import annotations

import pathlib
import sys

import joblib

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))

import features  # noqa: E402

AQUI = pathlib.Path(__file__).resolve().parents[1]

# Termos que caracterizam proxy socioeconômico. Qualquer feature cujo nome os
# contenha é recusada.
TERMOS_PROIBIDOS = ("aluguel", "renda", "salario", "patrimonio", "cpf", "score_credito")


def test_nenhuma_feature_e_proxy_socioeconomico():
    infratoras = [
        f for f in features.FEATURES if any(t in f.lower() for t in TERMOS_PROIBIDOS)
    ]
    assert not infratoras, (
        f"features de proxy socioeconômico em FEATURES: {infratoras}. "
        "Ver proposta.md, seção 3.4 — a exclusão é decisão ética, não esquecimento."
    )


def test_lista_explicita_de_proibidas_fora_das_features():
    for proibida in features.FEATURES_PROIBIDAS:
        assert proibida not in features.FEATURES, f"{proibida} não pode ser feature"


def test_modelo_treinado_nao_carrega_feature_proibida():
    """A guarda vale para o artefato publicado, não só para o código."""
    caminho = AQUI / "artefatos" / "modelo.joblib"
    if not caminho.exists():
        import pytest

        pytest.skip("modelo não treinado; rode `python treino.py`")
    artefato = joblib.load(caminho)
    infratoras = [
        f for f in artefato["features"] if any(t in f.lower() for t in TERMOS_PROIBIDOS)
    ]
    assert not infratoras, f"o modelo salvo usa feature proibida: {infratoras}"


def test_a_guarda_realmente_pega():
    """Prova que o teste detecta a violação, em vez de passar por vacuidade.

    Um teste de proibição que nunca viu uma violação não prova nada. Aqui a
    violação é simulada e a guarda tem de reprovar.
    """
    lista_contaminada = features.FEATURES + ["valor_aluguel_centavos"]
    infratoras = [
        f for f in lista_contaminada if any(t in f.lower() for t in TERMOS_PROIBIDOS)
    ]
    assert infratoras == ["valor_aluguel_centavos"], "a guarda não detectaria a violação"


def test_dado_proibido_existe_na_base():
    """O dado está lá: a exclusão é escolha, não indisponibilidade.

    Este é o teste que dá sentido aos outros. Se a coluna não existisse, não
    haveria decisão ética nenhuma — só ausência de dado.
    """
    import pandas as pd

    caminho = AQUI / "dados" / "contratos.csv"
    if not caminho.exists():
        import pytest

        pytest.skip("dados não gerados; rode `python gerador.py`")
    contratos = pd.read_csv(caminho)
    assert "valor_aluguel_centavos" in contratos.columns
    assert contratos["valor_aluguel_centavos"].notna().any()
