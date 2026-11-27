-- =============================================================================
-- crUEL — Sistema de Avaliação Anônima de Disciplinas e Docentes
-- Banco de Dados I — UEL
--
-- schema.sql — DDL completo (PostgreSQL 16+), normalizado até a 3FN.
-- Script idempotente: pode ser executado repetidas vezes em um banco vazio
-- ou já populado (as tabelas são recriadas).
--
-- Particularidade da natureza (RF6 — anonimato + unicidade):
--   * participacao_avaliacao  -> registra QUEM respondeu (PK (avaliacao, usuario)
--                                impede uma 2ª resposta do mesmo aluno).
--   * submissao_anonima       -> registra O QUE foi respondido, SEM qualquer
--   * resposta_questao           coluna/FK que leve ao usuário. A chave da
--                                submissão é um UUID aleatório e as datas são
--                                armazenadas apenas com granularidade de dia,
--                                impedindo correlacionar a ordem das
--                                participações com a ordem das submissões.
-- =============================================================================

-- Os scripts são UTF-8: força a codificação do cliente para não corromper acentos
-- no Windows (onde o psql usa a página de código do console, ex.: WIN1252).
SET client_encoding = 'UTF8';

DROP TABLE IF EXISTS resposta_questao       CASCADE;
DROP TABLE IF EXISTS submissao_anonima      CASCADE;
DROP TABLE IF EXISTS participacao_avaliacao CASCADE;
DROP TABLE IF EXISTS avaliacao_questao      CASCADE;
DROP TABLE IF EXISTS alternativa            CASCADE;
DROP TABLE IF EXISTS questao                CASCADE;
DROP TABLE IF EXISTS avaliacao              CASCADE;
DROP TABLE IF EXISTS matricula_oferta       CASCADE;
DROP TABLE IF EXISTS oferta_disciplina      CASCADE;
DROP TABLE IF EXISTS disciplina             CASCADE;
DROP TABLE IF EXISTS usuario                CASCADE;

-- -----------------------------------------------------------------------------
-- USUARIO: administradores, docentes e alunos.
-- senha_hash = SHA-256 (hex) da senha.
-- -----------------------------------------------------------------------------
CREATE TABLE usuario (
    id_usuario  SERIAL       PRIMARY KEY,
    matricula   VARCHAR(20)  NOT NULL,
    nome        VARCHAR(120) NOT NULL,
    email       VARCHAR(120) NOT NULL,
    senha_hash  CHAR(64)     NOT NULL,
    perfil      VARCHAR(10)  NOT NULL,
    CONSTRAINT uq_usuario_matricula UNIQUE (matricula),
    CONSTRAINT uq_usuario_email     UNIQUE (email),
    CONSTRAINT ck_usuario_perfil    CHECK (perfil IN ('ADMIN', 'DOCENTE', 'ALUNO')),
    CONSTRAINT ck_usuario_email     CHECK (email LIKE '%_@_%')
);

-- -----------------------------------------------------------------------------
-- DISCIPLINA: catálogo de disciplinas do curso.
-- -----------------------------------------------------------------------------
CREATE TABLE disciplina (
    id_disciplina SERIAL       PRIMARY KEY,
    codigo        VARCHAR(10)  NOT NULL,
    nome          VARCHAR(120) NOT NULL,
    carga_horaria INTEGER      NOT NULL,
    CONSTRAINT uq_disciplina_codigo UNIQUE (codigo),
    CONSTRAINT ck_disciplina_ch     CHECK (carga_horaria > 0)
);

-- -----------------------------------------------------------------------------
-- OFERTA_DISCIPLINA: uma disciplina ministrada por um docente em um ano/semestre.
-- -----------------------------------------------------------------------------
CREATE TABLE oferta_disciplina (
    id_oferta     SERIAL      PRIMARY KEY,
    id_disciplina INTEGER     NOT NULL,
    id_docente    INTEGER     NOT NULL,
    ano           INTEGER     NOT NULL,
    semestre      INTEGER     NOT NULL,
    turma         VARCHAR(10) NOT NULL,
    CONSTRAINT fk_oferta_disciplina FOREIGN KEY (id_disciplina)
        REFERENCES disciplina (id_disciplina) ON DELETE RESTRICT,
    CONSTRAINT fk_oferta_docente FOREIGN KEY (id_docente)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT uq_oferta UNIQUE (id_disciplina, ano, semestre, turma),
    CONSTRAINT ck_oferta_semestre CHECK (semestre IN (1, 2)),
    CONSTRAINT ck_oferta_ano      CHECK (ano >= 2020)
);

-- -----------------------------------------------------------------------------
-- MATRICULA_OFERTA: alunos matriculados (elegíveis a avaliar) em cada oferta.
-- -----------------------------------------------------------------------------
CREATE TABLE matricula_oferta (
    id_oferta  INTEGER NOT NULL,
    id_usuario INTEGER NOT NULL,
    CONSTRAINT pk_matricula_oferta PRIMARY KEY (id_oferta, id_usuario),
    CONSTRAINT fk_matricula_oferta FOREIGN KEY (id_oferta)
        REFERENCES oferta_disciplina (id_oferta) ON DELETE CASCADE,
    CONSTRAINT fk_matricula_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- AVALIACAO: questionário aplicado a uma oferta (no máximo uma por oferta).
-- Janela de resposta: [data_avaliacao hora_inicio, data_encerramento hora_fim].
-- -----------------------------------------------------------------------------
CREATE TABLE avaliacao (
    id_avaliacao      SERIAL       PRIMARY KEY,
    id_oferta         INTEGER      NOT NULL,
    titulo            VARCHAR(150) NOT NULL,
    descricao         TEXT,
    data_avaliacao    DATE         NOT NULL,
    data_encerramento DATE         NOT NULL,
    hora_inicio       TIME         NOT NULL,
    hora_fim          TIME         NOT NULL,
    ativa             BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_avaliacao_oferta FOREIGN KEY (id_oferta)
        REFERENCES oferta_disciplina (id_oferta) ON DELETE RESTRICT,
    CONSTRAINT uq_avaliacao_oferta UNIQUE (id_oferta),
    CONSTRAINT ck_avaliacao_horario CHECK (hora_fim > hora_inicio),
    CONSTRAINT ck_avaliacao_periodo CHECK (data_encerramento >= data_avaliacao)
);

-- -----------------------------------------------------------------------------
-- QUESTAO: banco de questões reutilizáveis entre avaliações.
-- -----------------------------------------------------------------------------
CREATE TABLE questao (
    id_questao SERIAL      PRIMARY KEY,
    enunciado  TEXT        NOT NULL,
    tipo       VARCHAR(20) NOT NULL,
    categoria  VARCHAR(20) NOT NULL,
    ativa      BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_questao_tipo CHECK (tipo IN ('MULTIPLA_ESCOLHA', 'ESCALA_1_5', 'TEXTO_LIVRE')),
    CONSTRAINT ck_questao_categoria CHECK (categoria IN ('DIDATICA', 'INFRAESTRUTURA', 'CONTEUDO', 'GERAL')),
    CONSTRAINT ck_questao_enunciado CHECK (length(trim(enunciado)) > 0)
);

-- -----------------------------------------------------------------------------
-- ALTERNATIVA: opções das questões de múltipla escolha.
-- UNIQUE (id_alternativa, id_questao) permite a FK composta em resposta_questao,
-- garantindo que a alternativa escolhida pertença à questão respondida.
-- -----------------------------------------------------------------------------
CREATE TABLE alternativa (
    id_alternativa SERIAL  PRIMARY KEY,
    id_questao     INTEGER NOT NULL,
    texto          TEXT    NOT NULL,
    valor_numerico INTEGER NOT NULL,
    ordem          INTEGER NOT NULL,
    CONSTRAINT fk_alternativa_questao FOREIGN KEY (id_questao)
        REFERENCES questao (id_questao) ON DELETE CASCADE,
    CONSTRAINT uq_alternativa_ordem UNIQUE (id_questao, ordem),
    CONSTRAINT uq_alternativa_questao UNIQUE (id_alternativa, id_questao),
    CONSTRAINT ck_alternativa_ordem CHECK (ordem > 0)
);

-- -----------------------------------------------------------------------------
-- AVALIACAO_QUESTAO: composição (N:N) de uma avaliação com questões do banco.
-- -----------------------------------------------------------------------------
CREATE TABLE avaliacao_questao (
    id_avaliacao INTEGER NOT NULL,
    id_questao   INTEGER NOT NULL,
    ordem        INTEGER NOT NULL,
    obrigatoria  BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_avaliacao_questao PRIMARY KEY (id_avaliacao, id_questao),
    CONSTRAINT fk_aq_avaliacao FOREIGN KEY (id_avaliacao)
        REFERENCES avaliacao (id_avaliacao) ON DELETE CASCADE,
    CONSTRAINT fk_aq_questao FOREIGN KEY (id_questao)
        REFERENCES questao (id_questao) ON DELETE RESTRICT,
    CONSTRAINT uq_aq_ordem UNIQUE (id_avaliacao, ordem),
    CONSTRAINT ck_aq_ordem CHECK (ordem > 0)
);

-- -----------------------------------------------------------------------------
-- PARTICIPACAO_AVALIACAO (RF6): quem já respondeu. A PK composta bloqueia a
-- segunda resposta do mesmo aluno à mesma avaliação (= oferta/semestre).
-- NÃO há nenhuma ligação desta tabela com as respostas.
-- -----------------------------------------------------------------------------
CREATE TABLE participacao_avaliacao (
    id_avaliacao      INTEGER NOT NULL,
    id_usuario        INTEGER NOT NULL,
    data_participacao DATE    NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT pk_participacao PRIMARY KEY (id_avaliacao, id_usuario),
    CONSTRAINT fk_participacao_avaliacao FOREIGN KEY (id_avaliacao)
        REFERENCES avaliacao (id_avaliacao) ON DELETE CASCADE,
    CONSTRAINT fk_participacao_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- SUBMISSAO_ANONIMA (RF6): um "envelope" de respostas, sem id_usuario.
-- Chave UUID aleatória (gen_random_uuid, nativo no PostgreSQL 13+) para que a
-- ordem de inserção não revele a ordem das participações.
-- -----------------------------------------------------------------------------
CREATE TABLE submissao_anonima (
    id_submissao   UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    id_avaliacao   INTEGER NOT NULL,
    data_submissao DATE    NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT fk_submissao_avaliacao FOREIGN KEY (id_avaliacao)
        REFERENCES avaliacao (id_avaliacao) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- RESPOSTA_QUESTAO: resposta a cada questão dentro de uma submissão anônima.
-- Exatamente um dos campos (alternativa | nota | texto) é preenchido.
-- -----------------------------------------------------------------------------
CREATE TABLE resposta_questao (
    id_resposta    SERIAL  PRIMARY KEY,
    id_submissao   UUID    NOT NULL,
    id_questao     INTEGER NOT NULL,
    id_alternativa INTEGER,
    nota_escala    INTEGER,
    texto_resposta TEXT,
    CONSTRAINT fk_resposta_submissao FOREIGN KEY (id_submissao)
        REFERENCES submissao_anonima (id_submissao) ON DELETE CASCADE,
    CONSTRAINT fk_resposta_questao FOREIGN KEY (id_questao)
        REFERENCES questao (id_questao) ON DELETE RESTRICT,
    CONSTRAINT fk_resposta_alternativa FOREIGN KEY (id_alternativa, id_questao)
        REFERENCES alternativa (id_alternativa, id_questao) ON DELETE RESTRICT,
    CONSTRAINT uq_resposta_submissao_questao UNIQUE (id_submissao, id_questao),
    CONSTRAINT ck_resposta_nota CHECK (nota_escala BETWEEN 1 AND 5 OR nota_escala IS NULL),
    CONSTRAINT ck_resposta_um_valor CHECK (num_nonnulls(id_alternativa, nota_escala, texto_resposta) = 1)
);

-- Índices de apoio às consultas analíticas e às FKs mais consultadas.
CREATE INDEX idx_oferta_periodo       ON oferta_disciplina (ano, semestre);
CREATE INDEX idx_matricula_usuario    ON matricula_oferta (id_usuario);
CREATE INDEX idx_participacao_usuario ON participacao_avaliacao (id_usuario);
CREATE INDEX idx_submissao_avaliacao  ON submissao_anonima (id_avaliacao);
CREATE INDEX idx_resposta_questao     ON resposta_questao (id_questao);
