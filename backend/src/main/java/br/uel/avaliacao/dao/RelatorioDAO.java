package br.uel.avaliacao.dao;

import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import br.uel.avaliacao.dto.RelatorioDTOs.AdesaoAvaliacao;
import br.uel.avaliacao.dto.RelatorioDTOs.AlunoPendente;
import br.uel.avaliacao.dto.RelatorioDTOs.ComparativoCategoria;
import br.uel.avaliacao.dto.RelatorioDTOs.Filtro;
import br.uel.avaliacao.dto.RelatorioDTOs.MediaDisciplina;
import br.uel.avaliacao.dto.RelatorioDTOs.QuestaoNaoUtilizada;
import br.uel.avaliacao.dto.RelatorioDTOs.RankingQuestao;

/**
 * Executa as consultas de {@code database/relatorios.sql} (RF7). Os valores fixos de período do
 * script viram filtros opcionais: {@code (CAST(:p AS INTEGER) IS NULL OR coluna = :p)}.
 */
@Repository
public class RelatorioDAO {

    /**
     * Filtros dinâmicos por período, disciplina e docente aplicados sobre {@code oferta_disciplina o}.
     * O filtro de docente garante que um DOCENTE nunca receba linhas de ofertas de outros docentes.
     */
    private static final String FILTRO_OFERTA = """
              AND (CAST(:ano AS INTEGER) IS NULL OR o.ano = :ano)
              AND (CAST(:semestre AS INTEGER) IS NULL OR o.semestre = :semestre)
              AND (CAST(:idDisciplina AS INTEGER) IS NULL OR o.id_disciplina = :idDisciplina)
              AND (CAST(:idDocente AS INTEGER) IS NULL OR o.id_docente = :idDocente)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RelatorioDAO(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Relatório 1 — JOIN, GROUP BY, AVG, COUNT, SUM(CASE), WHERE. */
    public List<MediaDisciplina> mediaPorDisciplina(Filtro f) {
        String sql = """
                SELECT d.codigo, d.nome AS disciplina, doc.nome AS docente, o.ano, o.semestre,
                       COUNT(DISTINCT s.id_submissao)                     AS total_submissoes,
                       COUNT(r.nota_escala)                               AS total_respostas,
                       ROUND(AVG(r.nota_escala), 2)                       AS media_escala,
                       SUM(CASE WHEN r.nota_escala = 1 THEN 1 ELSE 0 END) AS qtd_nota_1,
                       SUM(CASE WHEN r.nota_escala = 2 THEN 1 ELSE 0 END) AS qtd_nota_2,
                       SUM(CASE WHEN r.nota_escala = 3 THEN 1 ELSE 0 END) AS qtd_nota_3,
                       SUM(CASE WHEN r.nota_escala = 4 THEN 1 ELSE 0 END) AS qtd_nota_4,
                       SUM(CASE WHEN r.nota_escala = 5 THEN 1 ELSE 0 END) AS qtd_nota_5
                FROM avaliacao a
                JOIN oferta_disciplina o ON o.id_oferta     = a.id_oferta
                JOIN disciplina d        ON d.id_disciplina = o.id_disciplina
                JOIN usuario doc         ON doc.id_usuario  = o.id_docente
                JOIN submissao_anonima s ON s.id_avaliacao  = a.id_avaliacao
                JOIN resposta_questao r  ON r.id_submissao  = s.id_submissao
                WHERE r.nota_escala IS NOT NULL
                """ + FILTRO_OFERTA + """
                GROUP BY d.codigo, d.nome, doc.nome, o.ano, o.semestre
                ORDER BY o.ano, o.semestre, media_escala DESC
                """;
        return jdbc.query(sql, parametros(f), (rs, i) -> new MediaDisciplina(
                rs.getString("codigo"), rs.getString("disciplina"), rs.getString("docente"),
                rs.getInt("ano"), rs.getInt("semestre"),
                rs.getLong("total_submissoes"), rs.getLong("total_respostas"), rs.getBigDecimal("media_escala"),
                rs.getLong("qtd_nota_1"), rs.getLong("qtd_nota_2"), rs.getLong("qtd_nota_3"),
                rs.getLong("qtd_nota_4"), rs.getLong("qtd_nota_5")));
    }

    /** Relatório 2 — subconsulta correlacionada + tabela derivada, JOIN, GROUP BY, HAVING. */
    public List<ComparativoCategoria> comparativoPorCategoria(Filtro f) {
        String sql = """
                SELECT t.*, ROUND(t.media_oferta - t.media_curso, 2) AS diferenca
                FROM (
                    SELECT d.nome AS disciplina, doc.nome AS docente, o.ano, o.semestre, q.categoria,
                           COUNT(*)                     AS total_respostas,
                           ROUND(AVG(r.nota_escala), 2) AS media_oferta,
                           (SELECT ROUND(AVG(r2.nota_escala), 2)
                              FROM resposta_questao r2
                              JOIN questao q2           ON q2.id_questao   = r2.id_questao
                              JOIN submissao_anonima s2 ON s2.id_submissao = r2.id_submissao
                              JOIN avaliacao a2         ON a2.id_avaliacao = s2.id_avaliacao
                              JOIN oferta_disciplina o2 ON o2.id_oferta    = a2.id_oferta
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
                """ + FILTRO_OFERTA + """
                    GROUP BY d.nome, doc.nome, o.ano, o.semestre, q.categoria
                    HAVING COUNT(*) >= 5
                ) AS t
                ORDER BY t.ano, t.semestre, t.disciplina, t.categoria
                """;
        return jdbc.query(sql, parametros(f), (rs, i) -> new ComparativoCategoria(
                rs.getString("disciplina"), rs.getString("docente"), rs.getInt("ano"), rs.getInt("semestre"),
                rs.getString("categoria"), rs.getLong("total_respostas"), rs.getBigDecimal("media_oferta"),
                rs.getBigDecimal("media_curso"), rs.getBigDecimal("diferenca")));
    }

    /**
     * Relatório 3 — CTE, JOIN, LEFT JOIN, GROUP BY, RANK() OVER, ORDER BY, FETCH FIRST 10 ROWS ONLY.
     *
     * @param criticas {@code true} para as 10 mais críticas; {@code false} para as 10 melhores
     */
    public List<RankingQuestao> rankingQuestoes(Filtro f, boolean criticas) {
        String direcao = criticas ? "ASC" : "DESC";   // valor fixo (whitelist), nunca vindo do usuário
        String sql = """
                WITH indice AS (
                    SELECT q.id_questao, q.enunciado, q.tipo, q.categoria,
                           COUNT(*) AS total_respostas,
                           ROUND(100.0 * SUM(CASE WHEN r.nota_escala >= 4 OR alt.valor_numerico >= 3
                                                  THEN 1 ELSE 0 END) / COUNT(*), 1) AS indice_aprovacao,
                           ROUND(AVG(COALESCE(r.nota_escala, 1 + (alt.valor_numerico - 1) * 4.0 / 3)), 2) AS media_normalizada
                    FROM questao q
                    JOIN resposta_questao r   ON r.id_questao       = q.id_questao
                    JOIN submissao_anonima s  ON s.id_submissao     = r.id_submissao
                    JOIN avaliacao a          ON a.id_avaliacao     = s.id_avaliacao
                    JOIN oferta_disciplina o  ON o.id_oferta        = a.id_oferta
                    LEFT JOIN alternativa alt ON alt.id_alternativa = r.id_alternativa
                    WHERE q.tipo IN ('ESCALA_1_5', 'MULTIPLA_ESCOLHA')
                """ + FILTRO_OFERTA + """
                    GROUP BY q.id_questao, q.enunciado, q.tipo, q.categoria
                )
                SELECT RANK() OVER (ORDER BY indice_aprovacao %1$s, media_normalizada %1$s) AS posicao, indice.*
                FROM indice
                ORDER BY posicao, id_questao
                FETCH FIRST 10 ROWS ONLY
                """.formatted(direcao);
        return jdbc.query(sql, parametros(f), (rs, i) -> new RankingQuestao(
                rs.getLong("posicao"), rs.getLong("id_questao"), rs.getString("enunciado"), rs.getString("tipo"),
                rs.getString("categoria"), rs.getLong("total_respostas"), rs.getBigDecimal("indice_aprovacao"),
                rs.getBigDecimal("media_normalizada")));
    }

    /** Relatório 4a — LEFT JOIN (matrículas x participações), COUNT, CASE WHEN, NULLIF. */
    public List<AdesaoAvaliacao> adesaoPorAvaliacao(Filtro f) {
        String sql = """
                SELECT a.id_avaliacao, a.titulo, d.nome AS disciplina, o.ano, o.semestre,
                       COUNT(m.id_usuario)                       AS matriculados,
                       COUNT(p.id_usuario)                       AS respondentes,
                       COUNT(m.id_usuario) - COUNT(p.id_usuario) AS pendentes,
                       ROUND(100.0 * COUNT(p.id_usuario) / NULLIF(COUNT(m.id_usuario), 0), 1) AS taxa_adesao,
                       CASE
                           WHEN CAST(:hoje AS DATE) > a.data_encerramento THEN 'ENCERRADA'
                           WHEN CAST(:hoje AS DATE) < a.data_avaliacao    THEN 'AGENDADA'
                           ELSE 'ABERTA'
                       END AS situacao,
                       CASE
                           WHEN COUNT(p.id_usuario) >= 0.8 * COUNT(m.id_usuario) THEN 'ALTA'
                           WHEN COUNT(p.id_usuario) >= 0.5 * COUNT(m.id_usuario) THEN 'MÉDIA'
                           ELSE 'BAIXA'
                       END AS nivel_adesao
                FROM avaliacao a
                JOIN oferta_disciplina o           ON o.id_oferta     = a.id_oferta
                JOIN disciplina d                  ON d.id_disciplina = o.id_disciplina
                LEFT JOIN matricula_oferta m       ON m.id_oferta     = a.id_oferta
                LEFT JOIN participacao_avaliacao p ON p.id_avaliacao  = a.id_avaliacao
                                                  AND p.id_usuario    = m.id_usuario
                WHERE a.ativa = TRUE
                """ + FILTRO_OFERTA + """
                GROUP BY a.id_avaliacao, a.titulo, d.nome, o.ano, o.semestre, a.data_avaliacao, a.data_encerramento
                ORDER BY taxa_adesao DESC NULLS LAST, a.id_avaliacao
                """;
        MapSqlParameterSource params = parametros(f).addValue("hoje", java.sql.Date.valueOf(f.hoje()), Types.DATE);
        return jdbc.query(sql, params, (rs, i) -> new AdesaoAvaliacao(
                rs.getLong("id_avaliacao"), rs.getString("titulo"), rs.getString("disciplina"),
                rs.getInt("ano"), rs.getInt("semestre"), rs.getLong("matriculados"), rs.getLong("respondentes"),
                rs.getLong("pendentes"), rs.getBigDecimal("taxa_adesao"), rs.getString("situacao"),
                rs.getString("nivel_adesao")));
    }

    /** Relatório 4b — anti-join com LEFT JOIN: matriculados sem participação. */
    public List<AlunoPendente> alunosPendentes(Filtro f) {
        String sql = """
                SELECT a.id_avaliacao, d.nome AS disciplina, o.ano, o.semestre, u.matricula, u.nome AS aluno, u.email
                FROM avaliacao a
                JOIN oferta_disciplina o           ON o.id_oferta     = a.id_oferta
                JOIN disciplina d                  ON d.id_disciplina = o.id_disciplina
                JOIN matricula_oferta m            ON m.id_oferta     = a.id_oferta
                JOIN usuario u                     ON u.id_usuario    = m.id_usuario
                LEFT JOIN participacao_avaliacao p ON p.id_avaliacao  = a.id_avaliacao
                                                  AND p.id_usuario    = m.id_usuario
                WHERE p.id_usuario IS NULL
                  AND a.ativa = TRUE
                """ + FILTRO_OFERTA + """
                ORDER BY d.nome, o.ano, o.semestre, u.nome
                """;
        return jdbc.query(sql, parametros(f), (rs, i) -> new AlunoPendente(
                rs.getLong("id_avaliacao"), rs.getString("disciplina"), rs.getInt("ano"), rs.getInt("semestre"),
                rs.getString("matricula"), rs.getString("aluno"), rs.getString("email")));
    }

    /** Relatório 4c — questões do banco não usadas em nenhuma avaliação (LEFT JOIN ... IS NULL). */
    public List<QuestaoNaoUtilizada> questoesNaoUtilizadas() {
        String sql = """
                SELECT q.id_questao, q.enunciado, q.tipo, q.categoria
                FROM questao q
                LEFT JOIN avaliacao_questao aq ON aq.id_questao = q.id_questao
                WHERE aq.id_questao IS NULL
                ORDER BY q.id_questao
                """;
        return jdbc.query(sql, (rs, i) -> new QuestaoNaoUtilizada(
                rs.getLong("id_questao"), rs.getString("enunciado"), rs.getString("tipo"), rs.getString("categoria")));
    }

    private static MapSqlParameterSource parametros(Filtro f) {
        return new MapSqlParameterSource()
                .addValue("ano", f.ano(), Types.INTEGER)
                .addValue("semestre", f.semestre(), Types.INTEGER)
                .addValue("idDisciplina", f.idDisciplina(), Types.INTEGER)
                .addValue("idDocente", f.idDocente(), Types.INTEGER);
    }
}
