"""Gera o PDF de contrato de locação usado na demonstração.

Não havia nenhum contrato real para testar a extração, e usar um contrato de
verdade levantaria exatamente o problema de privacidade que a proposta discute:
contrato de locação traz CPF, renda e fiador de pessoas reais.

A cláusula de encargos é escrita **em prosa jurídica**, não como campo
etiquetado. É isso que justifica um modelo de linguagem em vez de uma expressão
regular: "as despesas ordinárias de condomínio correrão por conta do LOCATÁRIO"
carrega a mesma informação que `responsavel_taxas = 'INQUILINO'`, e as dez
formas de escrever essa frase não cabem em nenhum padrão.
"""

from __future__ import annotations

import pathlib

import fitz

LARGURA, ALTURA = fitz.paper_size("a4")
MARGEM = 62
CORPO = 10.2
ENTRELINHA = 15.2

CLAUSULAS = [
    ("INSTRUMENTO PARTICULAR DE CONTRATO DE LOCAÇÃO DE IMÓVEL RESIDENCIAL", True),
    ("", False),
    (
        "Pelo presente instrumento particular, de um lado MARIA HELENA ANDRADE GOULART, "
        "brasileira, portadora da cédula de identidade RG n.º 7.884.512-0, inscrita no CPF "
        "sob o n.º 471.228.390-55, residente e domiciliada nesta Capital, doravante "
        "denominada simplesmente LOCADORA; e de outro lado RAFAEL TEIXEIRA DE ALMEIDA, "
        "brasileiro, engenheiro civil, inscrito no CPF sob o n.º 035.914.772-18, doravante "
        "denominado LOCATÁRIO, têm entre si justo e contratado o quanto segue.",
        False,
    ),
    ("", False),
    ("CLÁUSULA PRIMEIRA — DO OBJETO", True),
    (
        "Constitui objeto da presente locação o imóvel residencial situado na Rua das "
        "Acácias, n.º 1.408, apartamento 302, Bloco A, integrante do empreendimento "
        "Residencial Parque Verde, nesta Capital, que o LOCATÁRIO declara receber em "
        "perfeito estado de conservação e habitabilidade.",
        False,
    ),
    ("", False),
    ("CLÁUSULA SEGUNDA — DO PRAZO", True),
    (
        "A presente locação vigorará pelo prazo determinado de 30 (trinta) meses, tendo "
        "seu termo inicial em 1.º de março de 2025 e termo final em 31 de agosto de 2027, "
        "data em que o LOCATÁRIO obriga-se a restituir o imóvel livre e desembaraçado de "
        "pessoas e coisas, independentemente de qualquer notificação ou interpelação.",
        False,
    ),
    ("", False),
    ("CLÁUSULA TERCEIRA — DO ALUGUEL", True),
    (
        "O aluguel mensal ora convencionado entre as partes é de R$ 2.850,00 (dois mil, "
        "oitocentos e cinquenta reais), a ser pago até o 5.º (quinto) dia útil de cada mês "
        "subsequente ao vencido, mediante depósito em conta de titularidade da LOCADORA, "
        "reajustável anualmente pela variação acumulada do IGP-M.",
        False,
    ),
    ("", False),
    ("CLÁUSULA QUARTA — DOS ENCARGOS E DESPESAS", True),
    (
        "Correrão integralmente por conta do LOCATÁRIO, além do aluguel, as despesas "
        "ordinárias de condomínio, assim compreendidas as de rateio de consumo de água e "
        "energia das áreas comuns, conservação, limpeza e remuneração do pessoal, bem como "
        "os consumos individuais de água, energia elétrica e gás, ficando a cargo exclusivo "
        "da LOCADORA tão somente as despesas extraordinárias de que trata o artigo 22, "
        "parágrafo único, da Lei n.º 8.245/91, e o imposto predial e territorial urbano.",
        False,
    ),
    ("", False),
    ("CLÁUSULA QUINTA — DA MORA", True),
    (
        "O atraso no pagamento de qualquer quantia devida sujeitará o LOCATÁRIO à multa "
        "moratória de 2% (dois por cento) sobre o débito, acrescida de juros de 1% (um por "
        "cento) ao mês e correção monetária, sem prejuízo do disposto na cláusula seguinte.",
        False,
    ),
    ("", False),
    ("CLÁUSULA SEXTA — DA GARANTIA", True),
    (
        "Em garantia das obrigações assumidas, o LOCATÁRIO presta caução em dinheiro "
        "equivalente a 3 (três) aluguéis, depositada em caderneta de poupança vinculada, "
        "nos termos do artigo 38 da Lei do Inquilinato.",
        False,
    ),
    ("", False),
    (
        "E por estarem assim justas e acordadas, firmam as partes o presente instrumento em "
        "2 (duas) vias de igual teor e forma, na presença das testemunhas abaixo.",
        False,
    ),
    ("", False),
    ("Curitiba, 24 de fevereiro de 2025.", False),
]


def quebrar(texto: str, fonte: str, tamanho: float, largura: float) -> list[str]:
    palavras, linhas, atual = texto.split(), [], ""
    for p in palavras:
        teste = p if not atual else f"{atual} {p}"
        if fitz.get_text_length(teste, fontname=fonte, fontsize=tamanho) <= largura or not atual:
            atual = teste
        else:
            linhas.append(atual)
            atual = p
    if atual:
        linhas.append(atual)
    return linhas


def gerar(destino: pathlib.Path) -> pathlib.Path:
    doc = fitz.open()
    pagina = doc.new_page(width=LARGURA, height=ALTURA)
    y = MARGEM
    util = LARGURA - 2 * MARGEM

    for texto, negrito in CLAUSULAS:
        if not texto:
            y += ENTRELINHA * 0.55
            continue
        fonte = "Times-Bold" if negrito else "Times-Roman"
        for linha in quebrar(texto, fonte, CORPO, util):
            if y > ALTURA - MARGEM:
                pagina = doc.new_page(width=LARGURA, height=ALTURA)
                y = MARGEM
            pagina.insert_text((MARGEM, y), linha, fontname=fonte, fontsize=CORPO)
            y += ENTRELINHA

    destino.parent.mkdir(parents=True, exist_ok=True)
    doc.save(destino)
    doc.close()
    return destino


if __name__ == "__main__":
    caminho = gerar(pathlib.Path(__file__).parent / "fixtures" / "contrato-exemplo.pdf")
    com = fitz.open(caminho)
    texto = "\n".join(p.get_text() for p in com)
    print(f"gerado: {caminho}")
    print(f"páginas: {com.page_count} · caracteres extraídos: {len(texto)}")
    com.close()
