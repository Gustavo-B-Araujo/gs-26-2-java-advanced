package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.dto.EnumeradorResponse;
import br.com.fiap.boletimplus.dto.NovoCursoRequest;
import br.com.fiap.boletimplus.exception.ApiException;
import br.com.fiap.boletimplus.model.TipoCurso;
import br.com.fiap.boletimplus.repository.TipoCursoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro de curso (matéria). As listas de enumeradores ficam no TipoController. */
@Service
public class TipoService {

    private final TipoCursoRepository cursos;

    public TipoService(TipoCursoRepository cursos) {
        this.cursos = cursos;
    }

    /**
     * O curso vale para a rede toda. UNIQUE (enumerador), e comparando sem acento nem maiúscula
     * para não ter "Historia" e "História" juntos.
     */
    @Transactional
    public EnumeradorResponse criarCurso(NovoCursoRequest dados) {
        RegrasDeNegocio.exigir(RegrasDeNegocio.validarTipoCurso(dados.enumerador()));
        String nome = dados.enumerador().trim();
        String chave = BoletimService.chave(nome);
        if (cursos.findAll().stream().anyMatch(c -> BoletimService.chave(c.getEnumerador()).equals(chave))) {
            throw ApiException.conflito("Este curso já existe.");
        }
        TipoCurso curso = new TipoCurso();
        curso.setEnumerador(nome);
        return EnumeradorResponse.de(cursos.saveAndFlush(curso));
    }
}
