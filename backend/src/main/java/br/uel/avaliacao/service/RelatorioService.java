package br.uel.avaliacao.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

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

/** RF7 — relatórios analíticos com filtros opcionais de ano, semestre e disciplina. */
@Service
public class RelatorioService {

    private final RelatorioDAO relatorioDAO;
    private final RespostaDAO respostaDAO;
    private final Clock clock;

    public RelatorioService(RelatorioDAO relatorioDAO, RespostaDAO respostaDAO, Clock clock) {
        this.relatorioDAO = relatorioDAO;
        this.respostaDAO = respostaDAO;
        this.clock = clock;
    }

    public Filtro filtro(Integer ano, Integer semestre, Long idDisciplina) {
        if (semestre != null && semestre != 1 && semestre != 2) {
            throw RegraNegocioException.requisicaoInvalida("O semestre deve ser 1 ou 2.");
        }
        return new Filtro(ano, semestre, idDisciplina, LocalDate.now(clock));
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

    public List<AlunoPendente> alunosPendentes(Filtro f) {
        return relatorioDAO.alunosPendentes(f);
    }

    public List<QuestaoNaoUtilizada> questoesNaoUtilizadas() {
        return relatorioDAO.questoesNaoUtilizadas();
    }

    public List<ComentarioAnonimo> comentariosAnonimos(Long idAvaliacao) {
        return respostaDAO.listarComentariosAnonimos(idAvaliacao);
    }
}
