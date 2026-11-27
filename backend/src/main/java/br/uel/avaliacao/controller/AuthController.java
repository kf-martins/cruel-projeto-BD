package br.uel.avaliacao.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uel.avaliacao.dto.CadastroDTOs.LoginRequest;
import br.uel.avaliacao.model.Usuario;
import br.uel.avaliacao.service.CatalogoService;
import jakarta.validation.Valid;

/**
 * Login simplificado (seção 12 do edital). Devolve o usuário; o front-end guarda a sessão no
 * localStorage e envia {@code X-User-Id} nas próximas requisições.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CatalogoService catalogoService;

    public AuthController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @PostMapping("/login")
    public Usuario login(@Valid @RequestBody LoginRequest req) {
        return catalogoService.autenticar(req.login(), req.senha());
    }
}
