package br.com.fiap.boletimplus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** POST /escolas/{cod}/turmas. */
public record NovaTurmaRequest(
        @JsonProperty("cod_tipo_curso") Long codTipoCurso,
        @JsonProperty("cod_professor") Long codProfessor,
        @JsonProperty("semestre_referencia") Integer semestreReferencia,
        @JsonProperty("ano_referencia") Integer anoReferencia) {
}
