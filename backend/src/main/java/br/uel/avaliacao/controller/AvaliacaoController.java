package br.uel.avaliacao.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.uel.avaliacao.config.AuthInterceptor;
import br.uel.avaliacao.dto.CadastroDTOs.NovaAvaliacaoRequest;
import br.uel.avaliacao.dto.CadastroDTOs.StatusRequest;
import br.uel.avaliacao.model.Avaliacao;
import br.uel.avaliacao.model.Usuario;
import br.uel.avaliacao.service.AvaliacaoService;
import jakarta.validation.Valid;

/** RF2/RF3 — avaliações (administração) e RF5 — avaliações disponíveis para o aluno. */
@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @GetMapping
    public List<Avaliacao> listar() {
        return avaliacaoService.listarAvaliacoes();
    }

    /** Avaliações do aluno logado, com indicador {@code respondida}. */
    @GetMapping("/minhas")
    public List<Avaliacao> minhas(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        return avaliacaoService.listarAvaliacoesDoAluno(usuario);
    }

    @GetMapping("/{idAvaliacao}")
    public Avaliacao buscar(@PathVariable Long idAvaliacao,
                            @RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        return avaliacaoService.buscarAvaliacao(idAvaliacao, usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Avaliacao criar(@Valid @RequestBody NovaAvaliacaoRequest req) {
        return avaliacaoService.criarAvaliacao(req);
    }

    @PatchMapping("/{idAvaliacao}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarStatus(@PathVariable Long idAvaliacao, @Valid @RequestBody StatusRequest req) {
        avaliacaoService.alterarStatusAvaliacao(idAvaliacao, req.ativa());
    }
}
