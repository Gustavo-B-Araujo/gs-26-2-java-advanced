package br.com.fiap.boletimplus.dto;

import java.time.LocalDate;

public record NotaDoAlunoResponse(
        Long avaliacaoCod,
        String avaliacaoNome,
        String tipoNota,
        LocalDate dataAvaliacao,
        Double nota) {
}
