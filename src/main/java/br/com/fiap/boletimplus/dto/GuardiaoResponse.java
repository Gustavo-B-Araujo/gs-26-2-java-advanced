package br.com.fiap.boletimplus.dto;

public record GuardiaoResponse(
        Long guardiaoCod,
        Long pessoaCod,
        String nome,
        String numDocumento,
        long totalDependentes) {
}
