package br.uel.avaliacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

import br.uel.avaliacao.dao.AvaliacaoDAO;
import br.uel.avaliacao.dao.RespostaDAO;
import br.uel.avaliacao.dto.SubmissaoRequestDTO.ItemRespostaDTO;
import br.uel.avaliacao.exception.RegraNegocioException;
import br.uel.avaliacao.model.Alternativa;
import br.uel.avaliacao.model.Avaliacao;
import br.uel.avaliacao.model.Questao;
import br.uel.avaliacao.model.RespostaQuestao;
import br.uel.avaliacao.model.Usuario;

/**
 * Testes das regras de RF5/RF6 do {@link RespostaService} com DAOs falsos em memória
 * (sem banco e sem frameworks de mock).
 */
class RespostaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final Long ID_AVALIACAO = 1L;

    private static final Usuario ALUNO = new Usuario(7L, "aluno01", "Ana Silva", "ana.silva@uel.br", Usuario.ALUNO);
    private static final Usuario OUTRO_ALUNO = new Usuario(8L, "aluno02", "Bruno Costa", "bruno.costa@uel.br", Usuario.ALUNO);
    private static final Usuario ADMIN = new Usuario(1L, "admin01", "Coord.", "coord@uel.br", Usuario.ADMIN);

    private FakeAvaliacaoDAO avaliacaoDAO;
    private FakeRespostaDAO respostaDAO;
    private RespostaService service;

    @BeforeEach
    void setUp() {
        avaliacaoDAO = new FakeAvaliacaoDAO();
        respostaDAO = new FakeRespostaDAO();
        service = servicoEm(LocalDateTime.of(2026, 11, 26, 10, 0));
    }

    private RespostaService servicoEm(LocalDateTime instante) {
        Clock clock = Clock.fixed(instante.atZone(ZONA).toInstant(), ZONA);
        return new RespostaService(avaliacaoDAO, respostaDAO, clock);
    }

    private static List<ItemRespostaDTO> respostasValidas() {
        return List.of(
                new ItemRespostaDTO(1L, null, 5, null),
                new ItemRespostaDTO(16L, 103L, null, null),
                new ItemRespostaDTO(26L, null, null, "Explica muito bem."));
    }

    @Test
    void registraParticipacaoESubmissaoAnonimaSeparadas() {
        service.registrarSubmissaoAnonima(ID_AVALIACAO, ALUNO, respostasValidas());

        assertThat(respostaDAO.participacoes).containsExactly(ID_AVALIACAO + ":" + ALUNO.idUsuario());
        assertThat(respostaDAO.submissoes).hasSize(1);
        assertThat(respostaDAO.respostas).hasSize(3)
                .allSatisfy(r -> assertThat(r.idSubmissao()).isEqualTo(respostaDAO.submissoes.getFirst()));
        assertThat(respostaDAO.respostas)
                .extracting(RespostaQuestao::idQuestao)
                .containsExactly(1L, 16L, 26L);
    }

    @Test
    void segundaRespostaDoMesmoAlunoRetorna409SemGravarNovaSubmissao() {
        service.registrarSubmissaoAnonima(ID_AVALIACAO, ALUNO, respostasValidas());

        assertThatThrownBy(() -> service.registrarSubmissaoAnonima(ID_AVALIACAO, ALUNO, respostasValidas()))
                .isInstanceOf(RegraNegocioException.class)
                .satisfies(e -> assertThat(((RegraNegocioException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(respostaDAO.submissoes).hasSize(1);
    }

    @Test
    void alunosDiferentesPodemResponderAMesmaAvaliacao() {
        service.registrarSubmissaoAnonima(ID_AVALIACAO, ALUNO, respostasValidas());
        service.registrarSubmissaoAnonima(ID_AVALIACAO, OUTRO_ALUNO, respostasValidas());

        assertThat(respostaDAO.participacoes).hasSize(2);
        assertThat(respostaDAO.submissoes).hasSize(2).doesNotHaveDuplicates();
    }

    @Test
    void violacaoDaChavePrimariaEmCorridaVira409() {
        respostaDAO.simularCorrida = true;   // jaParticipou() diz "não", mas o INSERT viola a PK

        assertThatThrownBy(() -> service.registrarSubmissaoAnonima(ID_AVALIACAO, ALUNO, respostasValidas()))
                .isInstanceOf(RegraNegocioException.class)
                .satisfies(e -> assertThat(((RegraNegocioException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(respostaDAO.submissoes).isEmpty();
    }

    @Test
    void foraDaJanelaDeRespostaERecusado() {
        RespostaService depoisDoPrazo = servicoEm(LocalDateTime.of(2026, 12, 20, 10, 0));

        assertThatThrownBy(() -> depoisDoPrazo.registrarSubmissaoAnonima(ID_AVALIACAO, ALUNO, respostasValidas()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ENCERRADA");
        assertThat(respostaDAO.participacoes).isEmpty();
    }

    @Test
    void alunoNaoMatriculadoRecebe403() {
        avaliacaoDAO.matriculados.remove(ALUNO.idUsuario());

        assertThatThrownBy(() -> service.registrarSubmissaoAnonima(ID_AVALIACAO, ALUNO, respostasValidas()))
                .isInstanceOf(RegraNegocioException.class)
                .satisfies(e -> assertThat(((RegraNegocioException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void perfilQueNaoEAlunoNaoPodeResponder() {
        assertThatThrownBy(() -> service.registrarSubmissaoAnonima(ID_AVALIACAO, ADMIN, respostasValidas()))
                .isInstanceOf(RegraNegocioException.class)
                .satisfies(e -> assertThat(((RegraNegocioException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void questaoObrigatoriaSemRespostaERecusada() {
        List<ItemRespostaDTO> semEscala = List.of(new ItemRespostaDTO(16L, 103L, null, null));

        assertThatThrownBy(() -> service.registrarSubmissaoAnonima(ID_AVALIACAO, ALUNO, semEscala))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("obrigatórias");
        assertThat(respostaDAO.participacoes).isEmpty();
    }

    @Test
    void alternativaDeOutraQuestaoERecusada() {
        List<ItemRespostaDTO> itens = List.of(
                new ItemRespostaDTO(1L, null, 4, null),
                new ItemRespostaDTO(16L, 999L, null, null));

        assertThatThrownBy(() -> service.registrarSubmissaoAnonima(ID_AVALIACAO, ALUNO, itens))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("alternativa inválida");
    }

    @Test
    void notaForaDaEscalaETipoIncoerenteSaoRecusados() {
        List<ItemRespostaDTO> notaInvalida = List.of(
                new ItemRespostaDTO(1L, null, 6, null),
                new ItemRespostaDTO(16L, 103L, null, null));
        List<ItemRespostaDTO> textoEmQuestaoDeEscala = List.of(
                new ItemRespostaDTO(1L, null, null, "ótimo"),
                new ItemRespostaDTO(16L, 103L, null, null));

        assertThatThrownBy(() -> service.registrarSubmissaoAnonima(ID_AVALIACAO, ALUNO, notaInvalida))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.registrarSubmissaoAnonima(ID_AVALIACAO, ALUNO, textoEmQuestaoDeEscala))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void questaoQueNaoPertenceAAvaliacaoERecusada() {
        List<ItemRespostaDTO> itens = new ArrayList<>(respostasValidas());
        itens.add(new ItemRespostaDTO(30L, null, null, "intrusa"));

        assertThatThrownBy(() -> service.registrarSubmissaoAnonima(ID_AVALIACAO, ALUNO, itens))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não pertence");
    }

    @Test
    void textoLivreOpcionalEmBrancoEIgnorado() {
        List<ItemRespostaDTO> itens = List.of(
                new ItemRespostaDTO(1L, null, 3, null),
                new ItemRespostaDTO(16L, 102L, null, null),
                new ItemRespostaDTO(26L, null, null, "   "));

        service.registrarSubmissaoAnonima(ID_AVALIACAO, ALUNO, itens);

        assertThat(respostaDAO.respostas).extracting(RespostaQuestao::idQuestao).containsExactly(1L, 16L);
    }

    // ------------------------------------------------------------------ fakes

    private static Avaliacao avaliacaoAberta() {
        Questao escala = new Questao(1L, "Clareza", Questao.ESCALA_1_5, "DIDATICA", true, List.of());
        Questao multipla = new Questao(16L, "Exemplos práticos?", Questao.MULTIPLA_ESCOLHA, "DIDATICA", true, List.of(
                new Alternativa(101L, 16L, "Nunca", 1, 1),
                new Alternativa(102L, 16L, "Raramente", 2, 2),
                new Alternativa(103L, 16L, "Frequentemente", 3, 3),
                new Alternativa(104L, 16L, "Sempre", 4, 4)));
        Questao texto = new Questao(26L, "Pontos fortes", Questao.TEXTO_LIVRE, "DIDATICA", true, List.of());
        return new Avaliacao(ID_AVALIACAO, 1L, "BD I", null,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 15), LocalTime.MIDNIGHT, LocalTime.of(23, 59, 59), true,
                "2COP001", "Banco de Dados I", "Prof. Anderson", 2026, 2, "A", 3, 18, 0, null, null,
                List.of(new Avaliacao.Item(1, true, escala),
                        new Avaliacao.Item(2, true, multipla),
                        new Avaliacao.Item(3, false, texto)));
    }

    private static final class FakeAvaliacaoDAO extends AvaliacaoDAO {
        final Set<Long> matriculados = new HashSet<>(Set.of(7L, 8L));

        FakeAvaliacaoDAO() {
            super(null, null);
        }

        @Override
        public Optional<Avaliacao> buscarCompletaPorId(Long idAvaliacao) {
            return ID_AVALIACAO.equals(idAvaliacao) ? Optional.of(avaliacaoAberta()) : Optional.empty();
        }

        @Override
        public boolean alunoMatriculado(Long idAvaliacao, Long idUsuario) {
            return matriculados.contains(idUsuario);
        }
    }

    private static final class FakeRespostaDAO extends RespostaDAO {
        final List<String> participacoes = new ArrayList<>();
        final List<UUID> submissoes = new ArrayList<>();
        final List<RespostaQuestao> respostas = new ArrayList<>();
        boolean simularCorrida;

        FakeRespostaDAO() {
            super(null);
        }

        @Override
        public boolean jaParticipou(Long idAvaliacao, Long idUsuario) {
            return !simularCorrida && participacoes.contains(idAvaliacao + ":" + idUsuario);
        }

        @Override
        public void registrarParticipacao(Long idAvaliacao, Long idUsuario, LocalDate data) {
            String chave = idAvaliacao + ":" + idUsuario;
            if (simularCorrida || participacoes.contains(chave)) {
                throw new DuplicateKeyException("pk_participacao");
            }
            participacoes.add(chave);
        }

        @Override
        public UUID inserirSubmissaoAnonima(Long idAvaliacao, LocalDate data) {
            UUID id = UUID.randomUUID();
            submissoes.add(id);
            return id;
        }

        @Override
        public void inserirRespostasQuestoes(UUID idSubmissao, List<RespostaQuestao> novas) {
            novas.forEach(r -> respostas.add(new RespostaQuestao(null, idSubmissao, r.idQuestao(),
                    r.idAlternativa(), r.notaEscala(), r.textoResposta())));
        }
    }
}
