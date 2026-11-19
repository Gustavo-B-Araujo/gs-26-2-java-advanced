package br.com.fiap.boletimplus.controller;

import br.com.fiap.boletimplus.dto.AlterarNomeRequest;
import br.com.fiap.boletimplus.dto.AlterarSenhaRequest;
import br.com.fiap.boletimplus.dto.CadastroRequest;
import br.com.fiap.boletimplus.dto.LoginRequest;
import br.com.fiap.boletimplus.dto.PessoaResponse;
import br.com.fiap.boletimplus.dto.SessaoResponse;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import br.com.fiap.boletimplus.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Conta", description = "Login, cadastro de responsável, senha e nome")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Entra com CPF e senha", description = "Devolve o JWT e os papéis. 401: CPF ou senha inválidos. 403: usuário não Ativo.")
    public SessaoResponse login(@RequestBody @Valid LoginRequest dados) {
        return authService.login(dados);
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Cadastra um responsável", description = "Cria Pessoa, Usuario Ativo e Guardiao. 409: CPF já cadastrado.")
    public SessaoResponse cadastrar(@RequestBody @Valid CadastroRequest dados) {
        return authService.cadastrar(dados);
    }

    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Altera a própria senha", description = "400: senha atual errada ou nova senha fora da regra.")
    public void alterarSenha(@AuthenticationPrincipal UsuarioAutenticado quem,
                             @RequestBody @Valid AlterarSenhaRequest dados) {
        authService.alterarSenha(quem, dados);
    }

    @PutMapping("/nome")
    @PreAuthorize("hasAnyRole('DIRETOR', 'GUARDIAO')")
    @Operation(summary = "Altera o próprio nome", description = "Só diretor e responsável.")
    public PessoaResponse alterarNome(@AuthenticationPrincipal UsuarioAutenticado quem,
                                      @RequestBody @Valid AlterarNomeRequest dados) {
        return authService.alterarNome(quem, dados);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Sai", description = "O JWT é stateless: o app apaga o token. O servidor só confirma.")
    public void logout() {
    }
}
