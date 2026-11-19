package br.com.fiap.boletimplus;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GuardiaoEVinculoControllerTest extends ApiTestBase {

    @Test
    void diretorListaResponsaveisPaginado() throws Exception {
        mvc.perform(get("/guardioes?size=2&page=0").header("Authorization", token(CPF_SKINNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(2))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.conteudo[0].nome").value("Homer Simpson"))
                .andExpect(jsonPath("$.conteudo[0].totalDependentes").value(2));
    }

    @Test
    void responsavelNaoAcessaOCrud() throws Exception {
        mvc.perform(get("/guardioes").header("Authorization", token(CPF_MARGE)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/vinculos").header("Authorization", token(CPF_MARGE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cicloCompletoDeResponsavelEVinculo() throws Exception {
        String diretor = token(CPF_SKINNER);

        String criado = mvc.perform(post("/guardioes").header("Authorization", diretor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Luann Van Houten\",\"num_documento\":\"11144477735\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        long guardiaoCod = corpo(criado).get("guardiaoCod").asLong();

        String vinculo = mvc.perform(post("/vinculos").header("Authorization", diretor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cod_guardiao\":" + guardiaoCod + ",\"cod_aluno\":3,\"cod_tipo_parentesco\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alunoNome").value("Milhouse Van Houten"))
                .andExpect(jsonPath("$.parentesco").value("Mãe"))
                .andReturn().getResponse().getContentAsString();
        long vinculoCod = corpo(vinculo).get("cod").asLong();

        mvc.perform(post("/vinculos").header("Authorization", diretor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cod_guardiao\":" + guardiaoCod + ",\"cod_aluno\":3,\"cod_tipo_parentesco\":2}"))
                .andExpect(status().isConflict());

        mvc.perform(put("/vinculos/" + vinculoCod).header("Authorization", diretor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cod_tipo_parentesco\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentesco").value("Representante Legal"));

        mvc.perform(get("/vinculos?alunoCod=3").header("Authorization", diretor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2));

        mvc.perform(delete("/guardioes/" + guardiaoCod).header("Authorization", diretor))
                .andExpect(status().isConflict());

        mvc.perform(delete("/vinculos/" + vinculoCod).header("Authorization", diretor))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/guardioes/" + guardiaoCod).header("Authorization", diretor))
                .andExpect(status().isNoContent());
        mvc.perform(get("/guardioes/" + guardiaoCod).header("Authorization", diretor))
                .andExpect(status().isNotFound());
    }

    @Test
    void vinculoComAlunoInexistenteResponde404() throws Exception {
        mvc.perform(post("/vinculos").header("Authorization", token(CPF_SKINNER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cod_guardiao\":1,\"cod_aluno\":999,\"cod_tipo_parentesco\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Aluno não encontrado."));
    }

    @Test
    void responsavelListaOsProprosDependentes() throws Exception {
        mvc.perform(get("/guardioes/2/dependentes").header("Authorization", token(CPF_MARGE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Bart Simpson"))
                .andExpect(jsonPath("$[0].parentesco").value("Mãe"));
        mvc.perform(get("/guardioes/1/dependentes").header("Authorization", token(CPF_MARGE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tiposDeParentesco() throws Exception {
        mvc.perform(get("/tipos/parentesco").header("Authorization", token(CPF_SKINNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].enumerador").value("Mãe"));
    }
}
