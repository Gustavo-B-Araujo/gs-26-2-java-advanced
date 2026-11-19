package br.com.fiap.boletimplus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Papéis da pessoa: cada campo é o `cod` na tabela do papel, ou null quando ela não tem o papel. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Papeis(Long diretorCod, Long professorCod, Long guardiaoCod, Long alunoCod) {
}
