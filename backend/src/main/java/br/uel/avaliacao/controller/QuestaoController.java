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
import br.uel.avaliacao.dto.CadastroDTOs.NovaQuestaoRequest;
import br.uel.avaliacao.dto.CadastroDTOs.StatusRequest;
import br.uel.avaliacao.model.Questao;
import br.uel.avaliacao.model.Usuario;
import br.uel.avaliacao.service.AvaliacaoService;
import jakarta.validation.Valid;

/** RF1 — banco de questões. */
@RestController
@RequestMapping("/api/questoes")
public class QuestaoController {

    private final AvaliacaoService avaliacaoService;

    public QuestaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @GetMapping
    public List<Questao> listar(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        return avaliacaoService.listarQuestoes(usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Questao criar(@Valid @RequestBody NovaQuestaoRequest req) {
        return avaliacaoService.criarQuestao(req);
    }

    @PatchMapping("/{idQuestao}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarStatus(@PathVariable Long idQuestao, @Valid @RequestBody StatusRequest req) {
        avaliacaoService.alterarStatusQuestao(idQuestao, req.ativa());
    }
}
