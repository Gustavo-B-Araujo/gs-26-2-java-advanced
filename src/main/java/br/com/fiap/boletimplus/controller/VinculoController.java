package br.com.fiap.boletimplus.controller;

import br.com.fiap.boletimplus.dto.AlterarVinculoRequest;
import br.com.fiap.boletimplus.dto.PaginaResponse;
import br.com.fiap.boletimplus.dto.VinculoRequest;
import br.com.fiap.boletimplus.dto.VinculoResponse;
import br.com.fiap.boletimplus.service.VinculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/vinculos")
@PreAuthorize("hasRole('DIRETOR')")
@Tag(name = "Vínculos", description = "Responsável ↔ aluno (AlunoGuardiao). Só diretor.")
public class VinculoController {

    private final VinculoService vinculoService;

    public VinculoController(VinculoService vinculoService) {
        this.vinculoService = vinculoService;
    }

    @GetMapping
    @Operation(summary = "Lista vínculos, paginado", description = "Filtros opcionais: guardiaoCod, alunoCod.")
    public PaginaResponse<VinculoResponse> listar(
            @RequestParam(required = false) Long guardiaoCod,
            @RequestParam(required = false) Long alunoCod,
            @PageableDefault(size = 10, sort = "cod") Pageable pageable) {
        return PaginaResponse.de(vinculoService.listar(guardiaoCod, alunoCod, pageable));
    }

    @GetMapping("/{cod}")
    @Operation(summary = "Busca um vínculo")
    public VinculoResponse buscar(@PathVariable Long cod) {
        return vinculoService.buscar(cod);
    }

    @PostMapping
    @Operation(summary = "Vincula responsável e aluno", description = "404: responsável, aluno ou parentesco inexistente. 409: vínculo repetido.")
    public ResponseEntity<VinculoResponse> criar(@RequestBody @Valid VinculoRequest dados) {
        VinculoResponse criado = vinculoService.criar(dados);
        var local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{cod}").buildAndExpand(criado.cod()).toUri();
        return ResponseEntity.created(local).body(criado);
    }

    @PutMapping("/{cod}")
    @Operation(summary = "Altera o parentesco")
    public VinculoResponse alterar(@PathVariable Long cod, @RequestBody @Valid AlterarVinculoRequest dados) {
        return vinculoService.alterar(cod, dados);
    }

    @DeleteMapping("/{cod}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desfaz o vínculo")
    public void excluir(@PathVariable Long cod) {
        vinculoService.excluir(cod);
    }
}
