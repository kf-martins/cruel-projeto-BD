package br.uel.avaliacao.dao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import br.uel.avaliacao.model.Usuario;

/** Acesso JDBC à tabela {@code usuario}. */
@Repository
public class UsuarioDAO {

    private static final String COLUNAS = "u.id_usuario, u.matricula, u.nome, u.email, u.perfil";

    static final RowMapper<Usuario> MAPPER = (rs, i) -> new Usuario(
            rs.getLong("id_usuario"),
            rs.getString("matricula"),
            rs.getString("nome"),
            rs.getString("email"),
            rs.getString("perfil"));

    private final JdbcTemplate jdbcTemplate;

    public UsuarioDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Login por e-mail ou matrícula; a senha é comparada pelo hash SHA-256. */
    public Optional<Usuario> autenticar(String login, String senha) {
        String sql = "SELECT " + COLUNAS + " FROM usuario u "
                + "WHERE (lower(u.email) = lower(?) OR u.matricula = ?) AND u.senha_hash = ?";
        String chave = login.trim();
        return jdbcTemplate.query(sql, MAPPER, chave, chave, sha256(senha)).stream().findFirst();
    }

    public Optional<Usuario> buscarPorId(Long idUsuario) {
        String sql = "SELECT " + COLUNAS + " FROM usuario u WHERE u.id_usuario = ?";
        return jdbcTemplate.query(sql, MAPPER, idUsuario).stream().findFirst();
    }

    public List<Usuario> listarPorPerfil(String perfil) {
        String sql = "SELECT " + COLUNAS + " FROM usuario u WHERE u.perfil = ? ORDER BY u.nome";
        return jdbcTemplate.query(sql, MAPPER, perfil);
    }

    public List<Usuario> listarAlunosPorOferta(Long idOferta) {
        String sql = "SELECT " + COLUNAS + " FROM usuario u "
                + "JOIN matricula_oferta m ON m.id_usuario = u.id_usuario "
                + "WHERE m.id_oferta = ? ORDER BY u.nome";
        return jdbcTemplate.query(sql, MAPPER, idOferta);
    }

    /** Quantos dos ids informados pertencem a usuários com o perfil dado (validação de lote). */
    public int contarComPerfil(List<Long> ids, String perfil) {
        if (ids.isEmpty()) {
            return 0;
        }
        String sql = "SELECT count(*) FROM usuario WHERE perfil = ? AND id_usuario = ANY (?)";
        Integer total = jdbcTemplate.query(con -> {
            var ps = con.prepareStatement(sql);
            ps.setString(1, perfil);
            ps.setArray(2, con.createArrayOf("bigint", ids.toArray()));
            return ps;
        }, rs -> rs.next() ? rs.getInt(1) : 0);
        return total == null ? 0 : total;
    }

    /** SHA-256 em hexadecimal — mesmo formato de {@code encode(sha256(...), 'hex')} usado no carga.sql. */
    public static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
