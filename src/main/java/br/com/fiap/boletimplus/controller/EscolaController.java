package br.com.fiap.boletimplus.controller;

import br.com.fiap.boletimplus.dto.AlunoDaEscolaResponse;
import br.com.fiap.boletimplus.dto.CodResponse;
import br.com.fiap.boletimplus.dto.EscolaResumoResponse;
import br.com.fiap.boletimplus.dto.NovaTurmaRequest;
import br.com.fiap.boletimplus.dto.ProfessorDaEscolaResponse;
import br.com.fiap.boletimplus.dto.TurmaResumoResponse;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import br.com.fiap.boletimplus.service.EscolaService;
import br.com.fiap.boletimplus.service.IdempotenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Gestão escolar: tudo aqui é do diretor da escola. */
@RestController
@PreAuthorize("hasRole('DIRETOR')")
@Tag(name = "Escolas", description = "Escolas do diretor, turmas, professores e alunos da escola")
public class EscolaController {

    private final EscolaService escolaService;
    private final IdempotenciaService idempotencia;

    public EscolaController(EscolaService escolaService, IdempotenciaService idempotencia) {
        this.escolaService = escolaService;
        this.idempotencia = idempotencia;
    }

    @GetMapping("/diretores/{cod}/escolas")
    @Operation(summary = "Lista as escolas do diretor. Só o próprio diretor.")
    public List<EscolaResumoResponse> escolasDoDiretor(@AuthenticationPrincipal UsuarioAutenticado quem,
                                                       @PathVariable Long cod) {
        return escolaService.escolasDoDiretor(quem, cod);
    }

    @GetMapping("/escolas/{cod}")
    @Operation(summary = "Dados da escola. Só o diretor da escola.")
    public EscolaResumoResponse obter(@AuthenticationPrincipal UsuarioAutenticado quem, @PathVariable Long cod) {
        return escolaService.obter(quem, cod);
    }

    @GetMapping("/escolas/{cod}/turmas")
    @Operation(summary = "Turmas da escola, da mais recente para a mais antiga.")
    public List<TurmaResumoResponse> turmas(@AuthenticationPrincipal UsuarioAutenticado quem, @PathVariable Long cod) {
        return escolaService.turmas(quem, cod);
    }

    @PostMapping("/escolas/{cod}/turmas")
    @Operation(summary = "Cria uma turma na escola. Só o diretor da escola.",
            description = "Exige Idempotency-Key. Professor obrigatório (qualquer um da rede); semestre 1 ou 2; ano a partir de 2000.")
    public ResponseEntity<CodResponse> criarTurma(
            @AuthenticationPrincipal UsuarioAutenticado quem, HttpServletRequest req, @PathVariable Long cod,
            @RequestHeader(value = IdempotenciaService.HEADER, required = false) String chave,
            @RequestBody NovaTurmaRequest dados) {
        return idempotencia.executar(quem, req, chave, List.of(cod, dados), () -> {
            Long turma = escolaService.criarTurma(quem, cod, dados);
            return ResponseEntity.created(URI.create("/turmas/" + turma)).body(new CodResponse(turma));
        });
    }

    @GetMapping("/escolas/{cod}/professores")
    @Operation(summary = "Professores que dão aula em turmas da escola, com essas turmas.")
    public List<ProfessorDaEscolaResponse> professores(@AuthenticationPrincipal UsuarioAutenticado quem,
                                                       @PathVariable Long cod) {
        return escolaService.professores(quem, cod);
    }

    @GetMapping("/escolas/{cod}/alunos")
    @Operation(summary = "Alunos matriculados em turmas da escola, com essas turmas.")
    public List<AlunoDaEscolaResponse> alunos(@AuthenticationPrincipal UsuarioAutenticado quem,
                                              @PathVariable Long cod) {
        return escolaService.alunos(quem, cod);
    }
}
