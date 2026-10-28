"""Gera o conjunto de contratos de locação da demonstração.

Seis contratos, cada um amarrado a uma unidade real do histórico sintético e
escolhido para produzir um desfecho DIFERENTE quando cruzado com os pagamentos:

  1. risco alto   · inquilino paga  → a cobrança vai para quem mora
  2. risco baixo  · inquilino paga  → bom pagador, nenhuma ação
  3. risco alto   · PROPRIETÁRIO paga → o alerta vai para o dono, não para quem mora
  4. risco médio  · contrato vencendo → a responsabilidade pode mudar em semanas
  5. sem histórico suficiente → o sistema recusa a previsão
  6. risco médio  · inquilino paga  → caso intermediário

A cláusula de encargos é redigida de **seis maneiras diferentes**, de propósito.
É o argumento central para usar um modelo de linguagem: a informação que o
condomínio precisa não vem etiquetada, vem em prosa jurídica, e cada escritório
escreve do seu jeito. Nenhuma expressão regular cobre esse espaço.

    python fixture_contratos.py
"""

from __future__ import annotations

import json
import pathlib

import fitz
import pandas as pd

AQUI = pathlib.Path(__file__).parent
DESTINO = AQUI / "fixtures" / "contratos"
INDICE = DESTINO / "indice.json"

LARGURA, ALTURA = fitz.paper_size("a4")
MARGEM = 62
CORPO = 10.2
ENTRELINHA = 15.2

# As seis redações da cláusula de encargos, e o que cada uma significa.
CLAUSULAS_ENCARGOS = {
    "INQUILINO": [
        "Correrão integralmente por conta do LOCATÁRIO, além do aluguel, as despesas "
        "ordinárias de condomínio, assim compreendidas as de rateio de consumo de água e "
        "energia das áreas comuns, conservação, limpeza e remuneração do pessoal, ficando "
        "a cargo exclusivo da LOCADORA tão somente as despesas extraordinárias de que trata "
        "o artigo 22, parágrafo único, da Lei n.º 8.245/91.",

        "Fica desde já ajustado entre as partes que o valor das quotas condominiais mensais "
        "será suportado pelo LOCATÁRIO, que as quitará diretamente junto à administradora do "
        "edifício, na mesma data de vencimento do aluguel, competindo ao LOCADOR apenas os "
        "encargos de natureza extraordinária e o imposto predial.",

        "Além da retribuição locatícia, incumbe ao inquilino o pagamento mensal da taxa de "
        "condomínio, dos consumos individuais de água, luz e gás, e de quaisquer tarifas "
        "incidentes sobre a unidade durante a vigência deste ajuste.",
    ],
    "PROPRIETARIO": [
        "As despesas ordinárias de condomínio, bem como as de caráter extraordinário, "
        "permanecerão sob a responsabilidade exclusiva do LOCADOR durante toda a vigência "
        "contratual, nada sendo devido pelo LOCATÁRIO a esse título, que arcará unicamente "
        "com os consumos individuais da unidade.",

        "Por liberalidade da parte locadora, e como condição expressamente negociada, o "
        "pagamento das taxas condominiais ordinárias ficará a cargo do proprietário do "
        "imóvel, não podendo tal benesse ser interpretada como novação quanto às demais "
        "obrigações aqui pactuadas.",

        "O valor mensal ajustado na cláusula anterior é tido como valor global, nele já "
        "compreendida a quota de condomínio, cujo recolhimento perante a administradora "
        "incumbirá ao LOCADOR.",
    ],
}


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


def montar(c: dict) -> list[tuple[str, bool]]:
    return [
        ("INSTRUMENTO PARTICULAR DE CONTRATO DE LOCAÇÃO DE IMÓVEL RESIDENCIAL", True),
        ("", False),
        (
            f"Pelo presente instrumento particular, de um lado {c['locador']}, "
            f"{c['locador_qualificacao']}, inscrita no CPF sob o n.º {c['locador_cpf']}, "
            f"doravante denominada simplesmente LOCADORA; e de outro lado {c['locatario']}, "
            f"{c['locatario_qualificacao']}, inscrito no CPF sob o n.º {c['locatario_cpf']}, "
            "doravante denominado LOCATÁRIO, têm entre si justo e contratado o quanto segue.",
            False,
        ),
        ("", False),
        ("CLÁUSULA PRIMEIRA — DO OBJETO", True),
        (
            f"Constitui objeto da presente locação o imóvel residencial situado na "
            f"{c['logradouro']}, apartamento {c['apartamento']}, Bloco {c['bloco']}, "
            "integrante do empreendimento Residencial Parque Verde, nesta Capital, que o "
            "LOCATÁRIO declara receber em perfeito estado de conservação.",
            False,
        ),
        ("", False),
        ("CLÁUSULA SEGUNDA — DO PRAZO", True),
        (
            f"A presente locação vigorará pelo prazo determinado de {c['meses']} "
            f"({c['meses_extenso']}) meses, tendo seu termo inicial em {c['inicio_extenso']} "
            f"e termo final em {c['fim_extenso']}, data em que o LOCATÁRIO obriga-se a "
            "restituir o imóvel livre e desembaraçado de pessoas e coisas.",
            False,
        ),
        ("", False),
        ("CLÁUSULA TERCEIRA — DO ALUGUEL", True),
        (
            f"O aluguel mensal ora convencionado é de {c['aluguel_extenso']}, a ser pago até "
            "o 5.º (quinto) dia útil de cada mês subsequente ao vencido, mediante depósito em "
            "conta de titularidade da LOCADORA, reajustável anualmente pelo IGP-M.",
            False,
        ),
        ("", False),
        ("CLÁUSULA QUARTA — DOS ENCARGOS E DESPESAS", True),
        (c["clausula_encargos"], False),
        ("", False),
        ("CLÁUSULA QUINTA — DA MORA", True),
        (
            "O atraso no pagamento de qualquer quantia devida sujeitará o devedor à multa "
            "moratória de 2% (dois por cento) sobre o débito, acrescida de juros de 1% (um "
            "por cento) ao mês e correção monetária.",
            False,
        ),
        ("", False),
        ("CLÁUSULA SEXTA — DA GARANTIA", True),
        (
            f"Em garantia das obrigações assumidas, o LOCATÁRIO presta {c['garantia']}, nos "
            "termos do artigo 37 da Lei do Inquilinato.",
            False,
        ),
        ("", False),
        (
            "E por estarem assim justas e acordadas, firmam as partes o presente instrumento "
            "em 2 (duas) vias de igual teor, na presença das testemunhas abaixo.",
            False,
        ),
        ("", False),
        (f"Curitiba, {c['assinatura_extenso']}.", False),
    ]


def gerar_pdf(c: dict, caminho: pathlib.Path) -> None:
    doc = fitz.open()
    pagina = doc.new_page(width=LARGURA, height=ALTURA)
    y = MARGEM
    util = LARGURA - 2 * MARGEM
    for texto, negrito in montar(c):
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
    caminho.parent.mkdir(parents=True, exist_ok=True)
    doc.save(caminho)
    doc.close()


def escolher_unidades() -> list[dict]:
    """Escolhe unidades reais que CASAM com o que cada contrato afirma.

    Coerência importa mais do que parece: se o PDF diz que o imóvel está
    alugado e o cadastro diz que é ocupado pelo proprietário, a explicação do
    risco sai contradizendo o documento que acabou de ser lido na tela. O
    professor vê isso em dois segundos.

    Por isso a seleção cruza o painel de risco com `contratos.csv` e só aceita
    unidades cujo `responsavel_taxas` seja o mesmo que a cláusula do contrato
    vai declarar.
    """
    import predicao

    painel = predicao.painel(500)
    if not painel:
        raise SystemExit("sem painel: rode `python gerador.py && python treino.py`")

    contratos = pd.read_csv(AQUI / "dados" / "contratos.csv")
    vigentes = contratos[contratos["ativo"]].set_index("unidade_id")["responsavel_taxas"]
    responsavel_de = vigentes.to_dict()

    def pegar(responsavel: str, faixa: str, usados: set) -> dict:
        """Unidade alugada, com o responsável pedido, na faixa de risco pedida."""
        elegiveis = [
            u for u in painel
            if u["unidade_id"] not in usados
            and responsavel_de.get(u["unidade_id"]) == responsavel
        ]
        if not elegiveis:
            elegiveis = [u for u in painel if u["unidade_id"] not in usados]
        elegiveis.sort(key=lambda u: -u["score"])
        n = len(elegiveis)
        escolhida = {
            "alto": elegiveis[0],
            "medio": elegiveis[n // 2],
            "baixo": elegiveis[-1],
        }[faixa]
        usados.add(escolhida["unidade_id"])
        return escolhida

    # Unidade nova de verdade: o modelo recusa a previsão hoje, não numa data
    # antiga artificial.
    ctx = predicao.carregar()
    sem_hist = None
    for u in ctx["unidades"]:
        if predicao.pontuar(u).get("sem_previsao"):
            sem_hist = {"unidade_id": u, "score": None}
            break

    usados: set = set()
    return [
        pegar("INQUILINO", "alto", usados),      # 1
        pegar("INQUILINO", "baixo", usados),     # 2
        pegar("PROPRIETARIO", "alto", usados),   # 3
        pegar("INQUILINO", "medio", usados),     # 4
        sem_hist or pegar("INQUILINO", "medio", usados),  # 5
        pegar("PROPRIETARIO", "medio", usados),  # 6
    ]


CASOS = [
    dict(nome="01-risco-alto-inquilino", responsavel="INQUILINO", variante=0, meses=30,
         locador="MARIA HELENA ANDRADE GOULART", locador_qualificacao="brasileira, aposentada",
         locador_cpf="471.228.390-55", locatario="RAFAEL TEIXEIRA DE ALMEIDA",
         locatario_qualificacao="brasileiro, engenheiro civil", locatario_cpf="035.914.772-18",
         logradouro="Rua das Acácias, n.º 1.408", apartamento="302", bloco="A",
         aluguel_extenso="R$ 2.850,00 (dois mil, oitocentos e cinquenta reais)",
         inicio_extenso="1.º de março de 2025", fim_extenso="31 de agosto de 2027",
         meses_extenso="trinta", assinatura_extenso="24 de fevereiro de 2025",
         garantia="caução em dinheiro equivalente a 3 (três) aluguéis"),

    dict(nome="02-risco-baixo-inquilino", responsavel="INQUILINO", variante=1, meses=36,
         locador="SÔNIA REGINA BITTENCOURT", locador_qualificacao="brasileira, médica",
         locador_cpf="602.337.189-04", locatario="CAMILA NOGUEIRA SANT'ANNA",
         locatario_qualificacao="brasileira, professora", locatario_cpf="118.445.206-73",
         logradouro="Rua das Acácias, n.º 1.408", apartamento="104", bloco="B",
         aluguel_extenso="R$ 1.980,00 (mil, novecentos e oitenta reais)",
         inicio_extenso="1.º de julho de 2024", fim_extenso="30 de junho de 2027",
         meses_extenso="trinta e seis", assinatura_extenso="20 de junho de 2024",
         garantia="fiança de terceiro idôneo"),

    dict(nome="03-risco-alto-proprietario", responsavel="PROPRIETARIO", variante=0, meses=24,
         locador="EDUARDO MALHEIROS PRADO", locador_qualificacao="brasileiro, empresário",
         locador_cpf="330.871.994-20", locatario="LUCAS FERRAZ DE MORAES",
         locatario_qualificacao="brasileiro, designer", locatario_cpf="947.112.308-66",
         logradouro="Rua das Acácias, n.º 1.408", apartamento="507", bloco="A",
         aluguel_extenso="R$ 3.400,00 (três mil e quatrocentos reais)",
         inicio_extenso="15 de janeiro de 2025", fim_extenso="14 de janeiro de 2027",
         meses_extenso="vinte e quatro", assinatura_extenso="10 de janeiro de 2025",
         garantia="seguro-fiança locatícia"),

    dict(nome="04-vigencia-terminando", responsavel="INQUILINO", variante=2, meses=18,
         locador="ANTÔNIO CARLOS REZENDE", locador_qualificacao="brasileiro, advogado",
         locador_cpf="255.908.471-39", locatario="PATRÍCIA LINS DO AMARAL",
         locatario_qualificacao="brasileira, analista de sistemas", locatario_cpf="784.230.115-92",
         logradouro="Rua das Acácias, n.º 1.408", apartamento="211", bloco="C",
         aluguel_extenso="R$ 2.240,00 (dois mil, duzentos e quarenta reais)",
         inicio_extenso="1.º de junho de 2025", fim_extenso="30 de novembro de 2026",
         meses_extenso="dezoito", assinatura_extenso="28 de maio de 2025",
         garantia="caução em dinheiro equivalente a 2 (dois) aluguéis"),

    dict(nome="05-sem-historico", responsavel="INQUILINO", variante=1, meses=30,
         locador="BEATRIZ QUEIROZ MONTENEGRO", locador_qualificacao="brasileira, arquiteta",
         locador_cpf="019.663.542-88", locatario="THIAGO BARRETO CAVALCANTI",
         locatario_qualificacao="brasileiro, músico", locatario_cpf="563.019.724-41",
         logradouro="Rua das Acácias, n.º 1.408", apartamento="806", bloco="B",
         aluguel_extenso="R$ 2.600,00 (dois mil e seiscentos reais)",
         inicio_extenso="1.º de outubro de 2026", fim_extenso="31 de março de 2029",
         meses_extenso="trinta", assinatura_extenso="26 de setembro de 2026",
         garantia="caução em dinheiro equivalente a 3 (três) aluguéis"),

    dict(nome="06-risco-medio-proprietario", responsavel="PROPRIETARIO", variante=2, meses=36,
         locador="JOSÉ RICARDO VASCONCELOS", locador_qualificacao="brasileiro, comerciante",
         locador_cpf="408.552.763-17", locatario="MARIANA OLIVEIRA DUTRA",
         locatario_qualificacao="brasileira, enfermeira", locatario_cpf="672.884.930-05",
         logradouro="Rua das Acácias, n.º 1.408", apartamento="403", bloco="C",
         aluguel_extenso="R$ 2.100,00 (dois mil e cem reais)",
         inicio_extenso="1.º de abril de 2025", fim_extenso="31 de março de 2028",
         meses_extenso="trinta e seis", assinatura_extenso="27 de março de 2025",
         garantia="fiança de terceiro idôneo"),
]


def main() -> None:
    unidades = escolher_unidades()
    indice = {}

    print(f"gerando {len(CASOS)} contratos\n")
    for caso, unidade in zip(CASOS, unidades):
        c = dict(caso)
        c["clausula_encargos"] = CLAUSULAS_ENCARGOS[c["responsavel"]][c["variante"]]
        caminho = DESTINO / f"{c['nome']}.pdf"
        gerar_pdf(c, caminho)

        chave = f"{c['bloco'].upper()}-{c['apartamento']}"
        indice[chave] = {
            "unidade_id": unidade["unidade_id"],
            "arquivo": caminho.name,
            "bloco": c["bloco"],
            "apartamento": c["apartamento"],
            "responsavel_esperado": c["responsavel"],
        }
        risco = unidade.get("score")
        print(f"  {caminho.name:<34} Bloco {c['bloco']} apto {c['apartamento']:<5} "
              f"{c['responsavel']:<13} risco {'—' if risco is None else f'{risco:.0%}'}")

    INDICE.write_text(json.dumps(indice, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"\níndice: {INDICE}")
    print("As seis cláusulas de encargos têm redações diferentes — é o que a regex não cobre.")


if __name__ == "__main__":
    main()
