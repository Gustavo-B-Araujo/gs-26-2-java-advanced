package br.com.fiap.boletimplus.dto;

/** `totalTurmas`: matrículas na rede inteira (0 = sem turma). */
public record AlunoResumoResponse(Long alunoCod, String nome, String numDocumento, long totalTurmas) {
}
