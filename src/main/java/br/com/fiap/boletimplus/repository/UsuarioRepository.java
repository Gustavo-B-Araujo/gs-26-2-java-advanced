package br.com.fiap.boletimplus.repository;

import br.com.fiap.boletimplus.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByPessoaNumDocumento(String numDocumento);

    Optional<Usuario> findByPessoaCod(Long pessoaCod);
}
