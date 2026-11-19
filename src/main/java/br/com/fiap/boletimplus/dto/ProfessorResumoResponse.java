package br.com.fiap.boletimplus.dto;

import java.util.List;

/** `materias`: TipoCurso das turmas do professor em toda a rede, sem repetir. */
public record ProfessorResumoResponse(Long professorCod, String nome, List<String> materias) {
}
