package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.dto.AlterarNomeRequest;
import br.com.fiap.boletimplus.dto.DependenteResponse;
import br.com.fiap.boletimplus.dto.GuardiaoRequest;
import br.com.fiap.boletimplus.dto.GuardiaoResponse;
import br.com.fiap.boletimplus.exception.ApiException;
import br.com.fiap.boletimplus.model.Guardiao;
import br.com.fiap.boletimplus.model.Pessoa;
import br.com.fiap.boletimplus.model.Usuario;
import br.com.fiap.boletimplus.repository.AlunoGuardiaoRepository;
import br.com.fiap.boletimplus.repository.GuardiaoRepository;
import br.com.fiap.boletimplus.repository.PessoaRepository;
import br.com.fiap.boletimplus.repository.UsuarioRepository;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD de responsáveis (Guardiao + Pessoa) e a lista de dependentes. */
@Service
public class GuardiaoService {

    private final GuardiaoRepository guardioes;
    private final PessoaRepository pessoas;
    private final UsuarioRepository usuarios;
    private final AlunoGuardiaoRepository vinculos;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public GuardiaoService(GuardiaoRepository guardioes, PessoaRepository pessoas, UsuarioRepository usuarios,
                           AlunoGuardiaoRepository vinculos, AuthService authService,
                           PasswordEncoder passwordEncoder) {
        this.guardioes = guardioes;
        this.pessoas = pessoas;
        this.usuarios = usuarios;
        this.vinculos = vinculos;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<GuardiaoResponse> listar(String nome, Pageable pageable) {
        return guardioes.findByPessoaNomeContainingIgnoreCase(nome == null ? "" : nome.trim(), pageable)
                .map(this::resposta);
    }

    @Transactional(readOnly = true)
    public GuardiaoResponse buscar(Long cod) {
        return resposta(encontrar(cod));
    }

    /**
     * CPF novo: cria Pessoa, Guardiao e, com senha, Usuario Ativo.
     * CPF de uma Pessoa que já existe (ex.: um professor que também é pai): ela só ganha o papel de guardião.
     */
    @Transactional
    public GuardiaoResponse criar(GuardiaoRequest dados) {
        Pessoa pessoa = pessoas.findByNumDocumento(dados.numDocumento()).orElse(null);
        if (pessoa != null && guardioes.findByPessoaCod(pessoa.getCod()).isPresent()) {
            throw ApiException.conflito("Este CPF já é de um responsável.");
        }
        if (pessoa == null) {
            pessoa = pessoas.saveAndFlush(new Pessoa(dados.nome().trim(), dados.numDocumento()));
        }
        if (dados.senha() != null && usuarios.findByPessoaCod(pessoa.getCod()).isEmpty()) {
            usuarios.save(new Usuario(pessoa, authService.statusAtivo(), passwordEncoder.encode(dados.senha())));
        }
        return resposta(guardioes.save(new Guardiao(pessoa)));
    }

    /** O nome é da Pessoa: vale para todos os papéis dela. */
    @Transactional
    public GuardiaoResponse alterarNome(Long cod, AlterarNomeRequest dados) {
        Guardiao guardiao = encontrar(cod);
        guardiao.getPessoa().setNome(dados.nome().trim());
        return resposta(guardiao);
    }

    /** Remove só o papel de guardião. Pessoa e Usuario ficam, porque podem ter outros papéis. */
    @Transactional
    public void excluir(Long cod) {
        Guardiao guardiao = encontrar(cod);
        if (vinculos.existsByGuardiaoCod(cod)) {
            throw ApiException.conflito("Remova os vínculos com alunos antes de excluir o responsável.");
        }
        guardioes.delete(guardiao);
    }

    /** Só o próprio responsável lista os dependentes dele. */
    @Transactional(readOnly = true)
    public List<DependenteResponse> dependentes(UsuarioAutenticado quem, Long guardiaoCod) {
        if (!guardiaoCod.equals(quem.papeis().guardiaoCod())) {
            throw ApiException.proibido("Acesso negado.");
        }
        return vinculos.findByGuardiaoCodOrderByAlunoPessoaNome(guardiaoCod).stream()
                .map(v -> new DependenteResponse(v.getAluno().getCod(), v.getAluno().getPessoa().getNome(),
                        v.getTipoParentesco().getEnumerador()))
                .toList();
    }

    private Guardiao encontrar(Long cod) {
        return guardioes.findById(cod).orElseThrow(() -> ApiException.naoEncontrado("Responsável não encontrado."));
    }

    private GuardiaoResponse resposta(Guardiao g) {
        Pessoa p = g.getPessoa();
        return new GuardiaoResponse(g.getCod(), p.getCod(), p.getNome(), p.getNumDocumento(),
                vinculos.countByGuardiaoCod(g.getCod()));
    }
}
