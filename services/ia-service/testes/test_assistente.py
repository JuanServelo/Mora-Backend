"""Assistente do síndico: o laço de tool calling, a ética e a fidelidade.

A rede é simulada: o "Gemini" aqui pede uma função, recebe o resultado e
responde. Os resultados das funções são os de verdade, do modelo treinado.
"""

import json

import httpx
import pytest

import assistente
import gemini


def test_nenhuma_ferramenta_devolve_valor_de_aluguel():
    """A guarda ética do treino vale também para o que o LLM lê."""
    saidas = [
        assistente.resumo_do_condominio(),
        assistente.unidades_em_risco(limite=20),
        assistente.risco_da_unidade("A-302"),
        assistente.contrato_da_unidade("A-302"),
        assistente.contrato_da_unidade("A-507"),
        assistente.desempenho_do_modelo(),
    ]
    bruto = json.dumps(saidas, ensure_ascii=False).lower()
    assert "aluguel" not in bruto and "valor_aluguel" not in bruto


def test_rotulos_estaveis_e_unidade_desconhecida_vira_erro_para_o_modelo():
    assert assistente.risco_da_unidade("a-302")["unidade"] == "A-302"
    r = assistente.executar("risco_da_unidade", {"unidade": "Z-999"})
    assert "não encontrada" in r["erro"]
    assert "desconhecida" in assistente.executar("apagar_tudo", {})["erro"]


def test_percentual_inventado_e_recusado():
    chamadas = [{"ferramenta": "risco_da_unidade", "argumentos": {},
                 "resultado": {"risco": "93%", "historico": {"pagas_em_dia_12m": "0%"}}}]
    assert assistente.verificar("A A-302 tem 93% de risco.", chamadas)["fiel"]
    v = assistente.verificar("A A-302 tem 97% de risco.", chamadas)
    assert not v["fiel"] and v["sem_origem"] == [97.0]


def test_resposta_infiel_vira_os_dados_consultados(monkeypatch):
    chamadas = [{"ferramenta": "resumo_do_condominio", "argumentos": {}, "resultado": {"em_risco_alto": 30}}]
    monkeypatch.setattr(gemini, "conversar", lambda *a, **k: {
        "resposta": "Cerca de 45% das unidades estão em risco.", "chamadas": chamadas, "modelo": "m"})
    r = assistente.perguntar("quantas em risco?")
    assert r["fonte"] == "dados"
    assert "em_risco_alto" in r["resposta"] and r["recusada"].startswith("Cerca de 45%")


@pytest.fixture
def gemini_simulado(monkeypatch, tmp_path):
    """Primeira volta pede `risco_da_unidade`; a segunda responde com o dado."""
    monkeypatch.setenv("GEMINI_API_KEY", "chave-falsa")
    monkeypatch.setenv("GEMINI_MODEL", "modelo-a")
    monkeypatch.setenv("IA_OFFLINE", "0")
    monkeypatch.setattr(gemini, "CACHE", tmp_path)
    pedidos = []

    def tratar(req: httpx.Request) -> httpx.Response:
        corpo = json.loads(req.content)
        pedidos.append(corpo)
        if len(pedidos) == 1:
            partes = [{"functionCall": {"id": "c1", "name": "risco_da_unidade", "args": {"unidade": "A-302"}},
                       "thoughtSignature": "assinatura-opaca"}]
        else:
            resultado = corpo["contents"][-1]["parts"][0]["functionResponse"]["response"]["resultado"]
            partes = [{"text": f"A A-302 tem {resultado['risco']} de risco."}]
        return httpx.Response(200, json={"candidates": [{"content": {"role": "model", "parts": partes}}]})

    original = httpx.Client
    monkeypatch.setattr(gemini.httpx, "Client", lambda **kw: original(transport=httpx.MockTransport(tratar), **kw))
    return pedidos


def test_laco_de_ferramentas(gemini_simulado):
    r = assistente.perguntar("Qual o risco da A-302?")
    pedidos = gemini_simulado

    assert r["fonte"] == "gemini" and r["verificacao"]["fiel"]
    assert [c["ferramenta"] for c in r["chamadas"]] == ["risco_da_unidade"]
    assert r["resposta"] == f"A A-302 tem {assistente.risco_da_unidade('A-302')['risco']} de risco."

    segunda = pedidos[1]["contents"]
    # A volta do modelo entra no histórico como veio, com a assinatura.
    assert segunda[1]["parts"][0]["thoughtSignature"] == "assinatura-opaca"
    # E o resultado volta amarrado ao id da chamada.
    assert segunda[2]["parts"][0]["functionResponse"]["id"] == "c1"
    assert "functionDeclarations" in pedidos[0]["tools"][0]


def test_offline_reproduz_a_conversa_gravada(gemini_simulado, monkeypatch):
    online = assistente.perguntar("Qual o risco da A-302?")
    monkeypatch.setenv("IA_OFFLINE", "1")
    assert assistente.perguntar("  qual o risco da a-302?  ")["resposta"] == online["resposta"]
    assert assistente.perguntar("outra pergunta")["fonte"] == "indisponivel"
