package br.uel.avaliacao.model;

/**
 * Oferta de uma disciplina por um docente em um ano/semestre (tabela {@code oferta_disciplina}),
 * acrescida de dados de leitura obtidos por JOIN (nomes, total de matriculados e avaliação vinculada).
 */
public record OfertaDisciplina(
        Long idOferta,
        Long idDisciplina,
        String codigoDisciplina,
        String nomeDisciplina,
        Long idDocente,
        String nomeDocente,
        Integer ano,
        Integer semestre,
        String turma,
        Integer totalMatriculados,
        Long idAvaliacao) {
}
