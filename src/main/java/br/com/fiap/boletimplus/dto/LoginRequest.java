package br.com.fiap.boletimplus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @JsonProperty("num_documento")
        @NotBlank(message = "Informe o CPF.")
        @Pattern(regexp = Regras.CPF, message = Regras.MSG_CPF)
        String numDocumento,

        @NotBlank(message = "Informe a senha.")
        String senha) {
}
