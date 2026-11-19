package br.com.fiap.boletimplus.dto;

import java.time.Instant;

public record AnaliseBoletimResponse(String texto, Instant geradoEm) {
}
