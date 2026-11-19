package br.com.fiap.boletimplus.exception;

import br.com.fiap.boletimplus.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErroResponse> regraDeNegocio(ApiException e, HttpServletRequest req) {
        return resposta(e.getStatus(), e.getMessage(), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<ErroResponse.CampoInvalido> campos = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErroResponse.CampoInvalido(f.getField(), f.getDefaultMessage()))
                .toList();
        String mensagem = campos.isEmpty() ? "Dados inválidos." : campos.getFirst().mensagem();
        return resposta(HttpStatus.BAD_REQUEST, mensagem, req, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido.", req, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + e.getName() + ".", req, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> parametroAusente(MissingServletRequestParameterException e,
                                                         HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Informe o parâmetro " + e.getParameterName() + ".", req, List.of());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErroResponse> headerAusente(MissingRequestHeaderException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Envie o header " + e.getHeaderName() + ".", req, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException e, HttpServletRequest req) {
        return resposta(HttpStatus.FORBIDDEN, "Você não tem permissão para esta ação.", req, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException e, HttpServletRequest req) {
        log.warn("Violação de integridade em {}: {}", req.getRequestURI(), e.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "O registro conflita com dados existentes.", req, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException e, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, "Rota não encontrada.", req, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodo(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método não suportado nesta rota.", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception e, HttpServletRequest req) {
        log.error("Erro inesperado em {}", req.getRequestURI(), e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente de novo.", req, List.of());
    }

    static ErroResponse corpo(HttpStatus status, String mensagem, String caminho, List<ErroResponse.CampoInvalido> campos) {
        return new ErroResponse(status.value(), status.getReasonPhrase(), mensagem, caminho, Instant.now(), campos);
    }

    private ResponseEntity<ErroResponse> resposta(
            HttpStatus status, String mensagem, HttpServletRequest req, List<ErroResponse.CampoInvalido> campos) {
        return ResponseEntity.status(status).body(corpo(status, mensagem, req.getRequestURI(), campos));
    }
}
