package br.com.fiap.boletimplus.dto;

import java.time.LocalDate;

public record AvaliacaoResumoResponse(
        Long cod,
        String nome,
        Long codTipoNota,
        String tipoNota,
        LocalDate dataAvaliacao,
        long notasLancadas) {
}
