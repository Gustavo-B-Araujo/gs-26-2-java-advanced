package br.com.fiap.boletimplus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POST /professores e POST /alunos. No aluno, `senha` null = sem Usuario (não entra no app).
 * Validado no serviço, depois do papel, com as mesmas mensagens da API .NET.
 */
public record NovaPessoaRequest(
        String nome,
        @JsonProperty("num_documento") String numDocumento,
        String senha) {
}
