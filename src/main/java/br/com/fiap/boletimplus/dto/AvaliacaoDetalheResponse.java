package br.com.fiap.boletimplus.dto;

import java.time.LocalDate;
import java.util.List;

/** AvaliacaoResumo com a turma e a nota de cada aluno. */
public record AvaliacaoDetalheResponse(
        Long cod,
        String nome,
        Long codTipoNota,
        String tipoNota,
        LocalDate dataAvaliacao,
        long notasLancadas,
        TurmaResumoResponse turma,
        List<NotaLancadaResponse> notas) {
}
