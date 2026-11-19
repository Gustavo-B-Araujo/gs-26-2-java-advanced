package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.dto.AlunoDaTurmaResponse;
import br.com.fiap.boletimplus.dto.AvaliacaoResumoResponse;
import br.com.fiap.boletimplus.dto.Papeis;
import br.com.fiap.boletimplus.dto.TurmaResumoResponse;
import br.com.fiap.boletimplus.exception.ApiException;
import br.com.fiap.boletimplus.model.Turma;
import br.com.fiap.boletimplus.repository.AvaliacaoRepository;
import br.com.fiap.boletimplus.repository.BoletimRepository;
import br.com.fiap.boletimplus.repository.EscolaRepository;
import br.com.fiap.boletimplus.repository.TurmaAlunoRepository;
import br.com.fiap.boletimplus.repository.TurmaRepository;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Projeções e regras de acesso por recurso usadas por mais de um serviço (escola, turma, avaliação). */
@Service
@Transactional(readOnly = true)
public class ConsultaService {

    private final TurmaRepository turmas;
    private final TurmaAlunoRepository matriculas;
    private final AvaliacaoRepository avaliacoes;
    private final BoletimRepository notas;
    private final EscolaRepository escolas;
    private final BoletimService boletimService;

    public ConsultaService(TurmaRepository turmas, TurmaAlunoRepository matriculas, AvaliacaoRepository avaliacoes,
                           BoletimRepository notas, EscolaRepository escolas, BoletimService boletimService) {
        this.turmas = turmas;
        this.matriculas = matriculas;
        this.avaliacoes = avaliacoes;
        this.notas = notas;
        this.escolas = escolas;
        this.boletimService = boletimService;
    }

    public TurmaResumoResponse resumo(Turma turma) {
        return boletimService.resumo(turma);
    }

    public List<TurmaResumoResponse> resumos(List<Turma> lista) {
        return lista.stream().map(this::resumo).toList();
    }

    /** Matérias (TipoCurso) das turmas de cada professor, em toda a rede, sem repetir e em ordem. */
    public Map<Long, List<String>> materiasPorProfessor() {
        Map<Long, List<String>> porProfessor = new TreeMap<>();
        for (Object[] par : turmas.materiasPorProfessor()) {
            porProfessor.computeIfAbsent((Long) par[0], k -> new ArrayList<>()).add((String) par[1]);
        }
        porProfessor.values().forEach(m -> m.sort(RegrasDeNegocio.ORDEM_NOME));
        return porProfessor;
    }

    /** Total de matrículas de cada aluno na rede. */
    public Map<Long, Long> totalTurmasPorAluno() {
        return matriculas.totalPorAluno().stream()
                .collect(Collectors.toMap(par -> (Long) par[0], par -> (Long) par[1]));
    }

    /** Alunos da turma em ordem alfabética. */
    public List<AlunoDaTurmaResponse> alunosDaTurma(Long turmaCod) {
        return matriculas.findByTurmaCod(turmaCod).stream()
                .map(ta -> new AlunoDaTurmaResponse(ta.getCod(), ta.getAluno().getCod(),
                        ta.getAluno().getPessoa().getNome()))
                .sorted(Comparator.comparing(AlunoDaTurmaResponse::nome, RegrasDeNegocio.ORDEM_NOME))
                .toList();
    }

    /** Avaliações da turma pela data; sem data vão para o fim. */
    public List<AvaliacaoResumoResponse> avaliacoesDaTurma(Long turmaCod) {
        return avaliacoes.daTurma(turmaCod).stream()
                .map(a -> new AvaliacaoResumoResponse(a.getCod(), a.getNome(), a.getTipoNota().getCod(),
                        a.getTipoNota().getEnumerador(), a.getDataAvaliacao(), notas.countByAvaliacaoCod(a.getCod())))
                .toList();
    }

    public Turma buscarTurma(Long turmaCod) {
        return turmas.findById(turmaCod).orElseThrow(() -> ApiException.naoEncontrado("Turma não encontrada."));
    }

    // --- Acesso por recurso ------------------------------------------------

    public boolean ehDiretorDaEscola(Papeis p, Long escolaCod) {
        return p.diretorCod() != null && escolas.existsByCodAndDiretorCod(escolaCod, p.diretorCod());
    }

    public void exigirDiretorDaEscola(UsuarioAutenticado quem, Long escolaCod) {
        if (!escolas.existsById(escolaCod)) throw ApiException.naoEncontrado("Escola não encontrada.");
        if (!ehDiretorDaEscola(quem.papeis(), escolaCod)) {
            throw ApiException.proibido("Só o diretor desta escola tem acesso.");
        }
    }

    /** Professor da turma, diretor da escola, aluno matriculado ou guardião de um matriculado. */
    public Turma exigirLeituraDaTurma(UsuarioAutenticado quem, Long turmaCod) {
        Turma turma = buscarTurma(turmaCod);
        Papeis p = quem.papeis();
        if (turma.getProfessor().getCod().equals(p.professorCod())) return turma;
        if (ehDiretorDaEscola(p, turma.getEscola().getCod())) return turma;
        if (p.alunoCod() != null && matriculas.existsByTurmaCodAndAlunoCod(turmaCod, p.alunoCod())) return turma;
        if (p.guardiaoCod() != null && matriculas.turmaTemDependenteDo(turmaCod, p.guardiaoCod())) return turma;
        throw ApiException.proibido("Você não tem acesso a esta turma.");
    }

    /** Escrita (chamada, notas, avaliações): só o professor da turma (Turma.cod_professor). */
    public Turma exigirProfessorDaTurma(UsuarioAutenticado quem, Long turmaCod) {
        Turma turma = buscarTurma(turmaCod);
        Long professorCod = quem.papeis().professorCod();
        if (professorCod == null || !turma.getProfessor().getCod().equals(professorCod)) {
            throw ApiException.proibido("Só o professor da turma pode lançar dados.");
        }
        return turma;
    }
}
