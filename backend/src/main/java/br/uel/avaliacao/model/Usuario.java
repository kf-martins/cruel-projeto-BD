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

    public boolean isDocente() {
        return DOCENTE.equals(perfil);
    }

    /**
     * Escopo de visibilidade dos dados de gestão: o id do docente (que só enxerga as próprias
     * ofertas) ou {@code null} para o ADMIN (sem restrição).
     */
    public Long idDocenteEscopo() {
        return isDocente() ? idUsuario : null;
    }
}
