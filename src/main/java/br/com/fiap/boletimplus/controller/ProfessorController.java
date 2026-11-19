package br.com.fiap.boletimplus.controller;

import br.com.fiap.boletimplus.dto.NovaPessoaRequest;
import br.com.fiap.boletimplus.dto.ProfessorCriadoResponse;
import br.com.fiap.boletimplus.dto.ProfessorResumoResponse;
import br.com.fiap.boletimplus.dto.RenomearRequest;
import br.com.fiap.boletimplus.dto.TurmaResumoResponse;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import br.com.fiap.boletimplus.service.IdempotenciaService;
import br.com.fiap.boletimplus.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/professores")
@Tag(name = "Professores", description = "Professores da rede: cadastro pelo diretor e turmas do professor")
public class ProfessorController {

    private final PessoaService pessoaService;
    private final IdempotenciaService idempotencia;

    public ProfessorController(PessoaService pessoaService, IdempotenciaService idempotencia) {
        this.pessoaService = pessoaService;
        this.idempotencia = idempotencia;
    }

    @GetMapping
    @PreAuthorize("hasRole('DIRETOR')")
    @Operation(summary = "Lista os professores da rede, com as matérias das turmas deles. Só diretor.")
    public List<ProfessorResumoResponse> listar() {
        return pessoaService.listarProfessores();
    }

    @PostMapping
    @PreAuthorize("hasRole('DIRETOR')")
    @Operation(summary = "Cadastra um professor (Pessoa + Usuario Ativo + Professor). Só diretor.",
            description = "Exige Idempotency-Key. Nome e sobrenome; CPF válido; senha com 8+ caracteres, letras e números. CPF repetido: 409.")
    public ResponseEntity<ProfessorCriadoResponse> cadastrar(
            @AuthenticationPrincipal UsuarioAutenticado quem, HttpServletRequest req,
            @RequestHeader(value = IdempotenciaService.HEADER, required = false) String chave,
            @RequestBody NovaPessoaRequest dados) {
        return idempotencia.executar(quem, req, chave, List.of(dados), () -> {
            Long cod = pessoaService.cadastrarProfessor(dados);
            return ResponseEntity.created(URI.create("/professores/" + cod)).body(new ProfessorCriadoResponse(cod));
        });
    }

    @PutMapping("/{cod}")
    @PreAuthorize("hasRole('DIRETOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Altera o nome do professor. Só diretor.")
    public void renomear(@PathVariable Long cod, @RequestBody RenomearRequest dados) {
        pessoaService.renomearProfessor(cod, dados);
    }

    @GetMapping("/{cod}/turmas")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Lista as turmas do professor. Só o próprio professor.")
    public List<TurmaResumoResponse> turmas(@AuthenticationPrincipal UsuarioAutenticado quem, @PathVariable Long cod) {
        return pessoaService.turmasDoProfessor(quem, cod);
    }
}
