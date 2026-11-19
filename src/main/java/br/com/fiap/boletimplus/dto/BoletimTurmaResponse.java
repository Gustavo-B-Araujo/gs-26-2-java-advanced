package br.com.fiap.boletimplus.dto;

import java.util.List;

/** `frequencia` é uma fração de 0 a 1. `media` e `frequencia` são null sem registros. */
public record BoletimTurmaResponse(
        TurmaResumoResponse turma,
        List<NotaDoAlunoResponse> notas,
        Double media,
        List<PresencaDoAlunoResponse> presencas,
        Double frequencia) {
}
