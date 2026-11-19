package br.com.fiap.boletimplus;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** POST repetido com a mesma Idempotency-Key não duplica o cadastro. */
class IdempotenciaTest extends ApiTestBase {

    private static final String NOVO_PROFESSOR =
            "{\"nome\":\"Dewey Largo\",\"num_documento\":\"52998224725\",\"senha\":\"musica2026\"}";

    @Test
    void mesmaChaveDevolveAMesmaRespostaSemCriarDeNovo() throws Exception {
        String diretor = token(CPF_SKINNER);
        String primeira = mvc.perform(post("/professores").header("Authorization", diretor)
                        .header("Idempotency-Key", "idem-prof-1")
                        .contentType(MediaType.APPLICATION_JSON).content(NOVO_PROFESSOR))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andReturn().getResponse().getContentAsString();
        String segunda = mvc.perform(post("/professores").header("Authorization", diretor)
                        .header("Idempotency-Key", "idem-prof-1")
                        .contentType(MediaType.APPLICATION_JSON).content(NOVO_PROFESSOR))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();

        assertEquals(corpo(primeira).get("professorCod").asLong(), corpo(segunda).get("professorCod").asLong());
        mvc.perform(get("/professores").header("Authorization", diretor))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void mesmaChaveComOutroCorpoResponde422() throws Exception {
        String diretor = token(CPF_SKINNER);
        mvc.perform(post("/professores").header("Authorization", diretor).header("Idempotency-Key", "idem-prof-2")
                        .contentType(MediaType.APPLICATION_JSON).content(NOVO_PROFESSOR))
                .andExpect(status().isCreated());
        mvc.perform(post("/professores").header("Authorization", diretor).header("Idempotency-Key", "idem-prof-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Otto Mann\",\"num_documento\":\"11144477735\",\"senha\":\"onibus2026\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void semAChaveResponde400() throws Exception {
        mvc.perform(post("/professores").header("Authorization", token(CPF_SKINNER))
                        .contentType(MediaType.APPLICATION_JSON).content(NOVO_PROFESSOR))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(
                        "Envie o header Idempotency-Key (até 255 caracteres), um valor novo por operação."));
    }

    @Test
    void avaliacaoRepetidaComAMesmaChaveVoltaAMesmaAvaliacao() throws Exception {
        String edna = token(CPF_EDNA);
        String corpo = "{\"nome\":\"Prova 2\",\"codTipoNota\":1,\"dataAvaliacao\":\"2026-11-20\"}";
        long[] cods = new long[2];
        for (int i = 0; i < 2; i++) {
            String resposta = mvc.perform(post("/turmas/1/avaliacoes").header("Authorization", edna)
                            .header("Idempotency-Key", "idem-av-1")
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            cods[i] = corpo(resposta).get("cod").asLong();
        }
        assertEquals(cods[0], cods[1]);
        mvc.perform(get("/turmas/1").header("Authorization", edna))
                .andExpect(jsonPath("$.avaliacoes.length()").value(3));
    }

    @Test
    void aMesmaChaveEmOutraTurmaNaoReaproveitaAResposta() throws Exception {
        String diretor = token(CPF_SKINNER);
        String corpo = "{\"cod_tipo_curso\":1,\"cod_professor\":1,\"semestre_referencia\":1,\"ano_referencia\":2027}";
        mvc.perform(post("/escolas/1/turmas").header("Authorization", diretor).header("Idempotency-Key", "idem-turma")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());
        mvc.perform(post("/escolas/1/turmas").header("Authorization", diretor).header("Idempotency-Key", "idem-turma")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"));
        mvc.perform(get("/escolas/1").header("Authorization", diretor))
                .andExpect(jsonPath("$.totalTurmas").value(4));
    }
}
