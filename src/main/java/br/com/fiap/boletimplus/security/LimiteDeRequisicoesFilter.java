package br.com.fiap.boletimplus.security;

import br.com.fiap.boletimplus.dto.ErroResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Rate limit em janela fixa, por cliente (usuário do token, ou IP sem token), como na API .NET:
 * <ul>
 * <li>global: `boletim.rate-limit.limite-por-janela` requisições por janela;</li>
 * <li>escrita: limite menor nos POST que criam recurso (professor, aluno, turma, avaliação).</li>
 * </ul>
 * Passou do limite: 429 com o header Retry-After em segundos (o app mostra "Tente de novo em N s").
 */
@Component
public class LimiteDeRequisicoesFilter extends OncePerRequestFilter {

    private static final Pattern ROTA_DE_ESCRITA =
            Pattern.compile("^/(professores|alunos|escolas/\\d+/turmas|turmas/\\d+/avaliacoes)/?$");

    private final JsonMapper jsonMapper;
    private final Janela global;
    private final Janela escrita;

    public LimiteDeRequisicoesFilter(
            JsonMapper jsonMapper,
            @Value("${boletim.rate-limit.limite-por-janela:100}") int limite,
            @Value("${boletim.rate-limit.janela:60s}") Duration janela,
            @Value("${boletim.rate-limit.limite-escrita:20}") int limiteEscrita,
            @Value("${boletim.rate-limit.janela-escrita:60s}") Duration janelaEscrita) {
        this.jsonMapper = jsonMapper;
        this.global = new Janela(limite, janela);
        this.escrita = new Janela(limiteEscrita, janelaEscrita);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cliente = cliente(request);
        Instant agora = Instant.now();
        OptionalLong espera = global.tentar(cliente, agora);
        if (espera.isEmpty() && "POST".equals(request.getMethod())
                && ROTA_DE_ESCRITA.matcher(request.getRequestURI()).matches()) {
            espera = escrita.tentar(cliente, agora);
        }
        if (espera.isPresent()) {
            recusar(request, response, espera.getAsLong());
            return;
        }
        chain.doFilter(request, response);
    }

    private static String cliente(HttpServletRequest request) {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioAutenticado u) {
            return "usuario:" + u.usuarioCod();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private void recusar(HttpServletRequest req, HttpServletResponse res, long segundos) throws IOException {
        var status = HttpStatus.TOO_MANY_REQUESTS;
        var corpo = new ErroResponse(status.value(), status.getReasonPhrase(),
                "Limite de requisições atingido. Tente de novo em " + segundos + " s.", req.getRequestURI(),
                Instant.now(), List.of());
        res.setStatus(status.value());
        res.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(segundos));
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(res.getOutputStream(), corpo);
    }

    /** Janela fixa por cliente. Em memória: cada instância da API conta separado. */
    static final class Janela {

        private final int limite;
        private final Duration duracao;
        private final Map<String, Contagem> contagens = new ConcurrentHashMap<>();

        Janela(int limite, Duration duracao) {
            this.limite = limite;
            this.duracao = duracao;
        }

        /** Vazio se a requisição passa; senão, os segundos até a janela virar (Retry-After). */
        OptionalLong tentar(String cliente, Instant agora) {
            if (contagens.size() > 10_000) contagens.values().removeIf(c -> !agora.isBefore(c.fim()));
            Contagem c = contagens.compute(cliente, (k, atual) -> atual == null || !agora.isBefore(atual.fim())
                    ? new Contagem(agora.plus(duracao), 1)
                    : new Contagem(atual.fim(), atual.total() + 1));
            if (c.total() <= limite) return OptionalLong.empty();
            long segundos = Math.max(1, (long) Math.ceil(Duration.between(agora, c.fim()).toMillis() / 1000.0));
            return OptionalLong.of(segundos);
        }

        private record Contagem(Instant fim, int total) {
        }
    }
}
