package br.com.fiap.boletimplus.repository;

import br.com.fiap.boletimplus.model.Diretor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiretorRepository extends JpaRepository<Diretor, Long> {

    Optional<Diretor> findByPessoaCod(Long pessoaCod);
}
