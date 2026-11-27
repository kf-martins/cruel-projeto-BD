# MER e Esquema Relacional

Documento de modelagem do **crUEL — Sistema de Avaliação Anônima de Disciplinas e Docentes**
(Banco de Dados I — UEL). O diagrama está em [`DER.md`](DER.md) / [`DER.png`](DER.png) e o DDL em
[`../database/schema.sql`](../database/schema.sql).

## 1. Modelo Entidade-Relacionamento (MER)

### Entidades

| Entidade | Descrição | Atributos | Identificador |
|---|---|---|---|
| **USUARIO** | Pessoas que acessam o sistema (administradores, docentes e alunos). | matrícula, nome, e-mail, hash da senha, perfil | id_usuario |
| **DISCIPLINA** | Disciplina do catálogo do curso. | código, nome, carga horária | id_disciplina |
| **OFERTA_DISCIPLINA** | Disciplina ministrada por um docente em um ano/semestre/turma. | ano, semestre, turma | id_oferta |
| **AVALIACAO** | Questionário aplicado a uma oferta, com janela de resposta. | título, descrição, data de abertura, data de encerramento, hora de início, hora de fim, ativa | id_avaliacao |
| **QUESTAO** | Questão reutilizável do banco de questões. | enunciado, tipo, categoria, ativa | id_questao |
| **ALTERNATIVA** | Opção de uma questão de múltipla escolha (entidade fraca de QUESTAO). | texto, valor numérico, ordem | id_alternativa |
| **SUBMISSAO_ANONIMA** | "Envelope" anônimo com as respostas de um aluno a uma avaliação. | data da submissão | id_submissao (UUID) |
| **RESPOSTA_QUESTAO** | Resposta a uma questão dentro de uma submissão. | alternativa escolhida **ou** nota 1–5 **ou** texto | id_resposta |

### Relacionamentos

| Relacionamento | Entidades | Cardinalidade | Atributos do relacionamento |
|---|---|---|---|
| ministra | USUARIO (docente) — OFERTA_DISCIPLINA | 1 : N | — |
| possui | DISCIPLINA — OFERTA_DISCIPLINA | 1 : N | — |
| **matricula** | OFERTA_DISCIPLINA — USUARIO (aluno) | N : M | — |
| é avaliada por | OFERTA_DISCIPLINA — AVALIACAO | 1 : 0..1 | — |
| **compõe** | AVALIACAO — QUESTAO | N : M | ordem, obrigatória |
| oferece | QUESTAO — ALTERNATIVA | 1 : N | — |
| **participa** | AVALIACAO — USUARIO (aluno) | N : M | data da participação |
| recebe | AVALIACAO — SUBMISSAO_ANONIMA | 1 : N | — |
| detalha | SUBMISSAO_ANONIMA — RESPOSTA_QUESTAO | 1 : N | — |
| é respondida em | QUESTAO — RESPOSTA_QUESTAO | 1 : N | — |
| é escolhida em | ALTERNATIVA — RESPOSTA_QUESTAO | 0..1 : N | — |

### Regras de negócio capturadas pelo modelo

- **RF6 — unicidade sem identificação.** O relacionamento *participa* (quem respondeu) e a entidade
  SUBMISSAO_ANONIMA (o que foi respondido) são independentes: compartilham apenas a avaliação.
- Cada **oferta** tem no máximo **uma avaliação** — logo, "uma resposta por avaliação" equivale a
  "uma resposta por disciplina/docente por semestre".
- Só alunos **matriculados** na oferta podem responder (verificado na transação do `RespostaService`).
- Uma resposta só pode escolher uma alternativa **da própria questão** (FK composta).

## 2. Mapeamento para o Esquema Relacional

Notação: **sublinhado/negrito** = chave primária; *itálico* = chave estrangeira.

```
USUARIO                (id_usuario, matricula, nome, email, senha_hash, perfil)
                       PK id_usuario · UNIQUE matricula · UNIQUE email
                       CHECK perfil IN ('ADMIN','DOCENTE','ALUNO')

DISCIPLINA             (id_disciplina, codigo, nome, carga_horaria)
                       PK id_disciplina · UNIQUE codigo · CHECK carga_horaria > 0

OFERTA_DISCIPLINA      (id_oferta, id_disciplina, id_docente, ano, semestre, turma)
                       PK id_oferta
                       FK id_disciplina → DISCIPLINA · FK id_docente → USUARIO
                       UNIQUE (id_disciplina, ano, semestre, turma)
                       CHECK semestre IN (1,2) · CHECK ano >= 2020

MATRICULA_OFERTA       (id_oferta, id_usuario)
                       PK (id_oferta, id_usuario)
                       FK id_oferta → OFERTA_DISCIPLINA · FK id_usuario → USUARIO

AVALIACAO              (id_avaliacao, id_oferta, titulo, descricao, data_avaliacao,
                        data_encerramento, hora_inicio, hora_fim, ativa)
                       PK id_avaliacao · FK id_oferta → OFERTA_DISCIPLINA · UNIQUE id_oferta
                       CHECK hora_fim > hora_inicio · CHECK data_encerramento >= data_avaliacao

QUESTAO                (id_questao, enunciado, tipo, categoria, ativa)
                       PK id_questao
                       CHECK tipo IN ('MULTIPLA_ESCOLHA','ESCALA_1_5','TEXTO_LIVRE')
                       CHECK categoria IN ('DIDATICA','INFRAESTRUTURA','CONTEUDO','GERAL')

ALTERNATIVA            (id_alternativa, id_questao, texto, valor_numerico, ordem)
                       PK id_alternativa · FK id_questao → QUESTAO
                       UNIQUE (id_questao, ordem) · UNIQUE (id_alternativa, id_questao)

AVALIACAO_QUESTAO      (id_avaliacao, id_questao, ordem, obrigatoria)
                       PK (id_avaliacao, id_questao) · UNIQUE (id_avaliacao, ordem)
                       FK id_avaliacao → AVALIACAO · FK id_questao → QUESTAO

PARTICIPACAO_AVALIACAO (id_avaliacao, id_usuario, data_participacao)
                       PK (id_avaliacao, id_usuario)      ← bloqueia a 2ª resposta (RF6)
                       FK id_avaliacao → AVALIACAO · FK id_usuario → USUARIO

SUBMISSAO_ANONIMA      (id_submissao, id_avaliacao, data_submissao)
                       PK id_submissao (UUID aleatório) · FK id_avaliacao → AVALIACAO
                       — nenhuma coluna ou FK para USUARIO (RF6)

RESPOSTA_QUESTAO       (id_resposta, id_submissao, id_questao, id_alternativa,
                        nota_escala, texto_resposta)
                       PK id_resposta
                       FK id_submissao → SUBMISSAO_ANONIMA · FK id_questao → QUESTAO
                       FK (id_alternativa, id_questao) → ALTERNATIVA (id_alternativa, id_questao)
                       UNIQUE (id_submissao, id_questao)
                       CHECK nota_escala BETWEEN 1 AND 5
                       CHECK num_nonnulls(id_alternativa, nota_escala, texto_resposta) = 1
```

### Regras de mapeamento aplicadas

1. **Entidades fortes** → uma tabela cada, com chave substituta `SERIAL` (ou `UUID` na submissão).
2. **Relacionamentos 1 : N** → FK no lado N (ex.: `id_disciplina` e `id_docente` em `OFERTA_DISCIPLINA`).
3. **Relacionamento 1 : 0..1** (oferta–avaliação) → FK em `AVALIACAO` com restrição `UNIQUE`.
4. **Relacionamentos N : M** → tabelas associativas com PK composta pelas duas FKs
   (`MATRICULA_OFERTA`, `AVALIACAO_QUESTAO`, `PARTICIPACAO_AVALIACAO`); atributos do relacionamento
   (ordem, obrigatória, data) ficam na própria tabela associativa.
5. **Entidade fraca** ALTERNATIVA → FK para QUESTAO com `ON DELETE CASCADE`.
6. **Atributos com domínio restrito** → `CHECK` (perfil, tipo, categoria, semestre, nota 1–5…).

## 3. Justificativa da 3ª Forma Normal (3FN)

**1FN.** Todas as relações possuem chave primária definida e apenas atributos atômicos, sem grupos
repetitivos: as alternativas de uma questão não são uma lista dentro de `QUESTAO`, mas linhas de
`ALTERNATIVA`; as questões de uma avaliação são linhas de `AVALIACAO_QUESTAO`; cada resposta é uma
linha de `RESPOSTA_QUESTAO` (e não colunas `resposta1`, `resposta2`…).

**2FN.** Nas relações com chave primária composta, todo atributo não-chave depende da chave
**inteira**: em `AVALIACAO_QUESTAO`, `ordem` e `obrigatoria` dependem do par (avaliação, questão) —
a mesma questão pode ser a 3ª e obrigatória em uma avaliação e a 10ª e opcional em outra; em
`PARTICIPACAO_AVALIACAO`, `data_participacao` depende do par (avaliação, aluno); `MATRICULA_OFERTA`
não possui atributos não-chave. As demais relações têm chave simples e, portanto, já estão em 2FN.

**3FN.** Não há dependências transitivas entre atributos não-chave:

- dados da disciplina (`nome`, `carga_horaria`) e do docente (`nome`, `email`) estão isolados em
  `DISCIPLINA` e `USUARIO` e são referenciados apenas por FK em `OFERTA_DISCIPLINA` — guardar
  `nome_disciplina` na oferta criaria a dependência `id_oferta → id_disciplina → nome_disciplina`;
- `AVALIACAO` guarda apenas `id_oferta`; disciplina, docente, ano e semestre são obtidos por JOIN;
- `RESPOSTA_QUESTAO` guarda `id_alternativa`, e não o texto ou o valor da alternativa, que dependem
  de `id_alternativa` (não da resposta);
- `SUBMISSAO_ANONIMA` guarda `id_avaliacao`, sem repetir dados da oferta.

Assim, em todas as relações cada atributo não-chave depende **da chave, da chave inteira e de nada
além da chave** — o esquema está na **3FN**.

> Observação: `RESPOSTA_QUESTAO` contém `id_questao` mesmo quando há `id_alternativa` (que também
> determina a questão). Isso **não** viola a 3FN, pois `id_questao` faz parte da FK composta
> `(id_alternativa, id_questao)` usada justamente para garantir, pelo próprio SGBD, que a alternativa
> escolhida pertence à questão respondida — e é necessário para respostas de escala e de texto.

## 4. Por que o anonimato é garantido no nível do esquema (RF6)

| Pergunta | Tabela consultada | Possível? |
|---|---|---|
| O aluno X já respondeu a avaliação Y? | `participacao_avaliacao` | Sim (é o que bloqueia a 2ª resposta) |
| Quantos alunos responderam / estão pendentes? | `matricula_oferta` LEFT JOIN `participacao_avaliacao` | Sim (relatório 4) |
| O que o aluno X respondeu? | — | **Não**: não há coluna, FK ou caminho de JOIN entre `usuario`/`participacao_avaliacao` e `submissao_anonima`/`resposta_questao` |
| Dá para cruzar pela ordem de gravação? | — | **Não**: a submissão usa UUID aleatório e ambas as tabelas guardam só a data (sem hora) |

As duas gravações acontecem na **mesma transação** JDBC (`RespostaService`, `@Transactional`): se a
inserção da participação falhar por violar a PK composta, nenhuma resposta é gravada, e vice-versa.
