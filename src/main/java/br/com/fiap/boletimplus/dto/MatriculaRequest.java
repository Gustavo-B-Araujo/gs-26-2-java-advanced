package br.com.fiap.boletimplus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** POST /turmas/{cod}/alunos. */
public record MatriculaRequest(@JsonProperty("cod_alunos") List<Long> codAlunos) {
}
