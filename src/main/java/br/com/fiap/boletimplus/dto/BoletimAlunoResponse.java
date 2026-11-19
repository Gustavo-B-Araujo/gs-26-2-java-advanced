package br.com.fiap.boletimplus.dto;

import java.util.List;

public record BoletimAlunoResponse(Long alunoCod, String nome, List<BoletimTurmaResponse> turmas) {
}
