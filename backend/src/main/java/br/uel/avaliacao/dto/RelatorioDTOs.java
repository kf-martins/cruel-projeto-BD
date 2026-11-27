package br.uel.avaliacao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Linhas retornadas pelos relatórios analíticos (RF7) — espelham as colunas de database/relatorios.sql. */
public final class RelatorioDTOs {

    private RelatorioDTOs() {
    }

    /**
     * Filtros opcionais aceitos por todos os relatórios ({@code null} = sem filtro).
     * {@code hoje} é a data de referência (relógio da aplicação) para calcular a situação das avaliações.
     */
    public record Filtro(Integer ano, Integer semestre, Long idDisciplina, LocalDate hoje) {
    }

    /** Relatório 1 — média geral e distribuição das notas por oferta. */
    public record MediaDisciplina(
            String codigo,
            String disciplina,
            String docente,
            Integer ano,
            Integer semestre,
            Long totalSubmissoes,
            Long totalRespostas,
            BigDecimal mediaEscala,
            Long qtdNota1,
            Long qtdNota2,
            Long qtdNota3,
            Long qtdNota4,
            Long qtdNota5) {
    }

    /** Relatório 2 — média da oferta por categoria vs. média do curso (subconsulta). */
    public record ComparativoCategoria(
            String disciplina,
            String docente,
            Integer ano,
            Integer semestre,
            String categoria,
            Long totalRespostas,
            BigDecimal mediaOferta,
            BigDecimal mediaCurso,
            BigDecimal diferenca) {
    }

    /** Relatório 3 — ranking das questões objetivas por índice de aprovação. */
    public record RankingQuestao(
            Long posicao,
            Long idQuestao,
            String enunciado,
            String tipo,
            String categoria,
            Long totalRespostas,
            BigDecimal indiceAprovacao,
            BigDecimal mediaNormalizada) {
    }

    /** Relatório 4a — adesão por avaliação. */
    public record AdesaoAvaliacao(
            Long idAvaliacao,
            String titulo,
            String disciplina,
            Integer ano,
            Integer semestre,
            Long matriculados,
            Long respondentes,
            Long pendentes,
            BigDecimal taxaAdesao,
            String situacao,
            String nivelAdesao) {
    }

    /** Relatório 4b — aluno matriculado que ainda não respondeu. */
    public record AlunoPendente(
            Long idAvaliacao,
            String disciplina,
            Integer ano,
            Integer semestre,
            String matricula,
            String aluno,
            String email) {
    }

    /** Relatório 4c — questão do banco não utilizada em nenhuma avaliação. */
    public record QuestaoNaoUtilizada(Long idQuestao, String enunciado, String tipo, String categoria) {
    }

    /** Comentário anônimo (texto livre) — sem qualquer identificador de submissão ou usuário. */
    public record ComentarioAnonimo(Long idQuestao, String enunciado, String texto) {
    }
}
