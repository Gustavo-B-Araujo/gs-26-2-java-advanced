package br.com.fiap.boletimplus.dto;

/** `nota` null: ainda sem nota lançada. */
public record NotaLancadaResponse(Long turmaAlunoCod, String alunoNome, Double nota) {
}
