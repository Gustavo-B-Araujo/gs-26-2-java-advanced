package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.dto.AlunoDaEscolaResponse;
import br.com.fiap.boletimplus.dto.EscolaResumoResponse;
import br.com.fiap.boletimplus.dto.MatriculaRequest;
import br.com.fiap.boletimplus.dto.NovaTurmaRequest;
import br.com.fiap.boletimplus.dto.ProfessorDaEscolaResponse;
import br.com.fiap.boletimplus.dto.TurmaResumoResponse;
import br.com.fiap.boletimplus.exception.ApiException;
import br.com.fiap.boletimplus.model.Aluno;
import br.com.fiap.boletimplus.model.Escola;
import br.com.fiap.boletimplus.model.Professor;
import br.com.fiap.boletimplus.model.TipoCurso;
import br.com.fiap.boletimplus.model.Turma;
import br.com.fiap.boletimplus.model.TurmaAluno;
import br.com.fiap.boletimplus.repository.AlunoRepository;
import br.com.fiap.boletimplus.repository.EscolaRepository;
import br.com.fiap.boletimplus.repository.ProfessorRepository;
import br.com.fiap.boletimplus.repository.TipoCursoRepository;
import br.com.fiap.boletimplus.repository.TurmaAlunoRepository;
import br.com.fiap.boletimplus.repository.TurmaRepository;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Gestão escolar: escolas do diretor, turmas, professores e alunos da escola, matrícula. */
@Service
public class EscolaService {

    private final EscolaRepository escolas;
    private final TurmaRepository turmas;
    private final TurmaAlunoRepository matriculas;
    private final TipoCursoRepository cursos;
    private final ProfessorRepository professores;
    private final AlunoRepository alunos;
    private final ConsultaService consultas;

    public EscolaService(EscolaRepository escolas, TurmaRepository turmas, TurmaAlunoRepository matriculas,
                         TipoCursoRepository cursos, ProfessorRepository professores, AlunoRepository alunos,
                         ConsultaService consultas) {
        this.escolas = escolas;
        this.turmas = turmas;
        this.matriculas = matriculas;
        this.cursos = cursos;
        this.professores = professores;
        this.alunos = alunos;
        this.consultas = consultas;
    }

    /** Só o próprio diretor. */
    @Transactional(readOnly = true)
    public List<EscolaResumoResponse> escolasDoDiretor(UsuarioAutenticado quem, Long diretorCod) {
        if (!diretorCod.equals(quem.papeis().diretorCod())) throw ApiException.proibido("Acesso negado.");
        return escolas.findByDiretorCodOrderByNome(diretorCod).stream().map(this::resumo).toList();
    }

    @Transactional(readOnly = true)
    public EscolaResumoResponse obter(UsuarioAutenticado quem, Long escolaCod) {
        consultas.exigirDiretorDaEscola(quem, escolaCod);
        return resumo(escolas.findById(escolaCod).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<TurmaResumoResponse> turmas(UsuarioAutenticado quem, Long escolaCod) {
        consultas.exigirDiretorDaEscola(quem, escolaCod);
        return consultas.resumos(turmas.daEscola(escolaCod));
    }

    /**
     * Só o diretor da escola. O professor é obrigatório (cod_professor NOT NULL) e pode ser
     * qualquer um da rede. Valida os CHECKs de semestre e ano antes do banco.
     */
    @Transactional
    public Long criarTurma(UsuarioAutenticado quem, Long escolaCod, NovaTurmaRequest dados) {
        consultas.exigirDiretorDaEscola(quem, escolaCod);
        RegrasDeNegocio.exigir(
                dados.codTipoCurso() == null ? "Informe o curso (cod_tipo_curso)." : null,
                dados.codProfessor() == null ? "Informe o professor (cod_professor)." : null,
                RegrasDeNegocio.validarSemestre(dados.semestreReferencia()),
                RegrasDeNegocio.validarAno(dados.anoReferencia()));
        TipoCurso curso = cursos.findById(dados.codTipoCurso())
                .orElseThrow(() -> ApiException.requisicaoInvalida("Tipo de curso inválido."));
        Professor professor = professores.findById(dados.codProfessor())
                .orElseThrow(() -> ApiException.naoEncontrado("Professor não encontrado."));
        Escola escola = escolas.findById(escolaCod).orElseThrow();
        Turma turma = turmas.save(new Turma(escola, curso, professor, dados.semestreReferencia(),
                dados.anoReferencia()));
        return turma.getCod();
    }

    /** Professores da escola = quem dá aula em alguma turma dela, com essas turmas. */
    @Transactional(readOnly = true)
    public List<ProfessorDaEscolaResponse> professores(UsuarioAutenticado quem, Long escolaCod) {
        consultas.exigirDiretorDaEscola(quem, escolaCod);
        Map<Long, List<String>> materias = consultas.materiasPorProfessor();
        Map<Long, List<TurmaResumoResponse>> porProfessor = consultas.resumos(turmas.daEscola(escolaCod)).stream()
                .collect(Collectors.groupingBy(TurmaResumoResponse::professorCod, LinkedHashMap::new,
                        Collectors.toList()));
        return porProfessor.entrySet().stream()
                .map(e -> new ProfessorDaEscolaResponse(e.getKey(), e.getValue().getFirst().professorNome(),
                        materias.getOrDefault(e.getKey(), List.of()), e.getValue()))
                .sorted(Comparator.comparing(ProfessorDaEscolaResponse::nome, RegrasDeNegocio.ORDEM_NOME))
                .toList();
    }

    /** Alunos com matrícula em turma da escola, uma vez cada, com as turmas dele ali. */
    @Transactional(readOnly = true)
    public List<AlunoDaEscolaResponse> alunos(UsuarioAutenticado quem, Long escolaCod) {
        consultas.exigirDiretorDaEscola(quem, escolaCod);
        List<TurmaResumoResponse> turmasDaEscola = consultas.resumos(turmas.daEscola(escolaCod));
        Map<Long, Long> totais = consultas.totalTurmasPorAluno();

        Map<Long, List<Long>> turmasPorAluno = new LinkedHashMap<>();
        Map<Long, Aluno> alunoPorCod = new LinkedHashMap<>();
        for (TurmaAluno ta : matriculas.findByTurmaEscolaCod(escolaCod)) {
            alunoPorCod.put(ta.getAluno().getCod(), ta.getAluno());
            turmasPorAluno.computeIfAbsent(ta.getAluno().getCod(), k -> new ArrayList<>()).add(ta.getTurma().getCod());
        }
        return alunoPorCod.values().stream()
                .map(a -> {
                    Set<Long> dele = Set.copyOf(turmasPorAluno.get(a.getCod()));
                    return new AlunoDaEscolaResponse(a.getCod(), a.getPessoa().getNome(),
                            a.getPessoa().getNumDocumento(), totais.getOrDefault(a.getCod(), 0L),
                            turmasDaEscola.stream().filter(t -> dele.contains(t.cod())).toList());
                })
                .sorted(Comparator.comparing(AlunoDaEscolaResponse::nome, RegrasDeNegocio.ORDEM_NOME))
                .toList();
    }

    /**
     * Só o diretor da escola da turma. Aluno já matriculado é ignorado (uq_turma_aluno),
     * então repetir a chamada é seguro.
     */
    @Transactional
    public void matricular(UsuarioAutenticado quem, Long turmaCod, MatriculaRequest dados) {
        Turma turma = consultas.buscarTurma(turmaCod);
        if (!consultas.ehDiretorDaEscola(quem.papeis(), turma.getEscola().getCod())) {
            throw ApiException.proibido("Só o diretor da escola da turma matricula alunos.");
        }
        List<Long> codigos = dados.codAlunos() == null ? List.of() : dados.codAlunos().stream().filter(Objects::nonNull).distinct().toList();
        if (codigos.isEmpty()) throw ApiException.requisicaoInvalida("Escolha ao menos um aluno.");

        Map<Long, Aluno> encontrados = alunos.findAllById(codigos).stream()
                .collect(Collectors.toMap(Aluno::getCod, Function.identity()));
        if (encontrados.size() != codigos.size()) throw ApiException.naoEncontrado("Aluno não encontrado.");

        for (Long cod : codigos) {
            if (!matriculas.existsByTurmaCodAndAlunoCod(turmaCod, cod)) {
                matriculas.save(new TurmaAluno(turma, encontrados.get(cod)));
            }
        }
    }

    private EscolaResumoResponse resumo(Escola e) {
        return new EscolaResumoResponse(e.getCod(), e.getNome(), e.getDiretor().getPessoa().getNome(),
                turmas.countByEscolaCod(e.getCod()));
    }
}
