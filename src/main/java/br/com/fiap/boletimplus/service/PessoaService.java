package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.dto.AlunoResumoResponse;
import br.com.fiap.boletimplus.dto.NovaPessoaRequest;
import br.com.fiap.boletimplus.dto.ProfessorResumoResponse;
import br.com.fiap.boletimplus.dto.RenomearRequest;
import br.com.fiap.boletimplus.dto.TurmaResumoResponse;
import br.com.fiap.boletimplus.exception.ApiException;
import br.com.fiap.boletimplus.model.Aluno;
import br.com.fiap.boletimplus.model.PapelDaPessoa;
import br.com.fiap.boletimplus.model.Pessoa;
import br.com.fiap.boletimplus.model.Professor;
import br.com.fiap.boletimplus.model.Usuario;
import br.com.fiap.boletimplus.repository.AlunoRepository;
import br.com.fiap.boletimplus.repository.PessoaRepository;
import br.com.fiap.boletimplus.repository.ProfessorRepository;
import br.com.fiap.boletimplus.repository.TurmaRepository;
import br.com.fiap.boletimplus.repository.UsuarioRepository;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Professores e alunos da rede. Qualquer diretor cadastra e renomeia: professor e aluno não
 * pertencem a uma escola (o DDL só os liga à escola pela Turma).
 */
@Service
public class PessoaService {

    private final ProfessorRepository professores;
    private final AlunoRepository alunos;
    private final PessoaRepository pessoas;
    private final UsuarioRepository usuarios;
    private final TurmaRepository turmas;
    private final ConsultaService consultas;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public PessoaService(ProfessorRepository professores, AlunoRepository alunos, PessoaRepository pessoas,
                         UsuarioRepository usuarios, TurmaRepository turmas, ConsultaService consultas,
                         AuthService authService, PasswordEncoder passwordEncoder) {
        this.professores = professores;
        this.alunos = alunos;
        this.pessoas = pessoas;
        this.usuarios = usuarios;
        this.turmas = turmas;
        this.consultas = consultas;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<ProfessorResumoResponse> listarProfessores() {
        Map<Long, List<String>> materias = consultas.materiasPorProfessor();
        return professores.findAll().stream()
                .map(p -> new ProfessorResumoResponse(p.getCod(), p.getPessoa().getNome(),
                        materias.getOrDefault(p.getCod(), List.of())))
                .sorted(Comparator.comparing(ProfessorResumoResponse::nome, RegrasDeNegocio.ORDEM_NOME))
                .toList();
    }

    /** Uma transação: Pessoa + Usuario ATIVO + Professor. */
    @Transactional
    public Long cadastrarProfessor(NovaPessoaRequest dados) {
        RegrasDeNegocio.exigir(
                RegrasDeNegocio.validarNome(dados.nome()),
                RegrasDeNegocio.validarCpf(dados.numDocumento()),
                RegrasDeNegocio.validarSenha(dados.senha()));
        Pessoa pessoa = novaPessoa(dados);
        usuarios.save(new Usuario(pessoa, authService.statusAtivo(), passwordEncoder.encode(dados.senha())));
        return professores.save(new Professor(pessoa)).getCod();
    }

    @Transactional
    public void renomearProfessor(Long professorCod, RenomearRequest dados) {
        renomear(professores, professorCod, dados, "Professor não encontrado.");
    }

    /** Só o próprio professor. */
    @Transactional(readOnly = true)
    public List<TurmaResumoResponse> turmasDoProfessor(UsuarioAutenticado quem, Long professorCod) {
        if (!professorCod.equals(quem.papeis().professorCod())) throw ApiException.proibido("Acesso negado.");
        return consultas.resumos(turmas.doProfessor(professorCod));
    }

    @Transactional(readOnly = true)
    public List<AlunoResumoResponse> listarAlunos() {
        Map<Long, Long> totais = consultas.totalTurmasPorAluno();
        return alunos.findAll().stream()
                .map(a -> new AlunoResumoResponse(a.getCod(), a.getPessoa().getNome(),
                        a.getPessoa().getNumDocumento(), totais.getOrDefault(a.getCod(), 0L)))
                .sorted(Comparator.comparing(AlunoResumoResponse::nome, RegrasDeNegocio.ORDEM_NOME))
                .toList();
    }

    /** Uma transação: Pessoa + Aluno, e Usuario ATIVO só quando vem senha. */
    @Transactional
    public Long cadastrarAluno(NovaPessoaRequest dados) {
        RegrasDeNegocio.exigir(
                RegrasDeNegocio.validarNome(dados.nome()),
                RegrasDeNegocio.validarCpf(dados.numDocumento()),
                dados.senha() == null ? null : RegrasDeNegocio.validarSenha(dados.senha()));
        Pessoa pessoa = novaPessoa(dados);
        if (dados.senha() != null) {
            usuarios.save(new Usuario(pessoa, authService.statusAtivo(), passwordEncoder.encode(dados.senha())));
        }
        return alunos.save(new Aluno(pessoa)).getCod();
    }

    @Transactional
    public void renomearAluno(Long alunoCod, RenomearRequest dados) {
        renomear(alunos, alunoCod, dados, "Aluno não encontrado.");
    }

    private Pessoa novaPessoa(NovaPessoaRequest dados) {
        if (pessoas.existsByNumDocumento(dados.numDocumento())) {
            throw ApiException.conflito("Este CPF já tem cadastro.");
        }
        return pessoas.saveAndFlush(new Pessoa(dados.nome().trim(), dados.numDocumento()));
    }

    /** UPDATE Pessoa SET nome: o nome vale para todos os papéis da pessoa. O CPF não muda. */
    private <T extends PapelDaPessoa> void renomear(JpaRepository<T, Long> tabela, Long cod, RenomearRequest dados,
                                                    String naoEncontrado) {
        RegrasDeNegocio.exigir(RegrasDeNegocio.validarNome(dados.nome()));
        T papel = tabela.findById(cod).orElseThrow(() -> ApiException.naoEncontrado(naoEncontrado));
        papel.getPessoa().setNome(dados.nome().trim());
    }
}
