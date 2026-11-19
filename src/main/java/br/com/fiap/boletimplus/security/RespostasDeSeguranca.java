package br.com.fiap.boletimplus.security;

import br.com.fiap.boletimplus.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/** Respostas 401 e 403 do filtro de segurança, no mesmo formato do GlobalExceptionHandler. */
@Component
public class RespostasDeSeguranca {

    private final JsonMapper jsonMapper;

    public RespostasDeSeguranca(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public AuthenticationEntryPoint naoAutenticado() {
        return (req, res, e) -> escrever(req, res, HttpStatus.UNAUTHORIZED, "Sessão expirada. Entre de novo.");
    }

    public AccessDeniedHandler acessoNegado() {
        return (req, res, e) -> escrever(req, res, HttpStatus.FORBIDDEN, "Você não tem permissão para esta ação.");
    }

    private void escrever(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String mensagem)
            throws IOException {
        var corpo = new ErroResponse(status.value(), status.getReasonPhrase(), mensagem, req.getRequestURI(),
                Instant.now(), List.of());
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(res.getOutputStream(), corpo);
    }
}
