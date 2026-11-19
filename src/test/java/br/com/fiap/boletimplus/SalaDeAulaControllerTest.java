package br.com.fiap.boletimplus;

import br.com.fiap.boletimplus.service.RegrasDeNegocio;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Turma, avaliações, notas e chamada (professor da turma). */
class SalaDeAulaControllerTest extends ApiTestBase {

    @Test
    void turmaTrazAlunosEAvaliacoesEmOrdem() throws Exception {
        mvc.perform(get("/turmas/1").header("Authorization", token(CPF_EDNA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoCurso").value("Português"))
                .andExpect(jsonPath("$.totalAlunos").value(2))
                .andExpect(jsonPath("$.alunos[0].nome").value("Bart Simpson"))
                .andExpect(jsonPath("$.alunos[0].turmaAlunoCod").value(1))
                .andExpect(jsonPath("$.alunos[1].nome").value("Milhouse Van Houten"))
                .andExpect(jsonPath("$.avaliacoes[0].nome").value("Prova 1"))
                .andExpect(jsonPath("$.avaliacoes[0].dataAvaliacao").value("2026-09-15"))
                .andExpect(jsonPath("$.avaliacoes[0].tipoNota").value("Prova"))
                .andExpect(jsonPath("$.avaliacoes[0].notasLancadas").value(2))
                .andExpect(jsonPath("$.avaliacoes[1].nome").value("Trabalho 1"));
    }

    @Test
    void leituraDaTurma() throws Exception {
        mvc.perform(get("/turmas/3").header("Authorization", token(CPF_MARGE))).andExpect(status().isOk());
        mvc.perform(get("/turmas/1").header("Authorization", token(CPF_BART))).andExpect(status().isOk());
        mvc.perform(get("/turmas/3").header("Authorization", token(CPF_EDNA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Você não tem acesso a esta turma."));
        mvc.perform(get("/turmas/99").header("Authorization", token(CPF_EDNA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void professorCriaEditaEExcluiAvaliacao() throws Exception {
        String edna = token(CPF_EDNA);
        String criada = mvc.perform(post("/turmas/1/avaliacoes").header("Authorization", edna)
                        .header("Idempotency-Key", "av-ciclo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Prova 2\",\"codTipoNota\":1,\"dataAvaliacao\":\"2026-11-20\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long cod = corpo(criada).get("cod").asLong();

        mvc.perform(put("/avaliacoes/" + cod).header("Authorization", edna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Prova 2 (recuperação)\",\"codTipoNota\":2,\"dataAvaliacao\":null}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/avaliacoes/" + cod).header("Authorization", edna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Prova 2 (recuperação)"))
                .andExpect(jsonPath("$.tipoNota").value("Trabalho"))
                .andExpect(jsonPath("$.codTipoNota").value(2))
                .andExpect(jsonPath("$.dataAvaliacao").isEmpty())
                .andExpect(jsonPath("$.notasLancadas").value(0))
                .andExpect(jsonPath("$.turma.cod").value(1))
                .andExpect(jsonPath("$.notas.length()").value(2))
                .andExpect(jsonPath("$.notas[0].nota").isEmpty());
        mvc.perform(delete("/avaliacoes/" + cod).header("Authorization", edna))
                .andExpect(status().isNoContent());
        mvc.perform(get("/avaliacoes/" + cod).header("Authorization", edna))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Avaliação não encontrada."));
    }

    @Test
    void excluirAvaliacaoApagaAsNotasAntes() throws Exception {
        String edna = token(CPF_EDNA);
        mvc.perform(delete("/avaliacoes/1").header("Authorization", edna)).andExpect(status().isNoContent());
        mvc.perform(get("/alunos/1/boletim").header("Authorization", edna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turmas[0].notas.length()").value(1));
    }

    @Test
    void nomeDeAvaliacaoRepetidoNaTurmaResponde409() throws Exception {
        mvc.perform(post("/turmas/1/avaliacoes").header("Authorization", token(CPF_EDNA))
                        .header("Idempotency-Key", "av-repetida").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Prova 1\",\"codTipoNota\":1,\"dataAvaliacao\":null}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Já existe uma avaliação com este nome nesta turma."));
    }

    @Test
    void soOProfessorDaTurmaCriaAvaliacao() throws Exception {
        String corpo = "{\"nome\":\"Prova X\",\"codTipoNota\":1,\"dataAvaliacao\":null}";
        mvc.perform(post("/turmas/1/avaliacoes").header("Authorization", token(CPF_SKINNER))
                        .header("Idempotency-Key", "av-x1").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/turmas/1/avaliacoes").header("Authorization", token(CPF_BART))
                        .header("Idempotency-Key", "av-x2").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/turmas/3/avaliacoes").header("Authorization", token(CPF_EDNA))
                        .header("Idempotency-Key", "av-x3").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Só o professor da turma pode lançar dados."));
    }

    @Test
    void avaliacaoSemNomeOuComTipoInvalidoResponde400() throws Exception {
        String edna = token(CPF_EDNA);
        mvc.perform(post("/turmas/1/avaliacoes").header("Authorization", edna).header("Idempotency-Key", "av-nome")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\" \",\"codTipoNota\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O nome precisa ter de 1 a 50 caracteres."));
        mvc.perform(post("/turmas/1/avaliacoes").header("Authorization", edna).header("Idempotency-Key", "av-tipo")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Prova 9\",\"codTipoNota\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Tipo de nota inválido."));
    }

    @Test
    void notasFazemUpsertENotaNulaApaga() throws Exception {
        String edna = token(CPF_EDNA);
        mvc.perform(put("/avaliacoes/1/notas").header("Authorization", edna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notas\":[{\"turmaAlunoCod\":1,\"nota\":88.456},{\"turmaAlunoCod\":2,\"nota\":null}]}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/avaliacoes/1").header("Authorization", edna))
                .andExpect(jsonPath("$.notasLancadas").value(1))
                .andExpect(jsonPath("$.notas[0].turmaAlunoCod").value(1))
                .andExpect(jsonPath("$.notas[0].nota").value(88.46))
                .andExpect(jsonPath("$.notas[1].nota").isEmpty());

        mvc.perform(put("/avaliacoes/1/notas").header("Authorization", edna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notas\":[{\"turmaAlunoCod\":2,\"nota\":70}]}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/avaliacoes/1").header("Authorization", edna))
                .andExpect(jsonPath("$.notasLancadas").value(2))
                .andExpect(jsonPath("$.notas[1].nota").value(70.0));
    }

    @Test
    void notaInvalidaResponde400() throws Exception {
        String edna = token(CPF_EDNA);
        mvc.perform(put("/avaliacoes/1/notas").header("Authorization", edna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notas\":[{\"turmaAlunoCod\":1,\"nota\":100.5}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A nota precisa estar entre 0 e 100."));
        mvc.perform(put("/avaliacoes/1/notas").header("Authorization", edna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notas\":[{\"turmaAlunoCod\":5,\"nota\":50}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Aluno fora da turma."));
        mvc.perform(put("/avaliacoes/5/notas").header("Authorization", edna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notas\":[{\"turmaAlunoCod\":5,\"nota\":50}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void chamadaDoDiaTrazAPresencaLancada() throws Exception {
        mvc.perform(get("/turmas/1/chamada?data=2026-11-05").header("Authorization", token(CPF_EDNA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].alunoNome").value("Bart Simpson"))
                .andExpect(jsonPath("$[0].codTipoPresenca").value(3))
                .andExpect(jsonPath("$[0].observacao").value("Atestado médico"))
                .andExpect(jsonPath("$[1].alunoNome").value("Milhouse Van Houten"))
                .andExpect(jsonPath("$[1].codTipoPresenca").isEmpty());
    }

    @Test
    void salvarChamadaFazUpsertPorAlunoEData() throws Exception {
        String edna = token(CPF_EDNA);
        String corpo = "{\"itens\":[{\"turmaAlunoCod\":1,\"codTipoPresenca\":1,\"observacao\":null},"
                + "{\"turmaAlunoCod\":2,\"codTipoPresenca\":2,\"observacao\":\" Chegou atrasado \"}]}";
        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/turmas/1/chamada?data=2026-11-04").header("Authorization", edna)
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isNoContent());
        }
        mvc.perform(get("/turmas/1/chamada?data=2026-11-04").header("Authorization", edna))
                .andExpect(jsonPath("$[0].codTipoPresenca").value(1))
                .andExpect(jsonPath("$[1].codTipoPresenca").value(2))
                .andExpect(jsonPath("$[1].observacao").value("Chegou atrasado"));
        mvc.perform(get("/alunos/3/boletim").header("Authorization", edna))
                .andExpect(jsonPath("$.turmas[0].presencas.length()").value(2));
    }

    @Test
    void chamadaEmDataFuturaOuFormatoErradoResponde400() throws Exception {
        String edna = token(CPF_EDNA);
        String amanha = RegrasDeNegocio.hoje().plusDays(1).toString();
        mvc.perform(put("/turmas/1/chamada?data=" + amanha).header("Authorization", edna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\":[{\"turmaAlunoCod\":1,\"codTipoPresenca\":1,\"observacao\":null}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Não é permitido registrar presença em data futura."));
        mvc.perform(get("/turmas/1/chamada?data=05-11-2026").header("Authorization", edna))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Data inválida: use o formato YYYY-MM-DD."));
        mvc.perform(get("/turmas/1/chamada").header("Authorization", edna))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/turmas/1/chamada?data=2026-11-04").header("Authorization", edna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\":[{\"turmaAlunoCod\":1,\"codTipoPresenca\":9}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Tipo de presença inválido."));
    }

    @Test
    void responsavelNaoLancaChamada() throws Exception {
        mvc.perform(put("/turmas/1/chamada?data=2026-11-04").header("Authorization", token(CPF_MARGE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"itens\":[]}"))
                .andExpect(status().isForbidden());
    }
}
