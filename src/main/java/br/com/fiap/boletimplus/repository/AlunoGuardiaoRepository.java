package br.com.fiap.boletimplus.repository;

import br.com.fiap.boletimplus.model.AlunoGuardiao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AlunoGuardiaoRepository extends JpaRepository<AlunoGuardiao, Long> {

    List<AlunoGuardiao> findByGuardiaoCodOrderByAlunoPessoaNome(Long guardiaoCod);

    List<AlunoGuardiao> findByAlunoCod(Long alunoCod);

    boolean existsByGuardiaoCodAndAlunoCod(Long guardiaoCod, Long alunoCod);

    boolean existsByGuardiaoCod(Long guardiaoCod);

    long countByGuardiaoCod(Long guardiaoCod);

    @Query("""
            select v from AlunoGuardiao v
            where (:guardiaoCod is null or v.guardiao.cod = :guardiaoCod)
              and (:alunoCod is null or v.aluno.cod = :alunoCod)
            """)
    Page<AlunoGuardiao> buscar(Long guardiaoCod, Long alunoCod, Pageable pageable);
}
