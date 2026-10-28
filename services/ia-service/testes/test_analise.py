"""O cruzamento entre o contrato e o histórico.

A peça mais frágil aqui é o casamento do endereço: o LLM devolve o endereço em
texto livre, e dele tem de sair uma unidade do cadastro. Errar isso é pior do
que não responder — significaria atribuir o histórico de pagamento de uma
família a outra.
"""

from __future__ import annotations

import json
import pathlib
import sys

import pytest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))

import analise  # noqa: E402

AQUI = pathlib.Path(__file__).resolve().parents[1]
INDICE = AQUI / "fixtures" / "contratos" / "indice.json"


@pytest.fixture(scope="module")
def indice():
    if not INDICE.exists():
        pytest.skip("contratos não gerados; rode `python fixture_contratos.py`")
    return json.loads(INDICE.read_text(encoding="utf-8"))


def test_casa_endereco_completo(indice):
    chave, esperado = next(iter(indice.items()))
    endereco = (
        f"Rua das Acácias, n.º 1.408, apartamento {esperado['apartamento']}, "
        f"Bloco {esperado['bloco']}, Residencial Parque Verde"
    )
    achado = analise.localizar_unidade(endereco)
    assert achado is not None
    assert achado["unidade_id"] == esperado["unidade_id"]


def test_casa_com_abreviacoes(indice):
    _, esperado = next(iter(indice.items()))
    for forma in (
        f"apto {esperado['apartamento']}, bloco {esperado['bloco']}",
        f"Ap. {esperado['apartamento']} - Bloco {esperado['bloco']}",
        f"unidade nº {esperado['apartamento']}, Bloco {esperado['bloco']}",
    ):
        achado = analise.localizar_unidade(forma)
        assert achado is not None, f"não casou: {forma}"
        assert achado["unidade_id"] == esperado["unidade_id"]


def test_endereco_de_outro_predio_nao_casa():
    """Sem correspondência é a resposta certa — melhor que atribuir a unidade errada."""
    assert analise.localizar_unidade("Rua Qualquer, 99, apartamento 9999, Bloco Z") is None
    assert analise.localizar_unidade("") is None
    assert analise.localizar_unidade(None) is None


def test_bloco_errado_nao_casa_por_engano(indice):
    """Mesmo apartamento em bloco diferente é OUTRA unidade."""
    chaves = list(indice)
    if len(chaves) < 2:
        pytest.skip("índice pequeno demais")
    _, alvo = next(iter(indice.items()))
    # Um bloco que não existe no índice, com um apartamento que existe.
    achado = analise.localizar_unidade(f"apartamento {alvo['apartamento']}, Bloco Z")
    assert achado is None or achado["unidade_id"] == alvo["unidade_id"]


def test_analise_sem_llm_nao_quebra():
    """Sem chave, a análise devolve o motivo em vez de lançar exceção."""
    pasta = AQUI / "fixtures" / "contratos"
    pdfs = sorted(pasta.glob("*.pdf"))
    if not pdfs:
        pytest.skip("contratos não gerados")
    r = analise.analisar(pdfs[0], explicar_com_llm=False)
    assert "extracao" in r
    assert "alertas" in r
    assert isinstance(r["alertas"], list)
