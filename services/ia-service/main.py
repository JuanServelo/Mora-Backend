"""ia-service — o módulo de IA do Mora.

Primeiro serviço Python da malha. Segue as convenções dos demais: `/health`
para o Consul, prefixo de rota próprio (`/api/ia`), configuração por variável de
ambiente com fallback local.

Entrada → processamento → saída, que é o requisito de maior peso do trabalho:
  entrada        histórico de faturas e contratos (CSV sintético) e o PDF do contrato
  processamento  features com janela temporal fechada, modelo publicado, LLM
  saída          painel de risco, explicação em português, cadastro pré-preenchido
"""

from __future__ import annotations

import json
import os
import pathlib
import tempfile

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field

import analise as mod_analise
import assistente as mod_assistente
import contrato as mod_contrato
import explicacao as mod_explicacao
import lembrete as mod_lembrete
import predicao

AQUI = pathlib.Path(__file__).parent

# Carrega o .env sem depender de python-dotenv.
_env = AQUI / ".env"
if _env.exists():
    for linha in _env.read_text(encoding="utf-8").splitlines():
        linha = linha.strip()
        if linha and not linha.startswith("#") and "=" in linha:
            k, v = linha.split("=", 1)
            os.environ.setdefault(k.strip(), v.strip())

app = FastAPI(
    title="Mora Inteligente — ia-service",
    description="Previsão de inadimplência com explicação, e leitura de contratos de locação.",
    version="0.1.0",
)


class PedidoLembrete(BaseModel):
    contrato: str = Field(..., description="PDF de demonstração, ex.: 01-risco-alto-inquilino.pdf")
    enviar: bool = Field(False, description="publica no sino do Mora pelo comunicacao-service")


class PedidoAssistente(BaseModel):
    pergunta: str = Field(..., description="pergunta do síndico, em português", max_length=500)


class PedidoPredicao(BaseModel):
    unidade_id: str = Field(..., description="UUID da unidade")
    explicar_com_llm: bool = Field(True, description="gera o parágrafo com o Gemini")


@app.get("/health")
def health():
    try:
        ctx = predicao.carregar()
        pronto = True
        unidades = len(ctx["unidades"])
    except predicao.SemModelo:
        pronto, unidades = False, 0
    return {
        "servico": "ia-service",
        "modelo_carregado": pronto,
        "unidades": unidades,
        "modo_offline": os.environ.get("IA_OFFLINE", "0") in ("1", "true", "sim"),
    }


@app.get("/api/ia/metricas")
def metricas():
    caminho = AQUI / "artefatos" / "metricas.json"
    if not caminho.exists():
        raise HTTPException(404, "métricas ausentes; rode `python treino.py`")
    return json.loads(caminho.read_text(encoding="utf-8"))


@app.get("/api/ia/painel")
def painel(limite: int = 40):
    """Unidades ordenadas por risco, para a competência que vem."""
    try:
        unidades = predicao.painel(limite)
        # O mesmo rótulo que o assistente usa: o síndico pergunta pelo que vê.
        for u in unidades:
            u["rotulo"] = mod_assistente.rotulo(u["unidade_id"])
        return {"competencia": str(predicao.carregar()["competencia_alvo"])[:10],
                "unidades": unidades}
    except predicao.SemModelo as e:
        raise HTTPException(503, str(e)) from e


@app.post("/api/ia/predicao")
def prever(pedido: PedidoPredicao):
    try:
        r = predicao.pontuar(pedido.unidade_id)
    except predicao.SemModelo as e:
        raise HTTPException(503, str(e)) from e

    if r["sem_previsao"]:
        return r

    if pedido.explicar_com_llm:
        r["explicacao"] = mod_explicacao.explicar(
            r["score"], r["contribuicoes"], r["features"].get("meses_historico")
        )
    else:
        r["explicacao"] = {
            "fonte": "deterministica",
            "explicacao": mod_explicacao.em_portugues_simples(r["score"], r["contribuicoes"]),
        }
    return r


async def _salvar_temporario(arquivo: UploadFile) -> str:
    if not (arquivo.filename or "").lower().endswith(".pdf"):
        raise HTTPException(415, "envie um PDF")
    bruto = await arquivo.read()
    with tempfile.NamedTemporaryFile(suffix=".pdf", delete=False) as tmp:
        tmp.write(bruto)
        return tmp.name


@app.post("/api/ia/contrato/extrair")
async def extrair_contrato(arquivo: UploadFile = File(...)):
    """PDF do contrato → campos para revisão humana, com citação do trecho."""
    caminho = await _salvar_temporario(arquivo)
    try:
        return mod_contrato.extrair(caminho)
    finally:
        pathlib.Path(caminho).unlink(missing_ok=True)


@app.post("/api/ia/contrato/analisar")
async def analisar_contrato(arquivo: UploadFile = File(...)):
    """O fluxo completo: contrato × histórico → risco e destinatário da cobrança.

    É aqui que as duas metades do trabalho se encontram. O LLM lê quem responde
    pelas taxas; o modelo lê o histórico de pagamento da unidade; e o resultado
    diz não só QUANTO é o risco, mas PARA QUEM a cobrança deve sair.
    """
    caminho = await _salvar_temporario(arquivo)
    try:
        r = mod_analise.analisar(caminho)
        r["arquivo"] = arquivo.filename
        return r
    finally:
        pathlib.Path(caminho).unlink(missing_ok=True)


@app.get("/api/ia/contratos")
def listar_contratos():
    """Os contratos de demonstração disponíveis, para o painel oferecer."""
    indice = AQUI / "fixtures" / "contratos" / "indice.json"
    if not indice.exists():
        return {"contratos": []}
    dados = json.loads(indice.read_text(encoding="utf-8"))
    return {
        "contratos": sorted(
            (
                {
                    "arquivo": v["arquivo"],
                    "bloco": v["bloco"],
                    "apartamento": v["apartamento"],
                    "url": f"/api/ia/contratos/{v['arquivo']}",
                }
                for v in dados.values()
            ),
            key=lambda d: d["arquivo"],
        )
    }


@app.get("/api/ia/contratos/{nome}")
def baixar_contrato(nome: str):
    """Serve um PDF de demonstração — permite analisar sem escolher arquivo."""
    if "/" in nome or "\\" in nome or not nome.endswith(".pdf"):
        raise HTTPException(400, "nome inválido")
    caminho = AQUI / "fixtures" / "contratos" / nome
    if not caminho.exists():
        raise HTTPException(404, "contrato não encontrado")
    return FileResponse(caminho, media_type="application/pdf", filename=nome)


@app.post("/api/ia/contrato/analisar-exemplo")
def analisar_exemplo(nome: str):
    """Analisa um dos contratos de demonstração, pelo nome do arquivo."""
    if "/" in nome or "\\" in nome or not nome.endswith(".pdf"):
        raise HTTPException(400, "nome inválido")
    caminho = AQUI / "fixtures" / "contratos" / nome
    if not caminho.exists():
        raise HTTPException(404, "contrato não encontrado")
    return mod_analise.analisar(caminho)


@app.post("/api/ia/lembrete")
def lembrete(pedido: PedidoLembrete):
    """Contrato × risco → lembrete escrito pelo Gemini para quem paga, e opcionalmente enviado.

    O lembrete não revela o risco: o texto que falar de previsão, histórico ou
    multa é recusado e sai o modelo fixo.
    """
    nome = pedido.contrato
    if "/" in nome or "\\" in nome or not nome.endswith(".pdf"):
        raise HTTPException(400, "nome inválido")
    caminho = AQUI / "fixtures" / "contratos" / nome
    if not caminho.exists():
        raise HTTPException(404, "contrato não encontrado")

    a = mod_analise.analisar(caminho, explicar_com_llm=False)
    risco, dest, unidade = a.get("risco"), a.get("destinatario"), a.get("unidade")
    if not (dest and unidade):
        raise HTTPException(409, "sem destinatário ou unidade: " + "; ".join(a["alertas"]))
    if not risco or risco.get("sem_previsao"):
        raise HTTPException(409, "unidade sem previsão: não há base para um lembrete preventivo")

    competencia = risco["competencia"]
    vencimento = mod_lembrete.vencimento_da_competencia(unidade["unidade_id"], competencia)
    rotulo = f"{unidade['bloco']}-{unidade['apartamento']}"
    texto = mod_lembrete.gerar(dest["nome"], dest["papel"], rotulo, competencia, vencimento)
    saida = {
        "contrato": nome,
        "unidade": rotulo,
        "destinatario": dest,
        "competencia": competencia,
        "vencimento": vencimento,
        # O lembrete preventivo é para quem o modelo marcou; os demais recebem
        # só a cobrança normal. A decisão é exposta, não escondida.
        "recomendado": risco["score"] >= 0.5,
        "lembrete": texto,
        "envio": None,
    }
    if pedido.enviar:
        saida["envio"] = mod_lembrete.enviar(
            texto["titulo"], texto["mensagem"], f"{unidade['unidade_id']}:{competencia}"
        )
    return saida


@app.post("/api/ia/assistente")
def assistente(pedido: PedidoAssistente):
    """Pergunta do síndico → Gemini escolhe as funções → resposta com os dados consultados."""
    return mod_assistente.perguntar(pedido.pergunta)


@app.get("/")
def raiz():
    pagina = AQUI / "static" / "index.html"
    if pagina.exists():
        return FileResponse(pagina)
    return {"servico": "ia-service", "docs": "/docs"}


if (AQUI / "static").exists():
    app.mount("/static", StaticFiles(directory=AQUI / "static"), name="static")
