package br.com.fiap.boletimplus.messaging;

/**
 * Evento publicado quando a análise encontra uma matéria em atenção:
 * média abaixo de 60 ou frequência abaixo de 75%.
 */
public record AlertaDesempenhoEvent(
        Long alunoCod,
        String alunoNome,
        Long turmaCod,
        String materia,
        Double media,
        Double frequencia,
        String motivo) {
}
