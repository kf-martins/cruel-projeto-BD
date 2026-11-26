package br.uel.avaliacao.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTOs de entrada dos cadastros administrativos (RF1–RF4) e do login. */
public final class CadastroDTOs {

    private CadastroDTOs() {
    }

    /** Login simplificado: e-mail OU matrícula + senha. */
    public record LoginRequest(@NotBlank String login, @NotBlank String senha) {
    }

    /** RF1 — nova questão. Alternativas obrigatórias (≥ 2) apenas para MULTIPLA_ESCOLHA. */
    public record NovaQuestaoRequest(
            @NotBlank @Size(max = 1000) String enunciado,
            @NotBlank String tipo,
            @NotBlank String categoria,
            List<@Valid NovaAlternativaRequest> alternativas) {
    }

    public record NovaAlternativaRequest(
            @NotBlank @Size(max = 300) String texto,
            @NotNull Integer valorNumerico) {
    }

    /** Oferta de disciplina (pré-requisito para criar uma nova avaliação). */
    public record NovaOfertaRequest(
            @NotNull Long idDisciplina,
            @NotNull Long idDocente,
            @NotNull @Min(2020) Integer ano,
            @NotNull Integer semestre,
            @NotBlank @Size(max = 10) String turma,
            List<Long> idsAlunos) {
    }

    /** RF4 — vincular alunos (turma) a uma oferta. */
    public record MatriculaRequest(@NotEmpty List<Long> idsAlunos) {
    }

    /** RF2 + RF3 (+ RF4 opcional) — avaliação com período, questões e alunos elegíveis. */
    public record NovaAvaliacaoRequest(
            @NotNull Long idOferta,
            @NotBlank @Size(max = 150) String titulo,
            String descricao,
            @NotNull LocalDate dataAvaliacao,
            @NotNull LocalDate dataEncerramento,
            @NotNull LocalTime horaInicio,
            @NotNull LocalTime horaFim,
            @NotEmpty List<@Valid QuestaoAvaliacaoRequest> questoes,
            List<Long> idsAlunos) {
    }

    /** Questão escolhida para a avaliação; a ordem é a posição na lista. */
    public record QuestaoAvaliacaoRequest(@NotNull Long idQuestao, Boolean obrigatoria) {
    }

    /** Ativar/desativar questão ou avaliação. */
    public record StatusRequest(@NotNull Boolean ativa) {
    }
}
