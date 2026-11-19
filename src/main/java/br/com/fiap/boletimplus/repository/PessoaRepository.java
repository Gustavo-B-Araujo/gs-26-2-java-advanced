package br.com.fiap.boletimplus.repository;

import br.com.fiap.boletimplus.model.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

    Optional<Pessoa> findByNumDocumento(String numDocumento);

    boolean existsByNumDocumento(String numDocumento);
}
