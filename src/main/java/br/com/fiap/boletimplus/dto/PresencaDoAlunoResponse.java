package br.com.fiap.boletimplus.dto;

import java.time.LocalDate;

public record PresencaDoAlunoResponse(LocalDate data, String tipo, String observacao) {
}
