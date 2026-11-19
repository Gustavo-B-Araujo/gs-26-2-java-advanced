package br.com.fiap.boletimplus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VinculoRequest(
        @JsonProperty("cod_guardiao")
        @NotNull(message = "Informe o responsável.")
        @Positive(message = "Código do responsável inválido.")
        Long codGuardiao,

        @JsonProperty("cod_aluno")
        @NotNull(message = "Informe o aluno.")
        @Positive(message = "Código do aluno inválido.")
        Long codAluno,

        @JsonProperty("cod_tipo_parentesco")
        @NotNull(message = "Informe o parentesco.")
        @Positive(message = "Código do parentesco inválido.")
        Long codTipoParentesco) {
}
