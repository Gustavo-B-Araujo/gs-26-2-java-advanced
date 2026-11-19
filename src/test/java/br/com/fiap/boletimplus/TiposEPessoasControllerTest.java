package br.com.fiap.boletimplus;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tipos (presença, nota, curso), professores e alunos da rede. */
class TiposEPessoasControllerTest extends ApiTestBase {

    @Test
    void qualquerLogadoListaOsTipos() throws Exception {
        String bart = token(CPF_BART);
        mvc.perform(get("/tipos/presenca").header("Authorization", bart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[2].enumerador").value("Falta Justificada"))
                .andExpect(jsonPath("$[0].data_criacao").exists());
        mvc.perform(get("/tipos/nota").header("Authorization", bart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enumerador").value("Prova"));
        mvc.perform(get("/tipos/curso").header("Authorization", bart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].enumerador").value("Matemática"));
    }

    @Test
    void semTokenResponde401() throws Exception {
        mvc.perform(get("/tipos/nota")).andExpect(status().isUnauthorized());
    }

    @Test
    void diretorCadastraCursoERepetidoSemAcentoResponde409() throws Exception {
        String diretor = token(CPF_SKINNER);
        mvc.perform(post("/tipos/curso").header("Authorization", diretor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enumerador\":\" Geografia \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cod").isNumber())
                .andExpect(jsonPath("$.enumerador").value("Geografia"))
                .andExpect(jsonPath("$.data_criacao").exists());
        mvc.perform(post("/tipos/curso").header("Authorization", diretor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enumerador\":\"historia\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Este curso já existe."));
        mvc.perform(post("/tipos/curso").header("Authorization", diretor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enumerador\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O nome precisa ter de 1 a 30 caracteres."));
    }

    @Test
    void professorNaoCadastraCurso() throws Exception {
        mvc.perform(post("/tipos/curso").header("Authorization", token(CPF_EDNA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enumerador\":\"Geografia\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void diretorListaProfessoresComMaterias() throws Exception {
        mvc.perform(get("/professores").header("Authorization", token(CPF_SKINNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Edna Krabappel"))
                .andExpect(jsonPath("$[0].professorCod").value(1))
                .andExpect(jsonPath("$[0].materias[0]").value("Matemática"))
                .andExpect(jsonPath("$[0].materias[1]").value("Português"))
                .andExpect(jsonPath("$[1].materias[0]").value("História"));
        mvc.perform(get("/professores").header("Authorization", token(CPF_EDNA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void diretorCadastraProfessorQueJaEntraNoApp() throws Exception {
        mvc.perform(post("/professores").header("Authorization", token(CPF_SKINNER))
                        .header("Idempotency-Key", "prof-ok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Dewey Largo\",\"num_documento\":\"52998224725\",\"senha\":\"musica2026\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/professores/")))
                .andExpect(jsonPath("$.professorCod").isNumber());

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"num_documento\":\"52998224725\",\"senha\":\"musica2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papeis.professorCod").isNumber());
    }

    @Test
    void cadastroDeProfessorValidaCpfESenhaERecusaCpfRepetido() throws Exception {
        String diretor = token(CPF_SKINNER);
        mvc.perform(post("/professores").header("Authorization", diretor).header("Idempotency-Key", "prof-cpf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Dewey Largo\",\"num_documento\":\"52998224726\",\"senha\":\"musica2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("CPF inválido."));
        mvc.perform(post("/professores").header("Authorization", diretor).header("Idempotency-Key", "prof-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Dewey Largo\",\"num_documento\":\"52998224725\",\"senha\":\"somenteletras\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Use letras e números."));
        mvc.perform(post("/alunos").header("Authorization", diretor).header("Idempotency-Key", "prof-aluno")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Nelson Muntz\",\"num_documento\":\"52998224725\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/professores").header("Authorization", diretor).header("Idempotency-Key", "prof-repetido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Nelson Muntz\",\"num_documento\":\"52998224725\",\"senha\":\"skate2026\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Este CPF já tem cadastro."));
    }

    @Test
    void diretorRenomeiaProfessorEAluno() throws Exception {
        String diretor = token(CPF_SKINNER);
        mvc.perform(put("/professores/2").header("Authorization", diretor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Elizabeth Hoover Silva\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(put("/alunos/3").header("Authorization", diretor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Milhouse Mussolini Van Houten\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/alunos").header("Authorization", diretor))
                .andExpect(jsonPath("$[2].nome").value("Milhouse Mussolini Van Houten"));
        mvc.perform(put("/professores/99").header("Authorization", diretor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Ninguem Aqui\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Professor não encontrado."));
        mvc.perform(put("/alunos/1").header("Authorization", diretor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Bart\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe nome e sobrenome."));
    }

    @Test
    void diretorListaAlunosComTotalDeTurmas() throws Exception {
        mvc.perform(get("/alunos").header("Authorization", token(CPF_SKINNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nome").value("Bart Simpson"))
                .andExpect(jsonPath("$[0].numDocumento").value("12345678904"))
                .andExpect(jsonPath("$[0].totalTurmas").value(3))
                .andExpect(jsonPath("$[1].nome").value("Lisa Simpson"))
                .andExpect(jsonPath("$[1].totalTurmas").value(1));
        mvc.perform(get("/alunos").header("Authorization", token(CPF_MARGE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void alunoSemSenhaNaoGanhaUsuario() throws Exception {
        mvc.perform(post("/alunos").header("Authorization", token(CPF_SKINNER)).header("Idempotency-Key", "aluno-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Nelson Muntz\",\"num_documento\":\"11144477735\",\"senha\":null}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alunoCod").isNumber());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"num_documento\":\"11144477735\",\"senha\":\"qualquer123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void professorVeSoAsPropriasTurmas() throws Exception {
        String edna = token(CPF_EDNA);
        mvc.perform(get("/professores/1/turmas").header("Authorization", edna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].cod").value(1))
                .andExpect(jsonPath("$[0].tipoCurso").value("Português"))
                .andExpect(jsonPath("$[0].totalAlunos").value(2));
        mvc.perform(get("/professores/2/turmas").header("Authorization", edna))
                .andExpect(status().isForbidden());
    }

    @Test
    void boletimContinuaNaMesmaRotaDeAlunos() throws Exception {
        mvc.perform(get("/alunos/1/boletim").header("Authorization", token(CPF_BART)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Bart Simpson"));
    }
}
