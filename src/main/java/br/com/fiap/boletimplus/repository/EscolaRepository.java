package br.com.fiap.boletimplus.repository;

import br.com.fiap.boletimplus.model.Escola;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscolaRepository extends JpaRepository<Escola, Long> {

    List<Escola> findByDiretorCodOrderByNome(Long diretorCod);

    boolean existsByCodAndDiretorCod(Long cod, Long diretorCod);
}
