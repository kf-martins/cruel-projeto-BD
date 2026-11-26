# DER — Diagrama Entidade-Relacionamento

Diagrama do banco de dados do **crUEL** (Avaliação Anônima de Disciplinas e Docentes), fiel ao
`database/schema.sql`. A imagem exportada está em [`DER.png`](DER.png).

![DER do crUEL](DER.png)

## Diagrama (Mermaid)

O GitHub renderiza o bloco abaixo automaticamente.

```mermaid
erDiagram
    USUARIO ||--o{ OFERTA_DISCIPLINA : "ministra (docente)"
    DISCIPLINA ||--o{ OFERTA_DISCIPLINA : "possui ofertas"
    OFERTA_DISCIPLINA ||--o{ MATRICULA_OFERTA : "tem alunos"
    USUARIO ||--o{ MATRICULA_OFERTA : "cursa (aluno)"
    OFERTA_DISCIPLINA ||--o| AVALIACAO : "é avaliada por"
    AVALIACAO ||--o{ AVALIACAO_QUESTAO : "contém"
    QUESTAO ||--o{ AVALIACAO_QUESTAO : "compõe"
    QUESTAO ||--o{ ALTERNATIVA : "oferece (múltipla escolha)"
    AVALIACAO ||--o{ PARTICIPACAO_AVALIACAO : "registra quem respondeu"
    USUARIO ||--o{ PARTICIPACAO_AVALIACAO : "participa"
    AVALIACAO ||--o{ SUBMISSAO_ANONIMA : "recebe"
    SUBMISSAO_ANONIMA ||--o{ RESPOSTA_QUESTAO : "detalha"
    QUESTAO ||--o{ RESPOSTA_QUESTAO : "é respondida em"
    ALTERNATIVA |o--o{ RESPOSTA_QUESTAO : "é escolhida em"

    USUARIO {
        serial id_usuario PK
        varchar matricula UK
        varchar nome
        varchar email UK
        char senha_hash "SHA-256 hex"
        varchar perfil "ADMIN | DOCENTE | ALUNO"
    }
    DISCIPLINA {
        serial id_disciplina PK
        varchar codigo UK
        varchar nome
        int carga_horaria "CHECK > 0"
    }
    OFERTA_DISCIPLINA {
        serial id_oferta PK
        int id_disciplina FK
        int id_docente FK
        int ano "CHECK >= 2020"
        int semestre "CHECK 1 ou 2"
        varchar turma
    }
    MATRICULA_OFERTA {
        int id_oferta PK, FK
        int id_usuario PK, FK
    }
    AVALIACAO {
        serial id_avaliacao PK
        int id_oferta FK "UNIQUE"
        varchar titulo
        text descricao
        date data_avaliacao "abertura"
        date data_encerramento "CHECK >= abertura"
        time hora_inicio
        time hora_fim "CHECK > hora_inicio"
        boolean ativa
    }
    QUESTAO {
        serial id_questao PK
        text enunciado
        varchar tipo "MULTIPLA_ESCOLHA | ESCALA_1_5 | TEXTO_LIVRE"
        varchar categoria "DIDATICA | INFRAESTRUTURA | CONTEUDO | GERAL"
        boolean ativa
    }
    ALTERNATIVA {
        serial id_alternativa PK
        int id_questao FK
        text texto
        int valor_numerico
        int ordem "UNIQUE (id_questao, ordem)"
    }
    AVALIACAO_QUESTAO {
        int id_avaliacao PK, FK
        int id_questao PK, FK
        int ordem "UNIQUE (id_avaliacao, ordem)"
        boolean obrigatoria
    }
    PARTICIPACAO_AVALIACAO {
        int id_avaliacao PK, FK
        int id_usuario PK, FK
        date data_participacao
    }
    SUBMISSAO_ANONIMA {
        uuid id_submissao PK "aleatório"
        int id_avaliacao FK
        date data_submissao
    }
    RESPOSTA_QUESTAO {
        serial id_resposta PK
        uuid id_submissao FK
        int id_questao FK
        int id_alternativa FK "NULL se escala/texto"
        int nota_escala "1..5, NULL se outro"
        text texto_resposta "NULL se objetiva"
    }
```

## Leitura do diagrama

| Relacionamento | Cardinalidade | Observação |
|---|---|---|
| Disciplina — Oferta | 1 : N | Uma disciplina é oferecida em vários anos/semestres/turmas. |
| Usuário (docente) — Oferta | 1 : N | Cada oferta tem um único docente responsável. |
| Oferta — Usuário (aluno) | N : M | Resolvido por `MATRICULA_OFERTA` (alunos elegíveis — RF4). |
| Oferta — Avaliação | 1 : 0..1 | `UNIQUE (id_oferta)`: no máximo uma avaliação por disciplina/semestre/turma. |
| Avaliação — Questão | N : M | Resolvido por `AVALIACAO_QUESTAO`, com ordem e obrigatoriedade (RF3). |
| Questão — Alternativa | 1 : N | Apenas para questões de múltipla escolha (RF1). |
| Avaliação — Usuário | N : M | `PARTICIPACAO_AVALIACAO`: **quem** respondeu (PK composta impede 2ª resposta). |
| Avaliação — Submissão anônima | 1 : N | **O que** foi respondido, sem qualquer ligação com o usuário (RF6). |
| Submissão — Resposta | 1 : N | Uma linha por questão respondida. |

### Particularidade da natureza (RF6)

Observe no diagrama que **não existe caminho** entre `USUARIO` e `SUBMISSAO_ANONIMA`/`RESPOSTA_QUESTAO`:
as duas "ilhas" só se tocam pela `AVALIACAO`. Assim é possível saber *que* um aluno respondeu (e
bloquear uma segunda resposta), mas é impossível saber *o que* ele respondeu. Para evitar correlação
pela ordem de gravação, a submissão usa chave **UUID aleatória** e as duas tabelas guardam apenas a
**data** (sem hora).

## Como regenerar o `DER.png`

1. Abra o [Mermaid Live Editor](https://mermaid.live), cole o bloco `erDiagram` acima e use **Actions → PNG**; ou
2. Com Node.js instalado (o código-fonte do diagrama também está em [`DER.mmd`](DER.mmd)):
   `npx -p @mermaid-js/mermaid-cli mmdc -i docs/DER.mmd -o docs/DER.png -b white -w 2400`
