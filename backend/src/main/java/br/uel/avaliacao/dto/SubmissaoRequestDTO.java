package br.uel.avaliacao.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code POST /api/respostas}. O aluno é identificado pela sessão (cabeçalho X-User-Id),
 * nunca pelo corpo — e essa identidade só é usada para gravar {@code participacao_avaliacao}.
 */
public record SubmissaoRequestDTO(
        @NotNull Long idAvaliacao,
        @NotEmpty List<@Valid ItemRespostaDTO> itens) {

    /** Resposta a uma questão: preencher apenas o campo correspondente ao tipo da questão. */
    public record ItemRespostaDTO(
            @NotNull Long idQuestao,
            Long idAlternativa,
            @Min(1) @Max(5) Integer notaEscala,
            @Size(max = 2000) String textoResposta) {
    }
}
