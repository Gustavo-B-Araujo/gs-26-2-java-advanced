package br.com.fiap.boletimplus.repository;

import br.com.fiap.boletimplus.model.Boletim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoletimRepository extends JpaRepository<Boletim, Long> {

    List<Boletim> findByTurmaAlunoCod(Long turmaAlunoCod);

    List<Boletim> findByAvaliacaoCod(Long avaliacaoCod);

    long countByAvaliacaoCod(Long avaliacaoCod);
}
