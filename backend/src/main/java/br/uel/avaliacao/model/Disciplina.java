package br.uel.avaliacao.model;

/** Disciplina do catálogo do curso (tabela {@code disciplina}). */
public record Disciplina(
        Long idDisciplina,
        String codigo,
        String nome,
        Integer cargaHoraria) {
}
