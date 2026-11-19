package br.com.fiap.boletimplus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Corpo padrão de erro. O app mostra `mensagem` na tela. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErroResponse(
        int status,
        String erro,
        String mensagem,
        String caminho,
        Instant timestamp,
        List<CampoInvalido> campos) {

    public record CampoInvalido(String campo, String mensagem) {
    }
}
