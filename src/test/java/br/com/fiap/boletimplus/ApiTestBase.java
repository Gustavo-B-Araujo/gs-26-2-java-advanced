package br.com.fiap.boletimplus;

import br.com.fiap.boletimplus.messaging.AlertaPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a API inteira com H2 em modo Oracle, carregado com o mesmo 01_ddl.sql e 02_dml.sql.
 * Cada teste roda numa transação desfeita no fim. LLM e RabbitMQ são simulados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BancoDeTeste.class)
@Transactional
public abstract class ApiTestBase {

    protected static final String SENHA_SEED = "123456";
    protected static final String CPF_SKINNER = "12345678900";
    protected static final String CPF_EDNA = "12345678901";
    protected static final String CPF_HOMER = "12345678902";
    protected static final String CPF_MARGE = "12345678903";
    protected static final String CPF_BART = "12345678904";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JsonMapper json;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    protected ChatClient chatClient;

    @MockitoBean
    protected AlertaPublisher alertaPublisher;

    @BeforeEach
    void limparMocks() {
        org.mockito.Mockito.reset(alertaPublisher);
    }

    protected JsonNode corpo(String conteudo) {
        return json.readTree(conteudo);
    }

    protected String token(String cpf) throws Exception {
        String resposta = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"num_documento\":\"" + cpf + "\",\"senha\":\"" + SENHA_SEED + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + corpo(resposta).get("token").asString();
    }
}
