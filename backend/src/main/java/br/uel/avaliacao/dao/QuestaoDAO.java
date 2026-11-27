package br.uel.avaliacao.dao;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.uel.avaliacao.model.Alternativa;
import br.uel.avaliacao.model.Questao;

/** Acesso JDBC às tabelas {@code questao} e {@code alternativa}. */
@Repository
public class QuestaoDAO {

    static final RowMapper<Questao> QUESTAO_MAPPER = (rs, i) -> new Questao(
            rs.getLong("id_questao"),
            rs.getString("enunciado"),
            rs.getString("tipo"),
            rs.getString("categoria"),
            rs.getBoolean("ativa"),
            List.of());

    static final RowMapper<Alternativa> ALTERNATIVA_MAPPER = (rs, i) -> new Alternativa(
            rs.getLong("id_alternativa"),
            rs.getLong("id_questao"),
            rs.getString("texto"),
            rs.getInt("valor_numerico"),
            rs.getInt("ordem"));

    private final JdbcTemplate jdbcTemplate;

    public QuestaoDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long inserirQuestao(Questao q) {
        String sql = "INSERT INTO questao (enunciado, tipo, categoria, ativa) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[] {"id_questao"});
            ps.setString(1, q.enunciado());
            ps.setString(2, q.tipo());
            ps.setString(3, q.categoria());
            ps.setBoolean(4, q.ativa());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /** Insere as alternativas em lote; a ordem é a posição na lista (1..n). */
    public void inserirAlternativas(Long idQuestao, List<Alternativa> alternativas) {
        String sql = "INSERT INTO alternativa (id_questao, texto, valor_numerico, ordem) VALUES (?, ?, ?, ?)";
        List<Object[]> parametros = new ArrayList<>();
        for (int i = 0; i < alternativas.size(); i++) {
            Alternativa a = alternativas.get(i);
            parametros.add(new Object[] {idQuestao, a.texto(), a.valorNumerico(), i + 1});
        }
        jdbcTemplate.batchUpdate(sql, parametros);
    }

    /** Lista todas as questões com suas alternativas (2 consultas + agrupamento em memória, sem N+1). */
    public List<Questao> listarTodasComAlternativas() {
        List<Questao> questoes = jdbcTemplate.query(
                "SELECT id_questao, enunciado, tipo, categoria, ativa FROM questao ORDER BY id_questao",
                QUESTAO_MAPPER);
        Map<Long, List<Alternativa>> alternativas = agruparAlternativas(jdbcTemplate.query(
                "SELECT id_alternativa, id_questao, texto, valor_numerico, ordem FROM alternativa ORDER BY id_questao, ordem",
                ALTERNATIVA_MAPPER));
        return questoes.stream()
                .map(q -> q.comAlternativas(alternativas.getOrDefault(q.idQuestao(), List.of())))
                .toList();
    }

    /** Questões usadas nas avaliações das ofertas do docente (com alternativas). */
    public List<Questao> listarDoDocenteComAlternativas(Long idDocente) {
        List<Questao> questoes = jdbcTemplate.query("""
                SELECT DISTINCT q.id_questao, q.enunciado, q.tipo, q.categoria, q.ativa
                FROM questao q
                JOIN avaliacao_questao aq ON aq.id_questao  = q.id_questao
                JOIN avaliacao a          ON a.id_avaliacao = aq.id_avaliacao
                JOIN oferta_disciplina o  ON o.id_oferta    = a.id_oferta
                WHERE o.id_docente = ?
                ORDER BY q.id_questao
                """, QUESTAO_MAPPER, idDocente);
        Map<Long, List<Alternativa>> alternativas = listarAlternativas(
                questoes.stream().map(Questao::idQuestao).toList());
        return questoes.stream()
                .map(q -> q.comAlternativas(alternativas.getOrDefault(q.idQuestao(), List.of())))
                .toList();
    }

    public Optional<Questao> buscarPorId(Long idQuestao) {
        return jdbcTemplate.query(
                        "SELECT id_questao, enunciado, tipo, categoria, ativa FROM questao WHERE id_questao = ?",
                        QUESTAO_MAPPER, idQuestao)
                .stream().findFirst()
                .map(q -> q.comAlternativas(listarAlternativas(List.of(idQuestao))
                        .getOrDefault(idQuestao, List.of())));
    }

    /** Alternativas das questões informadas, agrupadas por id_questao. */
    public Map<Long, List<Alternativa>> listarAlternativas(Collection<Long> idsQuestoes) {
        if (idsQuestoes.isEmpty()) {
            return Map.of();
        }
        String sql = "SELECT id_alternativa, id_questao, texto, valor_numerico, ordem FROM alternativa "
                + "WHERE id_questao = ANY (?) ORDER BY id_questao, ordem";
        List<Alternativa> lista = jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setArray(1, con.createArrayOf("bigint", idsQuestoes.toArray()));
            return ps;
        }, ALTERNATIVA_MAPPER);
        return agruparAlternativas(lista);
    }

    /** Questões (ativas) cujos ids foram informados — usado para validar a montagem de avaliações. */
    public List<Questao> listarAtivasPorIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT id_questao, enunciado, tipo, categoria, ativa FROM questao "
                + "WHERE ativa = TRUE AND id_questao = ANY (?)";
        return jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setArray(1, con.createArrayOf("bigint", ids.toArray()));
            return ps;
        }, QUESTAO_MAPPER);
    }

    public int atualizarAtiva(Long idQuestao, boolean ativa) {
        return jdbcTemplate.update("UPDATE questao SET ativa = ? WHERE id_questao = ?", ativa, idQuestao);
    }

    private static Map<Long, List<Alternativa>> agruparAlternativas(List<Alternativa> lista) {
        Map<Long, List<Alternativa>> mapa = new LinkedHashMap<>();
        for (Alternativa a : lista) {
            mapa.computeIfAbsent(a.idQuestao(), k -> new ArrayList<>()).add(a);
        }
        return mapa;
    }
}
