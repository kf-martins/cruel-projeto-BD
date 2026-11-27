package br.uel.avaliacao.model;

/** Alternativa de uma questão de múltipla escolha (tabela {@code alternativa}). */
public record Alternativa(
        Long idAlternativa,
        Long idQuestao,
        String texto,
        Integer valorNumerico,
        Integer ordem) {
}
