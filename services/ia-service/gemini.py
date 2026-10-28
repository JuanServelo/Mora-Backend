"""Cliente do Gemini via REST, sem SDK.

Sem SDK de propósito: uma dependência a menos para instalar no dia da defesa, e
o corpo da requisição fica visível — dá para mostrar na apresentação o que exatamente
foi pedido ao modelo.

Três garantias que importam:

**Cache em disco.** Toda chamada bem-sucedida é gravada em
`fixtures/respostas_gemini/<hash>.json`. Com `IA_OFFLINE=1` o cliente só lê o
cache e nunca toca a rede. Se a rede da sala cair no meio da defesa, a
demonstração continua — e o que se mostra é uma resposta real, gravada, não uma
simulação.

**Saída tipada.** `responseSchema` obriga o modelo a devolver JSON na forma que
pedimos. O schema aceito é um subconjunto de OpenAPI: só `type`, `properties`,
`required`, `enum`, `items`, `nullable` e `propertyOrdering`. Mandar `oneOf`,
`$ref`, `pattern` ou `additionalProperties` devolve 400.

**Nunca inventa.** JSON inválido dá uma tentativa de reparo; se falhar de novo,
o erro é explícito e o chamador devolve o formulário em branco. Uma cláusula de
contrato extraída errado inverte quem deve pagar a taxa — é o erro mais caro
que este sistema pode cometer, e o projeto prefere não responder.
"""

from __future__ import annotations

import hashlib
import json
import os
import pathlib
import re
import time

import httpx

BASE = "https://generativelanguage.googleapis.com/v1beta"
AQUI = pathlib.Path(__file__).parent
CACHE = AQUI / "fixtures" / "respostas_gemini"


def _carregar_env() -> None:
    """Lê o .env no import, para qualquer ponto de entrada funcionar.

    Antes cada script carregava o .env por conta própria, e quem esquecesse
    recebia "GEMINI_API_KEY ausente" sem a chave estar ausente de fato. O lugar
    certo é aqui: quem precisa da chave é este módulo.
    """
    env = AQUI / ".env"
    if not env.exists():
        return
    for linha in env.read_text(encoding="utf-8").splitlines():
        linha = linha.strip()
        if linha and not linha.startswith("#") and "=" in linha:
            k, v = linha.split("=", 1)
            os.environ.setdefault(k.strip(), v.strip())


_carregar_env()
# Orçamento de tempo, não generosidade. Numa apresentação ao vivo o custo de
# esperar é maior que o de cair na explicação determinística: ninguém assiste a
# uma tela escrita "consultando…" por dois minutos.
#
# TIMEOUT é por tentativa; MAX_ALTERNATIVAS limita a cascata. O pior caso
# fica em torno de 75s em vez dos 16 minutos que 8 modelos x 120s davam.
TIMEOUT = 25.0
# Vários modelos podem ter a cota diária esgotada ao mesmo tempo; a cascata
# precisa de fôlego para achar um que responda.
MAX_ALTERNATIVAS = 5

# Preferência de família, do mais novo ao mais antigo. O nome nunca é chumbado:
# a lista real vem de GET /models e o modelo é SONDADO antes de uso.
#
# A sondagem existe porque o catálogo mente nos dois sentidos: ele lista
# modelos que respondem 404 ("no longer available to new users") e lista
# modelos que respondem 503 por sobrecarga. Escolher pelo nome mais novo da
# lista derrubaria a demonstração na frente da banca.
PREFERIDOS = (
    "gemini-3.8-flash",
    "gemini-3.7-flash",
    "gemini-3.6-flash",
    "gemini-3.5-flash",
    "gemini-3-flash-preview",
    "flash",
)
# Famílias a evitar: geram áudio, imagem ou exigem outra API.
EXCLUIR = ("tts", "image", "audio", "embedding", "thinking", "antigravity")

_escolhido: str | None = None


class ErroGemini(RuntimeError):
    pass


# Acima disto, esperar não é opção: é cota diária esgotada, não rajada.
ESPERA_MAXIMA = 90.0


def _espera_sugerida(resposta: httpx.Response, padrao: float = 20.0) -> float:
    """Quanto esperar depois de um 429 — ou 0, se não adiantar esperar.

    O nível gratuito tem dois limites distintos, e a resposta certa é oposta
    em cada um:

      · por minuto  → `retryDelay` de alguns segundos. Esperar resolve.
      · por DIA, por modelo (`GenerateRequestsPerDayPerProjectPerModel`,
        20 requisições) → `retryDelay` de horas. Esperar não resolve;
        **trocar de modelo resolve**, porque a cota é por modelo.

    Devolver 0 sinaliza ao chamador que o caminho é a cascata, não o relógio.
    """
    try:
        for detalhe in resposta.json().get("error", {}).get("details", []):
            atraso = detalhe.get("retryDelay")
            if atraso:
                segundos = float(str(atraso).rstrip("s"))
                return 0.0 if segundos > ESPERA_MAXIMA else segundos + 1.0
    except (ValueError, TypeError, AttributeError):
        pass
    return padrao


def chave() -> str:
    k = os.environ.get("GEMINI_API_KEY", "").strip()
    if not k:
        raise ErroGemini(
            "GEMINI_API_KEY ausente. Grave a chave em services/ia-service/.env "
            "(o arquivo está no .gitignore) ou exporte a variável."
        )
    return k


def offline() -> bool:
    return os.environ.get("IA_OFFLINE", "").strip() in ("1", "true", "sim")


def modelos_disponiveis() -> list[str]:
    """Nomes que suportam generateContent, na conta desta chave."""
    try:
        with httpx.Client(timeout=TIMEOUT) as c:
            r = c.get(f"{BASE}/models", params={"key": chave()})
    except httpx.HTTPError as e:
        raise ErroGemini(f"não foi possível listar modelos: {e}") from e
    if r.status_code != 200:
        raise ErroGemini(f"GET /models devolveu {r.status_code}: {r.text[:300]}")
    saida = []
    for m in r.json().get("models", []):
        if "generateContent" in m.get("supportedGenerationMethods", []):
            saida.append(m["name"].removeprefix("models/"))
    return saida


def _responde(c: httpx.Client, modelo: str) -> bool:
    """Sondagem mínima: o modelo aceita uma geração de texto agora?"""
    r = c.post(
        f"{BASE}/models/{modelo}:generateContent",
        params={"key": chave()},
        json={
            "contents": [{"role": "user", "parts": [{"text": "ok"}]}],
            "generationConfig": {"maxOutputTokens": 2048},
        },
    )
    return r.status_code == 200


def candidatos() -> list[str]:
    """Nomes plausíveis, na ordem de preferência, sem os de outra modalidade."""
    disponiveis = [m for m in modelos_disponiveis() if not any(x in m for x in EXCLUIR)]
    ordenados: list[str] = []
    for pref in PREFERIDOS:
        for nome in disponiveis:
            if pref in nome and nome not in ordenados:
                ordenados.append(nome)
    for nome in disponiveis:
        if nome not in ordenados:
            ordenados.append(nome)
    return ordenados


def escolher_modelo() -> str:
    """Modelo de GEMINI_MODEL, ou o primeiro candidato que responde de fato."""
    global _escolhido
    fixado = os.environ.get("GEMINI_MODEL", "").strip()
    if fixado:
        return fixado
    if _escolhido:
        return _escolhido

    lista = candidatos()
    if not lista:
        raise ErroGemini("nenhum modelo com generateContent nesta chave")

    with httpx.Client(timeout=TIMEOUT) as c:
        for nome in lista[: MAX_ALTERNATIVAS + 1]:
            try:
                if _responde(c, nome):
                    _escolhido = nome
                    return nome
            except httpx.HTTPError:
                continue
    raise ErroGemini(
        "nenhum modelo respondeu. Candidatos sondados: "
        + ", ".join(lista[: MAX_ALTERNATIVAS + 1])
    )


def _chave_cache(corpo: dict) -> str:
    """Hash só do pedido, sem o nome do modelo.

    Incluir o modelo na chave jogava o cache fora a cada troca de modelo — e a
    troca acontece exatamente quando a rede está ruim, que é quando o cache
    mais importa. O pedido é o mesmo; a resposta serve.
    """
    bruto = json.dumps(corpo, sort_keys=True, ensure_ascii=False)
    return hashlib.sha256(bruto.encode("utf-8")).hexdigest()[:24]


def _limpar(texto: str) -> str:
    """Remove cercas markdown que o modelo às vezes acrescenta apesar do schema."""
    t = texto.strip()
    t = re.sub(r"^```(?:json)?\s*", "", t)
    t = re.sub(r"\s*```$", "", t)
    return t.strip()


def _chamar(c: httpx.Client, corpo: dict, modelo: str) -> httpx.Response:
    """Timeout e falha de conexão viram ErroGemini, não exceção crua.

    O chamador trata ErroGemini caindo na explicação determinística. Uma
    httpx.ReadTimeout escapando derruba o processo — exatamente no cenário
    de rede ruim para o qual o fallback foi escrito.
    """
    try:
        return c.post(
            f"{BASE}/models/{modelo}:generateContent",
            params={"key": chave()},
            json=corpo,
            headers={"Content-Type": "application/json"},
        )
    except httpx.TimeoutException as e:
        raise ErroGemini(f"{modelo} não respondeu em {TIMEOUT:.0f}s") from e
    except httpx.HTTPError as e:
        raise ErroGemini(f"falha de rede ao chamar {modelo}: {e}") from e


def _postar(c: httpx.Client, corpo: dict, modelo: str) -> tuple[httpx.Response, str]:
    """Uma geração com a cascata de modelos. Devolve a resposta 200 e quem respondeu.

    Comum ao JSON tipado e ao assistente com ferramentas: os dois precisam da
    mesma resistência a modelo lento, descontinuado ou sem cota.
    """
    # Timeout no modelo preferido vai para a cascata, como o 503. Antes ele
    # derrubava a chamada inteira: medido em 28/10, o mesmo gemini-3.6-flash
    # respondeu em 3s e, minutos antes, estourou os 25s — e a demonstração
    # caía na explicação determinística com outros modelos livres.
    try:
        r = _chamar(c, corpo, modelo)
    except ErroGemini as e:
        r, falha = None, str(e)
    else:
        falha = None

    # 429 por rajada: a espera sugerida é curta e resolve. 429 por cota
    # diária do modelo: a espera é de horas e _espera_sugerida devolve 0,
    # caindo direto na cascata, que troca de modelo — e a cota é por
    # modelo, então a troca realmente libera.
    if r is not None and r.status_code == 429:
        espera = _espera_sugerida(r)
        if espera > 0:
            time.sleep(espera)
            r = _chamar(c, corpo, modelo)

    # Modelo descontinuado (404) ou sobrecarregado (503/429): passa ao
    # próximo candidato em vez de falhar a demonstração.
    # Vale também quando GEMINI_MODEL está fixado: o pino expressa
    # preferência, não teimosia. Um 503 no modelo preferido não pode
    # derrubar a apresentação.
    if r is None or r.status_code in (404, 429, 503):
        tentadas = 0
        for alternativo in candidatos()[:8]:
            if tentadas >= MAX_ALTERNATIVAS:
                break
            tentadas += 1
            if alternativo == modelo:
                continue
            modelo = alternativo
            try:
                r = _chamar(c, corpo, modelo)
            except ErroGemini:
                continue  # alternativa lenta ou fora do ar: próxima
            if r.status_code == 200:
                globals()["_escolhido"] = alternativo
                break
    if r is None:
        raise ErroGemini(f"nenhum modelo respondeu a tempo; o primeiro: {falha}")
    if r.status_code != 200:
        raise ErroGemini(f"{modelo} devolveu {r.status_code}: {r.text[:400]}")
    return r, modelo


def gerar_json(
    instrucao: str,
    entrada: str,
    schema: dict,
    modelo: str | None = None,
    rotulo: str = "sem-rotulo",
) -> dict:
    """Pede ao Gemini um JSON conforme `schema`. Devolve o dicionário validado."""
    modelo = modelo or (os.environ.get("GEMINI_MODEL", "").strip() or None)

    corpo = {
        "systemInstruction": {"parts": [{"text": instrucao}]},
        "contents": [{"role": "user", "parts": [{"text": entrada}]}],
        "generationConfig": {
            "responseMimeType": "application/json",
            "responseSchema": schema,
            "temperature": 0.1,
            "maxOutputTokens": 4096,
        },
    }

    CACHE.mkdir(parents=True, exist_ok=True)

    # Em modo offline a chave do cache não pode depender do nome do modelo,
    # porque não há como descobri-lo sem rede.
    if offline():
        em_cache = sorted(CACHE.glob(f"{rotulo}-*.json"))
        if not em_cache:
            raise ErroGemini(
                f"IA_OFFLINE=1 e nenhuma resposta em cache para '{rotulo}'. "
                "Rode uma vez com a rede disponível para gravar o cache."
            )
        return json.loads(em_cache[-1].read_text(encoding="utf-8"))["resposta"]

    # Cache primeiro: se o pedido já foi respondido, nem descobrimos modelo.
    id_cache = _chave_cache(corpo)
    arquivo = CACHE / f"{rotulo}-{id_cache}.json"
    if arquivo.exists():
        return json.loads(arquivo.read_text(encoding="utf-8"))["resposta"]

    modelo = modelo or escolher_modelo()

    with httpx.Client(timeout=TIMEOUT) as c:
        r, modelo = _postar(c, corpo, modelo)

        def chamar(cliente: httpx.Client, corpo_atual: dict) -> httpx.Response:
            return _chamar(cliente, corpo_atual, modelo)

        dados = r.json()
        cand = (dados.get("candidates") or [{}])[0]

        # Truncamento: o JSON vem pela metade e nenhum parser salva.
        if cand.get("finishReason") == "MAX_TOKENS":
            corpo["generationConfig"]["maxOutputTokens"] = 8192
            r = chamar(c, corpo)
            dados = r.json()
            cand = (dados.get("candidates") or [{}])[0]

        partes = (cand.get("content") or {}).get("parts") or []
        texto = "".join(p.get("text", "") for p in partes)
        if not texto:
            raise ErroGemini(f"resposta vazia (finishReason={cand.get('finishReason')})")

        try:
            resposta = json.loads(_limpar(texto))
        except json.JSONDecodeError as e:
            # Uma tentativa de reparo, devolvendo o erro do parser ao modelo.
            corpo["contents"].append({"role": "model", "parts": [{"text": texto}]})
            corpo["contents"].append(
                {
                    "role": "user",
                    "parts": [
                        {
                            "text": f"O JSON acima é inválido: {e}. "
                            "Responda somente com o JSON corrigido."
                        }
                    ],
                }
            )
            r = chamar(c, corpo)
            if r.status_code != 200:
                raise ErroGemini(f"reparo falhou: {r.status_code}") from e
            partes = (
                ((r.json().get("candidates") or [{}])[0].get("content") or {}).get("parts") or []
            )
            try:
                resposta = json.loads(_limpar("".join(p.get("text", "") for p in partes)))
            except json.JSONDecodeError as e2:
                raise ErroGemini(f"JSON inválido mesmo após reparo: {e2}") from e2

    arquivo.write_text(
        json.dumps(
            {"modelo": modelo, "rotulo": rotulo, "resposta": resposta},
            indent=2,
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    return resposta


# ── Conversa com ferramentas (tool calling) ────────────────────────────────


def conversar(
    instrucao: str,
    pergunta: str,
    ferramentas: list[dict],
    executar,
    rotulo: str = "assistente",
    max_rodadas: int = 5,
) -> dict:
    """Pergunta em linguagem natural → o modelo pede funções → nós executamos.

    O modelo nunca toca os dados: ele devolve `functionCall` com nome e
    argumentos, quem executa é `executar(nome, args)`, aqui no serviço, e o
    resultado volta como `functionResponse`. A resposta final só pode usar o
    que as funções devolveram — quem confere é o chamador.

    A volta do modelo entra no histórico como veio, com as `thoughtSignature`
    que os Gemini 3 anexam às chamadas: sem elas, a rodada seguinte é recusada.

    Online, roda sempre ao vivo e grava a conversa inteira. Com IA_OFFLINE=1,
    reproduz a conversa gravada para a mesma pergunta — o plano B da defesa.
    """
    CACHE.mkdir(parents=True, exist_ok=True)
    arquivo = CACHE / f"{rotulo}-{_chave_cache({'pergunta': pergunta.strip().lower()})}.json"
    if offline():
        if not arquivo.exists():
            raise ErroGemini(
                "IA_OFFLINE=1 e esta pergunta nunca foi respondida com rede. "
                "Faça a pergunta uma vez online para gravar a conversa."
            )
        return json.loads(arquivo.read_text(encoding="utf-8"))["resposta"]

    base = {
        "systemInstruction": {"parts": [{"text": instrucao}]},
        "tools": [{"functionDeclarations": ferramentas}],
        "generationConfig": {"temperature": 0.1, "maxOutputTokens": 2048},
    }
    contents: list[dict] = [{"role": "user", "parts": [{"text": pergunta}]}]
    chamadas: list[dict] = []
    modelo = escolher_modelo()

    with httpx.Client(timeout=TIMEOUT) as c:
        for _ in range(max_rodadas):
            r, modelo = _postar(c, {**base, "contents": contents}, modelo)
            cand = (r.json().get("candidates") or [{}])[0]
            partes = (cand.get("content") or {}).get("parts") or []
            pedidos = [p["functionCall"] for p in partes if "functionCall" in p]

            if not pedidos:
                texto = "".join(p.get("text", "") for p in partes if not p.get("thought")).strip()
                if not texto:
                    raise ErroGemini(f"resposta vazia (finishReason={cand.get('finishReason')})")
                saida = {"resposta": texto, "chamadas": chamadas, "modelo": modelo}
                arquivo.write_text(
                    json.dumps({"modelo": modelo, "rotulo": rotulo, "pergunta": pergunta,
                                "resposta": saida}, indent=2, ensure_ascii=False),
                    encoding="utf-8",
                )
                return saida

            contents.append({"role": "model", "parts": partes})
            respostas = []
            for fc in pedidos:
                args = fc.get("args") or {}
                try:
                    resultado = executar(fc["name"], args)
                except Exception as e:  # noqa: BLE001 — o erro vira dado para o modelo
                    resultado = {"erro": str(e)}
                chamadas.append({"ferramenta": fc["name"], "argumentos": args, "resultado": resultado})
                resposta = {"name": fc["name"], "response": {"resultado": resultado}}
                if fc.get("id"):
                    resposta["id"] = fc["id"]
                respostas.append({"functionResponse": resposta})
            contents.append({"role": "user", "parts": respostas})

    raise ErroGemini(f"o assistente não concluiu em {max_rodadas} rodadas")


if __name__ == "__main__":
    print("modelos com generateContent nesta chave:")
    for m in modelos_disponiveis():
        print(" ", m)
    print("\nescolhido:", escolher_modelo())
