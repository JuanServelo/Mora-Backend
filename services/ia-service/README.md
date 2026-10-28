# ia-service

**Stack:** Python 3.12 · FastAPI · scikit-learn — **Porta:** 8000 — **Banco:** nenhum
**Status:** em operação — previsão, explicação, leitura de contrato, lembrete preventivo e assistente do síndico

Módulo de IA do Mora. Prevê o risco de atraso de cada unidade na próxima competência,
explica o porquê em português, lê o contrato de locação em PDF para pré-preencher o
cadastro, escreve o lembrete preventivo para quem paga e responde perguntas do síndico.

Primeiro serviço Python da malha, e o único sem banco próprio: lê os CSVs gerados e, no
futuro, o `financeiro` pela API que já existe.

---

## Como rodar

```bash
cd services/ia-service
pip install -r requirements.txt
cp .env.example .env          # e cole a GEMINI_API_KEY
python gerador.py             # gera o histórico sintético
python treino.py              # treina e grava métricas
python -m pytest testes -q    # 39 testes, sem rede
python -m uvicorn main:app --port 8000
```

Painel em <http://localhost:8000>. Documentação da API em `/docs`.

Para o roteiro completo de demonstração, em sete passos:

```bash
python demo.py
```

---

## O que faz, e por quê assim

### Previsão

Classificação binária: *esta unidade vai atrasar mais de 5 dias a fatura da competência N?*

**A Regressão Logística é o modelo publicado**, com o Gradient Boosting como comparação.
Não é falta de ambição. Num modelo aditivo a contribuição de cada feature é **exata** —
`coeficiente × valor padronizado`, em log-odds — e a soma das contribuições mais o
intercepto dá exatamente o logit do score. `testes/test_explicacao.py` verifica essa
identidade. Uma aproximação de modelo de árvore não dá essa garantia, e a explicação é
requisito do sistema, não enfeite.

### Validação temporal

Treina nas competências antigas, testa nas recentes; o `StandardScaler` é ajustado só no
treino. Split aleatório vazaria futuro e inflaria a métrica.

A armadilha mais sutil está resolvida em `features.py`: uma fatura de março paga em
setembro tem hoje `status = 'PAGA'`, mas no corte de junho estava **em aberto e vencida**.
Nenhuma feature lê `status` — coluna mutável é vazamento por construção. O estado é
reconstruído de `pago_em` contra o corte, e `testes/test_vazamento.py` prova isso
truncando a série e exigindo vetores idênticos.

### Dados

Sintéticos, por metodologia: o Mora não tem 12 meses de histórico de pagamento.
**O modelo não tem validade preditiva no mundo real** — o que se demonstra é o método.

O gerador foi calibrado pelo teto teórico que implica, não por tentativa. Como o alvo é
`Bernoulli(propensão)`, existe um AUC máximo alcançável mesmo conhecendo a propensão
exata:

| Parâmetro | Teto teórico | Janela de 12 meses | Modelo |
|---|---|---|---|
| `Beta(0.95, 3.0)` | 0,72 | 0,68 | — |
| `Beta(0.50, 1.3)` | **0,80** | 0,77 | **0,752** |

Com o primeiro, o gerador era o limitador: nenhuma engenharia de feature passaria de 0,72.

### Ética, em código

O valor do aluguel está no banco (`contratos_locacao.valor_aluguel_centavos`), é fácil de
ler e melhoraria a métrica. Fica fora: é proxy socioeconômico direto, e usá-lo seria
prever atraso a partir de quanto a pessoa pode pagar de moradia.

`testes/test_etica.py` quebra a build se alguém o acrescentar — inclusive um teste que
prova que a guarda pega a violação, em vez de passar por vacuidade, e outro que confirma
que o dado **existe** na base, porque sem isso não haveria decisão nenhuma, só ausência.

As métricas saem separadas por tipo de ocupação. O achado atual: o desempenho é diferente entre
unidades alugadas (AUC 0,764) e próprias (0,735). Diluído na média, um erro sistemático
contra um dos grupos desapareceria.

### LLM

Gemini via REST, sem SDK. Quatro usos, do mais contido ao mais autônomo:

**Leitura do contrato.** A cláusula de encargos vem em prosa jurídica, não como campo
etiquetado: *"Correrão integralmente por conta do LOCATÁRIO [...] as despesas ordinárias
de condomínio"* é a mesma informação que `responsavel_taxas = 'INQUILINO'`, e as dez
formas de escrever isso não cabem em nenhuma expressão regular. A saída é tipada por
`responseSchema` com o mesmo `enum` do CHECK da migration, traz confiança por campo e a
**citação do trecho** — que o serviço confere contra o documento antes de devolver.

**Explicação do score.** O LLM recebe o número já decidido e os fatores já medidos, e
escreve o parágrafo. `verificar_fidelidade()` confere que o texto cita o mesmo score; se
divergir, o texto é descartado e usa-se a versão determinística. O LLM traduz, não
recalcula.

**Lembrete preventivo.** Para a unidade de risco alto, o Gemini escreve um lembrete
cordial para quem paga — o inquilino ou o proprietário, conforme a cláusula do contrato.
O lembrete **nunca revela a previsão**: a entrada do LLM não leva score, fatores nem
histórico, e `lembrete.verificar()` recusa o texto que fale de risco, previsão, histórico,
atraso, multa ou percentual, que não cite o vencimento ou que trate a pessoa por outro nome.
Recusado, sai um texto fixo que passa na mesma regra. Com `enviar`, o lembrete vai ao sino
do Mora pela rota interna do `comunicacao-service`, com chave única: reenviar não duplica.

**Assistente do síndico (tool calling).** O único uso em que o LLM decide o que fazer. O
síndico pergunta em português; o Gemini escolhe entre cinco funções (`resumo_do_condominio`,
`unidades_em_risco`, `risco_da_unidade`, `contrato_da_unidade`, `desempenho_do_modelo`),
o serviço executa e devolve o resultado, e o modelo responde. Ele nunca toca os dados: as
funções são as mesmas do painel. Nenhuma devolve valor de aluguel, e todo percentual da
resposta precisa existir no que as funções devolveram — senão a resposta é descartada e o
síndico recebe os dados consultados.

Nada é gravado sem revisão humana. Uma cláusula extraída errado inverte quem deve pagar a
taxa — o erro mais caro que este sistema pode cometer —, então a falha devolve formulário
em branco com o motivo, em vez de um palpite.

---

## Robustez na apresentação

| Risco | O que o serviço faz |
|---|---|
| Modelo descontinuado (404) ou sobrecarregado (503/429) | Sonda candidatos e cai no próximo que responder |
| Modelo lento (timeout de 25s) | Mesma cascata do 503. Medido em 28/10: o `gemini-3.6-flash` alternou entre 3s e mais de 25s |
| Rede indisponível | `IA_OFFLINE=1` lê as respostas reais gravadas em `fixtures/respostas_gemini/`; o assistente reproduz a conversa gravada para a mesma pergunta |
| Chave ausente | Explicação determinística; extração devolve formulário em branco |
| JSON inválido do modelo | Uma tentativa de reparo, depois erro explícito |
| Unidade sem histórico | `sem_previsao`, com o motivo — não chuta |

O cache é indexado só pelo pedido, não pelo modelo: trocar de modelo não descarta o cache,
e a troca acontece justamente quando a rede está ruim.

---

## Endpoints

| Rota | O que faz |
|---|---|
| `GET /health` | estado do serviço e se o modelo está carregado |
| `GET /api/ia/metricas` | relatório de treino: métricas, auditoria de viés, importâncias |
| `GET /api/ia/painel?limite=` | unidades ordenadas por risco |
| `POST /api/ia/predicao` | score, classe, contribuições e explicação de uma unidade |
| `POST /api/ia/contrato/extrair` | PDF do contrato → campos para revisão humana |
| **`POST /api/ia/contrato/analisar`** | **o fluxo completo: PDF × histórico → risco e destinatário** |
| `GET /api/ia/contratos` | os contratos de demonstração disponíveis |
| `POST /api/ia/contrato/analisar-exemplo?nome=` | analisa um contrato de demonstração |
| **`POST /api/ia/lembrete`** | contrato de demonstração → lembrete escrito pelo Gemini para quem paga; `enviar: true` publica no sino do Mora |
| **`POST /api/ia/assistente`** | pergunta do síndico → funções chamadas pelo Gemini → resposta conferida |

### O fluxo que junta as duas metades

`POST /api/ia/contrato/analisar` é o centro da demonstração. Recebe o PDF e devolve,
numa resposta só:

1. os campos extraídos pelo LLM, com citação conferida contra o documento;
2. a unidade correspondente no cadastro, casada pelo endereço;
3. o histórico de pagamento que o modelo leu;
4. o risco da próxima competência, com explicação em português;
5. **para quem a cobrança deve sair** — inquilino ou proprietário, conforme a cláusula.

Prever o risco não basta: saber que a unidade 302 vai atrasar não serve de nada se o
lembrete for para o proprietário quando quem paga é o inquilino.

Seis contratos de demonstração cobrem desfechos diferentes — risco alto com o inquilino
pagando, risco alto com o **proprietário** pagando, contrato vencendo em 35 dias, e uma
unidade nova em que o sistema **recusa** a previsão. A cláusula de encargos é redigida de
seis maneiras distintas, que é o argumento para usar um LLM em vez de uma regex.

```bash
python fixture_contratos.py   # gera os seis PDFs e o índice
python analise.py             # analisa todos, no terminal
```

---

## Variáveis de ambiente

| Variável | Para quê |
|---|---|
| `GEMINI_API_KEY` | obrigatória para os dois usos de LLM |
| `GEMINI_MODEL` | fixa o modelo; vazio = descoberta por sondagem |
| `IA_OFFLINE` | `1` usa só o cache, sem rede |
| `IA_PORT` | porta do serviço (padrão 8000) |
| `COMUNICACAO_URL` | onde publicar o lembrete (padrão: o gateway, `http://localhost:8087/api/comunicacao`) |
| `SERVICO_TOKEN` | o mesmo do `docker/.env`; sem ele o lembrete é gerado mas não enviado |
| `IA_DEMO_USUARIO_ID`, `IA_DEMO_CONDOMINIO_ID` | quem recebe o lembrete na demonstração: os dados daqui são sintéticos, e o envio vai a um usuário real do seed (ex.: `4`, `cond-parque-verde`) |

O `.env` está no `.gitignore` e nunca entra no repositório.

---

## O que ficou de fora, por decisão

| Item | Por quê |
|---|---|
| Integração HTTP com o `financeiro` | Exigiria endpoints paginados que não existem, e o serviço roda sem Docker de propósito |
| Validação em base pública (UCI) | Download, EDA e pipeline paralelo sem rubrica própria |
| `shap` | Extensão C sem wheel estável para numpy 2.4; o modelo publicado é aditivo e a decomposição é exata |
| `xgboost` | Dependência binária que não acrescenta nada ao `GradientBoostingClassifier` |
