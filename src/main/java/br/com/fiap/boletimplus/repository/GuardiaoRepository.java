package br.com.fiap.boletimplus.repository;

import br.com.fiap.boletimplus.model.Guardiao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuardiaoRepository extends JpaRepository<Guardiao, Long> {

    Optional<Guardiao> findByPessoaCod(Long pessoaCod);

    Page<Guardiao> findByPessoaNomeContainingIgnoreCase(String nome, Pageable pageable);
}
