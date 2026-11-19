package br.com.fiap.boletimplus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AlterarNomeRequest(
        @NotBlank(message = Regras.MSG_NOME)
        @Size(max = 150, message = Regras.MSG_NOME_TAMANHO)
        @Pattern(regexp = Regras.NOME_E_SOBRENOME, message = Regras.MSG_NOME)
        String nome) {
}
