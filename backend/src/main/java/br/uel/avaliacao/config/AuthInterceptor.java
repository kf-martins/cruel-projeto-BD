package br.uel.avaliacao.config;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import br.uel.avaliacao.dao.UsuarioDAO;
import br.uel.avaliacao.exception.RegraNegocioException;
import br.uel.avaliacao.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autenticação simplificada (seção 12 do edital): o front-end envia o cabeçalho {@code X-User-Id}
 * obtido no login. O perfil NÃO é lido do cabeçalho {@code X-User-Role}; ele é sempre consultado no
 * banco, para que um aluno não consiga se passar por administrador alterando o cabeçalho.
 *
 * <p>Regras de acesso:
 * <ul>
 *   <li>{@code POST /api/respostas} e {@code GET /api/avaliacoes/minhas}: somente ALUNO;</li>
 *   <li>{@code GET /api/avaliacoes/{id}}: qualquer perfil (o service verifica a matrícula do aluno);</li>
 *   <li>{@code GET /api/usuarios} e {@code GET /api/ofertas/{id}/alunos}: somente ADMIN (dados de
 *       alunos e de outros docentes só interessam à gestão das turmas);</li>
 *   <li>{@code GET /api/relatorios/pendentes} (lista nominal) e {@code /questoes-nao-utilizadas}:
 *       o service recusa o DOCENTE — ele só vê o total de pendentes, para preservar o anonimato;</li>
 *   <li>{@code /api/relatorios/**} e demais leituras: ADMIN ou DOCENTE — para o DOCENTE, os services
 *       restringem o resultado às ofertas que ele ministra;</li>
 *   <li>qualquer escrita (POST/PUT/PATCH/DELETE): somente ADMIN.</li>
 * </ul>
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_USUARIO = "usuarioLogado";
    private static final Set<String> GESTAO = Set.of(Usuario.ADMIN, Usuario.DOCENTE);

    private final UsuarioDAO usuarioDAO;

    public AuthInterceptor(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        Usuario usuario = resolverUsuario(request.getHeader("X-User-Id"));
        request.setAttribute(ATRIBUTO_USUARIO, usuario);

        String path = request.getRequestURI();
        String metodo = request.getMethod();

        if (path.startsWith("/api/respostas") || path.equals("/api/avaliacoes/minhas")) {
            exigir(usuario, Set.of(Usuario.ALUNO));
        } else if ("GET".equals(metodo) && path.matches("/api/avaliacoes/\\d+")) {
            return true;
        } else if ("GET".equals(metodo)
                && (path.startsWith("/api/usuarios") || path.matches("/api/ofertas/\\d+/alunos"))) {
            exigir(usuario, Set.of(Usuario.ADMIN));
        } else if ("GET".equals(metodo)) {
            exigir(usuario, GESTAO);
        } else {
            exigir(usuario, Set.of(Usuario.ADMIN));
        }
        return true;
    }

    private Usuario resolverUsuario(String cabecalho) {
        if (cabecalho == null || cabecalho.isBlank()) {
            throw new RegraNegocioException(HttpStatus.UNAUTHORIZED, "Faça login para continuar.");
        }
        try {
            return usuarioDAO.buscarPorId(Long.parseLong(cabecalho.trim()))
                    .orElseThrow(() -> new RegraNegocioException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
        } catch (NumberFormatException e) {
            throw new RegraNegocioException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
        }
    }

    private static void exigir(Usuario usuario, Set<String> perfis) {
        if (!perfis.contains(usuario.perfil())) {
            throw new RegraNegocioException(HttpStatus.FORBIDDEN, "Seu perfil não tem acesso a este recurso.");
        }
    }
}
