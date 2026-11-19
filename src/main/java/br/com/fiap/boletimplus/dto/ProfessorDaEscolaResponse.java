package br.com.fiap.boletimplus.dto;

import java.util.List;

public record ProfessorDaEscolaResponse(
        Long professorCod,
        String nome,
        List<String> materias,
        List<TurmaResumoResponse> turmas) {
}
