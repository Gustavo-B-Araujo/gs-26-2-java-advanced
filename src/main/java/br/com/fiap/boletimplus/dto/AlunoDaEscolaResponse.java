package br.com.fiap.boletimplus.dto;

import java.util.List;

public record AlunoDaEscolaResponse(
        Long alunoCod,
        String nome,
        String numDocumento,
        long totalTurmas,
        List<TurmaResumoResponse> turmas) {
}
