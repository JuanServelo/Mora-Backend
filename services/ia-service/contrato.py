"""Extração estruturada do contrato de locação em PDF.

Este é o uso que justifica um modelo de linguagem em vez de uma expressão
regular. O campo que o condomínio precisa — quem responde pela taxa — não vem
etiquetado em lugar nenhum: vem redigido em prosa jurídica, e cada contrato
redige diferente. "Correrão integralmente por conta do LOCATÁRIO as despesas
ordinárias de condomínio" carrega a mesma informação que
`responsavel_taxas = 'INQUILINO'`, mas nenhum padrão textual cobre o espaço
dessas variações.

O schema espelha `contratos_locacao` do `mora_financeiro`, inclusive o `enum`
de `responsavel_taxas`, que é o mesmo CHECK da migration.

Nada é gravado sem revisão humana. A extração devolve confiança por campo e a
**citação do trecho** que embasou cada um, para a pessoa conferir ao lado do
formulário. O LLM economiza digitação; não assume responsabilidade contratual.
"""

from __future__ import annotations

import pathlib

import fitz

import gemini

INSTRUCAO = """Você extrai dados de contratos de locação residencial brasileiros para \
cadastro em um sistema de gestão de condomínios.

Regras:
- Responda SOMENTE com o JSON do schema.
- `responsavel_taxas` é quem paga a TAXA CONDOMINIAL (despesas ordinárias de condomínio), \
não o aluguel. LOCATÁRIO/inquilino => "INQUILINO". LOCADOR/proprietário => "PROPRIETARIO".
- Para cada campo extraído, informe `confianca` entre 0 e 1 e `citacao` com o trecho \
LITERAL do contrato que embasa a resposta, copiado sem alterar.
- Se um campo não estiver no texto, use null e confianca 0. NÃO INVENTE.
- Valores monetários em CENTAVOS, inteiros (R$ 2.850,00 => 285000).
- Datas em ISO 8601 (AAAA-MM-DD)."""

CAMPO = {
    "type": "object",
    "properties": {
        "valor": {"type": "string", "nullable": True},
        "confianca": {"type": "number"},
        "citacao": {"type": "string", "nullable": True},
    },
    "required": ["valor", "confianca", "citacao"],
}

SCHEMA = {
    "type": "object",
    "properties": {
        "locador": CAMPO,
        "locatario": CAMPO,
        "inicio": CAMPO,
        "fim": CAMPO,
        "valor_aluguel_centavos": CAMPO,
        "responsavel_taxas": {
            "type": "object",
            "properties": {
                "valor": {"type": "string", "enum": ["PROPRIETARIO", "INQUILINO"], "nullable": True},
                "confianca": {"type": "number"},
                "citacao": {"type": "string", "nullable": True},
            },
            "required": ["valor", "confianca", "citacao"],
        },
        "endereco_unidade": CAMPO,
    },
    "required": [
        "locador",
        "locatario",
        "inicio",
        "fim",
        "valor_aluguel_centavos",
        "responsavel_taxas",
        "endereco_unidade",
    ],
    "propertyOrdering": [
        "locador",
        "locatario",
        "endereco_unidade",
        "inicio",
        "fim",
        "valor_aluguel_centavos",
        "responsavel_taxas",
    ],
}

CAMPOS = list(SCHEMA["properties"].keys())


def texto_do_pdf(caminho: str | pathlib.Path) -> str:
    doc = fitz.open(caminho)
    try:
        partes = [p.get_text() for p in doc]
    finally:
        doc.close()
    texto = "\n".join(partes).strip()
    if len(texto) < 200:
        # PDF escaneado, sem camada de texto. Fora do escopo declarado: o
        # sistema avisa em vez de inventar.
        raise ValueError(
            "o PDF não tem camada de texto (provavelmente escaneado). "
            "OCR está fora do escopo deste trabalho."
        )
    return texto


def extrair(caminho: str | pathlib.Path) -> dict:
    """Devolve os campos extraídos, ou `extracao_falhou` com o motivo."""
    try:
        texto = texto_do_pdf(caminho)
    except ValueError as e:
        return {"extracao_falhou": True, "motivo": str(e), "campos": {}}

    try:
        bruto = gemini.gerar_json(
            instrucao=INSTRUCAO,
            entrada=f"Contrato:\n\n{texto}",
            schema=SCHEMA,
            rotulo="contrato",
        )
    except gemini.ErroGemini as e:
        # Formulário em branco, e o motivo declarado. Preferimos não responder a
        # responder errado: uma cláusula invertida troca quem deve pagar.
        return {"extracao_falhou": True, "motivo": str(e), "campos": {}}

    faltando = [c for c in CAMPOS if c not in bruto]
    if faltando:
        return {
            "extracao_falhou": True,
            "motivo": f"campos ausentes na resposta do modelo: {faltando}",
            "campos": bruto,
        }

    # A citação tem de existir de verdade no contrato. É a checagem que separa
    # extração de alucinação: um trecho que o modelo escreveu mas o documento
    # não contém invalida o campo.
    normalizado = " ".join(texto.split()).lower()
    for nome, campo in bruto.items():
        citacao = (campo or {}).get("citacao")
        if citacao:
            trecho = " ".join(str(citacao).split()).lower()
            campo["citacao_confere"] = trecho[:120] in normalizado
        else:
            campo["citacao_confere"] = None

    return {"extracao_falhou": False, "motivo": None, "campos": bruto}


if __name__ == "__main__":
    import json
    import sys

    alvo = sys.argv[1] if len(sys.argv) > 1 else str(
        pathlib.Path(__file__).parent / "fixtures" / "contrato-exemplo.pdf"
    )
    r = extrair(alvo)
    print(json.dumps(r, indent=2, ensure_ascii=False))
    if not r["extracao_falhou"]:
        rt = r["campos"]["responsavel_taxas"]
        print(f"\n  responsavel_taxas = {rt['valor']!r}  (confiança {rt['confianca']})")
        print(f"  citação confere no documento: {rt['citacao_confere']}")
