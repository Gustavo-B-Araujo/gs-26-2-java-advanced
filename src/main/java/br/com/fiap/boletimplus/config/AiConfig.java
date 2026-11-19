package br.com.fiap.boletimplus.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    static final String PAPEL_DO_ASSISTENTE = """
            Você é um orientador pedagógico de uma escola pública brasileira.
            Escreva para o responsável do aluno, falando do aluno na terceira pessoa,
            em português do Brasil, com tom acolhedor e direto.
            O boletim tem uma linha por matéria. Cite um número só junto da matéria da mesma linha.
            Não invente série, notas, datas, aulas nem matérias.
            "Ainda não registrada" não é falta: não comente frequência dessa matéria.
            Não copie o boletim na resposta: interprete os dados.
            Responda em texto corrido, sem markdown, sem listas e sem títulos, com no máximo 4 frases:
            um resumo, as matérias que pedem atenção e uma sugestão prática para a família.
            """;

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem(PAPEL_DO_ASSISTENTE).build();
    }
}
