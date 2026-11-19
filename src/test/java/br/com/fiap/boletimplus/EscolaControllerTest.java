package br.com.fiap.boletimplus;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Gestão escolar: escolas do diretor, turmas, professores e alunos da escola, matrícula. */
class EscolaControllerTest extends ApiTestBase {

    private static final String NOVA_TURMA =
            "{\"cod_tipo_curso\":3,\"cod_professor\":1,\"semestre_referencia\":1,\"ano_referencia\":2027}";

    @Test
    void diretorVeAsPropriasEscolas() throws Exception {
        String diretor = token(CPF_SKINNER);
        mvc.perform(get("/diretores/1/escolas").header("Authorization", diretor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cod").value(1))
                .andExpect(jsonPath("$[0].nome").value("Escola Primária de Springfield"))
                .andExpect(jsonPath("$[0].diretorNome").value("Seymour Skinner"))
                .andExpect(jsonPath("$[0].totalTurmas").value(3));
        mvc.perform(get("/diretores/2/escolas").header("Authorization", diretor))
                .andExpect(status().isForbidden());
        mvc.perform(get("/escolas/1").header("Authorization", diretor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTurmas").value(3));
        mvc.perform(get("/escolas/99").header("Authorization", diretor))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Escola não encontrada."));
    }

    @Test
    void soDiretorAcessaEscola() throws Exception {
        mvc.perform(get("/escolas/1/turmas").header("Authorization", token(CPF_EDNA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void turmasProfessoresEAlunosDaEscola() throws Exception {
        String diretor = token(CPF_SKINNER);
        mvc.perform(get("/escolas/1/turmas").header("Authorization", diretor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].escolaNome").value("Escola Primária de Springfield"))
                .andExpect(jsonPath("$[0].professorNome").value("Edna Krabappel"))
                .andExpect(jsonPath("$[0].semestre").value(2))
                .andExpect(jsonPath("$[0].ano").value(2026));
        mvc.perform(get("/escolas/1/professores").header("Authorization", diretor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Edna Krabappel"))
                .andExpect(jsonPath("$[0].turmas.length()").value(2))
                .andExpect(jsonPath("$[1].nome").value("Elizabeth Hoover"))
                .andExpect(jsonPath("$[1].materias[0]").value("História"));
        mvc.perform(get("/escolas/1/alunos").header("Authorization", diretor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nome").value("Bart Simpson"))
                .andExpect(jsonPath("$[0].totalTurmas").value(3))
                .andExpect(jsonPath("$[0].turmas.length()").value(3))
                .andExpect(jsonPath("$[2].nome").value("Milhouse Van Houten"))
                .andExpect(jsonPath("$[2].turmas[0].cod").value(1));
    }

    @Test
    void diretorCriaTurma() throws Exception {
        String diretor = token(CPF_SKINNER);
        String criada = mvc.perform(post("/escolas/1/turmas").header("Authorization", diretor)
                        .header("Idempotency-Key", "turma-ok")
                        .contentType(MediaType.APPLICATION_JSON).content(NOVA_TURMA))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        long cod = corpo(criada).get("cod").asLong();

        mvc.perform(get("/escolas/1/turmas").header("Authorization", diretor))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].cod").value(cod))
                .andExpect(jsonPath("$[0].tipoCurso").value("História"))
                .andExpect(jsonPath("$[0].totalAlunos").value(0));
    }

    @Test
    void turmaComDadosInvalidosResponde400Ou404() throws Exception {
        String diretor = token(CPF_SKINNER);
        mvc.perform(post("/escolas/1/turmas").header("Authorization", diretor).header("Idempotency-Key", "turma-sem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cod_tipo_curso\":1,\"cod_professor\":1,\"semestre_referencia\":3,\"ano_referencia\":2027}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Semestre inválido: use 1 ou 2."));
        mvc.perform(post("/escolas/1/turmas").header("Authorization", diretor).header("Idempotency-Key", "turma-ano")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cod_tipo_curso\":1,\"cod_professor\":1,\"semestre_referencia\":1,\"ano_referencia\":1999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O ano precisa ser 2000 ou depois."));
        mvc.perform(post("/escolas/1/turmas").header("Authorization", diretor).header("Idempotency-Key", "turma-prof")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cod_tipo_curso\":1,\"cod_professor\":99,\"semestre_referencia\":1,\"ano_referencia\":2027}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Professor não encontrado."));
        mvc.perform(post("/escolas/99/turmas").header("Authorization", diretor).header("Idempotency-Key", "turma-esc")
                        .contentType(MediaType.APPLICATION_JSON).content(NOVA_TURMA))
                .andExpect(status().isNotFound());
    }

    @Test
    void matriculaIgnoraQuemJaEstaNaTurma() throws Exception {
        String diretor = token(CPF_SKINNER);
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/turmas/1/alunos").header("Authorization", diretor)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"cod_alunos\":[2,1]}"))
                    .andExpect(status().isNoContent());
        }
        mvc.perform(get("/turmas/1").header("Authorization", diretor))
                .andExpect(jsonPath("$.totalAlunos").value(3))
                .andExpect(jsonPath("$.alunos[1].nome").value("Lisa Simpson"));
    }

    @Test
    void matriculaValidaLista() throws Exception {
        String diretor = token(CPF_SKINNER);
        mvc.perform(post("/turmas/1/alunos").header("Authorization", diretor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cod_alunos\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Escolha ao menos um aluno."));
        mvc.perform(post("/turmas/1/alunos").header("Authorization", diretor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cod_alunos\":[2,999]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Aluno não encontrado."));
        mvc.perform(post("/turmas/99/alunos").header("Authorization", diretor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cod_alunos\":[2]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Turma não encontrada."));
        mvc.perform(post("/turmas/1/alunos").header("Authorization", token(CPF_EDNA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cod_alunos\":[2]}"))
                .andExpect(status().isForbidden());
    }
}
