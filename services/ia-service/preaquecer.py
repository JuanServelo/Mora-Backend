"""Pré-aquece o cache de respostas do LLM antes da apresentação.

    python preaquecer.py [quantas_unidades]

Por que isto existe: a chamada ao Gemini leva alguns segundos quando a rede
está boa, e pode levar muito mais quando o modelo está sobrecarregado — e
"sobrecarregado" é o estado normal de um modelo novo no horário comercial.
Uma tela escrita "consultando…" na frente da banca custa mais do que vale.

Rodando isto antes, as explicações das unidades do topo do painel e a extração
do contrato ficam gravadas em `fixtures/respostas_gemini/`. Durante a defesa o
clique responde na hora, e com `IA_OFFLINE=1` nem rede é preciso — e o que se
mostra continua sendo uma resposta real do modelo, gravada, não uma simulação.
"""

from __future__ import annotations

import os
import pathlib
import sys
import time

AQUI = pathlib.Path(__file__).parent


def carregar_env() -> None:
    env = AQUI / ".env"
    if not env.exists():
        return
    for linha in env.read_text(encoding="utf-8").splitlines():
        linha = linha.strip()
        if linha and not linha.startswith("#") and "=" in linha:
            k, v = linha.split("=", 1)
            os.environ.setdefault(k.strip(), v.strip())


def main() -> None:
    carregar_env()
    quantas = int(sys.argv[1]) if len(sys.argv) > 1 else 10

    import contrato
    import explicacao
    import predicao

    print(f"pré-aquecendo o cache para as {quantas} unidades de maior risco\n")

    # O nível gratuito limita requisições por minuto. Dez chamadas seguidas
    # estouram a janela e voltam 429; a pausa mantém o ritmo abaixo do teto.
    PAUSA = 6.0

    ok = falhas = 0
    for i, linha in enumerate(predicao.painel(quantas), 1):
        r = predicao.pontuar(linha["unidade_id"])

        ex, dt = None, 0.0
        for tentativa in (1, 2):
            t0 = time.monotonic()
            ex = explicacao.explicar(
                r["score"], r["contribuicoes"], r["features"]["meses_historico"]
            )
            dt = time.monotonic() - t0
            if ex["fonte"] == "gemini":
                break
            if tentativa == 1 and "429" in (ex.get("motivo") or ""):
                # Limite por minuto: esperar é a correção, não insistir.
                print(f"  {i:>2}. {linha['unidade_id'][:8]}  limite por minuto, aguardando 30s…")
                time.sleep(30)

        marca = "ok     " if ex["fonte"] == "gemini" else "FALHOU "
        if ex["fonte"] == "gemini":
            ok += 1
        else:
            falhas += 1
        print(f"  {i:>2}. {linha['unidade_id'][:8]}  risco {r['score']:>5.1%}  {marca} {dt:5.1f}s")
        if ex["fonte"] != "gemini" and ex.get("motivo"):
            print(f"      motivo: {ex['motivo'][:90]}")
        time.sleep(PAUSA)

    print("\n  contrato de exemplo…", end=" ", flush=True)
    pdf = AQUI / "fixtures" / "contrato-exemplo.pdf"
    if not pdf.exists():
        import fixture_contrato

        fixture_contrato.gerar(pdf)
    saida = contrato.extrair(pdf)
    print("ok" if not saida["extracao_falhou"] else f"FALHOU: {saida['motivo'][:70]}")

    cache = list((AQUI / "fixtures" / "respostas_gemini").glob("*.json"))
    print(f"\n{ok} explicações gravadas · {falhas} falharam · {len(cache)} respostas no cache")
    print("Durante a defesa: IA_OFFLINE=1 serve tudo do cache, sem rede.")


if __name__ == "__main__":
    main()
