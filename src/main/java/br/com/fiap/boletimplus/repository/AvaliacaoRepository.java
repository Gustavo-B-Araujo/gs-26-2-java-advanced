package br.com.fiap.boletimplus.repository;

import br.com.fiap.boletimplus.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    @Query("""
            select a from Avaliacao a
            where a.turma.cod = :turmaCod
            order by case when a.dataAvaliacao is null then 1 else 0 end, a.dataAvaliacao, a.cod
            """)
    List<Avaliacao> daTurma(Long turmaCod);

    boolean existsByTurmaCodAndNome(Long turmaCod, String nome);

    boolean existsByTurmaCodAndNomeAndCodNot(Long turmaCod, String nome, Long cod);
}
