package br.uel.avaliacao.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import br.uel.avaliacao.dao.DisciplinaDAO;
import br.uel.avaliacao.dao.UsuarioDAO;
import br.uel.avaliacao.exception.RegraNegocioException;
import br.uel.avaliacao.model.Disciplina;
import br.uel.avaliacao.model.OfertaDisciplina;
import br.uel.avaliacao.model.Usuario;

/** Consultas de apoio (login, usuários, disciplinas e ofertas) — mantém os controllers sem acesso direto a DAOs. */
@Service
public class CatalogoService {

    private final UsuarioDAO usuarioDAO;
    private final DisciplinaDAO disciplinaDAO;

    public CatalogoService(UsuarioDAO usuarioDAO, DisciplinaDAO disciplinaDAO) {
        this.usuarioDAO = usuarioDAO;
        this.disciplinaDAO = disciplinaDAO;
    }

    public Usuario autenticar(String login, String senha) {
        return usuarioDAO.autenticar(login, senha)
                .orElseThrow(() -> new RegraNegocioException(HttpStatus.UNAUTHORIZED, "E-mail/matrícula ou senha inválidos."));
    }

    public List<Usuario> listarUsuarios(String perfil) {
        String p = perfil.trim().toUpperCase(Locale.ROOT);
        if (!List.of(Usuario.ADMIN, Usuario.DOCENTE, Usuario.ALUNO).contains(p)) {
            throw RegraNegocioException.requisicaoInvalida("Perfil inválido: " + perfil);
        }
        return usuarioDAO.listarPorPerfil(p);
    }

    public List<Disciplina> listarDisciplinas() {
        return disciplinaDAO.listarDisciplinas();
    }

    public List<OfertaDisciplina> listarOfertas() {
        return disciplinaDAO.listarOfertas();
    }

    public List<Usuario> listarAlunosDaOferta(Long idOferta) {
        if (disciplinaDAO.buscarOfertaPorId(idOferta).isEmpty()) {
            throw RegraNegocioException.naoEncontrado("Oferta não encontrada.");
        }
        return usuarioDAO.listarAlunosPorOferta(idOferta);
    }
}
