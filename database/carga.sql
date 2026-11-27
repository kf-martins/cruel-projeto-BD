-- =============================================================================
-- crUEL — carga.sql (DML)
-- Massa de dados realista do curso de Ciência da Computação / UEL.
-- Pré-requisito: database/schema.sql executado (tabelas vazias).
--
--   * 27 usuários  (2 ADMIN, 4 DOCENTE, 21 ALUNO) — senha de todos: 123456
--   * 4 disciplinas, 5 ofertas (4 em 2026/2 + 1 histórica em 2026/1)
--   * 5 avaliações, 30 questões (15 escala 1–5, 10 múltipla escolha, 5 texto livre)
--   * ~60 participações + submissões anônimas com respostas
--
-- Propositalmente:
--   * alguns alunos matriculados NÃO responderam (relatório de pendentes / LEFT JOIN);
--   * as questões 13, 23 e 30 não pertencem a nenhuma avaliação (LEFT JOIN de questões).
--
-- Os IDs são explícitos para facilitar a leitura; ao final, as sequências são
-- reajustadas com setval() para que novos cadastros via aplicação não colidam.
-- =============================================================================

-- Os scripts são UTF-8: força a codificação do cliente para não corromper acentos
-- no Windows (onde o psql usa a página de código do console, ex.: WIN1252).
SET client_encoding = 'UTF8';

BEGIN;

-- -----------------------------------------------------------------------------
-- USUÁRIOS — senha_hash = SHA-256 hex de '123456' (função nativa sha256 do PG 11+)
-- -----------------------------------------------------------------------------
INSERT INTO usuario (id_usuario, matricula, nome, email, senha_hash, perfil)
SELECT v.id, v.matricula, v.nome, v.email, encode(sha256('123456'::bytea), 'hex'), v.perfil
FROM (VALUES
    ( 1, 'admin01', 'Coord. Computação',          'coordenacao.dc@uel.br',      'ADMIN'),
    ( 2, 'admin02', 'Admin Acadêmico',            'admin.academico@uel.br',     'ADMIN'),
    ( 3, 'doc01',   'Prof. Dr. Anderson Santos',  'anderson.santos@uel.br',     'DOCENTE'),
    ( 4, 'doc02',   'Profa. Dra. Mariana Lima',   'mariana.lima@uel.br',        'DOCENTE'),
    ( 5, 'doc03',   'Prof. Dr. Carlos Mendes',    'carlos.mendes@uel.br',       'DOCENTE'),
    ( 6, 'doc04',   'Profa. Dra. Beatriz Rocha',  'beatriz.rocha@uel.br',       'DOCENTE'),
    ( 7, 'aluno01', 'Ana Silva',                  'ana.silva@uel.br',           'ALUNO'),
    ( 8, 'aluno02', 'Bruno Costa',                'bruno.costa@uel.br',         'ALUNO'),
    ( 9, 'aluno03', 'Camila Oliveira',            'camila.oliveira@uel.br',     'ALUNO'),
    (10, 'aluno04', 'Diego Santos',               'diego.santos@uel.br',        'ALUNO'),
    (11, 'aluno05', 'Eduarda Pereira',            'eduarda.pereira@uel.br',     'ALUNO'),
    (12, 'aluno06', 'Felipe Almeida',             'felipe.almeida@uel.br',      'ALUNO'),
    (13, 'aluno07', 'Gabriela Souza',             'gabriela.souza@uel.br',      'ALUNO'),
    (14, 'aluno08', 'Henrique Ribeiro',           'henrique.ribeiro@uel.br',    'ALUNO'),
    (15, 'aluno09', 'Isabela Carvalho',           'isabela.carvalho@uel.br',    'ALUNO'),
    (16, 'aluno10', 'João Martins',               'joao.martins@uel.br',        'ALUNO'),
    (17, 'aluno11', 'Larissa Gomes',              'larissa.gomes@uel.br',       'ALUNO'),
    (18, 'aluno12', 'Lucas Ferreira',             'lucas.ferreira@uel.br',      'ALUNO'),
    (19, 'aluno13', 'Mariana Barbosa',            'mariana.barbosa@uel.br',     'ALUNO'),
    (20, 'aluno14', 'Nicolas Rodrigues',          'nicolas.rodrigues@uel.br',   'ALUNO'),
    (21, 'aluno15', 'Olívia Araújo',              'olivia.araujo@uel.br',       'ALUNO'),
    (22, 'aluno16', 'Pedro Nascimento',           'pedro.nascimento@uel.br',    'ALUNO'),
    (23, 'aluno17', 'Rafaela Lopes',              'rafaela.lopes@uel.br',       'ALUNO'),
    (24, 'aluno18', 'Samuel Teixeira',            'samuel.teixeira@uel.br',     'ALUNO'),
    (25, 'aluno19', 'Thaís Moreira',              'thais.moreira@uel.br',       'ALUNO'),
    (26, 'aluno20', 'Vinícius Cardoso',           'vinicius.cardoso@uel.br',    'ALUNO'),
    (27, 'aluno21', 'Yasmin Freitas',             'yasmin.freitas@uel.br',      'ALUNO')
) AS v (id, matricula, nome, email, perfil);

-- -----------------------------------------------------------------------------
-- DISCIPLINAS
-- -----------------------------------------------------------------------------
INSERT INTO disciplina (id_disciplina, codigo, nome, carga_horaria) VALUES
    (1, '2COP001', 'Banco de Dados I',          72),
    (2, '2COP002', 'Engenharia de Software',    60),
    (3, '2COP003', 'Sistemas Operacionais',     72),
    (4, '2COP004', 'Redes de Computadores',     60);

-- -----------------------------------------------------------------------------
-- OFERTAS — 4 ofertas em 2026/2 e 1 oferta histórica (BD I em 2026/1)
-- -----------------------------------------------------------------------------
INSERT INTO oferta_disciplina (id_oferta, id_disciplina, id_docente, ano, semestre, turma) VALUES
    (1, 1, 3, 2026, 2, 'A'),   -- Banco de Dados I        — Prof. Anderson
    (2, 2, 4, 2026, 2, 'A'),   -- Engenharia de Software  — Profa. Mariana
    (3, 3, 5, 2026, 2, 'A'),   -- Sistemas Operacionais   — Prof. Carlos
    (4, 4, 6, 2026, 2, 'A'),   -- Redes de Computadores   — Profa. Beatriz
    (5, 1, 3, 2026, 1, 'A');   -- Banco de Dados I (hist.) — Prof. Anderson

-- -----------------------------------------------------------------------------
-- MATRÍCULAS (alunos elegíveis) — id_usuario 7..27 = aluno01..aluno21
-- -----------------------------------------------------------------------------
INSERT INTO matricula_oferta (id_oferta, id_usuario)
SELECT 1, u FROM generate_series(7, 24) AS u                                  -- BD I 2026/2: aluno01..18
UNION ALL
SELECT 2, u FROM generate_series(7, 18) AS u                                  -- ES: aluno01..12
UNION ALL
SELECT 2, u FROM generate_series(25, 27) AS u                                 --     + aluno19..21
UNION ALL
SELECT 3, u FROM generate_series(10, 27) AS u                                 -- SO: aluno04..21
UNION ALL
SELECT 4, u FROM generate_series(7, 18) AS u                                  -- Redes: aluno01..12
UNION ALL
SELECT 5, u FROM generate_series(16, 27) AS u;                                -- BD I 2026/1: aluno10..21

-- -----------------------------------------------------------------------------
-- AVALIAÇÕES — as de 2026/2 estão abertas; a de 2026/1 já foi encerrada.
-- -----------------------------------------------------------------------------
INSERT INTO avaliacao (id_avaliacao, id_oferta, titulo, descricao, data_avaliacao, data_encerramento, hora_inicio, hora_fim, ativa) VALUES
    (1, 1, 'Avaliação de Banco de Dados I — 2026/2',
        'Avalie a disciplina e o docente. Suas respostas são anônimas.',
        DATE '2026-11-01', DATE '2026-12-15', TIME '00:00', TIME '23:59:59', TRUE),
    (2, 2, 'Avaliação de Engenharia de Software — 2026/2',
        'Avalie a disciplina e o docente. Suas respostas são anônimas.',
        DATE '2026-11-01', DATE '2026-12-15', TIME '00:00', TIME '23:59:59', TRUE),
    (3, 3, 'Avaliação de Sistemas Operacionais — 2026/2',
        'Avalie a disciplina e o docente. Suas respostas são anônimas.',
        DATE '2026-11-01', DATE '2026-12-15', TIME '00:00', TIME '23:59:59', TRUE),
    (4, 4, 'Avaliação de Redes de Computadores — 2026/2',
        'Avalie a disciplina e o docente. Suas respostas são anônimas.',
        DATE '2026-11-01', DATE '2026-12-15', TIME '00:00', TIME '23:59:59', TRUE),
    (5, 5, 'Avaliação de Banco de Dados I — 2026/1',
        'Avaliação do semestre anterior (encerrada).',
        DATE '2026-06-15', DATE '2026-07-10', TIME '00:00', TIME '23:59:59', TRUE);

-- -----------------------------------------------------------------------------
-- QUESTÕES (30)
-- -----------------------------------------------------------------------------
INSERT INTO questao (id_questao, enunciado, tipo, categoria, ativa) VALUES
    -- Escala 1–5 (15)
    ( 1, 'O docente explica o conteúdo com clareza.',                                   'ESCALA_1_5', 'DIDATICA',       TRUE),
    ( 2, 'O docente demonstra domínio do conteúdo.',                                    'ESCALA_1_5', 'DIDATICA',       TRUE),
    ( 3, 'O docente estimula a participação dos alunos.',                               'ESCALA_1_5', 'DIDATICA',       TRUE),
    ( 4, 'O docente é pontual e cumpre o horário das aulas.',                           'ESCALA_1_5', 'DIDATICA',       TRUE),
    ( 5, 'O docente está disponível para tirar dúvidas fora do horário de aula.',       'ESCALA_1_5', 'DIDATICA',       TRUE),
    ( 6, 'O conteúdo programático foi cumprido integralmente.',                         'ESCALA_1_5', 'CONTEUDO',       TRUE),
    ( 7, 'O material de apoio (slides, listas, apostilas) é adequado.',                 'ESCALA_1_5', 'CONTEUDO',       TRUE),
    ( 8, 'As avaliações (provas e trabalhos) são coerentes com o conteúdo ministrado.', 'ESCALA_1_5', 'CONTEUDO',       TRUE),
    ( 9, 'Os critérios de avaliação foram apresentados com clareza.',                   'ESCALA_1_5', 'CONTEUDO',       TRUE),
    (10, 'A carga de trabalhos extraclasse é adequada.',                                'ESCALA_1_5', 'CONTEUDO',       TRUE),
    (11, 'A sala de aula oferece condições adequadas (iluminação, ventilação, acústica).', 'ESCALA_1_5', 'INFRAESTRUTURA', TRUE),
    (12, 'Os laboratórios e computadores atendem às necessidades da disciplina.',       'ESCALA_1_5', 'INFRAESTRUTURA', TRUE),
    (13, 'A conexão de internet do campus é suficiente para as atividades.',            'ESCALA_1_5', 'INFRAESTRUTURA', TRUE),
    (14, 'De modo geral, estou satisfeito(a) com a disciplina.',                        'ESCALA_1_5', 'GERAL',          TRUE),
    (15, 'Eu recomendaria esta disciplina e este docente a outros colegas.',            'ESCALA_1_5', 'GERAL',          TRUE),
    -- Múltipla escolha (10)
    (16, 'Com que frequência o docente utiliza exemplos práticos?',                     'MULTIPLA_ESCOLHA', 'DIDATICA',       TRUE),
    (17, 'Como você avalia o ritmo das aulas?',                                         'MULTIPLA_ESCOLHA', 'DIDATICA',       TRUE),
    (18, 'Como você avalia o retorno (feedback) dado às atividades?',                   'MULTIPLA_ESCOLHA', 'DIDATICA',       TRUE),
    (19, 'Quão atualizado é o conteúdo da disciplina?',                                 'MULTIPLA_ESCOLHA', 'CONTEUDO',       TRUE),
    (20, 'Quanto a bibliografia recomendada contribuiu para seu aprendizado?',          'MULTIPLA_ESCOLHA', 'CONTEUDO',       TRUE),
    (21, 'Quanto você sente que aprendeu na disciplina?',                               'MULTIPLA_ESCOLHA', 'CONTEUDO',       TRUE),
    (22, 'Como você avalia os softwares e ferramentas disponibilizados?',               'MULTIPLA_ESCOLHA', 'INFRAESTRUTURA', TRUE),
    (23, 'Como você avalia o ambiente virtual de aprendizagem (Moodle)?',               'MULTIPLA_ESCOLHA', 'INFRAESTRUTURA', TRUE),
    (24, 'Como você avalia a organização geral da disciplina?',                         'MULTIPLA_ESCOLHA', 'GERAL',          TRUE),
    (25, 'Como você avalia o relacionamento entre docente e turma?',                    'MULTIPLA_ESCOLHA', 'GERAL',          TRUE),
    -- Texto livre (5)
    (26, 'Quais são os pontos fortes do docente?',                                      'TEXTO_LIVRE', 'DIDATICA',       TRUE),
    (27, 'O que poderia ser melhorado no conteúdo ou nas avaliações?',                  'TEXTO_LIVRE', 'CONTEUDO',       TRUE),
    (28, 'Sugestões de melhoria para a infraestrutura (salas, laboratórios, rede).',    'TEXTO_LIVRE', 'INFRAESTRUTURA', TRUE),
    (29, 'Comentários gerais sobre a disciplina.',                                      'TEXTO_LIVRE', 'GERAL',          TRUE),
    (30, 'Deixe uma mensagem para a coordenação do curso.',                             'TEXTO_LIVRE', 'GERAL',          TRUE);

-- -----------------------------------------------------------------------------
-- ALTERNATIVAS — 4 por questão de múltipla escolha, valoradas de 1 (pior) a 4 (melhor)
-- -----------------------------------------------------------------------------
INSERT INTO alternativa (id_questao, texto, valor_numerico, ordem)
SELECT q.id_questao, alt.texto, alt.ordem, alt.ordem
FROM (VALUES
    (16, ARRAY['Nunca', 'Raramente', 'Frequentemente', 'Sempre']),
    (17, ARRAY['Muito inadequado', 'Inadequado', 'Adequado', 'Muito adequado']),
    (18, ARRAY['Inexistente', 'Pouco útil', 'Útil', 'Muito útil']),
    (19, ARRAY['Desatualizado', 'Pouco atualizado', 'Atualizado', 'Muito atualizado']),
    (20, ARRAY['Nada', 'Pouco', 'Bastante', 'Muito']),
    (21, ARRAY['Muito pouco', 'Pouco', 'Bastante', 'Muito']),
    (22, ARRAY['Ruim', 'Regular', 'Bom', 'Ótimo']),
    (23, ARRAY['Ruim', 'Regular', 'Bom', 'Ótimo']),
    (24, ARRAY['Ruim', 'Regular', 'Boa', 'Ótima']),
    (25, ARRAY['Ruim', 'Regular', 'Bom', 'Ótimo'])
) AS q (id_questao, textos)
CROSS JOIN LATERAL unnest(q.textos) WITH ORDINALITY AS alt (texto, ordem)
ORDER BY q.id_questao, alt.ordem;

-- -----------------------------------------------------------------------------
-- COMPOSIÇÃO DAS AVALIAÇÕES (ordem = posição no array).
-- Questões de texto livre são opcionais; as demais, obrigatórias.
-- -----------------------------------------------------------------------------
INSERT INTO avaliacao_questao (id_avaliacao, id_questao, ordem, obrigatoria)
SELECT c.id_avaliacao, x.id_questao, x.ordem, q.tipo <> 'TEXTO_LIVRE'
FROM (VALUES
    (1, ARRAY[1, 2, 3, 4, 6, 7, 8, 12, 14, 15, 16, 18, 21, 22, 26, 27, 29]),
    (2, ARRAY[1, 2, 3, 5, 6, 7, 9, 10, 14, 15, 16, 17, 20, 24, 26, 27, 29]),
    (3, ARRAY[1, 2, 4, 6, 8, 9, 11, 12, 14, 15, 17, 19, 21, 22, 25, 26, 28]),
    (4, ARRAY[1, 2, 3, 6, 7, 11, 12, 14, 15, 16, 18, 19, 22, 24, 26, 29]),
    (5, ARRAY[1, 2, 3, 4, 6, 7, 8, 12, 14, 15, 16, 18, 21, 22, 26, 27, 29])
) AS c (id_avaliacao, questoes)
CROSS JOIN LATERAL unnest(c.questoes) WITH ORDINALITY AS x (id_questao, ordem)
JOIN questao q ON q.id_questao = x.id_questao;

-- -----------------------------------------------------------------------------
-- PARTICIPAÇÕES (quem respondeu) — todos os matriculados EXCETO os pendentes.
-- Pendentes propositais: BD I/2026-2: aluno01, aluno05, aluno09 | ES: aluno03, aluno20
--                        SO: aluno07, aluno12, aluno16, aluno21 | Redes: aluno01, aluno06
--                        BD I/2026-1: aluno15
-- -----------------------------------------------------------------------------
SELECT setseed(0.2026);

INSERT INTO participacao_avaliacao (id_avaliacao, id_usuario, data_participacao)
SELECT a.id_avaliacao,
       m.id_usuario,
       a.data_avaliacao + floor(random() * (LEAST(a.data_encerramento, DATE '2026-11-25') - a.data_avaliacao + 1))::int
FROM avaliacao a
JOIN matricula_oferta m ON m.id_oferta = a.id_oferta
JOIN usuario u          ON u.id_usuario = m.id_usuario
WHERE (a.id_avaliacao, u.matricula) NOT IN (
    (1, 'aluno01'), (1, 'aluno05'), (1, 'aluno09'),
    (2, 'aluno03'), (2, 'aluno20'),
    (3, 'aluno07'), (3, 'aluno12'), (3, 'aluno16'), (3, 'aluno21'),
    (4, 'aluno01'), (4, 'aluno06'),
    (5, 'aluno15')
);

-- -----------------------------------------------------------------------------
-- SUBMISSÕES ANÔNIMAS + RESPOSTAS
-- Para cada avaliação, cria tantas submissões quantas participações existirem,
-- SEM qualquer referência ao usuário. As notas seguem um "perfil de qualidade"
-- por oferta (BD I, do Prof. Anderson, tem as melhores notas e ainda melhora de
-- 2026/1 para 2026/2; infraestrutura é pior avaliada),
-- com ruído pseudoaleatório determinístico (setseed).
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    r_av     RECORD;
    r_q      RECORD;
    v_n      INTEGER;
    v_sub    UUID;
    v_base   NUMERIC;
    v_adj    NUMERIC;
    v_nota   INTEGER;
    v_valor  INTEGER;
    v_alt    INTEGER;
    v_textos TEXT[];
    c_didatica TEXT[] := ARRAY[
        'Explica muito bem e sempre traz exemplos práticos.',
        'Muito didático(a) e paciente com as dúvidas da turma.',
        'Domina o conteúdo e relaciona com casos reais do mercado.',
        'As aulas práticas no laboratório ajudam muito a fixar o conteúdo.',
        'Sempre disponível para tirar dúvidas, inclusive por e-mail.',
        'Às vezes o ritmo é rápido demais, mas a explicação é boa.'];
    c_conteudo TEXT[] := ARRAY[
        'Mais listas de exercícios antes das provas.',
        'As provas são longas para o tempo disponível.',
        'Disponibilizar o gabarito das listas ajudaria bastante.',
        'O trabalho final poderia ter um prazo maior.',
        'Gostaria de mais conteúdo sobre aplicações reais.',
        'Os slides poderiam ser disponibilizados antes da aula.'];
    c_infra TEXT[] := ARRAY[
        'Os computadores do laboratório estão lentos.',
        'O ar-condicionado da sala não funciona direito.',
        'Faltam tomadas na sala de aula para os notebooks.',
        'O projetor precisa ser trocado, a imagem está fraca.',
        'O Wi-Fi cai com frequência durante as aulas.'];
    c_geral TEXT[] := ARRAY[
        'Disciplina excelente, aprendi bastante.',
        'Conteúdo pesado, mas muito útil para a carreira.',
        'Gostei muito, recomendo a todos.',
        'A disciplina poderia ter mais aulas práticas.',
        'Bem organizada do início ao fim do semestre.',
        'Achei a carga de trabalhos um pouco alta.'];
BEGIN
    FOR r_av IN
        SELECT a.id_avaliacao, a.data_avaliacao,
               LEAST(a.data_encerramento, DATE '2026-11-25') AS data_limite,
               d.codigo, o.semestre
        FROM avaliacao a
        JOIN oferta_disciplina o ON o.id_oferta = a.id_oferta
        JOIN disciplina d        ON d.id_disciplina = o.id_disciplina
        ORDER BY a.id_avaliacao
    LOOP
        -- BD I (Prof. Anderson Santos) é a oferta mais bem avaliada do curso nos dois semestres.
        v_base := CASE
            WHEN r_av.codigo = '2COP001' AND r_av.semestre = 2 THEN 4.8
            WHEN r_av.codigo = '2COP001'                       THEN 4.2
            WHEN r_av.codigo = '2COP002'                       THEN 3.9
            WHEN r_av.codigo = '2COP003'                       THEN 3.1
            ELSE 3.6
        END;

        SELECT count(*) INTO v_n FROM participacao_avaliacao WHERE id_avaliacao = r_av.id_avaliacao;

        FOR i IN 1 .. v_n LOOP
            INSERT INTO submissao_anonima (id_avaliacao, data_submissao)
            VALUES (r_av.id_avaliacao,
                    r_av.data_avaliacao + floor(random() * (r_av.data_limite - r_av.data_avaliacao + 1))::int)
            RETURNING id_submissao INTO v_sub;

            FOR r_q IN
                SELECT q.id_questao, q.tipo, q.categoria
                FROM avaliacao_questao aq
                JOIN questao q ON q.id_questao = aq.id_questao
                WHERE aq.id_avaliacao = r_av.id_avaliacao
                ORDER BY aq.ordem
            LOOP
                v_adj := CASE r_q.categoria
                    WHEN 'INFRAESTRUTURA' THEN -0.8
                    WHEN 'DIDATICA'       THEN  0.15
                    ELSE 0 END;

                IF r_q.tipo = 'ESCALA_1_5' THEN
                    v_nota := LEAST(5, GREATEST(1, round(v_base + v_adj + (random() - 0.5) * 2.4)));
                    INSERT INTO resposta_questao (id_submissao, id_questao, nota_escala)
                    VALUES (v_sub, r_q.id_questao, v_nota);

                ELSIF r_q.tipo = 'MULTIPLA_ESCOLHA' THEN
                    v_valor := LEAST(4, GREATEST(1, round(1 + (v_base + v_adj - 1) * 0.75 + (random() - 0.5) * 2)));
                    SELECT id_alternativa INTO v_alt
                    FROM alternativa
                    WHERE id_questao = r_q.id_questao AND valor_numerico = v_valor;
                    INSERT INTO resposta_questao (id_submissao, id_questao, id_alternativa)
                    VALUES (v_sub, r_q.id_questao, v_alt);

                ELSIF random() < 0.6 THEN  -- texto livre é opcional
                    v_textos := CASE r_q.categoria
                        WHEN 'DIDATICA'       THEN c_didatica
                        WHEN 'CONTEUDO'       THEN c_conteudo
                        WHEN 'INFRAESTRUTURA' THEN c_infra
                        ELSE c_geral END;
                    INSERT INTO resposta_questao (id_submissao, id_questao, texto_resposta)
                    VALUES (v_sub, r_q.id_questao,
                            v_textos[1 + floor(random() * array_length(v_textos, 1))::int]);
                END IF;
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

-- -----------------------------------------------------------------------------
-- Reajuste das sequências após inserções com IDs explícitos.
-- -----------------------------------------------------------------------------
SELECT setval('usuario_id_usuario_seq',             (SELECT max(id_usuario)    FROM usuario));
SELECT setval('disciplina_id_disciplina_seq',       (SELECT max(id_disciplina) FROM disciplina));
SELECT setval('oferta_disciplina_id_oferta_seq',    (SELECT max(id_oferta)     FROM oferta_disciplina));
SELECT setval('avaliacao_id_avaliacao_seq',         (SELECT max(id_avaliacao)  FROM avaliacao));
SELECT setval('questao_id_questao_seq',             (SELECT max(id_questao)    FROM questao));

COMMIT;
