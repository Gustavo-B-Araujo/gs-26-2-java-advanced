package br.com.fiap.boletimplus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequest(
        @JsonProperty("senha_atual")
        @NotBlank(message = "Informe a senha atual.")
        String senhaAtual,

        @JsonProperty("nova_senha")
        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, message = Regras.MSG_SENHA_TAMANHO)
        @Pattern(regexp = Regras.LETRAS_E_NUMEROS, message = Regras.MSG_SENHA_REGRA)
        String novaSenha) {
}
