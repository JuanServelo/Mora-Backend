"""Timeout no modelo preferido não pode derrubar a chamada.

Medido em 28/10: o mesmo gemini-3.6-flash respondeu em 3s e, minutos antes,
estourou os 25s. Antes desta guarda o timeout virava ErroGemini direto, e a
explicação caía no texto determinístico com outros modelos livres. Rede
simulada: nenhum teste aqui toca a API de verdade.
"""

import json

import httpx
import pytest

import gemini

SCHEMA = {"type": "OBJECT", "properties": {"soma": {"type": "INTEGER"}}, "required": ["soma"]}


@pytest.fixture
def rede(monkeypatch, tmp_path):
    """Isola o cliente: chave falsa, cache temporário, modelos A (lento) e B."""
    monkeypatch.setenv("GEMINI_API_KEY", "chave-falsa")
    monkeypatch.setenv("GEMINI_MODEL", "modelo-a")
    monkeypatch.setenv("IA_OFFLINE", "0")
    monkeypatch.setattr(gemini, "CACHE", tmp_path)
    monkeypatch.setattr(gemini, "candidatos", lambda: ["modelo-a", "modelo-b"])

    chamados = []

    def montar(comportamento):
        def tratar(pedido: httpx.Request) -> httpx.Response:
            modelo = pedido.url.path.split("/models/")[1].split(":")[0]
            chamados.append(modelo)
            acao = comportamento[modelo]
            if acao == "timeout":
                raise httpx.ReadTimeout("simulado", request=pedido)
            if acao == 503:
                return httpx.Response(503, json={"error": {"message": "high demand"}})
            texto = json.dumps({"soma": 42})
            return httpx.Response(200, json={"candidates": [{"content": {"parts": [{"text": texto}]}}]})

        original = httpx.Client
        monkeypatch.setattr(
            gemini.httpx, "Client",
            lambda **kw: original(transport=httpx.MockTransport(tratar), **kw),
        )

    return montar, chamados


def test_timeout_no_preferido_passa_ao_proximo(rede):
    montar, chamados = rede
    montar({"modelo-a": "timeout", "modelo-b": 200})
    assert gemini.gerar_json("x", "17+25", SCHEMA, rotulo="cascata") == {"soma": 42}
    assert chamados == ["modelo-a", "modelo-b"]


def test_503_continua_passando_ao_proximo(rede):
    montar, chamados = rede
    montar({"modelo-a": 503, "modelo-b": 200})
    assert gemini.gerar_json("x", "17+25", SCHEMA, rotulo="cascata") == {"soma": 42}


def test_todos_lentos_da_erro_explicito(rede):
    montar, _ = rede
    montar({"modelo-a": "timeout", "modelo-b": "timeout"})
    with pytest.raises(gemini.ErroGemini, match="nenhum modelo respondeu a tempo"):
        gemini.gerar_json("x", "17+25", SCHEMA, rotulo="cascata")
