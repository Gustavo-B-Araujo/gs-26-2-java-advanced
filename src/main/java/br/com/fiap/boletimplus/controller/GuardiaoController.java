package br.com.fiap.boletimplus.controller;

import br.com.fiap.boletimplus.dto.AlterarNomeRequest;
import br.com.fiap.boletimplus.dto.DependenteResponse;
import br.com.fiap.boletimplus.dto.GuardiaoRequest;
import br.com.fiap.boletimplus.dto.GuardiaoResponse;
import br.com.fiap.boletimplus.dto.PaginaResponse;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import br.com.fiap.boletimplus.service.GuardiaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

import java.util.List;

@RestController
@RequestMapping("/guardioes")
@Tag(name = "Responsáveis", description = "CRUD de responsáveis (só diretor) e dependentes")
public class GuardiaoController {

    private final GuardiaoService guardiaoService;

    public GuardiaoController(GuardiaoService guardiaoService) {
        this.guardiaoService = guardiaoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('DIRETOR')")
    @Operation(summary = "Lista responsáveis, paginado", description = "Filtro opcional por nome. Ex.: ?nome=simp&page=0&size=10&sort=pessoa.nome")
    public PaginaResponse<GuardiaoResponse> listar(
            @RequestParam(required = false) String nome,
            @PageableDefault(size = 10, sort = "pessoa.nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return PaginaResponse.de(guardiaoService.listar(nome, pageable));
    }

    @GetMapping("/{cod}")
    @PreAuthorize("hasRole('DIRETOR')")
    @Operation(summary = "Busca um responsável")
    public GuardiaoResponse buscar(@PathVariable Long cod) {
        return guardiaoService.buscar(cod);
    }

    @PostMapping
    @PreAuthorize("hasRole('DIRETOR')")
    @Operation(summary = "Cadastra um responsável", description = "Sem senha, não cria acesso ao app. 409: CPF já é de um responsável.")
    public ResponseEntity<GuardiaoResponse> criar(@RequestBody @Valid GuardiaoRequest dados) {
        GuardiaoResponse criado = guardiaoService.criar(dados);
        var local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{cod}")
                .buildAndExpand(criado.guardiaoCod()).toUri();
        return ResponseEntity.created(local).body(criado);
    }

    @PutMapping("/{cod}")
    @PreAuthorize("hasRole('DIRETOR')")
    @Operation(summary = "Altera o nome do responsável")
    public GuardiaoResponse alterar(@PathVariable Long cod, @RequestBody @Valid AlterarNomeRequest dados) {
        return guardiaoService.alterarNome(cod, dados);
    }

    @DeleteMapping("/{cod}")
    @PreAuthorize("hasRole('DIRETOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui o responsável", description = "409 se ainda tem vínculos com alunos.")
    public void excluir(@PathVariable Long cod) {
        guardiaoService.excluir(cod);
    }

    @GetMapping("/{cod}/dependentes")
    @PreAuthorize("hasRole('GUARDIAO')")
    @Operation(summary = "Dependentes do responsável", description = "Só o próprio responsável.")
    public List<DependenteResponse> dependentes(@AuthenticationPrincipal UsuarioAutenticado quem,
                                                @PathVariable Long cod) {
        return guardiaoService.dependentes(quem, cod);
    }
}
