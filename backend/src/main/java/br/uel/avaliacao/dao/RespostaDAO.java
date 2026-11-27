package br.uel.avaliacao.dao;

import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import br.uel.avaliacao.dto.RelatorioDTOs.ComentarioAnonimo;
import br.uel.avaliacao.model.RespostaQuestao;

/**
 * Acesso JDBC às tabelas da submissão (RF5/RF6): {@code participacao_avaliacao},
 * {@code submissao_anonima} e {@code resposta_questao}.
 *
 * <p>Repare que NENHUM método recebe ao mesmo tempo o usuário e a submissão: a participação
 * (quem respondeu) e o conteúdo (o que foi respondido) são gravados por métodos independentes,
 * sem qualquer chave em comum além da avaliação.
 */
@Repository
public class RespostaDAO {

    private final JdbcTemplate jdbcTemplate;

    public RespostaDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean jaParticipou(Long idAvaliacao, Long idUsuario) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM participacao_avaliacao WHERE id_avaliacao = ? AND id_usuario = ?",
                Integer.class, idAvaliacao, idUsuario);
        return total != null && total > 0;
    }

    /**
     * Registra que o aluno respondeu. Uma segunda tentativa viola a PK (id_avaliacao, id_usuario)
     * e o driver lança {@link org.springframework.dao.DuplicateKeyException}.
     */
    public void registrarParticipacao(Long idAvaliacao, Long idUsuario, LocalDate data) {
        jdbcTemplate.update(
                "INSERT INTO participacao_avaliacao (id_avaliacao, id_usuario, data_participacao) VALUES (?, ?, ?)",
                idAvaliacao, idUsuario, Date.valueOf(data));
    }

    /** Cria o envelope anônimo (sem id_usuario) e devolve o UUID aleatório gerado pelo banco. */
    public UUID inserirSubmissaoAnonima(Long idAvaliacao, LocalDate data) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO submissao_anonima (id_avaliacao, data_submissao) VALUES (?, ?) RETURNING id_submissao",
                UUID.class, idAvaliacao, Date.valueOf(data));
    }

    /** Insere as respostas da submissão em lote. */
    public void inserirRespostasQuestoes(UUID idSubmissao, List<RespostaQuestao> respostas) {
        String sql = "INSERT INTO resposta_questao (id_submissao, id_questao, id_alternativa, nota_escala, texto_resposta) "
                + "VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, respostas, 100, (ps, r) -> {
            ps.setObject(1, idSubmissao);
            ps.setLong(2, r.idQuestao());
            ps.setObject(3, r.idAlternativa(), Types.INTEGER);
            ps.setObject(4, r.notaEscala(), Types.INTEGER);
            ps.setString(5, r.textoResposta());
        });
    }

    /**
     * Comentários de texto livre de uma avaliação. Ordenados por questão e texto (e não pela ordem
     * de inserção) e sem expor o id da submissão, para não permitir correlação com quem participou.
     */
    public List<ComentarioAnonimo> listarComentariosAnonimos(Long idAvaliacao) {
        String sql = """
                SELECT q.id_questao, q.enunciado, r.texto_resposta
                FROM resposta_questao r
                JOIN submissao_anonima s ON s.id_submissao = r.id_submissao
                JOIN questao q           ON q.id_questao   = r.id_questao
                JOIN avaliacao_questao aq ON aq.id_avaliacao = s.id_avaliacao AND aq.id_questao = q.id_questao
                WHERE s.id_avaliacao = ? AND r.texto_resposta IS NOT NULL
                ORDER BY aq.ordem, r.texto_resposta
                """;
        return jdbcTemplate.query(sql, (rs, i) -> new ComentarioAnonimo(
                rs.getLong("id_questao"), rs.getString("enunciado"), rs.getString("texto_resposta")), idAvaliacao);
    }
}
