package br.com.fiap.boletimplus.dto;

import br.com.fiap.boletimplus.model.Enumerador;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record EnumeradorResponse(
        Long cod,
        String enumerador,
        @JsonProperty("data_criacao") LocalDateTime dataCriacao) {

    public static EnumeradorResponse de(Enumerador e) {
        return new EnumeradorResponse(e.getCod(), e.getEnumerador(), e.getDataCriacao());
    }
}
