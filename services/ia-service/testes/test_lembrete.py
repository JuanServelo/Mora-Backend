"""Lembrete preventivo: o texto do Gemini só sai se não revelar a previsão.

Nenhum teste toca a rede: `gemini.gerar_json` é substituído pelo texto que se
quer pôr à prova.
"""

import pytest

import gemini
import lembrete

VENC = "10/11/2026"


def test_texto_correto_passa():
    v = lembrete.verificar(
        "Lembrete: taxa condominial",
        f"Olá, Rafael! A taxa vence em {VENC}. O boleto e o PIX estão no aplicativo Mora.",
        VENC, "RAFAEL TEIXEIRA",
    )
    assert v == {"aprovado": True, "problemas": []}


@pytest.mark.parametrize("trecho, problema", [
    ("Nossa análise indica alto risco de atraso.", "risco"),
    ("Há 93% de chance de você não pagar.", "percentual"),
    ("Evite novas multas e juros.", "multa"),
    ("Seu histórico mostra pendências.", "historico"),
    ("Você está inadimplente.", "inadimpl"),
    ("O sistema de previsão marcou sua unidade.", "previs"),
])
def test_texto_que_revela_a_previsao_e_recusado(trecho, problema):
    v = lembrete.verificar("Lembrete", f"Olá, Rafael! {trecho} Vence em {VENC}.", VENC, "Rafael")
    assert not v["aprovado"]
    assert any(problema in p for p in v["problemas"])


def test_sem_vencimento_ou_com_outro_nome_e_recusado():
    assert not lembrete.verificar("Lembrete", "Olá, Rafael! Pague logo.", VENC, "Rafael")["aprovado"]
    outro = lembrete.verificar("Lembrete", f"Olá, Marina! Vence em {VENC}.", VENC, "Rafael Teixeira")
    assert "não trata Rafael pelo nome" in outro["problemas"]


def test_recusado_cai_no_texto_fixo_e_guarda_o_recusado(monkeypatch):
    ruim = {"titulo": "Atenção", "mensagem": f"Rafael, seu risco de atraso é alto. Vence em {VENC}."}
    monkeypatch.setattr(gemini, "gerar_json", lambda *a, **k: ruim)
    r = lembrete.gerar("Rafael Teixeira", "INQUILINO", "A-302", "2026-11-01", "2026-11-10")
    assert r["fonte"] == "fixo"
    assert r["recusado"] == ruim
    assert VENC in r["mensagem"] and "Rafael" in r["mensagem"]
    assert lembrete.verificar(r["titulo"], r["mensagem"], VENC, "Rafael")["aprovado"], \
        "o texto fixo precisa passar na mesma regra"


def test_sem_rede_sai_o_texto_fixo(monkeypatch):
    def falha(*a, **k):
        raise gemini.ErroGemini("sem rede")
    monkeypatch.setattr(gemini, "gerar_json", falha)
    r = lembrete.gerar("Eduardo Prado", "PROPRIETÁRIO", "A-507", "2026-11-01", "2026-11-10")
    assert r["fonte"] == "fixo" and r["motivo"] == "sem rede"


def test_entrada_do_llm_nao_leva_score_nem_historico(monkeypatch):
    """O que o modelo não recebe ele não pode vazar."""
    recebido = {}

    def captura(instrucao, entrada, schema, rotulo):
        recebido["entrada"] = entrada
        return {"titulo": "Lembrete", "mensagem": f"Olá, Rafael! Vence em {VENC}."}

    monkeypatch.setattr(gemini, "gerar_json", captura)
    lembrete.gerar("Rafael Teixeira", "INQUILINO", "A-302", "2026-11-01", "2026-11-10")
    assert "%" not in recebido["entrada"]
    for termo in ("score", "risco", "atraso", "multa"):
        assert termo not in recebido["entrada"].lower()


def test_envio_sem_configuracao_nao_lanca(monkeypatch):
    for var in ("SERVICO_TOKEN", "IA_DEMO_USUARIO_ID", "IA_DEMO_CONDOMINIO_ID"):
        monkeypatch.delenv(var, raising=False)
    r = lembrete.enviar("t", "m", "x")
    assert r["enviado"] is False and "não configurado" in r["motivo"]
