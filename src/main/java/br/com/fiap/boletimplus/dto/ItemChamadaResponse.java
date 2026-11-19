package br.com.fiap.boletimplus.dto;

/** Aluno da turma com a presença do dia; `codTipoPresenca` null = chamada ainda não lançada. */
public record ItemChamadaResponse(Long turmaAlunoCod, String alunoNome, Long codTipoPresenca, String observacao) {
}
