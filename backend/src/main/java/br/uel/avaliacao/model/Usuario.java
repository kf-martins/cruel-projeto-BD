package br.uel.avaliacao.model;

/** Usuário do sistema (tabela {@code usuario}). A senha nunca sai do banco/DAO. */
public record Usuario(
        Long idUsuario,
        String matricula,
        String nome,
        String email,
        String perfil) {

    public static final String ADMIN = "ADMIN";
    public static final String DOCENTE = "DOCENTE";
    public static final String ALUNO = "ALUNO";

    public boolean isAluno() {
        return ALUNO.equals(perfil);
    }
}
