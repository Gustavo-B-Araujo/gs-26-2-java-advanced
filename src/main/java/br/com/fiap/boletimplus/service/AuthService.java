package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.dto.AlterarNomeRequest;
import br.com.fiap.boletimplus.dto.AlterarSenhaRequest;
import br.com.fiap.boletimplus.dto.CadastroRequest;
import br.com.fiap.boletimplus.dto.LoginRequest;
import br.com.fiap.boletimplus.dto.PessoaResponse;
import br.com.fiap.boletimplus.dto.SessaoResponse;
import br.com.fiap.boletimplus.exception.ApiException;
import br.com.fiap.boletimplus.model.Guardiao;
import br.com.fiap.boletimplus.model.Pessoa;
import br.com.fiap.boletimplus.model.StatusUsuario;
import br.com.fiap.boletimplus.model.Usuario;
import br.com.fiap.boletimplus.repository.GuardiaoRepository;
import br.com.fiap.boletimplus.repository.PessoaRepository;
import br.com.fiap.boletimplus.repository.StatusUsuarioRepository;
import br.com.fiap.boletimplus.repository.UsuarioRepository;
import br.com.fiap.boletimplus.security.JwtService;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PessoaRepository pessoas;
    private final GuardiaoRepository guardioes;
    private final StatusUsuarioRepository status;
    private final PapelService papelService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarios, PessoaRepository pessoas, GuardiaoRepository guardioes,
                       StatusUsuarioRepository status, PapelService papelService, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarios = usuarios;
        this.pessoas = pessoas;
        this.guardioes = guardioes;
        this.status = status;
        this.papelService = papelService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Compara a senha com `senha_hash` (bcrypt). Só usuário Ativo entra. */
    @Transactional(readOnly = true)
    public SessaoResponse login(LoginRequest dados) {
        Usuario usuario = usuarios.findByPessoaNumDocumento(dados.numDocumento())
                .filter(u -> passwordEncoder.matches(dados.senha(), u.getSenhaHash()))
                .orElseThrow(() -> ApiException.naoAutorizado("CPF ou senha inválidos."));
        if (!PapelService.estaAtivo(usuario)) {
            String situacao = usuario.getStatus().getEnumerador().toLowerCase();
            throw ApiException.proibido("Usuário " + situacao + ". Procure a secretaria.");
        }
        return sessao(usuario);
    }

    /** Cadastro de responsável: cria Pessoa, Usuario Ativo e Guardiao numa transação. */
    @Transactional
    public SessaoResponse cadastrar(CadastroRequest dados) {
        if (pessoas.existsByNumDocumento(dados.numDocumento())) {
            throw ApiException.conflito("Este CPF já tem cadastro. Entre com sua senha.");
        }
        Pessoa pessoa = pessoas.saveAndFlush(new Pessoa(dados.nome().trim(), dados.numDocumento()));
        Usuario usuario = usuarios.save(new Usuario(pessoa, statusAtivo(), passwordEncoder.encode(dados.senha())));
        guardioes.save(new Guardiao(pessoa));
        return sessao(usuario);
    }

    /** Senha atual errada responde 400, não 401: o app trata 401 como sessão vencida. */
    @Transactional
    public void alterarSenha(UsuarioAutenticado quem, AlterarSenhaRequest dados) {
        Usuario usuario = usuarios.findById(quem.usuarioCod())
                .orElseThrow(() -> ApiException.naoAutorizado("Sessão expirada. Entre de novo."));
        if (!passwordEncoder.matches(dados.senhaAtual(), usuario.getSenhaHash())) {
            throw ApiException.requisicaoInvalida("A senha atual não confere.");
        }
        if (dados.novaSenha().equals(dados.senhaAtual())) {
            throw ApiException.requisicaoInvalida("A nova senha precisa ser diferente da atual.");
        }
        usuario.setSenhaHash(passwordEncoder.encode(dados.novaSenha()));
    }

    /** Só diretor e guardião trocam o próprio nome. O CPF não muda. */
    @Transactional
    public PessoaResponse alterarNome(UsuarioAutenticado quem, AlterarNomeRequest dados) {
        if (quem.papeis().diretorCod() == null && quem.papeis().guardiaoCod() == null) {
            throw ApiException.proibido("Só diretor e responsável alteram o próprio nome.");
        }
        Pessoa pessoa = pessoas.findById(quem.pessoaCod())
                .orElseThrow(() -> ApiException.naoEncontrado("Pessoa não encontrada."));
        pessoa.setNome(dados.nome().trim());
        return PessoaResponse.de(pessoa);
    }

    StatusUsuario statusAtivo() {
        return status.findByEnumeradorIgnoreCase(PapelService.STATUS_ATIVO)
                .orElseThrow(() -> new IllegalStateException("StatusUsuario 'Ativo' não existe no banco."));
    }

    private SessaoResponse sessao(Usuario usuario) {
        Pessoa pessoa = usuario.getPessoa();
        return new SessaoResponse(
                jwtService.gerar(usuario.getCod()),
                usuario.getCod(),
                PessoaResponse.de(pessoa),
                papelService.papeisDa(pessoa.getCod()));
    }
}
