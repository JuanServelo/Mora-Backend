"""Roteiro de demonstração, na ordem da apresentação.

    python demo.py

Roda tudo do zero e imprime o que interessa à banca, em sete passos. Serve de
ensaio e de rede de segurança: se o navegador falhar na hora, este terminal
mostra o trabalho inteiro.
"""

from __future__ import annotations

import json
import os
import pathlib
import subprocess
import sys

AQUI = pathlib.Path(__file__).parent
LARGURA = 78


def titulo(n: int, texto: str) -> None:
    print(f"\n{'─' * LARGURA}\n{n}. {texto}\n{'─' * LARGURA}")


def rodar(*args: str) -> int:
    return subprocess.call([sys.executable, *args], cwd=AQUI)


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

    print("\n" + "═" * LARGURA)
    print("  MORA INTELIGENTE — previsão de inadimplência e leitura de contratos")
    print("  TDE2 · Sistemas de Informação Inteligentes")
    print("═" * LARGURA)

    titulo(1, "ENTRADA — gerar o histórico sintético")
    print("Espelha o esquema real de mora_financeiro (faturas, contratos_locacao).")
    print("Dado sintético por metodologia: o Mora não tem 12 meses de histórico.")
    print("A limitação é declarada — o modelo NÃO tem validade preditiva real.\n")
    if rodar("gerador.py") != 0:
        sys.exit("falhou ao gerar os dados")

    titulo(2, "PROCESSAMENTO — treinar com validação temporal")
    print("Treina nas competências antigas, testa nas recentes. Split aleatório")
    print("vazaria futuro para o treino e inflaria a métrica.\n")
    if rodar("treino.py") != 0:
        sys.exit("falhou ao treinar")

    titulo(3, "ÉTICA — a restrição como código, não como promessa")
    print("O valor do aluguel está no banco e melhoraria a métrica. Fica fora:")
    print("é proxy socioeconômico. Os testes quebram se alguém o acrescentar.\n")
    rodar("-m", "pytest", "testes", "-q")

    titulo(4, "AUDITORIA DE VIÉS — desempenho por tipo de ocupação")
    m = json.loads((AQUI / "artefatos" / "metricas.json").read_text(encoding="utf-8"))
    seg = m.get("auditoria_vies_por_ocupacao", {})
    for nome, v in seg.items():
        print(f"  {nome:<10} AUC {v['auc_roc']:.3f}   n={v['n']:<5} positivos {v['taxa_positivos']:.1%}")
    if {"alugadas", "proprias"} <= set(seg):
        d = seg["proprias"]["auc_roc"] - seg["alugadas"]["auc_roc"]
        print(f"\n  Diferença: {d:+.3f} em favor das unidades próprias.")
        print("  O modelo acerta MENOS em unidades alugadas. Isso é reportado em")
        print("  separado de propósito: diluído na média, o erro desapareceria.")

    titulo(5, "SAÍDA — painel de risco da próxima competência")
    import predicao

    linhas = predicao.painel(8)
    print(f"  {'unidade':<10}{'risco':>8}  {'ocupação':<10}{'atrasos 12m':>12}")
    for d in linhas:
        print(
            f"  {d['unidade_id'][:8]:<10}{d['score']:>7.1%}  "
            f"{'alugada' if d['alugada'] else 'própria':<10}{d['atrasos_12m']:>12.0f}"
        )

    titulo(6, "EXPLICAÇÃO — decomposição exata e texto em português")
    alvo = linhas[0]["unidade_id"]
    r = predicao.pontuar(alvo)
    print(f"  unidade {alvo[:8]} · risco {r['score']:.1%} · {r['classe']}\n")
    print("  contribuições em log-odds (somam o logit do score):")
    for c in r["contribuicoes"][:5]:
        print(f"    {c['feature']:<28}{c['valor']:>9.2f}  {c['contribuicao_log_odds']:+.3f}")

    import explicacao

    ex = explicacao.explicar(r["score"], r["contribuicoes"], r["features"]["meses_historico"])
    print(f"\n  [{ex['fonte']}] {ex['explicacao']}")
    if ex.get("verificacao"):
        v = ex["verificacao"]
        print(f"\n  fidelidade: o texto cita {v['score_citado']:.1%}, o modelo calculou "
              f"{v['score_modelo']:.1%} → {'OK' if v['fiel'] else 'DIVERGIU, texto descartado'}")
    elif ex.get("motivo"):
        print(f"  (texto determinístico: {ex['motivo']})")

    print("\n  Unidade sem histórico suficiente:")
    sem = next((u for u in predicao.carregar()["unidades"]
                if predicao.pontuar(u, predicao.carregar()["dados"]["faturas"][u]["competencia"][0])
                .get("sem_previsao")), None)
    if sem:
        rr = predicao.pontuar(sem, predicao.carregar()["dados"]["faturas"][sem]["competencia"][0])
        print(f"    {rr['motivo']}  → o sistema diz que não sabe, em vez de chutar.")

    titulo(7, "LLM — leitura do contrato de locação em PDF")
    import fixture_contrato

    pdf = AQUI / "fixtures" / "contrato-exemplo.pdf"
    if not pdf.exists():
        fixture_contrato.gerar(pdf)
    print(f"  contrato: {pdf.name}")
    print("  cláusula de encargos redigida em prosa, não como campo etiquetado:")
    print('    "Correrão integralmente por conta do LOCATÁRIO [...] as despesas')
    print('     ordinárias de condomínio"')
    print("  nenhuma expressão regular cobre as dez formas de escrever isso.\n")

    import contrato as mod_contrato

    saida = mod_contrato.extrair(pdf)
    if saida["extracao_falhou"]:
        print(f"  extração não realizada: {saida['motivo']}")
        print("  formulário fica em branco de propósito — uma cláusula extraída")
        print("  errado inverte quem deve pagar a taxa.")
    else:
        for nome, campo in saida["campos"].items():
            confere = campo.get("citacao_confere")
            marca = "" if confere is None else ("  [trecho confere]" if confere else "  [NÃO confere]")
            print(f"    {nome:<26}{str(campo['valor'])[:38]:<40}conf {campo['confianca']}{marca}")
        rt = saida["campos"]["responsavel_taxas"]
        print(f"\n  responsavel_taxas = {rt['valor']!r} — o mesmo enum do CHECK da migration.")

    print(f"\n{'═' * LARGURA}")
    print("  Painel no navegador:  python -m uvicorn main:app --port 8000  →  http://localhost:8000")
    print("  Não entregue hoje: assistente com tool calling, integração HTTP com")
    print("  o financeiro, disparo de lembrete pelo comunicacao-service.")
    print("═" * LARGURA + "\n")


if __name__ == "__main__":
    main()
