package br.uel.avaliacao.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;

import br.uel.avaliacao.dao.AvaliacaoDAO;
import br.uel.avaliacao.dao.RelatorioDAO;
import br.uel.avaliacao.dao.RespostaDAO;
import br.uel.avaliacao.dto.RelatorioDTOs.AdesaoAvaliacao;
import br.uel.avaliacao.dto.RelatorioDTOs.AlunoPendente;
import br.uel.avaliacao.dto.RelatorioDTOs.ComentarioAnonimo;
import br.uel.avaliacao.dto.RelatorioDTOs.ComparativoCategoria;
import br.uel.avaliacao.dto.RelatorioDTOs.Filtro;
import br.uel.avaliacao.dto.RelatorioDTOs.MediaDisciplina;
import br.uel.avaliacao.dto.RelatorioDTOs.QuestaoNaoUtilizada;
import br.uel.avaliacao.dto.RelatorioDTOs.RankingQuestao;
import br.uel.avaliacao.exception.RegraNegocioException;
import br.uel.avaliacao.model.Usuario;

/**
 * RF7 — relatórios analíticos com filtros opcionais de ano, semestre e disciplina.
 * Quando o usuário logado é DOCENTE, todos os relatórios ficam restritos às ofertas que ele ministra.
 */
@Service
public class RelatorioService {

    private final RelatorioDAO relatorioDAO;
    private final RespostaDAO respostaDAO;
    private final AvaliacaoDAO avaliacaoDAO;
    private final Clock clock;

    public RelatorioService(RelatorioDAO relatorioDAO, RespostaDAO respostaDAO, AvaliacaoDAO avaliacaoDAO, Clock clock) {
        this.relatorioDAO = relatorioDAO;
        this.respostaDAO = respostaDAO;
        this.avaliacaoDAO = avaliacaoDAO;
        this.clock = clock;
    }

    /** Monta o filtro; o escopo de docente vem do usuário logado, nunca de parâmetro da requisição. */
    public Filtro filtro(Integer ano, Integer semestre, Long idDisciplina, Usuario usuario) {
        if (semestre != null && semestre != 1 && semestre != 2) {
            throw RegraNegocioException.requisicaoInvalida("O semestre deve ser 1 ou 2.");
        }
        return new Filtro(ano, semestre, idDisciplina, usuario.idDocenteEscopo(), LocalDate.now(clock));
    }

    public List<MediaDisciplina> mediaPorDisciplina(Filtro f) {
        return relatorioDAO.mediaPorDisciplina(f);
    }

    public List<ComparativoCategoria> comparativoPorCategoria(Filtro f) {
        return relatorioDAO.comparativoPorCategoria(f);
    }

    public List<RankingQuestao> rankingQuestoes(Filtro f, String ordem) {
        boolean criticas = "criticas".equalsIgnoreCase(ordem);
        if (!criticas && !"melhores".equalsIgnoreCase(ordem)) {
            throw RegraNegocioException.requisicaoInvalida("Use ordem=melhores ou ordem=criticas.");
        }
        return relatorioDAO.rankingQuestoes(f, criticas);
    }

    public List<AdesaoAvaliacao> adesaoPorAvaliacao(Filtro f) {
        return relatorioDAO.adesaoPorAvaliacao(f);
    }

    /**
     * Lista nominal de quem ainda não respondeu — exclusiva do ADMIN. Para o DOCENTE isso
     * enfraqueceria o anonimato (em turmas pequenas, saber quem falta ajuda a deduzir quem
     * respondeu o quê); ele vê apenas o total de pendentes no relatório de adesão.
     */
    public List<AlunoPendente> alunosPendentes(Filtro f, Usuario usuario) {
        if (usuario.isDocente()) {
            throw new RegraNegocioException(HttpStatus.FORBIDDEN,
                    "Para preservar o anonimato, docentes veem apenas o total de alunos pendentes.");
        }
        return relatorioDAO.alunosPendentes(f);
    }

    /** Visão do banco de questões inteiro — exclusiva do ADMIN. */
    public List<QuestaoNaoUtilizada> questoesNaoUtilizadas(Usuario usuario) {
        if (usuario.isDocente()) {
            throw new RegraNegocioException(HttpStatus.FORBIDDEN, "Relatório disponível apenas para administradores.");
        }
        return relatorioDAO.questoesNaoUtilizadas();
    }

    public List<ComentarioAnonimo> comentariosAnonimos(Long idAvaliacao, Usuario usuario) {
        if (usuario.isDocente() && !avaliacaoDAO.pertenceAoDocente(idAvaliacao, usuario.idUsuario())) {
            throw new RegraNegocioException(HttpStatus.FORBIDDEN, "Você só pode ver os comentários das suas avaliações.");
        }
        return respostaDAO.listarComentariosAnonimos(idAvaliacao);
    }
}
