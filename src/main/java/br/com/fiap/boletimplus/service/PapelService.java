package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.dto.Papeis;
import br.com.fiap.boletimplus.model.PapelDaPessoa;
import br.com.fiap.boletimplus.model.Usuario;
import br.com.fiap.boletimplus.repository.AlunoRepository;
import br.com.fiap.boletimplus.repository.DiretorRepository;
import br.com.fiap.boletimplus.repository.GuardiaoRepository;
import br.com.fiap.boletimplus.repository.ProfessorRepository;
import br.com.fiap.boletimplus.repository.UsuarioRepository;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Lê no banco os papéis de uma pessoa: diretor, professor, guardião e aluno. */
@Service
public class PapelService {

    public static final String STATUS_ATIVO = "Ativo";

    private final UsuarioRepository usuarios;
    private final DiretorRepository diretores;
    private final ProfessorRepository professores;
    private final GuardiaoRepository guardioes;
    private final AlunoRepository alunos;

    public PapelService(UsuarioRepository usuarios, DiretorRepository diretores, ProfessorRepository professores,
                        GuardiaoRepository guardioes, AlunoRepository alunos) {
        this.usuarios = usuarios;
        this.diretores = diretores;
        this.professores = professores;
        this.guardioes = guardioes;
        this.alunos = alunos;
    }

    @Transactional(readOnly = true)
    public Papeis papeisDa(Long pessoaCod) {
        return new Papeis(
                diretores.findByPessoaCod(pessoaCod).map(PapelDaPessoa::getCod).orElse(null),
                professores.findByPessoaCod(pessoaCod).map(PapelDaPessoa::getCod).orElse(null),
                guardioes.findByPessoaCod(pessoaCod).map(PapelDaPessoa::getCod).orElse(null),
                alunos.findByPessoaCod(pessoaCod).map(PapelDaPessoa::getCod).orElse(null));
    }

    /** Usuário do token, só se ainda existe e está Ativo. Bloqueado no meio da sessão perde o acesso. */
    @Transactional(readOnly = true)
    public Optional<UsuarioAutenticado> usuarioAtivo(Long usuarioCod) {
        return usuarios.findById(usuarioCod)
                .filter(PapelService::estaAtivo)
                .map(u -> new UsuarioAutenticado(u.getCod(), u.getPessoa().getCod(), papeisDa(u.getPessoa().getCod())));
    }

    public static boolean estaAtivo(Usuario usuario) {
        return STATUS_ATIVO.equalsIgnoreCase(usuario.getStatus().getEnumerador());
    }
}
