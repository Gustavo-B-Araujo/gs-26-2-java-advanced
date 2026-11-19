package br.com.fiap.boletimplus.dto;

public record TurmaResumoResponse(
        Long cod,
        Long escolaCod,
        String escolaNome,
        String tipoCurso,
        Long professorCod,
        String professorNome,
        Integer semestre,
        Integer ano,
        long totalAlunos) {
}
