package br.com.fiap.boletimplus.dto;

import java.util.List;

/** TurmaResumo com alunos e avaliações, tudo no mesmo nível do JSON (como no app). */
public record TurmaDetalheResponse(
        Long cod,
        Long escolaCod,
        String escolaNome,
        String tipoCurso,
        Long professorCod,
        String professorNome,
        Integer semestre,
        Integer ano,
        long totalAlunos,
        List<AlunoDaTurmaResponse> alunos,
        List<AvaliacaoResumoResponse> avaliacoes) {

    public static TurmaDetalheResponse de(TurmaResumoResponse t, List<AlunoDaTurmaResponse> alunos,
                                          List<AvaliacaoResumoResponse> avaliacoes) {
        return new TurmaDetalheResponse(t.cod(), t.escolaCod(), t.escolaNome(), t.tipoCurso(), t.professorCod(),
                t.professorNome(), t.semestre(), t.ano(), t.totalAlunos(), alunos, avaliacoes);
    }
}
