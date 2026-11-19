package br.com.fiap.boletimplus.controller;

import br.com.fiap.boletimplus.dto.AvaliacaoDetalheResponse;
import br.com.fiap.boletimplus.dto.DadosAvaliacaoRequest;
import br.com.fiap.boletimplus.dto.NotasRequest;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import br.com.fiap.boletimplus.service.SalaDeAulaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/avaliacoes/{cod}")
@Tag(name = "Avaliações", description = "Avaliação e notas. Escrita só do professor da turma.")
public class AvaliacaoController {

    private final SalaDeAulaService salaDeAula;

    public AvaliacaoController(SalaDeAulaService salaDeAula) {
        this.salaDeAula = salaDeAula;
    }

    @GetMapping
    @Operation(summary = "Avaliação com a turma e a nota de cada aluno")
    public AvaliacaoDetalheResponse obter(@AuthenticationPrincipal UsuarioAutenticado quem, @PathVariable Long cod) {
        return salaDeAula.obterAvaliacao(quem, cod);
    }

    @PutMapping
    @PreAuthorize("hasRole('PROFESSOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Edita a avaliação. Só o professor da turma.")
    public void atualizar(@AuthenticationPrincipal UsuarioAutenticado quem, @PathVariable Long cod,
                          @RequestBody DadosAvaliacaoRequest dados) {
        salaDeAula.atualizarAvaliacao(quem, cod, dados);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('PROFESSOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui a avaliação e as notas dela. Só o professor da turma.")
    public void excluir(@AuthenticationPrincipal UsuarioAutenticado quem, @PathVariable Long cod) {
        salaDeAula.excluirAvaliacao(quem, cod);
    }

    @PutMapping("/notas")
    @PreAuthorize("hasRole('PROFESSOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Lança as notas (upsert por aluno). Nota null apaga. Só o professor da turma.",
            description = "Nota de 0 a 100, guardada com 2 casas. Repetir o PUT deixa o mesmo estado.")
    public void salvarNotas(@AuthenticationPrincipal UsuarioAutenticado quem, @PathVariable Long cod,
                            @RequestBody NotasRequest dados) {
        salaDeAula.salvarNotas(quem, cod, dados);
    }
}
