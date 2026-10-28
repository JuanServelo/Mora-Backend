"""Cruza o contrato de locação com o histórico de pagamento da unidade.

É aqui que as duas metades do trabalho se encontram. Separadas, elas são duas
demonstrações; juntas, são o sistema que a proposta descreveu:

    PDF do contrato  →  LLM extrai quem responde pelas taxas
                     →  casa com a unidade no cadastro
                     →  modelo lê o histórico e estima o risco
                     →  a cobrança vai para a pessoa certa, com antecedência

O ponto que isso prova — e que nenhuma das metades prova sozinha — é que
**prever o risco não basta**. Saber que a unidade 302 vai atrasar não serve de
nada se o lembrete for para o proprietário quando quem paga é o inquilino. O
contrato é o que decide o destinatário, e ele muda com o tempo.
"""

from __future__ import annotations

import json
import pathlib
import re
from datetime import date

import contrato as mod_contrato
import explicacao as mod_explicacao
import predicao

AQUI = pathlib.Path(__file__).parent
INDICE = AQUI / "fixtures" / "contratos" / "indice.json"

# Um contrato que termina dentro disto exige aviso: a responsabilidade pelas
# taxas pode mudar antes que alguém perceba.
DIAS_ALERTA_VIGENCIA = 90


def _indice() -> dict:
    if not INDICE.exists():
        return {}
    return json.loads(INDICE.read_text(encoding="utf-8"))


def localizar_unidade(endereco: str | None) -> dict | None:
    """Casa o endereço extraído com uma unidade do cadastro.

    Num sistema real isto consultaria o portaria-service, que é o dono dos
    apartamentos. Aqui o índice faz esse papel.
    """
    if not endereco:
        return None
    texto = endereco.lower()

    m_apto = re.search(r"(?:apartamento|apto\.?|ap\.?|unidade)\s*n?\.?º?\s*(\d{2,4})", texto)
    m_bloco = re.search(r"bloco\s*([a-z0-9]{1,3})\b", texto)
    if not m_apto:
        # Último recurso: o maior número com 3 dígitos costuma ser o apartamento.
        numeros = re.findall(r"\b(\d{3})\b", texto)
        if not numeros:
            return None
        apto = numeros[-1]
    else:
        apto = m_apto.group(1)

    indice = _indice()
    if m_bloco:
        chave = f"{m_bloco.group(1).upper()}-{apto}"
        if chave in indice:
            return indice[chave]
    # Sem bloco legível, aceita se o apartamento for único no condomínio.
    candidatos = [v for k, v in indice.items() if k.split("-", 1)[1] == apto]
    return candidatos[0] if len(candidatos) == 1 else None


def _dias_ate(iso: str | None) -> int | None:
    if not iso:
        return None
    try:
        return (date.fromisoformat(str(iso)[:10]) - date.today()).days
    except ValueError:
        return None


def _valor(campos: dict, nome: str):
    c = campos.get(nome) or {}
    return c.get("valor")


def analisar(caminho: str | pathlib.Path, explicar_com_llm: bool = True) -> dict:
    """Extrai o contrato, encontra a unidade, pontua o risco e decide o destinatário."""
    extracao = mod_contrato.extrair(caminho)
    saida: dict = {
        "arquivo": pathlib.Path(caminho).name,
        "extracao": extracao,
        "unidade": None,
        "risco": None,
        "destinatario": None,
        "vigencia": None,
        "alertas": [],
    }

    if extracao["extracao_falhou"]:
        saida["alertas"].append(
            "Não foi possível ler o contrato: " + (extracao["motivo"] or "motivo desconhecido")
        )
        return saida

    campos = extracao["campos"]
    responsavel = _valor(campos, "responsavel_taxas")
    locador = _valor(campos, "locador")
    locatario = _valor(campos, "locatario")

    # --- vigência -------------------------------------------------------
    dias = _dias_ate(_valor(campos, "fim"))
    if dias is not None:
        saida["vigencia"] = {
            "fim": _valor(campos, "fim"),
            "dias_restantes": dias,
            "encerrado": dias < 0,
            "proximo_do_fim": 0 <= dias <= DIAS_ALERTA_VIGENCIA,
        }
        if dias < 0:
            saida["alertas"].append(
                f"Contrato vencido há {abs(dias)} dias — o responsável pelas taxas pode já ter mudado."
            )
        elif dias <= DIAS_ALERTA_VIGENCIA:
            saida["alertas"].append(
                f"Contrato vence em {dias} dias. Quando terminar, a responsabilidade pelas "
                "taxas pode mudar e a cobrança passa a sair para outra pessoa."
            )

    # --- destinatário da cobrança ---------------------------------------
    if responsavel == "INQUILINO":
        saida["destinatario"] = {
            "papel": "INQUILINO", "nome": locatario,
            "porque": "a cláusula de encargos atribui as despesas ordinárias ao locatário",
        }
    elif responsavel == "PROPRIETARIO":
        saida["destinatario"] = {
            "papel": "PROPRIETÁRIO", "nome": locador,
            "porque": "a cláusula de encargos mantém as taxas condominiais com o locador",
        }
        saida["alertas"].append(
            "A cobrança NÃO vai para quem mora: neste contrato quem responde pelas taxas é o "
            "proprietário."
        )

    # --- unidade e histórico --------------------------------------------
    unidade = localizar_unidade(_valor(campos, "endereco_unidade"))
    if not unidade:
        saida["alertas"].append(
            "Endereço não corresponde a nenhuma unidade cadastrada: sem histórico, não há previsão."
        )
        return saida

    saida["unidade"] = unidade
    r = predicao.pontuar(unidade["unidade_id"])

    if r.get("sem_previsao"):
        saida["risco"] = {"sem_previsao": True, "motivo": r["motivo"]}
        saida["alertas"].append(
            "Unidade sem histórico suficiente. O sistema não arrisca um palpite — e, numa "
            "unidade que acabou de trocar de inquilino, herdar o comportamento de quem se "
            "mudou seria injusto com quem chegou."
        )
        return saida

    f = r["features"]
    saida["risco"] = {
        "sem_previsao": False,
        "score": r["score"],
        "classe": r["classe"],
        "competencia": r["competencia"],
        "contribuicoes": r["contribuicoes"][:6],
        "historico": {
            "faturas_analisadas": f["meses_historico"],
            "atrasos_12m": f["atrasos_12m"],
            "taxa_em_dia_12m": f["taxa_em_dia_12m"],
            "media_dias_atraso": f["media_dias_atraso"],
            "multas_12m": f["multas_12m"],
            "abertas_vencidas": f["abertas_vencidas_no_corte"],
        },
    }

    if explicar_com_llm:
        saida["risco"]["explicacao"] = mod_explicacao.explicar(
            r["score"], r["contribuicoes"], f["meses_historico"]
        )
    else:
        saida["risco"]["explicacao"] = {
            "fonte": "deterministica",
            "explicacao": mod_explicacao.em_portugues_simples(r["score"], r["contribuicoes"]),
        }

    if r["score"] >= 0.5 and saida["destinatario"]:
        saida["alertas"].append(
            f"Risco alto ({r['score']:.0%}). Lembrete preventivo recomendado para "
            f"{saida['destinatario']['nome']} ({saida['destinatario']['papel'].lower()}), "
            "antes do vencimento."
        )

    return saida


if __name__ == "__main__":
    import sys

    pasta = AQUI / "fixtures" / "contratos"
    alvos = [pathlib.Path(a) for a in sys.argv[1:]] or sorted(pasta.glob("*.pdf"))

    for pdf in alvos:
        r = analisar(pdf, explicar_com_llm=False)
        print("═" * 76)
        print(f"  {r['arquivo']}")
        print("═" * 76)
        if r["extracao"]["extracao_falhou"]:
            print(f"  extração falhou: {r['extracao']['motivo'][:70]}\n")
            continue
        c = r["extracao"]["campos"]
        print(f"  locador    {_valor(c,'locador')}")
        print(f"  locatário  {_valor(c,'locatario')}")
        print(f"  unidade    {_valor(c,'endereco_unidade')}")
        print(f"  vigência   {_valor(c,'inicio')} a {_valor(c,'fim')}", end="")
        if r["vigencia"]:
            print(f"   ({r['vigencia']['dias_restantes']} dias restantes)")
        else:
            print()
        if r["destinatario"]:
            d = r["destinatario"]
            print(f"  COBRANÇA   {d['papel']}: {d['nome']}")
        if r["risco"] and not r["risco"].get("sem_previsao"):
            h = r["risco"]["historico"]
            print(f"  histórico  {h['faturas_analisadas']:.0f} faturas · "
                  f"{h['atrasos_12m']:.0f} atrasos em 12m · "
                  f"{h['taxa_em_dia_12m']:.0%} em dia")
            print(f"  RISCO      {r['risco']['score']:.1%}  {r['risco']['classe']}")
            print(f"             {r['risco']['explicacao']['explicacao'][:150]}")
        elif r["risco"]:
            print(f"  RISCO      sem previsão — {r['risco']['motivo']}")
        for a in r["alertas"]:
            print(f"  ! {a}")
        print()
