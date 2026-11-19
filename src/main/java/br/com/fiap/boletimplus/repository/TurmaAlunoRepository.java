package br.com.fiap.boletimplus.repository;

import br.com.fiap.boletimplus.model.TurmaAluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TurmaAlunoRepository extends JpaRepository<TurmaAluno, Long> {

    List<TurmaAluno> findByAlunoCod(Long alunoCod);

    List<TurmaAluno> findByTurmaCod(Long turmaCod);

    List<TurmaAluno> findByTurmaEscolaCod(Long escolaCod);

    boolean existsByTurmaCodAndAlunoCod(Long turmaCod, Long alunoCod);

    long countByAlunoCod(Long alunoCod);

    /** Total de matrículas de cada aluno na rede: pares (aluno, total). */
    @Query("select ta.aluno.cod, count(ta) from TurmaAluno ta group by ta.aluno.cod")
    List<Object[]> totalPorAluno();

    @Query("""
            select count(ta) > 0 from TurmaAluno ta, AlunoGuardiao ag
            where ta.turma.cod = :turmaCod and ag.aluno = ta.aluno and ag.guardiao.cod = :guardiaoCod
            """)
    boolean turmaTemDependenteDo(Long turmaCod, Long guardiaoCod);

    long countByTurmaCod(Long turmaCod);

    @Query("""
            select count(ta) > 0 from TurmaAluno ta
            where ta.aluno.cod = :alunoCod
              and (ta.turma.professor.cod = :professorCod or ta.turma.escola.diretor.cod = :diretorCod)
            """)
    boolean alunoEstaComProfessorOuDiretor(Long alunoCod, Long professorCod, Long diretorCod);
}
