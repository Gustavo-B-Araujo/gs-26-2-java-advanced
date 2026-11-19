package br.com.fiap.boletimplus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cadastro de responsável pelo diretor. Se o CPF já é de uma Pessoa, ela ganha o papel de guardião.
 * Sem `senha`, não cria Usuario: o responsável cria o acesso depois.
 */
public record GuardiaoRequest(
        @NotBlank(message = Regras.MSG_NOME)
        @Size(max = 150, message = Regras.MSG_NOME_TAMANHO)
        @Pattern(regexp = Regras.NOME_E_SOBRENOME, message = Regras.MSG_NOME)
        String nome,

        @JsonProperty("num_documento")
        @NotBlank(message = "Informe o CPF.")
        @Pattern(regexp = Regras.CPF, message = Regras.MSG_CPF)
        String numDocumento,

        @Size(min = 8, message = Regras.MSG_SENHA_TAMANHO)
        @Pattern(regexp = Regras.LETRAS_E_NUMEROS, message = Regras.MSG_SENHA_REGRA)
        String senha) {
}
