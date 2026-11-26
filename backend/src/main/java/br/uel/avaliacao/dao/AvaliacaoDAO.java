package br.uel.avaliacao.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.uel.avaliacao.model.Alternativa;
import br.uel.avaliacao.model.Avaliacao;
import br.uel.avaliacao.model.Questao;

/** Acesso JDBC às tabelas {@code avaliacao} e {@code avaliacao_questao}. */
@Repository
public class AvaliacaoDAO {

    /** Item de composição da avaliação (questão, ordem e obrigatoriedade). */
    public record ItemComposicao(Long idQuestao, int ordem, boolean obrigatoria) {
    }

    private static final String SELECT_BASE = """
            SELECT a.id_avaliacao, a.id_oferta, a.titulo, a.descricao, a.data_avaliacao, a.data_encerramento,
                   a.hora_inicio, a.hora_fim, a.ativa,
                   d.codigo, d.nome AS disciplina, doc.nome AS docente, o.ano, o.semestre, o.turma,
                   (SELECT count(*) FROM avaliacao_questao aq     WHERE aq.id_avaliacao = a.id_avaliacao) AS total_questoes,
                   (SELECT count(*) FROM matricula_oferta m       WHERE m.id_oferta     = a.id_oferta)    AS total_matriculados,
                   (SELECT count(*) FROM participacao_avaliacao p WHERE p.id_avaliacao  = a.id_avaliacao) AS total_respondentes
            """;

    private static final String FROM_BASE = """
            FROM avaliacao a
            JOIN oferta_disciplina o ON o.id_oferta     = a.id_oferta
            JOIN disciplina d        ON d.id_disciplina = o.id_disciplina
            JOIN usuario doc         ON doc.id_usuario  = o.id_docente
            """;

    private final JdbcTemplate jdbcTemplate;
    private final QuestaoDAO questaoDAO;

    public AvaliacaoDAO(JdbcTemplate jdbcTemplate, QuestaoDAO questaoDAO) {
        this.jdbcTemplate = jdbcTemplate;
        this.questaoDAO = questaoDAO;
    }

    public Long inserirAvaliacao(Avaliacao a) {
        String sql = "INSERT INTO avaliacao (id_oferta, titulo, descricao, data_avaliacao, data_encerramento, "
                + "hora_inicio, hora_fim, ativa) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[] {"id_avaliacao"});
            ps.setLong(1, a.idOferta());
            ps.setString(2, a.titulo());
            ps.setString(3, a.descricao());
            ps.setDate(4, Date.valueOf(a.dataAvaliacao()));
            ps.setDate(5, Date.valueOf(a.dataEncerramento()));
            ps.setTime(6, Time.valueOf(a.horaInicio()));
            ps.setTime(7, Time.valueOf(a.horaFim()));
            ps.setBoolean(8, a.ativa());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /** RF3 — vincula as questões do banco à avaliação (lote). */
    public void vincularQuestoes(Long idAvaliacao, List<ItemComposicao> itens) {
        String sql = "INSERT INTO avaliacao_questao (id_avaliacao, id_questao, ordem, obrigatoria) VALUES (?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, itens, 100, (ps, item) -> {
            ps.setLong(1, idAvaliacao);
            ps.setLong(2, item.idQuestao());
            ps.setInt(3, item.ordem());
            ps.setBoolean(4, item.obrigatoria());
        });
    }

    public List<Avaliacao> listarTodas() {
        String sql = SELECT_BASE + ", NULL::boolean AS respondida " + FROM_BASE
                + " ORDER BY o.ano DESC, o.semestre DESC, d.nome";
        return jdbcTemplate.query(sql, (rs, i) -> mapear(rs));
    }

    /**
     * Avaliações das ofertas em que o aluno está matriculado, com o indicador {@code respondida}
     * (consulta apenas {@code participacao_avaliacao} — nunca as respostas).
     */
    public List<Avaliacao> listarDoAluno(Long idUsuario) {
        String sql = SELECT_BASE + """
                , EXISTS (SELECT 1 FROM participacao_avaliacao p
                          WHERE p.id_avaliacao = a.id_avaliacao AND p.id_usuario = ?) AS respondida
                """ + FROM_BASE + """
                JOIN matricula_oferta mo ON mo.id_oferta = a.id_oferta AND mo.id_usuario = ?
                WHERE a.ativa = TRUE
                ORDER BY o.ano DESC, o.semestre DESC, d.nome
                """;
        return jdbcTemplate.query(sql, (rs, i) -> mapear(rs), idUsuario, idUsuario);
    }

    public Optional<Avaliacao> buscarPorId(Long idAvaliacao) {
        String sql = SELECT_BASE + ", NULL::boolean AS respondida " + FROM_BASE + " WHERE a.id_avaliacao = ?";
        return jdbcTemplate.query(sql, (rs, i) -> mapear(rs), idAvaliacao).stream().findFirst();
    }

    /** Avaliação + questões (na ordem definida) + alternativas das questões de múltipla escolha. */
    public Optional<Avaliacao> buscarCompletaPorId(Long idAvaliacao) {
        return buscarPorId(idAvaliacao).map(avaliacao -> {
            String sql = """
                    SELECT aq.ordem, aq.obrigatoria, q.id_questao, q.enunciado, q.tipo, q.categoria, q.ativa
                    FROM avaliacao_questao aq
                    JOIN questao q ON q.id_questao = aq.id_questao
                    WHERE aq.id_avaliacao = ?
                    ORDER BY aq.ordem
                    """;
            List<Avaliacao.Item> itens = jdbcTemplate.query(sql, (rs, i) -> new Avaliacao.Item(
                    rs.getInt("ordem"), rs.getBoolean("obrigatoria"), QuestaoDAO.QUESTAO_MAPPER.mapRow(rs, i)),
                    idAvaliacao);
            Map<Long, List<Alternativa>> alternativas = questaoDAO.listarAlternativas(
                    itens.stream().map(it -> it.questao().idQuestao()).toList());
            List<Avaliacao.Item> completos = new ArrayList<>();
            for (Avaliacao.Item it : itens) {
                Questao q = it.questao().comAlternativas(alternativas.getOrDefault(it.questao().idQuestao(), List.of()));
                completos.add(new Avaliacao.Item(it.ordem(), it.obrigatoria(), q));
            }
            return avaliacao.comItens(completos);
        });
    }

    public boolean existeParaOferta(Long idOferta) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM avaliacao WHERE id_oferta = ?", Integer.class, idOferta);
        return total != null && total > 0;
    }

    /** O aluno está matriculado na oferta avaliada? (elegibilidade — RF4/RF5) */
    public boolean alunoMatriculado(Long idAvaliacao, Long idUsuario) {
        Integer total = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM avaliacao a
                JOIN matricula_oferta m ON m.id_oferta = a.id_oferta
                WHERE a.id_avaliacao = ? AND m.id_usuario = ?
                """, Integer.class, idAvaliacao, idUsuario);
        return total != null && total > 0;
    }

    public int atualizarAtiva(Long idAvaliacao, boolean ativa) {
        return jdbcTemplate.update("UPDATE avaliacao SET ativa = ? WHERE id_avaliacao = ?", ativa, idAvaliacao);
    }

    private static Avaliacao mapear(ResultSet rs) throws SQLException {
        return new Avaliacao(
                rs.getLong("id_avaliacao"),
                rs.getLong("id_oferta"),
                rs.getString("titulo"),
                rs.getString("descricao"),
                rs.getDate("data_avaliacao").toLocalDate(),
                rs.getDate("data_encerramento").toLocalDate(),
                rs.getTime("hora_inicio").toLocalTime(),
                rs.getTime("hora_fim").toLocalTime(),
                rs.getBoolean("ativa"),
                rs.getString("codigo"),
                rs.getString("disciplina"),
                rs.getString("docente"),
                rs.getInt("ano"),
                rs.getInt("semestre"),
                rs.getString("turma"),
                rs.getInt("total_questoes"),
                rs.getInt("total_matriculados"),
                rs.getInt("total_respondentes"),
                rs.getObject("respondida", Boolean.class),
                null,
                List.of());
    }
}
