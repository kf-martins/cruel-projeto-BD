package br.uel.avaliacao.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uel.avaliacao.dao.AvaliacaoDAO;
import br.uel.avaliacao.dao.AvaliacaoDAO.ItemComposicao;
import br.uel.avaliacao.dao.DisciplinaDAO;
import br.uel.avaliacao.dao.QuestaoDAO;
import br.uel.avaliacao.dao.UsuarioDAO;
import br.uel.avaliacao.dto.CadastroDTOs.NovaAlternativaRequest;
import br.uel.avaliacao.dto.CadastroDTOs.NovaAvaliacaoRequest;
import br.uel.avaliacao.dto.CadastroDTOs.NovaOfertaRequest;
import br.uel.avaliacao.dto.CadastroDTOs.NovaQuestaoRequest;
import br.uel.avaliacao.dto.CadastroDTOs.QuestaoAvaliacaoRequest;
import br.uel.avaliacao.exception.RegraNegocioException;
import br.uel.avaliacao.model.Alternativa;
import br.uel.avaliacao.model.Avaliacao;
import br.uel.avaliacao.model.OfertaDisciplina;
import br.uel.avaliacao.model.Questao;
import br.uel.avaliacao.model.Usuario;

/**
 * Regras de negócio dos cadastros administrativos:
 * RF1 (questões + alternativas), RF2/RF3 (avaliação + questões) e RF4 (turma/alunos elegíveis).
 * Toda gravação em mais de uma tabela roda em uma única transação JDBC.
 */
@Service
public class AvaliacaoService {

    private static final Logger log = LoggerFactory.getLogger(AvaliacaoService.class);
    private static final int MIN_ALTERNATIVAS = 2;
    private static final int MAX_ALTERNATIVAS = 10;

    private final QuestaoDAO questaoDAO;
    private final AvaliacaoDAO avaliacaoDAO;
    private final DisciplinaDAO disciplinaDAO;
    private final UsuarioDAO usuarioDAO;
    private final Clock clock;

    public AvaliacaoService(QuestaoDAO questaoDAO, AvaliacaoDAO avaliacaoDAO, DisciplinaDAO disciplinaDAO,
                            UsuarioDAO usuarioDAO, Clock clock) {
        this.questaoDAO = questaoDAO;
        this.avaliacaoDAO = avaliacaoDAO;
        this.disciplinaDAO = disciplinaDAO;
        this.usuarioDAO = usuarioDAO;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ RF1 — questões

    public List<Questao> listarQuestoes() {
        return questaoDAO.listarTodasComAlternativas();
    }

    /** RF1 — grava a questão e (se múltipla escolha) suas alternativas na mesma transação. */
    @Transactional(rollbackFor = Exception.class)
    public Questao criarQuestao(NovaQuestaoRequest req) {
        String tipo = req.tipo().trim().toUpperCase(Locale.ROOT);
        String categoria = req.categoria().trim().toUpperCase(Locale.ROOT);
        if (!Questao.TIPOS.contains(tipo)) {
            throw RegraNegocioException.requisicaoInvalida("Tipo de questão inválido: " + req.tipo());
        }
        if (!Questao.CATEGORIAS.contains(categoria)) {
            throw RegraNegocioException.requisicaoInvalida("Categoria inválida: " + req.categoria());
        }

        List<NovaAlternativaRequest> alternativas = req.alternativas() == null ? List.of() : req.alternativas();
        if (Questao.MULTIPLA_ESCOLHA.equals(tipo)) {
            if (alternativas.size() < MIN_ALTERNATIVAS || alternativas.size() > MAX_ALTERNATIVAS) {
                throw RegraNegocioException.requisicaoInvalida(
                        "Questões de múltipla escolha precisam de " + MIN_ALTERNATIVAS + " a " + MAX_ALTERNATIVAS + " alternativas.");
            }
            Set<String> textos = new HashSet<>();
            for (NovaAlternativaRequest a : alternativas) {
                if (!textos.add(a.texto().trim().toLowerCase(Locale.ROOT))) {
                    throw RegraNegocioException.requisicaoInvalida("Alternativas repetidas: " + a.texto());
                }
            }
        } else if (!alternativas.isEmpty()) {
            throw RegraNegocioException.requisicaoInvalida("Apenas questões de múltipla escolha possuem alternativas.");
        }

        Long id = questaoDAO.inserirQuestao(new Questao(null, req.enunciado().trim(), tipo, categoria, true, List.of()));
        if (!alternativas.isEmpty()) {
            questaoDAO.inserirAlternativas(id, alternativas.stream()
                    .map(a -> new Alternativa(null, id, a.texto().trim(), a.valorNumerico(), null))
                    .toList());
        }
        log.info("Questão {} criada ({}, {} alternativas)", id, tipo, alternativas.size());
        return questaoDAO.buscarPorId(id).orElseThrow();
    }

    @Transactional(rollbackFor = Exception.class)
    public void alterarStatusQuestao(Long idQuestao, boolean ativa) {
        if (questaoDAO.atualizarAtiva(idQuestao, ativa) == 0) {
            throw RegraNegocioException.naoEncontrado("Questão não encontrada.");
        }
    }

    // ------------------------------------------------------------------ RF4 — ofertas e turmas

    /** Cria uma oferta (disciplina + docente + ano/semestre/turma) e, opcionalmente, já matricula a turma. */
    @Transactional(rollbackFor = Exception.class)
    public OfertaDisciplina criarOferta(NovaOfertaRequest req) {
        if (req.semestre() != 1 && req.semestre() != 2) {
            throw RegraNegocioException.requisicaoInvalida("O semestre deve ser 1 ou 2.");
        }
        if (!disciplinaDAO.disciplinaExiste(req.idDisciplina())) {
            throw RegraNegocioException.naoEncontrado("Disciplina não encontrada.");
        }
        if (usuarioDAO.contarComPerfil(List.of(req.idDocente()), Usuario.DOCENTE) != 1) {
            throw RegraNegocioException.requisicaoInvalida("O docente informado não existe ou não tem perfil DOCENTE.");
        }
        Long idOferta = disciplinaDAO.criarOferta(req.idDisciplina(), req.idDocente(), req.ano(), req.semestre(),
                req.turma().trim().toUpperCase(Locale.ROOT));
        if (req.idsAlunos() != null && !req.idsAlunos().isEmpty()) {
            matricular(idOferta, req.idsAlunos());
        }
        log.info("Oferta {} criada", idOferta);
        return disciplinaDAO.buscarOfertaPorId(idOferta).orElseThrow();
    }

    /** RF4 — vincula alunos (turma) a uma oferta existente. */
    @Transactional(rollbackFor = Exception.class)
    public int matricularAlunos(Long idOferta, List<Long> idsAlunos) {
        if (disciplinaDAO.buscarOfertaPorId(idOferta).isEmpty()) {
            throw RegraNegocioException.naoEncontrado("Oferta não encontrada.");
        }
        return matricular(idOferta, idsAlunos);
    }

    private int matricular(Long idOferta, List<Long> idsAlunos) {
        List<Long> distintos = new ArrayList<>(new LinkedHashSet<>(idsAlunos));
        if (usuarioDAO.contarComPerfil(distintos, Usuario.ALUNO) != distintos.size()) {
            throw RegraNegocioException.requisicaoInvalida("Todos os usuários matriculados devem existir e ter perfil ALUNO.");
        }
        return disciplinaDAO.vincularAlunosOferta(idOferta, distintos);
    }

    // ------------------------------------------------------------------ RF2 / RF3 — avaliações

    public List<Avaliacao> listarAvaliacoes() {
        LocalDateTime agora = LocalDateTime.now(clock);
        return avaliacaoDAO.listarTodas().stream().map(a -> a.comSituacao(agora)).toList();
    }

    public List<Avaliacao> listarAvaliacoesDoAluno(Usuario aluno) {
        LocalDateTime agora = LocalDateTime.now(clock);
        return avaliacaoDAO.listarDoAluno(aluno.idUsuario()).stream().map(a -> a.comSituacao(agora)).toList();
    }

    /** Questionário completo. Alunos só acessam avaliações ativas das ofertas em que estão matriculados. */
    public Avaliacao buscarAvaliacao(Long idAvaliacao, Usuario usuario) {
        Avaliacao avaliacao = avaliacaoDAO.buscarCompletaPorId(idAvaliacao)
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Avaliação não encontrada."));
        if (usuario.isAluno()) {
            if (!avaliacao.ativa() || !avaliacaoDAO.alunoMatriculado(idAvaliacao, usuario.idUsuario())) {
                throw new RegraNegocioException(HttpStatus.FORBIDDEN, "Você não está matriculado na oferta desta avaliação.");
            }
        }
        return avaliacao.comSituacao(LocalDateTime.now(clock));
    }

    /** RF2 + RF3 (+ RF4): cria a avaliação, vincula as questões e, opcionalmente, matricula alunos — atomicamente. */
    @Transactional(rollbackFor = Exception.class)
    public Avaliacao criarAvaliacao(NovaAvaliacaoRequest req) {
        if (disciplinaDAO.buscarOfertaPorId(req.idOferta()).isEmpty()) {
            throw RegraNegocioException.naoEncontrado("Oferta não encontrada.");
        }
        if (avaliacaoDAO.existeParaOferta(req.idOferta())) {
            throw RegraNegocioException.conflito("Esta oferta já possui uma avaliação (uma por disciplina/semestre/turma).");
        }
        if (req.dataEncerramento().isBefore(req.dataAvaliacao())) {
            throw RegraNegocioException.requisicaoInvalida("A data de encerramento deve ser igual ou posterior à data de abertura.");
        }
        if (!req.horaFim().isAfter(req.horaInicio())) {
            throw RegraNegocioException.requisicaoInvalida("A hora de fim deve ser posterior à hora de início.");
        }

        List<QuestaoAvaliacaoRequest> escolhidas = req.questoes();
        Set<Long> ids = new LinkedHashSet<>();
        for (QuestaoAvaliacaoRequest q : escolhidas) {
            if (!ids.add(q.idQuestao())) {
                throw RegraNegocioException.requisicaoInvalida("Questão repetida na avaliação: " + q.idQuestao());
            }
        }
        Map<Long, Questao> ativas = questaoDAO.listarAtivasPorIds(ids).stream()
                .collect(Collectors.toMap(Questao::idQuestao, Function.identity()));
        if (ativas.size() != ids.size()) {
            throw RegraNegocioException.requisicaoInvalida("Há questões inexistentes ou inativas na seleção.");
        }

        Avaliacao nova = new Avaliacao(null, req.idOferta(), req.titulo().trim(), req.descricao(),
                req.dataAvaliacao(), req.dataEncerramento(), req.horaInicio(), req.horaFim(), true,
                null, null, null, null, null, null, null, null, null, null, null, List.of());
        Long idAvaliacao = avaliacaoDAO.inserirAvaliacao(nova);

        List<ItemComposicao> itens = new ArrayList<>();
        for (int i = 0; i < escolhidas.size(); i++) {
            QuestaoAvaliacaoRequest q = escolhidas.get(i);
            boolean obrigatoria = q.obrigatoria() != null
                    ? q.obrigatoria()
                    : !Questao.TEXTO_LIVRE.equals(ativas.get(q.idQuestao()).tipo());
            itens.add(new ItemComposicao(q.idQuestao(), i + 1, obrigatoria));
        }
        avaliacaoDAO.vincularQuestoes(idAvaliacao, itens);

        if (req.idsAlunos() != null && !req.idsAlunos().isEmpty()) {
            matricular(req.idOferta(), req.idsAlunos());
        }
        log.info("Avaliação {} criada com {} questões", idAvaliacao, itens.size());
        return avaliacaoDAO.buscarCompletaPorId(idAvaliacao).orElseThrow().comSituacao(LocalDateTime.now(clock));
    }

    @Transactional(rollbackFor = Exception.class)
    public void alterarStatusAvaliacao(Long idAvaliacao, boolean ativa) {
        if (avaliacaoDAO.atualizarAtiva(idAvaliacao, ativa) == 0) {
            throw RegraNegocioException.naoEncontrado("Avaliação não encontrada.");
        }
    }
}
