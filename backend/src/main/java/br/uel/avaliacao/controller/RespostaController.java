package br.uel.avaliacao.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.uel.avaliacao.config.AuthInterceptor;
import br.uel.avaliacao.dto.SubmissaoRequestDTO;
import br.uel.avaliacao.model.Usuario;
import br.uel.avaliacao.service.RespostaService;
import jakarta.validation.Valid;

/** RF5/RF6 — envio anônimo das respostas. A resposta HTTP não devolve nenhum identificador da submissão. */
@RestController
@RequestMapping("/api/respostas")
public class RespostaController {

    private final RespostaService respostaService;

    public RespostaController(RespostaService respostaService) {
        this.respostaService = respostaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> responder(@Valid @RequestBody SubmissaoRequestDTO req,
                                         @RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        respostaService.registrarSubmissaoAnonima(req.idAvaliacao(), usuario, req.itens());
        return Map.of("mensagem", "Respostas registradas de forma anônima. Obrigado por avaliar!");
    }
}
