package br.com.fiap.boletimplus;

import br.com.fiap.boletimplus.messaging.AlertaDesempenhoEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BoletimControllerTest extends ApiTestBase {

    @Test
    void responsavelVeOBoletimDoDependente() throws Exception {
        mvc.perform(get("/alunos/1/boletim").header("Authorization", token(CPF_MARGE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Bart Simpson"))
                .andExpect(jsonPath("$.turmas.length()").value(3))
                .andExpect(jsonPath("$.turmas[0].turma.tipoCurso").value("Português"))
                .andExpect(jsonPath("$.turmas[0].media").value(52.5))
                .andExpect(jsonPath("$.turmas[0].frequencia").value(1.0 / 3))
                .andExpect(jsonPath("$.turmas[0].notas[0].dataAvaliacao").value("2026-09-15"))
                .andExpect(jsonPath("$.turmas[0].presencas[0].tipo").value("Falta Justificada"));
    }

    @Test
    void responsavelNaoVeAlunoSemVinculo() throws Exception {
        mvc.perform(get("/alunos/3/boletim").header("Authorization", token(CPF_MARGE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void professoraVeAlunoDaTurmaDela() throws Exception {
        mvc.perform(get("/alunos/3/boletim").header("Authorization", token(CPF_EDNA)))
                .andExpect(status().isOk());
    }

    @Test
    @SuppressWarnings("unchecked")
    void analiseDevolveOTextoDaIaEPublicaOsAlertas() throws Exception {
        when(chatClient.prompt().user(any(Consumer.class)).call().content()).thenReturn("  Bart precisa de atenção.  ");

        mvc.perform(post("/alunos/1/boletim/analise").header("Authorization", token(CPF_MARGE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("Bart precisa de atenção."))
                .andExpect(jsonPath("$.geradoEm").isString());

        var eventos = ArgumentCaptor.forClass(AlertaDesempenhoEvent.class);
        verify(alertaPublisher, times(3)).publicar(eventos.capture());
        assertThat(eventos.getAllValues()).extracting(AlertaDesempenhoEvent::materia)
                .containsExactlyInAnyOrder("Português", "Matemática", "História");
    }

    @Test
    @SuppressWarnings("unchecked")
    void falhaNoLlmResponde503() throws Exception {
        when(chatClient.prompt().user(any(Consumer.class)).call().content())
                .thenThrow(new IllegalStateException("Ollama fora do ar"));

        mvc.perform(post("/alunos/1/boletim/analise").header("Authorization", token(CPF_BART)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem").value("A análise com IA está indisponível agora. Tente de novo mais tarde."));
    }
}
