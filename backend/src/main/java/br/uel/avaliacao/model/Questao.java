package br.uel.avaliacao.model;

import java.util.List;
import java.util.Set;

/** Questão do banco de questões (tabela {@code questao}) com suas alternativas. */
public record Questao(
        Long idQuestao,
        String enunciado,
        String tipo,
        String categoria,
        boolean ativa,
        List<Alternativa> alternativas) {

    public static final String MULTIPLA_ESCOLHA = "MULTIPLA_ESCOLHA";
    public static final String ESCALA_1_5 = "ESCALA_1_5";
    public static final String TEXTO_LIVRE = "TEXTO_LIVRE";

    public static final Set<String> TIPOS = Set.of(MULTIPLA_ESCOLHA, ESCALA_1_5, TEXTO_LIVRE);
    public static final Set<String> CATEGORIAS = Set.of("DIDATICA", "INFRAESTRUTURA", "CONTEUDO", "GERAL");

    public Questao comAlternativas(List<Alternativa> novas) {
        return new Questao(idQuestao, enunciado, tipo, categoria, ativa, novas);
    }
}
