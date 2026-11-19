package br.com.fiap.boletimplus.dto;

public record SessaoResponse(String token, Long usuarioCod, PessoaResponse pessoa, Papeis papeis) {
}
