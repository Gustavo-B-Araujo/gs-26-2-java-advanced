package br.com.fiap.boletimplus.repository;

import br.com.fiap.boletimplus.model.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    Optional<Aluno> findByPessoaCod(Long pessoaCod);
}
