-- =============================================================================
-- crUEL — relatorios.sql
-- Consultas analíticas que alimentam a tela de Relatórios (RF7).
-- Os valores fixos nos filtros (ano = 2026, semestre = 2) são exemplos; no
-- back-end (RelatorioDAO) eles viram parâmetros opcionais (?) de ano, semestre
-- e disciplina.
--
-- Cobertura dos recursos SQL obrigatórios (seção 5 do edital):
-- | Recurso                              | Relatórios        |
-- |--------------------------------------|-------------------|
-- | JOIN (INNER)                         | 1, 2, 3, 4        |
-- | LEFT JOIN                            | 3, 4 (4a, 4b, 4c) |
-- | GROUP BY + agregação (AVG, COUNT…)   | 1, 2, 3, 4        |
-- | Subconsulta (correlacionada/derivada)| 2                 |
-- | ORDER BY + RANK() + FETCH FIRST      | 3                 |
-- | WHERE com filtros (período, etc.)    | 1, 2, 3, 4        |
-- Recursos adicionais: HAVING (2), CASE WHEN (1, 3, 4), CTE/WITH (3), NULLIF (4).
-- =============================================================================


-- =============================================================================
-- RELATÓRIO 1 — Média geral e distribuição das notas por disciplina/docente
-- Objetivo : média de satisfação (questões de escala 1–5), total de submissões
--            anônimas e distribuição das notas de cada oferta no período.
-- Recursos : JOIN, GROUP BY, AVG, COUNT, SUM(CASE WHEN), WHERE (período)
-- Gráfico  : barras (média por disciplina) + linha (evolução por semestre,
--            quando o filtro de período é removido).
-- =============================================================================
SELECT d.codigo,
       d.nome                                              AS disciplina,
       doc.nome                                            AS docente,
       o.ano,
       o.semestre,
       COUNT(DISTINCT s.id_submissao)                      AS total_submissoes,
       COUNT(r.nota_escala)                                AS total_respostas,
       ROUND(AVG(r.nota_escala), 2)                        AS media_escala,
       SUM(CASE WHEN r.nota_escala = 1 THEN 1 ELSE 0 END)  AS qtd_nota_1,
       SUM(CASE WHEN r.nota_escala = 2 THEN 1 ELSE 0 END)  AS qtd_nota_2,
       SUM(CASE WHEN r.nota_escala = 3 THEN 1 ELSE 0 END)  AS qtd_nota_3,
       SUM(CASE WHEN r.nota_escala = 4 THEN 1 ELSE 0 END)  AS qtd_nota_4,
       SUM(CASE WHEN r.nota_escala = 5 THEN 1 ELSE 0 END)  AS qtd_nota_5
FROM avaliacao a
JOIN oferta_disciplina o ON o.id_oferta     = a.id_oferta
JOIN disciplina d        ON d.id_disciplina = o.id_disciplina
JOIN usuario doc         ON doc.id_usuario  = o.id_docente
JOIN submissao_anonima s ON s.id_avaliacao  = a.id_avaliacao
JOIN resposta_questao r  ON r.id_submissao  = s.id_submissao
WHERE r.nota_escala IS NOT NULL
  AND o.ano = 2026
  AND o.semestre = 2
GROUP BY d.codigo, d.nome, doc.nome, o.ano, o.semestre
ORDER BY media_escala DESC;


-- =============================================================================
-- RELATÓRIO 2 — Desempenho por categoria vs. média geral do curso
-- Objetivo : comparar, para cada oferta, a média em Didática, Conteúdo e
--            Infraestrutura com a média de TODAS as ofertas do mesmo período
--            naquela categoria (subconsulta correlacionada).
-- Recursos : JOIN, GROUP BY, AVG, COUNT, Subconsulta correlacionada,
--            Subconsulta na cláusula FROM (tabela derivada), HAVING, WHERE
-- Gráfico  : barras comparativas (oferta x média do curso) por categoria.
-- =============================================================================
SELECT t.*,
       ROUND(t.media_oferta - t.media_curso, 2) AS diferenca
FROM (
    SELECT d.nome                       AS disciplina,
           doc.nome                     AS docente,
           o.ano,
           o.semestre,
           q.categoria,
           COUNT(*)                     AS total_respostas,
           ROUND(AVG(r.nota_escala), 2) AS media_oferta,
           (SELECT ROUND(AVG(r2.nota_escala), 2)
              FROM resposta_questao r2
              JOIN questao q2            ON q2.id_questao   = r2.id_questao
              JOIN submissao_anonima s2  ON s2.id_submissao = r2.id_submissao
              JOIN avaliacao a2          ON a2.id_avaliacao = s2.id_avaliacao
              JOIN oferta_disciplina o2  ON o2.id_oferta    = a2.id_oferta
             WHERE q2.categoria = q.categoria
               AND o2.ano       = o.ano
               AND o2.semestre  = o.semestre
               AND r2.nota_escala IS NOT NULL) AS media_curso
    FROM avaliacao a
    JOIN oferta_disciplina o ON o.id_oferta     = a.id_oferta
    JOIN disciplina d        ON d.id_disciplina = o.id_disciplina
    JOIN usuario doc         ON doc.id_usuario  = o.id_docente
    JOIN submissao_anonima s ON s.id_avaliacao  = a.id_avaliacao
    JOIN resposta_questao r  ON r.id_submissao  = s.id_submissao
    JOIN questao q           ON q.id_questao    = r.id_questao
    WHERE r.nota_escala IS NOT NULL
      AND q.categoria IN ('DIDATICA', 'CONTEUDO', 'INFRAESTRUTURA')
      AND o.ano = 2026
      AND o.semestre = 2
    GROUP BY d.nome, doc.nome, o.ano, o.semestre, q.categoria
    HAVING COUNT(*) >= 5          -- ignora grupos com amostra muito pequena
) AS t
ORDER BY t.disciplina, t.categoria;


-- =============================================================================
-- RELATÓRIO 3 — Ranking das questões objetivas (Top 10 melhores e mais críticas)
-- Objetivo : ranquear as questões de escala e múltipla escolha pelo índice de
--            aprovação (% de notas 4–5 na escala ou alternativas de valor 3–4).
-- Recursos : CTE (WITH), JOIN, LEFT JOIN, GROUP BY, COUNT, AVG, CASE WHEN,
--            RANK() OVER (ORDER BY …), ORDER BY, FETCH FIRST 10 ROWS ONLY, WHERE
-- Gráfico  : barras horizontais (ranking).
-- =============================================================================

-- 3a) Top 10 mais bem avaliadas
WITH indice AS (
    SELECT q.id_questao,
           q.enunciado,
           q.tipo,
           q.categoria,
           COUNT(*) AS total_respostas,
           ROUND(100.0 * SUM(CASE WHEN r.nota_escala >= 4 OR alt.valor_numerico >= 3
                                  THEN 1 ELSE 0 END) / COUNT(*), 1) AS indice_aprovacao,
           -- média normalizada para a escala 1–5 (alternativas 1–4 são reescaladas)
           ROUND(AVG(COALESCE(r.nota_escala, 1 + (alt.valor_numerico - 1) * 4.0 / 3)), 2) AS media_normalizada
    FROM questao q
    JOIN resposta_questao r  ON r.id_questao      = q.id_questao
    JOIN submissao_anonima s ON s.id_submissao    = r.id_submissao
    JOIN avaliacao a         ON a.id_avaliacao    = s.id_avaliacao
    JOIN oferta_disciplina o ON o.id_oferta       = a.id_oferta
    LEFT JOIN alternativa alt ON alt.id_alternativa = r.id_alternativa
    WHERE q.tipo IN ('ESCALA_1_5', 'MULTIPLA_ESCOLHA')
      AND o.ano = 2026
      AND o.semestre = 2
    GROUP BY q.id_questao, q.enunciado, q.tipo, q.categoria
)
SELECT RANK() OVER (ORDER BY indice_aprovacao DESC, media_normalizada DESC) AS posicao,
       indice.*
FROM indice
ORDER BY posicao
FETCH FIRST 10 ROWS ONLY;

-- 3b) Top 10 mais críticas (mesmo índice, ordem inversa)
WITH indice AS (
    SELECT q.id_questao,
           q.enunciado,
           q.tipo,
           q.categoria,
           COUNT(*) AS total_respostas,
           ROUND(100.0 * SUM(CASE WHEN r.nota_escala >= 4 OR alt.valor_numerico >= 3
                                  THEN 1 ELSE 0 END) / COUNT(*), 1) AS indice_aprovacao,
           ROUND(AVG(COALESCE(r.nota_escala, 1 + (alt.valor_numerico - 1) * 4.0 / 3)), 2) AS media_normalizada
    FROM questao q
    JOIN resposta_questao r  ON r.id_questao      = q.id_questao
    JOIN submissao_anonima s ON s.id_submissao    = r.id_submissao
    JOIN avaliacao a         ON a.id_avaliacao    = s.id_avaliacao
    JOIN oferta_disciplina o ON o.id_oferta       = a.id_oferta
    LEFT JOIN alternativa alt ON alt.id_alternativa = r.id_alternativa
    WHERE q.tipo IN ('ESCALA_1_5', 'MULTIPLA_ESCOLHA')
      AND o.ano = 2026
      AND o.semestre = 2
    GROUP BY q.id_questao, q.enunciado, q.tipo, q.categoria
)
SELECT RANK() OVER (ORDER BY indice_aprovacao ASC, media_normalizada ASC) AS posicao,
       indice.*
FROM indice
ORDER BY posicao
FETCH FIRST 10 ROWS ONLY;


-- =============================================================================
-- RELATÓRIO 4 — Taxa de adesão e alunos pendentes por avaliação
-- Objetivo : para cada avaliação, quantos matriculados já responderam e quem
--            ainda está pendente. Usa APENAS participacao_avaliacao (quem
--            respondeu), nunca as respostas — o anonimato é preservado.
-- Recursos : LEFT JOIN, JOIN, GROUP BY, COUNT, CASE WHEN, NULLIF, WHERE
-- Gráfico  : barras empilhadas (respondentes x pendentes) + tabela de pendentes.
-- =============================================================================

-- 4a) Resumo de adesão por avaliação
SELECT a.id_avaliacao,
       a.titulo,
       d.nome                                    AS disciplina,
       o.ano,
       o.semestre,
       COUNT(m.id_usuario)                       AS matriculados,
       COUNT(p.id_usuario)                       AS respondentes,
       COUNT(m.id_usuario) - COUNT(p.id_usuario) AS pendentes,
       ROUND(100.0 * COUNT(p.id_usuario) / NULLIF(COUNT(m.id_usuario), 0), 1) AS taxa_adesao,
       CASE
           WHEN CURRENT_DATE > a.data_encerramento THEN 'ENCERRADA'
           WHEN CURRENT_DATE < a.data_avaliacao    THEN 'AGENDADA'
           ELSE 'ABERTA'
       END                                       AS situacao,
       CASE
           WHEN COUNT(p.id_usuario) >= 0.8 * COUNT(m.id_usuario) THEN 'ALTA'
           WHEN COUNT(p.id_usuario) >= 0.5 * COUNT(m.id_usuario) THEN 'MÉDIA'
           ELSE 'BAIXA'
       END                                       AS nivel_adesao
FROM avaliacao a
JOIN oferta_disciplina o            ON o.id_oferta     = a.id_oferta
JOIN disciplina d                   ON d.id_disciplina = o.id_disciplina
LEFT JOIN matricula_oferta m        ON m.id_oferta     = a.id_oferta
LEFT JOIN participacao_avaliacao p  ON p.id_avaliacao  = a.id_avaliacao
                                   AND p.id_usuario    = m.id_usuario
WHERE a.ativa = TRUE
GROUP BY a.id_avaliacao, a.titulo, d.nome, o.ano, o.semestre, a.data_avaliacao, a.data_encerramento
ORDER BY taxa_adesao DESC NULLS LAST;

-- 4b) Alunos matriculados que ainda não responderam (anti-join com LEFT JOIN)
SELECT a.id_avaliacao,
       d.nome      AS disciplina,
       o.ano,
       o.semestre,
       u.matricula,
       u.nome      AS aluno,
       u.email
FROM avaliacao a
JOIN oferta_disciplina o           ON o.id_oferta     = a.id_oferta
JOIN disciplina d                  ON d.id_disciplina = o.id_disciplina
JOIN matricula_oferta m            ON m.id_oferta     = a.id_oferta
JOIN usuario u                     ON u.id_usuario    = m.id_usuario
LEFT JOIN participacao_avaliacao p ON p.id_avaliacao  = a.id_avaliacao
                                  AND p.id_usuario    = m.id_usuario
WHERE p.id_usuario IS NULL
  AND a.ativa = TRUE
ORDER BY d.nome, u.nome;

-- 4c) Questões do banco que não são usadas em nenhuma avaliação (LEFT JOIN)
SELECT q.id_questao,
       q.enunciado,
       q.tipo,
       q.categoria
FROM questao q
LEFT JOIN avaliacao_questao aq ON aq.id_questao = q.id_questao
WHERE aq.id_questao IS NULL
ORDER BY q.id_questao;
