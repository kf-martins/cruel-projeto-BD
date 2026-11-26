package br.uel.avaliacao.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Avaliação aplicada a uma oferta (tabela {@code avaliacao}) com dados de leitura da oferta.
 * {@code itens} só é preenchido na consulta completa (questionário para responder/visualizar).
 */
public record Avaliacao(
        Long idAvaliacao,
        Long idOferta,
        String titulo,
        String descricao,
        LocalDate dataAvaliacao,
        LocalDate dataEncerramento,
        LocalTime horaInicio,
        LocalTime horaFim,
        boolean ativa,
        String codigoDisciplina,
        String nomeDisciplina,
        String nomeDocente,
        Integer ano,
        Integer semestre,
        String turma,
        Integer totalQuestoes,
        Integer totalMatriculados,
        Integer totalRespondentes,
        Boolean respondida,
        String situacao,
        List<Item> itens) {

    /** Questão dentro da avaliação (tabela {@code avaliacao_questao}). */
    public record Item(Integer ordem, boolean obrigatoria, Questao questao) {
    }

    public LocalDateTime abertura() {
        return LocalDateTime.of(dataAvaliacao, horaInicio);
    }

    public LocalDateTime encerramento() {
        return LocalDateTime.of(dataEncerramento, horaFim);
    }

    /** ABERTA, AGENDADA, ENCERRADA ou INATIVA no instante informado. */
    public String calcularSituacao(LocalDateTime agora) {
        if (!ativa) {
            return "INATIVA";
        }
        if (agora.isBefore(abertura())) {
            return "AGENDADA";
        }
        if (agora.isAfter(encerramento())) {
            return "ENCERRADA";
        }
        return "ABERTA";
    }

    public Avaliacao comItens(List<Item> novos) {
        return new Avaliacao(idAvaliacao, idOferta, titulo, descricao, dataAvaliacao, dataEncerramento,
                horaInicio, horaFim, ativa, codigoDisciplina, nomeDisciplina, nomeDocente, ano, semestre,
                turma, totalQuestoes, totalMatriculados, totalRespondentes, respondida, situacao, novos);
    }

    /** Cópia com o campo {@code situacao} calculado para o instante informado. */
    public Avaliacao comSituacao(LocalDateTime agora) {
        return new Avaliacao(idAvaliacao, idOferta, titulo, descricao, dataAvaliacao, dataEncerramento,
                horaInicio, horaFim, ativa, codigoDisciplina, nomeDisciplina, nomeDocente, ano, semestre,
                turma, totalQuestoes, totalMatriculados, totalRespondentes, respondida, calcularSituacao(agora), itens);
    }
}
