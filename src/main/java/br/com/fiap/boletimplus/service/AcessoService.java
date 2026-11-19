package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.dto.Papeis;
import br.com.fiap.boletimplus.exception.ApiException;
import br.com.fiap.boletimplus.repository.AlunoGuardiaoRepository;
import br.com.fiap.boletimplus.repository.TurmaAlunoRepository;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quem pode ver os dados de um aluno: o próprio aluno, um responsável vinculado,
 * o professor de uma turma dele ou o diretor da escola de uma turma dele.
 */
@Service
public class AcessoService {

    private final AlunoGuardiaoRepository vinculos;
    private final TurmaAlunoRepository matriculas;

    public AcessoService(AlunoGuardiaoRepository vinculos, TurmaAlunoRepository matriculas) {
        this.vinculos = vinculos;
        this.matriculas = matriculas;
    }

    @Transactional(readOnly = true)
    public boolean podeVerAluno(UsuarioAutenticado quem, Long alunoCod) {
        Papeis p = quem.papeis();
        if (alunoCod.equals(p.alunoCod())) return true;
        if (p.guardiaoCod() != null && vinculos.existsByGuardiaoCodAndAlunoCod(p.guardiaoCod(), alunoCod)) return true;
        if (p.professorCod() == null && p.diretorCod() == null) return false;
        return matriculas.alunoEstaComProfessorOuDiretor(alunoCod, p.professorCod(), p.diretorCod());
    }

    public void exigirAcessoAoAluno(UsuarioAutenticado quem, Long alunoCod) {
        if (!podeVerAluno(quem, alunoCod)) {
            throw ApiException.proibido("Você não tem acesso a este aluno.");
        }
    }
}
