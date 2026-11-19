package br.com.fiap.boletimplus.controller;

import br.com.fiap.boletimplus.dto.EnumeradorResponse;
import br.com.fiap.boletimplus.dto.NovoCursoRequest;
import br.com.fiap.boletimplus.model.Enumerador;
import br.com.fiap.boletimplus.repository.TipoCursoRepository;
import br.com.fiap.boletimplus.repository.TipoNotaRepository;
import br.com.fiap.boletimplus.repository.TipoParentescoRepository;
import br.com.fiap.boletimplus.repository.TipoPresencaRepository;
import br.com.fiap.boletimplus.service.TipoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/tipos")
@Tag(name = "Tipos", description = "Enumeradores usados nos formulários")
public class TipoController {

    private final TipoParentescoRepository parentescos;
    private final TipoPresencaRepository presencas;
    private final TipoNotaRepository notas;
    private final TipoCursoRepository cursos;
    private final TipoService tipoService;

    public TipoController(TipoParentescoRepository parentescos, TipoPresencaRepository presencas,
                          TipoNotaRepository notas, TipoCursoRepository cursos, TipoService tipoService) {
        this.parentescos = parentescos;
        this.presencas = presencas;
        this.notas = notas;
        this.cursos = cursos;
        this.tipoService = tipoService;
    }

    @GetMapping("/parentesco")
    @Operation(summary = "Tipos de parentesco", description = "Para escolher o parentesco no vínculo.")
    public List<EnumeradorResponse> parentescos() {
        return listar(parentescos);
    }

    @GetMapping("/presenca")
    @Operation(summary = "Tipos de presença", description = "Presente, Falta, Falta Justificada.")
    public List<EnumeradorResponse> presencas() {
        return listar(presencas);
    }

    @GetMapping("/nota")
    @Operation(summary = "Tipos de nota", description = "Prova, Trabalho, Caderno.")
    public List<EnumeradorResponse> notas() {
        return listar(notas);
    }

    @GetMapping("/curso")
    @Operation(summary = "Cursos (matérias) da rede")
    public List<EnumeradorResponse> cursos() {
        return listar(cursos);
    }

    @PostMapping("/curso")
    @PreAuthorize("hasRole('DIRETOR')")
    @Operation(summary = "Cadastra um curso (matéria) na rede. Só diretor.",
            description = "Nome de 1 a 30 caracteres. Nome repetido (sem considerar acento e maiúscula) responde 409.")
    public ResponseEntity<EnumeradorResponse> criarCurso(@RequestBody NovoCursoRequest dados) {
        return ResponseEntity.created(URI.create("/tipos/curso")).body(tipoService.criarCurso(dados));
    }

    private static <T extends Enumerador> List<EnumeradorResponse> listar(JpaRepository<T, Long> tabela) {
        return tabela.findAll(Sort.by("cod")).stream().map(EnumeradorResponse::de).toList();
    }
}
