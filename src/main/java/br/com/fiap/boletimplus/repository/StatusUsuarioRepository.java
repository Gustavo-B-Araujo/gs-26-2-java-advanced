package br.com.fiap.boletimplus.repository;

import br.com.fiap.boletimplus.model.StatusUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusUsuarioRepository extends JpaRepository<StatusUsuario, Long> {

    Optional<StatusUsuario> findByEnumeradorIgnoreCase(String enumerador);
}
