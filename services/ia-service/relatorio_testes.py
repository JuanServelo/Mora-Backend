"""Gera o relatório de testes em PDF, no formato 16:9, para projetar na defesa.

    python relatorio_testes.py

Faz três coisas, nesta ordem:

1. Roda a suíte e captura o resultado de cada teste.
2. **Injeta a violação ética de verdade** — acrescenta `valor_aluguel_centavos`
   à lista de features — e roda de novo, para provar que a guarda reprova.
   Depois restaura o arquivo. Um teste de proibição que nunca viu uma violação
   não prova nada; este viu.
3. Desenha tudo num PDF com a identidade visual do Mora.

O PDF é a evidência que se projeta. O terminal continua disponível para quem
quiser ver rodando ao vivo.
"""

from __future__ import annotations

import pathlib
import re
import shutil
import subprocess
import sys

import fitz

AQUI = pathlib.Path(__file__).parent
SAIDA = AQUI / "fixtures" / "relatorio-testes.pdf"

# 16:9 em pontos — projeta em tela cheia sem bordas.
L, A = 960.0, 540.0
M = 46.0

BG = (0.102, 0.102, 0.141)
CARD = (0.137, 0.137, 0.184)
CARD2 = (0.169, 0.169, 0.227)
LINHA = (0.208, 0.208, 0.290)
INDIGO = (0.424, 0.388, 1.0)
TEXTO = (0.910, 0.910, 0.941)
SUAVE = (0.765, 0.765, 0.831)
MUDO = (0.604, 0.604, 0.682)
VERDE = (0.298, 0.686, 0.314)
VERMELHO = (0.957, 0.263, 0.212)
AMBAR = (0.961, 0.647, 0.141)

# O que cada teste defende. Sem isto o relatório é uma lista de nomes.
PROPOSITO = {
    "test_nenhuma_feature_e_proxy_socioeconomico":
        "nenhuma feature tem nome de proxy socioeconômico",
    "test_lista_explicita_de_proibidas_fora_das_features":
        "a lista explícita de proibidas não entrou no modelo",
    "test_modelo_treinado_nao_carrega_feature_proibida":
        "a guarda vale para o artefato salvo, não só para o código",
    "test_a_guarda_realmente_pega":
        "a guarda detecta a violação — não passa por vacuidade",
    "test_dado_proibido_existe_na_base":
        "o dado proibido EXISTE: a exclusão é escolha, não ausência",
    "test_contribuicoes_somam_o_logit_do_score":
        "a decomposição é exata: contribuições + intercepto = logit(score)",
    "test_texto_que_altera_o_score_e_reprovado":
        "texto do LLM que cita outro número é descartado",
    "test_texto_fiel_e_aprovado_em_fracao_e_em_percentual":
        "0,73 e 73 são a mesma afirmação e ambas passam",
    "test_virgula_decimal_nao_confunde_a_verificacao":
        "\"91,8%\" é lido como 0,918, não como 0,08",
    "test_explicacao_deterministica_nao_depende_de_rede":
        "sempre há explicação, mesmo sem LLM nenhum",
    "test_truncar_o_futuro_nao_muda_as_features":
        "apagar o futuro da base não altera nenhuma feature",
    "test_pagamento_posterior_ao_corte_conta_como_em_aberto":
        "fatura paga depois do corte estava EM ABERTO no corte",
    "test_unidade_sem_historico_nao_recebe_previsao":
        "sem histórico, o sistema diz que não sabe",
}

GRUPOS = {
    "test_etica.py": ("ÉTICA", "A restrição como código, não como promessa de documento", AMBAR),
    "test_explicacao.py": ("EXPLICAÇÃO", "O que vai ao morador tem de ser verdade", INDIGO),
    "test_vazamento.py": ("VAZAMENTO TEMPORAL", "O futuro não entra nas features", VERDE),
}


def rodar_suite(extra: list[str] | None = None) -> tuple[list[tuple[str, str, str]], str]:
    r = subprocess.run(
        [sys.executable, "-m", "pytest", "testes", "-v", "--no-header", *(extra or [])],
        cwd=AQUI, capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    saida = r.stdout + r.stderr
    testes = []
    for linha in saida.splitlines():
        m = re.match(r"testes[/\\](\S+\.py)::(\S+)\s+(PASSED|FAILED|SKIPPED)", linha)
        if m:
            testes.append((m.group(1), m.group(2), m.group(3)))
    resumo = ""
    for linha in saida.splitlines():
        if re.search(r"\d+ (passed|failed)", linha):
            resumo = re.sub(r"=+", "", linha).strip()
    return testes, resumo


def experimento_etico() -> tuple[str, list[str]]:
    """Insere a feature proibida, roda os testes, restaura. Devolve o que quebrou."""
    alvo = AQUI / "features.py"
    backup = AQUI / "features.py.bak"
    shutil.copy(alvo, backup)
    try:
        s = alvo.read_text(encoding="utf-8")
        s = s.replace('    "troca_recente_3m",\n]',
                      '    "troca_recente_3m",\n    "valor_aluguel_centavos",\n]', 1)
        alvo.write_text(s, encoding="utf-8")
        testes, resumo = rodar_suite(["-k", "etica"])
        quebrados = [n for _, n, st in testes if st == "FAILED"]
        return resumo, quebrados
    finally:
        shutil.copy(backup, alvo)
        backup.unlink(missing_ok=True)


# ---------------------------------------------------------------- desenho ---
def retangulo(p, x, y, w, h, cor, borda=LINHA, raio=0.08):
    # No PyMuPDF o raio é uma FRAÇÃO do menor lado (0 a 0.5), não pontos.
    p.draw_rect(fitz.Rect(x, y, x + w, y + h), color=borda, fill=cor,
                width=0.7, radius=min(raio, 0.5))


# As fontes base do PDF (helv/hebo) não têm travessão nem aspas tipográficas:
# o que não existe vira "?" na página. Trocamos antes de desenhar.
SUBSTITUICOES = {"—": " - ", "–": "-", "“": '"', "”": '"', "‘": "'", "’": "'", "…": "..."}


def limpar(s: str) -> str:
    for a, b in SUBSTITUICOES.items():
        s = s.replace(a, b)
    return s


def txt(p, x, y, s, tam=10, cor=SUAVE, negrito=False, largura=None):
    s = limpar(s)
    fonte = "hebo" if negrito else "helv"
    if largura:
        p.insert_textbox(fitz.Rect(x, y, x + largura, y + tam * 4.5), s,
                         fontname=fonte, fontsize=tam, color=cor, lineheight=1.35)
    else:
        p.insert_text((x, y + tam), s, fontname=fonte, fontsize=tam, color=cor)


def cabecalho(p, kicker, titulo):
    p.draw_rect(fitz.Rect(0, 0, L, A), color=BG, fill=BG, width=0)
    txt(p, M, 24, kicker, 8.5, INDIGO, True)
    txt(p, M, 40, titulo, 21, TEXTO, True)
    txt(p, L - M - 46, 24, "Mora", 9, INDIGO, True)


def pagina_resultados(doc, testes, resumo):
    p = doc.new_page(width=L, height=A)
    cabecalho(p, "MORA INTELIGENTE · TDE2", "Suíte de testes — o que o projeto garante")

    y = 86
    for arquivo, (nome, sub, cor) in GRUPOS.items():
        do_grupo = [(n, st) for a, n, st in testes if a == arquivo]
        if not do_grupo:
            continue
        alt = 24 + len(do_grupo) * 19 + 14
        retangulo(p, M, y, L - 2 * M, alt, CARD)
        txt(p, M + 14, y + 7, nome, 9.5, cor, True)
        txt(p, M + 150, y + 7, sub, 8.5, MUDO)
        yy = y + 27
        for n, st in do_grupo:
            ok = st == "PASSED"
            p.draw_circle(fitz.Point(M + 22, yy + 6), 5.2,
                          color=VERDE if ok else VERMELHO,
                          fill=VERDE if ok else VERMELHO, width=0)
            txt(p, M + 19.5, yy + 1.5, "v" if ok else "x", 7, (1, 1, 1), True)
            txt(p, M + 36, yy, n.replace("test_", "").replace("_", " "), 8.8, TEXTO)
            txt(p, M + 320, yy, PROPOSITO.get(n, ""), 8.5, MUDO)
            yy += 19
        y += alt + 10

    retangulo(p, M, A - 60, L - 2 * M, 34, CARD2)
    txt(p, M + 14, A - 53, resumo or "", 10.5, VERDE, True)
    txt(p, M + 150, A - 52, "python -m pytest testes -q     ·     reprodutível com semente fixa (SEED = 42)",
        8.5, MUDO)


def pagina_experimento(doc, resumo_violacao, quebrados):
    p = doc.new_page(width=L, height=A)
    cabecalho(p, "PROVA DA GUARDA ÉTICA",
              "O que acontece se alguém acrescentar o valor do aluguel")

    txt(p, M, 76, "Um teste de proibição que nunca viu uma violação não prova nada. "
                  "Este relatório insere a violação de verdade no código, roda a suíte, "
                  "e depois restaura o arquivo.", 9.5, SUAVE, largura=L - 2 * M)

    meia = (L - 2 * M - 16) / 2

    retangulo(p, M, 118, meia, 150, CARD)
    txt(p, M + 14, 126, "ANTES — código como está", 9, VERDE, True)
    txt(p, M + 14, 148, 'FEATURES = [', 8.5, MUDO)
    txt(p, M + 26, 164, '"atrasos_3m", "taxa_em_dia_12m",', 8.5, TEXTO)
    txt(p, M + 26, 180, '"media_dias_atraso", "multas_12m",', 8.5, TEXTO)
    txt(p, M + 26, 196, '"alugada", "responsavel_inquilino", ...', 8.5, TEXTO)
    txt(p, M + 14, 212, ']', 8.5, MUDO)
    txt(p, M + 14, 236, "13 testes passam.", 9.5, VERDE, True)

    x2 = M + meia + 16
    retangulo(p, x2, 118, meia, 150, CARD)
    txt(p, x2 + 14, 126, "DEPOIS — feature proibida inserida", 9, VERMELHO, True)
    txt(p, x2 + 14, 148, 'FEATURES = [', 8.5, MUDO)
    txt(p, x2 + 26, 164, '"atrasos_3m", "taxa_em_dia_12m",', 8.5, TEXTO)
    txt(p, x2 + 26, 180, '"media_dias_atraso", ...', 8.5, TEXTO)
    txt(p, x2 + 26, 196, '"valor_aluguel_centavos",', 8.5, VERMELHO)
    txt(p, x2 + 14, 212, ']', 8.5, MUDO)
    txt(p, x2 + 14, 236, resumo_violacao or "a suíte reprova.", 9.5, VERMELHO, True)

    retangulo(p, M, 286, L - 2 * M, 22 + len(quebrados) * 17 + 10, CARD2)
    txt(p, M + 14, 292, "TESTES QUE REPROVARAM A VIOLAÇÃO", 8.5, AMBAR, True)
    yy = 312
    for n in quebrados:
        p.draw_circle(fitz.Point(M + 22, yy + 5), 5.0, color=VERMELHO, fill=VERMELHO, width=0)
        txt(p, M + 19.5, yy + 0.5, "x", 7, (1, 1, 1), True)
        txt(p, M + 36, yy - 1, n.replace("test_", "").replace("_", " "), 8.8, TEXTO)
        txt(p, M + 320, yy - 1, PROPOSITO.get(n, ""), 8.5, MUDO)
        yy += 17

    base = 286 + 22 + len(quebrados) * 17 + 22
    retangulo(p, M, base, L - 2 * M, 72, CARD)
    txt(p, M + 14, base + 8, "POR QUE O VALOR DO ALUGUEL FICA DE FORA", 8.5, INDIGO, True)
    txt(p, M + 14, base + 26,
        "Ele está no banco (contratos_locacao.valor_aluguel_centavos), é fácil de ler e "
        "provavelmente melhoraria a métrica. É indicador socioeconômico direto: usá-lo "
        "significaria treinar um modelo que prevê atraso a partir de quanto a pessoa pode "
        "pagar de moradia — classificação de renda com outro nome.",
        9, SUAVE, largura=L - 2 * M - 28)


def main() -> None:
    print("rodando a suíte…")
    testes, resumo = rodar_suite()
    print(f"  {resumo}")

    print("injetando a violação ética para provar que a guarda reprova…")
    resumo_violacao, quebrados = experimento_etico()
    print(f"  {resumo_violacao}  ({len(quebrados)} testes reprovaram)")

    doc = fitz.open()
    pagina_resultados(doc, testes, resumo)
    pagina_experimento(doc, resumo_violacao, quebrados)
    SAIDA.parent.mkdir(parents=True, exist_ok=True)
    doc.save(SAIDA)
    doc.close()
    print(f"\nrelatório: {SAIDA}")


if __name__ == "__main__":
    main()
