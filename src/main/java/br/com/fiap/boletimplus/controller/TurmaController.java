package br.com.fiap.boletimplus.controller;

import br.com.fiap.boletimplus.dto.ChamadaRequest;
import br.com.fiap.boletimplus.dto.CodResponse;
import br.com.fiap.boletimplus.dto.DadosAvaliacaoRequest;
import br.com.fiap.boletimplus.dto.ItemChamadaResponse;
import br.com.fiap.boletimplus.dto.MatriculaRequest;
import br.com.fiap.boletimplus.dto.TurmaDetalheResponse;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import br.com.fiap.boletimplus.service.EscolaService;
import br.com.fiap.boletimplus.service.IdempotenciaService;
import br.com.fiap.boletimplus.service.RegrasDeNegocio;
import br.com.fiap.boletimplus.service.SalaDeAulaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Turma: detalhe, matrícula, avaliações e chamada. Escrita de sala de aula só do professor da turma. */
@RestController
@RequestMapping("/turmas/{cod}")
@Tag(name = "Turmas", description = "Detalhe da turma, matrícula, avaliações e chamada")
public class TurmaController {

    private final SalaDeAulaService salaDeAula;
    private final EscolaService escolaService;
    private final IdempotenciaService idempotencia;

    public TurmaController(SalaDeAulaService salaDeAula, EscolaService escolaService,
                           IdempotenciaService idempotencia) {
        this.salaDeAula = salaDeAula;
        this.escolaService = escolaService;
        this.idempotencia = idempotencia;
    }

    @GetMapping
    @Operation(summary = "Turma com alunos e avaliações",
            description = "Professor da turma, diretor da escola, aluno matriculado ou responsável por um matriculado.")
    public TurmaDetalheResponse obter(@AuthenticationPrincipal UsuarioAutenticado quem, @PathVariable Long cod) {
        return salaDeAula.obterTurma(quem, cod);
    }

    @PostMapping("/alunos")
    @PreAuthorize("hasRole('DIRETOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Matricula alunos da rede na turma. Só o diretor da escola da turma.",
            description = "Aluno já matriculado é ignorado: repetir não duplica. Lista vazia: 400; aluno inexistente: 404.")
    public void matricular(@AuthenticationPrincipal UsuarioAutenticado quem, @PathVariable Long cod,
                           @RequestBody MatriculaRequest dados) {
        escolaService.matricular(quem, cod, dados);
    }

    @PostMapping("/avaliacoes")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Cria uma avaliação na turma. Só o professor da turma.",
            description = "Exige Idempotency-Key: a mesma chave devolve a mesma avaliação. Nome de 1 a 50, único na turma (409).")
    public ResponseEntity<CodResponse> criarAvaliacao(
            @AuthenticationPrincipal UsuarioAutenticado quem, HttpServletRequest req, @PathVariable Long cod,
            @RequestHeader(value = IdempotenciaService.HEADER, required = false) String chave,
            @RequestBody DadosAvaliacaoRequest dados) {
        return idempotencia.executar(quem, req, chave, List.of(cod, dados), () -> {
            Long avaliacao = salaDeAula.criarAvaliacao(quem, cod, dados);
            return ResponseEntity.created(URI.create("/avaliacoes/" + avaliacao)).body(new CodResponse(avaliacao));
        });
    }

    @GetMapping("/chamada")
    @Operation(summary = "Chamada do dia: cada aluno da turma com a presença lançada (ou null)",
            description = "Parâmetro data no formato YYYY-MM-DD.")
    public List<ItemChamadaResponse> chamada(@AuthenticationPrincipal UsuarioAutenticado quem, @PathVariable Long cod,
                                             @RequestParam(required = false) String data) {
        return salaDeAula.obterChamada(quem, cod, RegrasDeNegocio.lerData(data));
    }

    @PutMapping("/chamada")
    @PreAuthorize("hasRole('PROFESSOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Salva a chamada do dia (upsert por aluno e data). Só o professor da turma.",
            description = "Sem data futura. Repetir o PUT com o mesmo corpo deixa o mesmo estado.")
    public void salvarChamada(@AuthenticationPrincipal UsuarioAutenticado quem, @PathVariable Long cod,
                              @RequestParam(required = false) String data, @RequestBody ChamadaRequest dados) {
        salaDeAula.salvarChamada(quem, cod, RegrasDeNegocio.lerData(data), dados);
    }
}
