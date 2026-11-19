package br.com.fiap.boletimplus.security;

import br.com.fiap.boletimplus.dto.Papeis;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

/** Quem fez a requisição. Os papéis vêm do banco a cada requisição, nunca do token. */
public record UsuarioAutenticado(Long usuarioCod, Long pessoaCod, Papeis papeis) {

    public List<SimpleGrantedAuthority> authorities() {
        List<SimpleGrantedAuthority> lista = new ArrayList<>();
        if (papeis.diretorCod() != null) lista.add(new SimpleGrantedAuthority("ROLE_DIRETOR"));
        if (papeis.professorCod() != null) lista.add(new SimpleGrantedAuthority("ROLE_PROFESSOR"));
        if (papeis.guardiaoCod() != null) lista.add(new SimpleGrantedAuthority("ROLE_GUARDIAO"));
        if (papeis.alunoCod() != null) lista.add(new SimpleGrantedAuthority("ROLE_ALUNO"));
        return lista;
    }
}
