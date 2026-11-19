package br.com.fiap.boletimplus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AlterarVinculoRequest(
        @JsonProperty("cod_tipo_parentesco")
        @NotNull(message = "Informe o parentesco.")
        @Positive(message = "Código do parentesco inválido.")
        Long codTipoParentesco) {
}
