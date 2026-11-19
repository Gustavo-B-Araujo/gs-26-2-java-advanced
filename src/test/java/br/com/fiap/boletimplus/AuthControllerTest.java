package br.com.fiap.boletimplus;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends ApiTestBase {

    @Test
    void loginDoDiretorDevolveTokenEPapeis() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"num_documento\":\"" + CPF_SKINNER + "\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.usuarioCod").value(1))
                .andExpect(jsonPath("$.pessoa.nome").value("Seymour Skinner"))
                .andExpect(jsonPath("$.pessoa.num_documento").value(CPF_SKINNER))
                .andExpect(jsonPath("$.papeis.diretorCod").value(1))
                .andExpect(jsonPath("$.papeis.alunoCod").doesNotExist());
    }

    @Test
    void senhaErradaResponde401() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"num_documento\":\"" + CPF_SKINNER + "\",\"senha\":\"errada1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("CPF ou senha inválidos."));
    }

    @Test
    void usuarioBloqueadoResponde403() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"num_documento\":\"" + CPF_HOMER + "\",\"senha\":\"123456\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Usuário bloqueado. Procure a secretaria."));
    }

    @Test
    void cadastroInvalidoResponde400ComOsCampos() throws Exception {
        mvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ned\",\"num_documento\":\"123\",\"senha\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(4));
    }

    @Test
    void cadastroCriaResponsavelELoga() throws Exception {
        mvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ned Flanders\",\"num_documento\":\"52998224725\",\"senha\":\"okily123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pessoa.nome").value("Ned Flanders"))
                .andExpect(jsonPath("$.papeis.guardiaoCod").isNumber());
    }

    @Test
    void cadastroComCpfRepetidoResponde409() throws Exception {
        mvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Marge Bouvier\",\"num_documento\":\"" + CPF_MARGE + "\",\"senha\":\"marge123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rotaProtegidaSemTokenResponde401() throws Exception {
        mvc.perform(put("/auth/nome").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Fulano de Tal\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Sessão expirada. Entre de novo."));
    }

    @Test
    void tokenAdulteradoResponde401() throws Exception {
        String token = token(CPF_SKINNER) + "x";
        mvc.perform(put("/auth/nome").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Fulano de Tal\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void alunoNaoAlteraOProprioNome() throws Exception {
        mvc.perform(put("/auth/nome").header("Authorization", token(CPF_BART))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"El Barto\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void responsavelAlteraOProprioNome() throws Exception {
        mvc.perform(put("/auth/nome").header("Authorization", token(CPF_MARGE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Marjorie Simpson\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Marjorie Simpson"));
    }

    @Test
    void senhaAtualErradaResponde400() throws Exception {
        mvc.perform(put("/auth/senha").header("Authorization", token(CPF_MARGE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senha_atual\":\"errada99\",\"nova_senha\":\"nova12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A senha atual não confere."));
    }
}
