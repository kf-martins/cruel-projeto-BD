package br.uel.avaliacao.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.uel.avaliacao.dto.CadastroDTOs.MatriculaRequest;
import br.uel.avaliacao.dto.CadastroDTOs.NovaOfertaRequest;
import br.uel.avaliacao.model.Disciplina;
import br.uel.avaliacao.model.OfertaDisciplina;
import br.uel.avaliacao.model.Usuario;
import br.uel.avaliacao.service.AvaliacaoService;
import br.uel.avaliacao.service.CatalogoService;
import jakarta.validation.Valid;

/** Catálogo: disciplinas, ofertas, turmas (RF4) e usuários por perfil. */
@RestController
@RequestMapping("/api")
public class DisciplinaController {

    private final CatalogoService catalogoService;
    private final AvaliacaoService avaliacaoService;

    public DisciplinaController(CatalogoService catalogoService, AvaliacaoService avaliacaoService) {
        this.catalogoService = catalogoService;
        this.avaliacaoService = avaliacaoService;
    }

    @GetMapping("/disciplinas")
    public List<Disciplina> disciplinas() {
        return catalogoService.listarDisciplinas();
    }

    @GetMapping("/ofertas")
    public List<OfertaDisciplina> ofertas() {
        return catalogoService.listarOfertas();
    }

    @PostMapping("/ofertas")
    @ResponseStatus(HttpStatus.CREATED)
    public OfertaDisciplina criarOferta(@Valid @RequestBody NovaOfertaRequest req) {
        return avaliacaoService.criarOferta(req);
    }

    @GetMapping("/ofertas/{idOferta}/alunos")
    public List<Usuario> alunosDaOferta(@PathVariable Long idOferta) {
        return catalogoService.listarAlunosDaOferta(idOferta);
    }

    /** RF4 — vincula alunos à oferta (turma). */
    @PostMapping("/ofertas/{idOferta}/alunos")
    public Map<String, Object> matricular(@PathVariable Long idOferta, @Valid @RequestBody MatriculaRequest req) {
        int novas = avaliacaoService.matricularAlunos(idOferta, req.idsAlunos());
        return Map.of("novasMatriculas", novas);
    }

    @GetMapping("/usuarios")
    public List<Usuario> usuarios(@RequestParam String perfil) {
        return catalogoService.listarUsuarios(perfil);
    }
}
