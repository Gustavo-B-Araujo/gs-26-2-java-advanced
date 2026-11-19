package br.com.fiap.boletimplus.repository;

import br.com.fiap.boletimplus.model.Turma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Listas de turmas já em ordem: mais recente primeiro (ano e semestre decrescentes), depois cod. */
public interface TurmaRepository extends JpaRepository<Turma, Long> {

    @Query("""
            select t from Turma t where t.escola.cod = :escolaCod
            order by t.anoReferencia desc, t.semestreReferencia desc, t.cod
            """)
    List<Turma> daEscola(Long escolaCod);

    @Query("""
            select t from Turma t where t.professor.cod = :professorCod
            order by t.anoReferencia desc, t.semestreReferencia desc, t.cod
            """)
    List<Turma> doProfessor(Long professorCod);

    long countByEscolaCod(Long escolaCod);

    /** Pares (professor, matéria) sem repetir, em toda a rede. */
    @Query("select distinct t.professor.cod, t.tipoCurso.enumerador from Turma t")
    List<Object[]> materiasPorProfessor();
}
