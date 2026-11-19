package br.com.fiap.boletimplus.dto;

import java.time.LocalDate;

/** POST /turmas/{cod}/avaliacoes e PUT /avaliacoes/{cod}. Data no formato YYYY-MM-DD, ou null. */
public record DadosAvaliacaoRequest(String nome, Long codTipoNota, LocalDate dataAvaliacao) {
}
