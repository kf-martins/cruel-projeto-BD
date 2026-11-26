package br.uel.avaliacao.model;

import java.util.UUID;

/**
 * Resposta a uma questão dentro de uma submissão anônima (tabela {@code resposta_questao}).
 * Exatamente um entre {@code idAlternativa}, {@code notaEscala} e {@code textoResposta} é preenchido.
 */
public record RespostaQuestao(
        Long idResposta,
        UUID idSubmissao,
        Long idQuestao,
        Long idAlternativa,
        Integer notaEscala,
        String textoResposta) {
}
