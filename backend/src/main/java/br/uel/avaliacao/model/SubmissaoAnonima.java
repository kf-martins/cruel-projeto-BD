package br.uel.avaliacao.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Envelope anônimo de respostas (tabela {@code submissao_anonima}).
 * Propositalmente NÃO possui qualquer campo que identifique o usuário (RF6).
 */
public record SubmissaoAnonima(
        UUID idSubmissao,
        Long idAvaliacao,
        LocalDate dataSubmissao) {
}
