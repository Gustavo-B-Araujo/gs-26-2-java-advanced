package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.dto.AlterarVinculoRequest;
import br.com.fiap.boletimplus.dto.VinculoRequest;
import br.com.fiap.boletimplus.dto.VinculoResponse;
import br.com.fiap.boletimplus.exception.ApiException;
import br.com.fiap.boletimplus.model.Aluno;
import br.com.fiap.boletimplus.model.AlunoGuardiao;
import br.com.fiap.boletimplus.model.Guardiao;
import br.com.fiap.boletimplus.model.TipoParentesco;
import br.com.fiap.boletimplus.repository.AlunoGuardiaoRepository;
import br.com.fiap.boletimplus.repository.AlunoRepository;
import br.com.fiap.boletimplus.repository.GuardiaoRepository;
import br.com.fiap.boletimplus.repository.TipoParentescoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD do vínculo responsável ↔ aluno (AlunoGuardiao). É ele que libera o boletim para o responsável. */
@Service
public class VinculoService {

    private final AlunoGuardiaoRepository vinculos;
    private final GuardiaoRepository guardioes;
    private final AlunoRepository alunos;
    private final TipoParentescoRepository parentescos;

    public VinculoService(AlunoGuardiaoRepository vinculos, GuardiaoRepository guardioes, AlunoRepository alunos,
                          TipoParentescoRepository parentescos) {
        this.vinculos = vinculos;
        this.guardioes = guardioes;
        this.alunos = alunos;
        this.parentescos = parentescos;
    }

    @Transactional(readOnly = true)
    public Page<VinculoResponse> listar(Long guardiaoCod, Long alunoCod, Pageable pageable) {
        return vinculos.buscar(guardiaoCod, alunoCod, pageable).map(VinculoResponse::de);
    }

    @Transactional(readOnly = true)
    public VinculoResponse buscar(Long cod) {
        return VinculoResponse.de(encontrar(cod));
    }

    /** O par (responsável, aluno) é único: `uq_aluno_guardiao`. */
    @Transactional
    public VinculoResponse criar(VinculoRequest dados) {
        Guardiao guardiao = guardioes.findById(dados.codGuardiao())
                .orElseThrow(() -> ApiException.naoEncontrado("Responsável não encontrado."));
        Aluno aluno = alunos.findById(dados.codAluno())
                .orElseThrow(() -> ApiException.naoEncontrado("Aluno não encontrado."));
        if (vinculos.existsByGuardiaoCodAndAlunoCod(guardiao.getCod(), aluno.getCod())) {
            throw ApiException.conflito("Este responsável já está vinculado a este aluno.");
        }
        if (guardiao.getPessoa().getCod().equals(aluno.getPessoa().getCod())) {
            throw ApiException.requisicaoInvalida("O aluno não pode ser responsável por si mesmo.");
        }
        return VinculoResponse.de(vinculos.save(new AlunoGuardiao(guardiao, aluno, parentesco(dados.codTipoParentesco()))));
    }

    @Transactional
    public VinculoResponse alterar(Long cod, AlterarVinculoRequest dados) {
        AlunoGuardiao vinculo = encontrar(cod);
        vinculo.setTipoParentesco(parentesco(dados.codTipoParentesco()));
        return VinculoResponse.de(vinculo);
    }

    @Transactional
    public void excluir(Long cod) {
        vinculos.delete(encontrar(cod));
    }

    private AlunoGuardiao encontrar(Long cod) {
        return vinculos.findById(cod).orElseThrow(() -> ApiException.naoEncontrado("Vínculo não encontrado."));
    }

    private TipoParentesco parentesco(Long cod) {
        return parentescos.findById(cod).orElseThrow(() -> ApiException.naoEncontrado("Parentesco não encontrado."));
    }
}
