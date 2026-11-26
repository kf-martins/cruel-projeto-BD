package br.uel.avaliacao.dao;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.uel.avaliacao.model.Disciplina;
import br.uel.avaliacao.model.OfertaDisciplina;

/** Acesso JDBC às tabelas {@code disciplina}, {@code oferta_disciplina} e {@code matricula_oferta}. */
@Repository
public class DisciplinaDAO {

    private static final RowMapper<Disciplina> DISCIPLINA_MAPPER = (rs, i) -> new Disciplina(
            rs.getLong("id_disciplina"),
            rs.getString("codigo"),
            rs.getString("nome"),
            rs.getInt("carga_horaria"));

    private static final RowMapper<OfertaDisciplina> OFERTA_MAPPER = (rs, i) -> new OfertaDisciplina(
            rs.getLong("id_oferta"),
            rs.getLong("id_disciplina"),
            rs.getString("codigo"),
            rs.getString("disciplina"),
            rs.getLong("id_docente"),
            rs.getString("docente"),
            rs.getInt("ano"),
            rs.getInt("semestre"),
            rs.getString("turma"),
            rs.getInt("total_matriculados"),
            idOuNulo(rs.getLong("id_avaliacao"), rs.wasNull()));

    private static final String SELECT_OFERTA = """
            SELECT o.id_oferta, o.id_disciplina, d.codigo, d.nome AS disciplina,
                   o.id_docente, u.nome AS docente, o.ano, o.semestre, o.turma,
                   (SELECT count(*) FROM matricula_oferta m WHERE m.id_oferta = o.id_oferta) AS total_matriculados,
                   a.id_avaliacao
            FROM oferta_disciplina o
            JOIN disciplina d     ON d.id_disciplina = o.id_disciplina
            JOIN usuario u        ON u.id_usuario    = o.id_docente
            LEFT JOIN avaliacao a ON a.id_oferta     = o.id_oferta
            """;

    private final JdbcTemplate jdbcTemplate;

    private static Long idOuNulo(long valor, boolean nulo) {
        return nulo ? null : valor;
    }

    public DisciplinaDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Disciplina> listarDisciplinas() {
        return jdbcTemplate.query(
                "SELECT id_disciplina, codigo, nome, carga_horaria FROM disciplina ORDER BY codigo",
                DISCIPLINA_MAPPER);
    }

    public List<OfertaDisciplina> listarOfertas() {
        return jdbcTemplate.query(SELECT_OFERTA + " ORDER BY o.ano DESC, o.semestre DESC, d.nome", OFERTA_MAPPER);
    }

    public Optional<OfertaDisciplina> buscarOfertaPorId(Long idOferta) {
        return jdbcTemplate.query(SELECT_OFERTA + " WHERE o.id_oferta = ?", OFERTA_MAPPER, idOferta)
                .stream().findFirst();
    }

    public boolean disciplinaExiste(Long idDisciplina) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM disciplina WHERE id_disciplina = ?", Integer.class, idDisciplina);
        return total != null && total > 0;
    }

    /** Insere a oferta e devolve o id gerado (SERIAL) via {@link KeyHolder}. */
    public Long criarOferta(Long idDisciplina, Long idDocente, int ano, int semestre, String turma) {
        String sql = "INSERT INTO oferta_disciplina (id_disciplina, id_docente, ano, semestre, turma) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[] {"id_oferta"});
            ps.setLong(1, idDisciplina);
            ps.setLong(2, idDocente);
            ps.setInt(3, ano);
            ps.setInt(4, semestre);
            ps.setString(5, turma);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /** RF4 — matricula alunos na oferta em lote; matrículas já existentes são ignoradas. */
    public int vincularAlunosOferta(Long idOferta, List<Long> idsAlunos) {
        String sql = "INSERT INTO matricula_oferta (id_oferta, id_usuario) VALUES (?, ?) ON CONFLICT DO NOTHING";
        int[][] resultado = jdbcTemplate.batchUpdate(sql, idsAlunos, 100, (ps, idAluno) -> {
            ps.setLong(1, idOferta);
            ps.setLong(2, idAluno);
        });
        int total = 0;
        for (int[] lote : resultado) {
            for (int linhas : lote) {
                total += Math.max(linhas, 0);
            }
        }
        return total;
    }
}
