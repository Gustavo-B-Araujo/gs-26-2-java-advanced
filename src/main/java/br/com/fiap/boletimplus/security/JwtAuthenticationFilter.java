package br.com.fiap.boletimplus.security;

import br.com.fiap.boletimplus.service.PapelService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lê `Authorization: Bearer <jwt>`, valida o token e carrega o usuário e os papéis do banco.
 * Token ausente ou inválido segue sem autenticação: a rota protegida responde 401.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final PapelService papelService;

    public JwtAuthenticationFilter(JwtService jwtService, PapelService papelService) {
        this.jwtService = jwtService;
        this.papelService = papelService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIXO)) {
            jwtService.validar(header.substring(PREFIXO.length()))
                    .flatMap(papelService::usuarioAtivo)
                    .ifPresent(usuario -> {
                        var autenticacao = new UsernamePasswordAuthenticationToken(usuario, null, usuario.authorities());
                        SecurityContextHolder.getContext().setAuthentication(autenticacao);
                    });
        }
        chain.doFilter(request, response);
    }
}
