package br.uel.avaliacao.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.uel.avaliacao.config.AuthInterceptor;
import br.uel.avaliacao.dto.RelatorioDTOs.AdesaoAvaliacao;
import br.uel.avaliacao.dto.RelatorioDTOs.AlunoPendente;
import br.uel.avaliacao.dto.RelatorioDTOs.ComentarioAnonimo;
import br.uel.avaliacao.dto.RelatorioDTOs.ComparativoCategoria;
import br.uel.avaliacao.dto.RelatorioDTOs.MediaDisciplina;
import br.uel.avaliacao.dto.RelatorioDTOs.QuestaoNaoUtilizada;
import br.uel.avaliacao.dto.RelatorioDTOs.RankingQuestao;
import br.uel.avaliacao.model.Usuario;
import br.uel.avaliacao.service.RelatorioService;

/**
 * RF7 — relatórios. Todos aceitam os filtros opcionais {@code ano}, {@code semestre} e {@code idDisciplina};
 * para o DOCENTE, o service acrescenta o filtro pelas ofertas do próprio docente.
 */
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/media-disciplinas")
    public List<MediaDisciplina> mediaDisciplinas(@RequestParam(required = false) Integer ano,
                                                  @RequestParam(required = false) Integer semestre,
                                                  @RequestParam(required = false) Long idDisciplina,
                                                  @RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        return relatorioService.mediaPorDisciplina(relatorioService.filtro(ano, semestre, idDisciplina, usuario));
    }

    @GetMapping("/comparativo-categorias")
    public List<ComparativoCategoria> comparativo(@RequestParam(required = false) Integer ano,
                                                  @RequestParam(required = false) Integer semestre,
                                                  @RequestParam(required = false) Long idDisciplina,
                                                  @RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        return relatorioService.comparativoPorCategoria(relatorioService.filtro(ano, semestre, idDisciplina, usuario));
    }

    @GetMapping("/ranking-questoes")
    public List<RankingQuestao> ranking(@RequestParam(defaultValue = "melhores") String ordem,
                                        @RequestParam(required = false) Integer ano,
                                        @RequestParam(required = false) Integer semestre,
                                        @RequestParam(required = false) Long idDisciplina,
                                        @RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        return relatorioService.rankingQuestoes(relatorioService.filtro(ano, semestre, idDisciplina, usuario), ordem);
    }

    @GetMapping("/adesao")
    public List<AdesaoAvaliacao> adesao(@RequestParam(required = false) Integer ano,
                                        @RequestParam(required = false) Integer semestre,
                                        @RequestParam(required = false) Long idDisciplina,
                                        @RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        return relatorioService.adesaoPorAvaliacao(relatorioService.filtro(ano, semestre, idDisciplina, usuario));
    }

    @GetMapping("/pendentes")
    public List<AlunoPendente> pendentes(@RequestParam(required = false) Integer ano,
                                         @RequestParam(required = false) Integer semestre,
                                         @RequestParam(required = false) Long idDisciplina,
                                         @RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        return relatorioService.alunosPendentes(relatorioService.filtro(ano, semestre, idDisciplina, usuario), usuario);
    }

    @GetMapping("/questoes-nao-utilizadas")
    public List<QuestaoNaoUtilizada> questoesNaoUtilizadas(
            @RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        return relatorioService.questoesNaoUtilizadas(usuario);
    }

    @GetMapping("/comentarios/{idAvaliacao}")
    public List<ComentarioAnonimo> comentarios(@PathVariable Long idAvaliacao,
                                               @RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        return relatorioService.comentariosAnonimos(idAvaliacao, usuario);
    }
}
