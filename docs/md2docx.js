const fs = require('fs');
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType,
  Table, TableRow, TableCell, WidthType, ShadingType, BorderStyle,
  TableOfContents, PageBreak, LevelFormat, convertInchesToTwip,
} = require('docx');

const [, , entrada, saida] = process.argv;
const md = fs.readFileSync(entrada, 'utf8');

/* ---------------------------------------------------------------- estilo --- */

const AZUL = '1F4E79';
const CINZA_CAB = 'E7EDF3';
const CINZA_COD = 'F4F4F6';
const LARGURA_TABELA = 9020; // A4 menos margens, em DXA

/* --------------------------------------------------- formatação inline --- */

/** Converte **negrito**, `código` e *itálico* em TextRuns. */
function runs(texto, base = {}) {
  const saida = [];
  const re = /(\*\*[^*]+\*\*|`[^`]+`|\*[^*]+\*)/g;
  let ultimo = 0;
  let m;
  while ((m = re.exec(texto)) !== null) {
    if (m.index > ultimo) {
      saida.push(new TextRun({ ...base, text: limpar(texto.slice(ultimo, m.index)) }));
    }
    const t = m[0];
    if (t.startsWith('**')) {
      saida.push(new TextRun({ ...base, text: limpar(t.slice(2, -2)), bold: true }));
    } else if (t.startsWith('`')) {
      saida.push(new TextRun({ ...base, text: t.slice(1, -1), font: 'Consolas', size: 19 }));
    } else {
      saida.push(new TextRun({ ...base, text: limpar(t.slice(1, -1)), italics: true }));
    }
    ultimo = m.index + t.length;
  }
  if (ultimo < texto.length) {
    saida.push(new TextRun({ ...base, text: limpar(texto.slice(ultimo)) }));
  }
  return saida.length ? saida : [new TextRun({ ...base, text: '' })];
}

/** Remove sintaxe de link e escapes que não fazem sentido no Word. */
function limpar(t) {
  return t
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/\\([_*])/g, '$1');
}

/* ------------------------------------------------------------- tabelas --- */

function celulas(linha) {
  return linha.replace(/^\||\|$/g, '').split('|').map((c) => c.trim());
}

function montarTabela(linhas) {
  const cabecalho = celulas(linhas[0]);
  const corpo = linhas.slice(2).map(celulas);
  const nCols = cabecalho.length;
  const larguraCol = Math.floor(LARGURA_TABELA / nCols);
  const colWidths = Array(nCols).fill(larguraCol);
  colWidths[nCols - 1] = LARGURA_TABELA - larguraCol * (nCols - 1);

  const linhaDe = (valores, ehCabecalho) => new TableRow({
    tableHeader: ehCabecalho,
    children: valores.slice(0, nCols).concat(Array(Math.max(0, nCols - valores.length)).fill(''))
      .map((valor, i) => new TableCell({
        width: { size: colWidths[i], type: WidthType.DXA },
        shading: ehCabecalho
          ? { type: ShadingType.CLEAR, fill: CINZA_CAB, color: 'auto' }
          : undefined,
        margins: { top: 60, bottom: 60, left: 100, right: 100 },
        children: [new Paragraph({
          spacing: { before: 0, after: 0 },
          children: runs(valor, ehCabecalho ? { bold: true, size: 19 } : { size: 19 }),
        })],
      })),
  });

  return new Table({
    columnWidths: colWidths,
    width: { size: LARGURA_TABELA, type: WidthType.DXA },
    rows: [linhaDe(cabecalho, true), ...corpo.map((l) => linhaDe(l, false))],
  });
}

/* ------------------------------------------------------- blocos de código --- */

let nFigura = 0;

function montarCodigo(linhas, linguagem) {
  const blocos = [];
  const ehDiagrama = linguagem === 'mermaid';

  if (ehDiagrama) {
    nFigura += 1;
    blocos.push(new Paragraph({
      spacing: { before: 160, after: 60 },
      children: [new TextRun({
        text: `Figura ${nFigura} — diagrama (código-fonte Mermaid)`,
        bold: true, size: 18, color: '666666',
      })],
    }));
  }

  linhas.forEach((linha, i) => {
    blocos.push(new Paragraph({
      spacing: { before: i === 0 ? 40 : 0, after: i === linhas.length - 1 ? 160 : 0 },
      shading: { type: ShadingType.CLEAR, fill: CINZA_COD, color: 'auto' },
      indent: { left: convertInchesToTwip(0.15) },
      children: [new TextRun({ text: linha || ' ', font: 'Consolas', size: 17 })],
    }));
  });

  return blocos;
}

/* --------------------------------------------------------------- parser --- */

const linhas = md.split(/\r?\n/);
const filhos = [];
let i = 0;

// Capa: tudo antes do primeiro "---" isolado
while (i < linhas.length) {
  const l = linhas[i];

  // ── bloco de código
  if (l.trimStart().startsWith('```')) {
    const linguagem = l.trim().replace(/```/g, '').trim();
    const buffer = [];
    i += 1;
    while (i < linhas.length && !linhas[i].trimStart().startsWith('```')) {
      buffer.push(linhas[i]);
      i += 1;
    }
    i += 1;
    filhos.push(...montarCodigo(buffer, linguagem));
    continue;
  }

  // ── tabela
  if (l.trim().startsWith('|') && linhas[i + 1] && /^\s*\|[\s:|-]+\|\s*$/.test(linhas[i + 1])) {
    const buffer = [];
    while (i < linhas.length && linhas[i].trim().startsWith('|')) {
      buffer.push(linhas[i].trim());
      i += 1;
    }
    filhos.push(montarTabela(buffer));
    filhos.push(new Paragraph({ spacing: { after: 160 }, children: [] }));
    continue;
  }

  // ── sumário: troca a lista manual por um campo de TOC do Word
  if (/^##\s+Sumário\s*$/.test(l)) {
    filhos.push(new Paragraph({
      heading: HeadingLevel.HEADING_1,
      spacing: { before: 360, after: 160 },
      children: [new TextRun({ text: 'Sumário', bold: true, size: 30, color: AZUL })],
      border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: 'C6D3E0', space: 4 } },
    }));
    filhos.push(new TableOfContents('Sumário', { hyperlink: true, headingStyleRange: '1-3' }));
    filhos.push(new Paragraph({ children: [new PageBreak()] }));
    i += 1;
    // pula a lista manual até a próxima régua
    while (i < linhas.length && !/^---+$/.test(linhas[i].trim())) i += 1;
    continue;
  }

  // ── títulos
  const tit = l.match(/^(#{1,4})\s+(.*)$/);
  if (tit) {
    const nivel = tit[1].length;
    // Remove só a numeração de seção de topo ("6. Requisitos"), preservando
    // a composta das subseções ("5.1 Cadastro").
    const texto = nivel === 2
      ? limpar(tit[2]).replace(/^\d+\.\s+/, '')
      : limpar(tit[2]);
    if (nivel === 1) {
      filhos.push(new Paragraph({
        heading: HeadingLevel.TITLE,
        alignment: AlignmentType.CENTER,
        spacing: { before: 0, after: 240 },
        children: [new TextRun({ text: texto, bold: true, size: 40, color: AZUL })],
      }));
    } else {
      const mapa = { 2: HeadingLevel.HEADING_1, 3: HeadingLevel.HEADING_2, 4: HeadingLevel.HEADING_3 };
      const tamanho = { 2: 30, 3: 25, 4: 22 }[nivel];
      filhos.push(new Paragraph({
        heading: mapa[nivel],
        spacing: { before: nivel === 2 ? 360 : 240, after: 120 },
        children: [new TextRun({ text: texto, bold: true, size: tamanho, color: AZUL })],
        ...(nivel === 2 && {
          border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: 'C6D3E0', space: 4 } },
        }),
      }));
    }
    i += 1;
    continue;
  }

  // ── régua
  if (/^---+$/.test(l.trim())) { i += 1; continue; }

  // ── citação
  if (l.trimStart().startsWith('>')) {
    const buffer = [];
    while (i < linhas.length && linhas[i].trimStart().startsWith('>')) {
      buffer.push(linhas[i].replace(/^\s*>\s?/, ''));
      i += 1;
    }
    filhos.push(new Paragraph({
      spacing: { before: 120, after: 160 },
      indent: { left: convertInchesToTwip(0.25) },
      border: { left: { style: BorderStyle.SINGLE, size: 12, color: AZUL, space: 8 } },
      children: runs(buffer.join(' '), { italics: true, color: '444444' }),
    }));
    continue;
  }

  // ── lista
  const item = l.match(/^\s*([-*]|\d+\.)\s+(.*)$/);
  if (item) {
    const numerada = /^\d+\./.test(item[1]);
    filhos.push(new Paragraph({
      numbering: { reference: numerada ? 'lista-num' : 'lista-bullet', level: 0 },
      spacing: { before: 20, after: 20 },
      children: runs(item[2]),
    }));
    i += 1;
    continue;
  }

  // ── parágrafo (junta linhas contíguas)
  if (l.trim()) {
    const buffer = [];
    while (
      i < linhas.length && linhas[i].trim()
      && !linhas[i].trim().startsWith('|')
      && !linhas[i].trimStart().startsWith('```')
      && !/^#{1,4}\s/.test(linhas[i])
      && !/^\s*([-*]|\d+\.)\s+/.test(linhas[i])
      && !linhas[i].trimStart().startsWith('>')
      && !/^---+$/.test(linhas[i].trim())
    ) {
      buffer.push(linhas[i].trim());
      i += 1;
    }
    filhos.push(new Paragraph({
      spacing: { before: 40, after: 120, line: 276 },
      alignment: AlignmentType.JUSTIFIED,
      children: runs(buffer.join(' ')),
    }));
    continue;
  }

  i += 1;
}

// Sumário automático depois da capa
const posSumario = filhos.findIndex((f) => f.constructor.name === 'Paragraph' && f.options
  && f.options.heading === HeadingLevel.HEADING_1);

const doc = new Document({
  creator: 'Equipe Mora — PUCPR',
  title: 'Especificação do Projeto — Mora',
  description: 'Especificação de requisitos do sistema Mora',
  numbering: {
    config: [
      {
        reference: 'lista-bullet',
        levels: [{
          level: 0, format: LevelFormat.BULLET, text: '•',
          alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 460, hanging: 230 } } },
        }],
      },
      {
        reference: 'lista-num',
        levels: [{
          level: 0, format: LevelFormat.DECIMAL, text: '%1.',
          alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 460, hanging: 230 } } },
        }],
      },
    ],
  },
  styles: {
    default: {
      document: { run: { font: 'Calibri', size: 21 } },
    },
  },
  sections: [{
    properties: {
      page: { margin: { top: 1134, right: 1134, bottom: 1134, left: 1134 } },
    },
    children: filhos,
  }],
});

Packer.toBuffer(doc).then((buf) => {
  fs.writeFileSync(saida, buf);
  console.log(`gerado: ${saida} (${(buf.length / 1024).toFixed(0)} KB, ${nFigura} diagramas)`);
});
