package br.com.fiap.boletimplus.repository;

import br.com.fiap.boletimplus.model.ListaPresenca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ListaPresencaRepository extends JpaRepository<ListaPresenca, Long> {

    List<ListaPresenca> findByTurmaAlunoCodOrderByDataPresencaDesc(Long turmaAlunoCod);

    @Query("select l from ListaPresenca l where l.turmaAluno.turma.cod = :turmaCod and l.dataPresenca = :data")
    List<ListaPresenca> daTurmaNoDia(Long turmaCod, LocalDate data);
}
