package br.uel.avaliacao.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uel.avaliacao.dao.AvaliacaoDAO;
import br.uel.avaliacao.dao.RespostaDAO;
import br.uel.avaliacao.dto.SubmissaoRequestDTO.ItemRespostaDTO;
import br.uel.avaliacao.exception.RegraNegocioException;
import br.uel.avaliacao.model.Avaliacao;
import br.uel.avaliacao.model.Questao;
import br.uel.avaliacao.model.RespostaQuestao;
import br.uel.avaliacao.model.Usuario;

/**
 * RF5 + RF6 — submissão anônima com unicidade por aluno/avaliação.
 *
 * <p>Em UMA transação: (1) grava em {@code participacao_avaliacao} QUEM respondeu — a PK composta
 * impede a 2ª resposta; (2) grava em {@code submissao_anonima}/{@code resposta_questao} O QUE foi
 * respondido, sem nenhuma referência ao usuário. Se qualquer passo falhar, nada é gravado.
 */
@Service
public class RespostaService {

    private static final Logger log = LoggerFactory.getLogger(RespostaService.class);
    static final String MSG_JA_RESPONDEU = "Você já respondeu esta avaliação. Cada aluno pode responder apenas uma vez.";

    private final AvaliacaoDAO avaliacaoDAO;
    private final RespostaDAO respostaDAO;
    private final Clock clock;

    public RespostaService(AvaliacaoDAO avaliacaoDAO, RespostaDAO respostaDAO, Clock clock) {
        this.avaliacaoDAO = avaliacaoDAO;
        this.respostaDAO = respostaDAO;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public void registrarSubmissaoAnonima(Long idAvaliacao, Usuario aluno, List<ItemRespostaDTO> itens) {
        if (!aluno.isAluno()) {
            throw new RegraNegocioException(HttpStatus.FORBIDDEN, "Apenas alunos podem responder avaliações.");
        }

        // 1. Avaliação existe, está ativa e dentro da janela de data/hora
        Avaliacao avaliacao = avaliacaoDAO.buscarCompletaPorId(idAvaliacao)
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Avaliação não encontrada."));
        LocalDateTime agora = LocalDateTime.now(clock);
        String situacao = avaliacao.calcularSituacao(agora);
        if (!"ABERTA".equals(situacao)) {
            throw RegraNegocioException.requisicaoInvalida(
                    "Esta avaliação não está aberta para respostas (situação: " + situacao + ").");
        }

        // 2. Elegibilidade: aluno matriculado na oferta
        if (!avaliacaoDAO.alunoMatriculado(idAvaliacao, aluno.idUsuario())) {
            throw new RegraNegocioException(HttpStatus.FORBIDDEN, "Você não está matriculado na oferta desta avaliação.");
        }

        // 3. Unicidade (checagem amigável; a garantia real é a PK de participacao_avaliacao)
        if (respostaDAO.jaParticipou(idAvaliacao, aluno.idUsuario())) {
            throw RegraNegocioException.conflito(MSG_JA_RESPONDEU);
        }

        // 4. Valida as respostas contra as questões da avaliação
        List<RespostaQuestao> respostas = validarItens(avaliacao, itens);

        // 5. Quem respondeu (identificado) ...
        LocalDate hoje = agora.toLocalDate();
        try {
            respostaDAO.registrarParticipacao(idAvaliacao, aluno.idUsuario(), hoje);
        } catch (DuplicateKeyException e) {
            // Duas requisições simultâneas do mesmo aluno: a PK composta barra a segunda.
            throw RegraNegocioException.conflito(MSG_JA_RESPONDEU);
        }

        // 6. ... e o que foi respondido (anônimo) — nenhum dado do aluno é passado daqui em diante.
        UUID idSubmissao = respostaDAO.inserirSubmissaoAnonima(idAvaliacao, hoje);
        respostaDAO.inserirRespostasQuestoes(idSubmissao, respostas);

        // Log propositalmente sem id do usuário nem da submissão.
        log.info("Nova submissão anônima registrada na avaliação {} ({} respostas)", idAvaliacao, respostas.size());
    }

    /** Converte e valida os itens: questão pertence à avaliação, tipo coerente, obrigatórias respondidas. */
    List<RespostaQuestao> validarItens(Avaliacao avaliacao, List<ItemRespostaDTO> itens) {
        Map<Long, Avaliacao.Item> porQuestao = avaliacao.itens().stream()
                .collect(Collectors.toMap(it -> it.questao().idQuestao(), Function.identity()));

        Set<Long> respondidas = new HashSet<>();
        List<RespostaQuestao> respostas = new ArrayList<>();

        for (ItemRespostaDTO item : itens) {
            Avaliacao.Item itemAvaliacao = porQuestao.get(item.idQuestao());
            if (itemAvaliacao == null) {
                throw RegraNegocioException.requisicaoInvalida("A questão " + item.idQuestao() + " não pertence a esta avaliação.");
            }
            if (!respondidas.add(item.idQuestao())) {
                throw RegraNegocioException.requisicaoInvalida("A questão " + item.idQuestao() + " foi respondida mais de uma vez.");
            }
            Questao q = itemAvaliacao.questao();
            String rotulo = "Questão " + itemAvaliacao.ordem();

            switch (q.tipo()) {
                case Questao.ESCALA_1_5 -> {
                    if (item.notaEscala() == null || item.idAlternativa() != null || item.textoResposta() != null) {
                        throw RegraNegocioException.requisicaoInvalida(rotulo + ": informe apenas uma nota de 1 a 5.");
                    }
                    if (item.notaEscala() < 1 || item.notaEscala() > 5) {
                        throw RegraNegocioException.requisicaoInvalida(rotulo + ": a nota deve estar entre 1 e 5.");
                    }
                    respostas.add(new RespostaQuestao(null, null, q.idQuestao(), null, item.notaEscala(), null));
                }
                case Questao.MULTIPLA_ESCOLHA -> {
                    if (item.idAlternativa() == null || item.notaEscala() != null || item.textoResposta() != null) {
                        throw RegraNegocioException.requisicaoInvalida(rotulo + ": escolha exatamente uma alternativa.");
                    }
                    boolean valida = q.alternativas().stream()
                            .anyMatch(a -> a.idAlternativa().equals(item.idAlternativa()));
                    if (!valida) {
                        throw RegraNegocioException.requisicaoInvalida(rotulo + ": alternativa inválida para esta questão.");
                    }
                    respostas.add(new RespostaQuestao(null, null, q.idQuestao(), item.idAlternativa(), null, null));
                }
                default -> { // TEXTO_LIVRE
                    if (item.idAlternativa() != null || item.notaEscala() != null) {
                        throw RegraNegocioException.requisicaoInvalida(rotulo + ": responda com texto.");
                    }
                    String texto = item.textoResposta() == null ? "" : item.textoResposta().trim();
                    if (texto.isEmpty()) {
                        respondidas.remove(q.idQuestao());   // texto em branco = não respondida
                    } else {
                        respostas.add(new RespostaQuestao(null, null, q.idQuestao(), null, null, texto));
                    }
                }
            }
        }

        List<Integer> faltantes = avaliacao.itens().stream()
                .filter(it -> it.obrigatoria() && !respondidas.contains(it.questao().idQuestao()))
                .map(Avaliacao.Item::ordem)
                .sorted()
                .toList();
        if (!faltantes.isEmpty()) {
            throw RegraNegocioException.requisicaoInvalida("Responda as questões obrigatórias: " + faltantes);
        }
        if (respostas.isEmpty()) {
            throw RegraNegocioException.requisicaoInvalida("Nenhuma resposta foi informada.");
        }
        return respostas;
    }
}
