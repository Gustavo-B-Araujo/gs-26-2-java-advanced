package br.com.fiap.boletimplus.controller;

import br.com.fiap.boletimplus.dto.AlunoCriadoResponse;
import br.com.fiap.boletimplus.dto.AlunoResumoResponse;
import br.com.fiap.boletimplus.dto.AnaliseBoletimResponse;
import br.com.fiap.boletimplus.dto.BoletimAlunoResponse;
import br.com.fiap.boletimplus.dto.NovaPessoaRequest;
import br.com.fiap.boletimplus.dto.RenomearRequest;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import br.com.fiap.boletimplus.service.AcessoService;
import br.com.fiap.boletimplus.service.AnaliseBoletimService;
import br.com.fiap.boletimplus.service.BoletimService;
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
@RequestMapping("/alunos")
public class AlunoController {

    private final AcessoService acesso;
    private final BoletimService boletimService;
    private final AnaliseBoletimService analiseService;
    private final PessoaService pessoaService;
    private final IdempotenciaService idempotencia;

    public AlunoController(AcessoService acesso, BoletimService boletimService, AnaliseBoletimService analiseService,
                           PessoaService pessoaService, IdempotenciaService idempotencia) {
        this.acesso = acesso;
        this.boletimService = boletimService;
        this.analiseService = analiseService;
        this.pessoaService = pessoaService;
        this.idempotencia = idempotencia;
    }

    @GetMapping("/{cod}/boletim")
    @Tag(name = "Boletim", description = "Notas, frequência e análise com IA")
    @Operation(summary = "Boletim do aluno", description = "Aluno, responsável vinculado, professor ou diretor de uma turma dele.")
    public BoletimAlunoResponse boletim(@AuthenticationPrincipal UsuarioAutenticado quem, @PathVariable Long cod) {
        acesso.exigirAcessoAoAluno(quem, cod);
        return boletimService.montar(cod);
    }

    @PostMapping("/{cod}/boletim/analise")
    @Tag(name = "Boletim", description = "Notas, frequência e análise com IA")
    @Operation(summary = "Analisa o boletim com IA",
            description = "Spring AI + Ollama. Publica no RabbitMQ um alerta por matéria em atenção. 503: LLM indisponível.")
    public AnaliseBoletimResponse analisar(@AuthenticationPrincipal UsuarioAutenticado quem, @PathVariable Long cod) {
        acesso.exigirAcessoAoAluno(quem, cod);
        return analiseService.analisar(cod);
    }

    @GetMapping
    @PreAuthorize("hasRole('DIRETOR')")
    @Tag(name = "Alunos", description = "Alunos da rede (só diretor)")
    @Operation(summary = "Lista os alunos da rede, com o total de matrículas (0 = sem turma). Só diretor.")
    public List<AlunoResumoResponse> listar() {
        return pessoaService.listarAlunos();
    }

    @PostMapping
    @PreAuthorize("hasRole('DIRETOR')")
    @Tag(name = "Alunos", description = "Alunos da rede (só diretor)")
    @Operation(summary = "Cadastra um aluno na rede (Pessoa + Aluno, e Usuario só com senha). Só diretor.",
            description = "Exige Idempotency-Key. Sem senha (null), o aluno não entra no app. CPF repetido responde 409.")
    public ResponseEntity<AlunoCriadoResponse> cadastrar(
            @AuthenticationPrincipal UsuarioAutenticado quem, HttpServletRequest req,
            @RequestHeader(value = IdempotenciaService.HEADER, required = false) String chave,
            @RequestBody NovaPessoaRequest dados) {
        return idempotencia.executar(quem, req, chave, List.of(dados), () -> {
            Long cod = pessoaService.cadastrarAluno(dados);
            return ResponseEntity.created(URI.create("/alunos/" + cod)).body(new AlunoCriadoResponse(cod));
        });
    }

    @PutMapping("/{cod}")
    @PreAuthorize("hasRole('DIRETOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Tag(name = "Alunos", description = "Alunos da rede (só diretor)")
    @Operation(summary = "Altera o nome do aluno. Só diretor.")
    public void renomear(@PathVariable Long cod, @RequestBody RenomearRequest dados) {
        pessoaService.renomearAluno(cod, dados);
    }
}
