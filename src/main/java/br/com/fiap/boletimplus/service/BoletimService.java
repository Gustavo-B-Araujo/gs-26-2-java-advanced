package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.dto.BoletimAlunoResponse;
import br.com.fiap.boletimplus.dto.BoletimTurmaResponse;
import br.com.fiap.boletimplus.dto.NotaDoAlunoResponse;
import br.com.fiap.boletimplus.dto.PresencaDoAlunoResponse;
import br.com.fiap.boletimplus.dto.TurmaResumoResponse;
import br.com.fiap.boletimplus.exception.ApiException;
import br.com.fiap.boletimplus.model.Aluno;
import br.com.fiap.boletimplus.model.Boletim;
import br.com.fiap.boletimplus.model.Turma;
import br.com.fiap.boletimplus.model.TurmaAluno;
import br.com.fiap.boletimplus.repository.AlunoRepository;
import br.com.fiap.boletimplus.repository.AvaliacaoRepository;
import br.com.fiap.boletimplus.repository.BoletimRepository;
import br.com.fiap.boletimplus.repository.ListaPresencaRepository;
import br.com.fiap.boletimplus.repository.TurmaAlunoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Monta o boletim do aluno: notas e média por turma, presenças e frequência. */
@Service
public class BoletimService {

    private final AlunoRepository alunos;
    private final TurmaAlunoRepository matriculas;
    private final AvaliacaoRepository avaliacoes;
    private final BoletimRepository notas;
    private final ListaPresencaRepository presencas;

    public BoletimService(AlunoRepository alunos, TurmaAlunoRepository matriculas, AvaliacaoRepository avaliacoes,
                          BoletimRepository notas, ListaPresencaRepository presencas) {
        this.alunos = alunos;
        this.matriculas = matriculas;
        this.avaliacoes = avaliacoes;
        this.notas = notas;
        this.presencas = presencas;
    }

    @Transactional(readOnly = true)
    public BoletimAlunoResponse montar(Long alunoCod) {
        Aluno aluno = alunos.findById(alunoCod)
                .orElseThrow(() -> ApiException.naoEncontrado("Aluno não encontrado."));
        List<BoletimTurmaResponse> turmas = matriculas.findByAlunoCod(alunoCod).stream()
                .map(this::boletimDaTurma)
                .sorted(Comparator.comparing((BoletimTurmaResponse b) -> b.turma().ano()).reversed()
                        .thenComparing(b -> b.turma().semestre(), Comparator.reverseOrder())
                        .thenComparing(b -> b.turma().cod()))
                .toList();
        return new BoletimAlunoResponse(aluno.getCod(), aluno.getPessoa().getNome(), turmas);
    }

    private BoletimTurmaResponse boletimDaTurma(TurmaAluno matricula) {
        Map<Long, Double> notaPorAvaliacao = notas.findByTurmaAlunoCod(matricula.getCod()).stream()
                .collect(Collectors.toMap(b -> b.getAvaliacao().getCod(), (Boletim b) -> b.getNota().doubleValue()));

        List<NotaDoAlunoResponse> notasDaTurma = avaliacoes.daTurma(matricula.getTurma().getCod()).stream()
                .map(a -> new NotaDoAlunoResponse(a.getCod(), a.getNome(), a.getTipoNota().getEnumerador(),
                        a.getDataAvaliacao(), notaPorAvaliacao.get(a.getCod())))
                .toList();

        List<PresencaDoAlunoResponse> presencasDaTurma = presencas
                .findByTurmaAlunoCodOrderByDataPresencaDesc(matricula.getCod()).stream()
                .map(p -> new PresencaDoAlunoResponse(p.getDataPresenca(), p.getTipoPresenca().getEnumerador(),
                        p.getObservacao()))
                .toList();

        return new BoletimTurmaResponse(
                resumo(matricula.getTurma()),
                notasDaTurma,
                media(notasDaTurma.stream().map(NotaDoAlunoResponse::nota).filter(Objects::nonNull).toList()),
                presencasDaTurma,
                frequencia(presencasDaTurma.stream().map(PresencaDoAlunoResponse::tipo).toList()));
    }

    public TurmaResumoResponse resumo(Turma t) {
        return new TurmaResumoResponse(
                t.getCod(),
                t.getEscola().getCod(),
                t.getEscola().getNome(),
                t.getTipoCurso().getEnumerador(),
                t.getProfessor().getCod(),
                t.getProfessor().getPessoa().getNome(),
                t.getSemestreReferencia(),
                t.getAnoReferencia(),
                matriculas.countByTurmaCod(t.getCod()));
    }

    /** Média simples das notas lançadas; null sem notas. */
    static Double media(List<Double> lancadas) {
        return lancadas.isEmpty() ? null : lancadas.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    /** Fração de 0 a 1. Só 'Presente' conta como presença; null sem chamadas. */
    static Double frequencia(List<String> tipos) {
        if (tipos.isEmpty()) return null;
        long presentes = tipos.stream().map(BoletimService::chave).filter("PRESENTE"::equals).count();
        return (double) presentes / tipos.size();
    }

    /** 'Falta Justificada' → 'FALTA_JUSTIFICADA'. O app compara os enumeradores pelo mesmo critério. */
    static String chave(String enumerador) {
        String semAcento = Normalizer.normalize(enumerador, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.trim().toUpperCase().replaceAll("\\s+", "_");
    }
}
