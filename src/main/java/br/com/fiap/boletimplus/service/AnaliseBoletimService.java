package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.dto.AnaliseBoletimResponse;
import br.com.fiap.boletimplus.dto.BoletimAlunoResponse;
import br.com.fiap.boletimplus.dto.BoletimTurmaResponse;
import br.com.fiap.boletimplus.exception.ApiException;
import br.com.fiap.boletimplus.messaging.AlertaDesempenhoEvent;
import br.com.fiap.boletimplus.messaging.AlertaPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Análise do boletim com IA (Spring AI + Ollama):
 * 1. monta o boletim do banco; 2. publica um alerta por matéria em atenção (RabbitMQ);
 * 3. manda o boletim em texto ao LLM e devolve a orientação para a família.
 */
@Service
public class AnaliseBoletimService {

    static final double MEDIA_ATENCAO = 60.0;
    static final double FREQUENCIA_ATENCAO = 0.75;

    private static final Logger log = LoggerFactory.getLogger(AnaliseBoletimService.class);

    private final BoletimService boletimService;
    private final AlertaPublisher alertas;
    private final ChatClient chatClient;

    public AnaliseBoletimService(BoletimService boletimService, AlertaPublisher alertas, ChatClient chatClient) {
        this.boletimService = boletimService;
        this.alertas = alertas;
        this.chatClient = chatClient;
    }

    public AnaliseBoletimResponse analisar(Long alunoCod) {
        BoletimAlunoResponse boletim = boletimService.montar(alunoCod);
        if (boletim.turmas().isEmpty()) {
            throw ApiException.requisicaoInvalida("O aluno ainda não tem turmas para analisar.");
        }
        alertasDe(boletim).forEach(alertas::publicar);

        String texto;
        try {
            texto = chatClient.prompt()
                    .user(u -> u.text("Analise o boletim abaixo.\n\n{boletim}").param("boletim", descrever(boletim)))
                    .call()
                    .content();
        } catch (RuntimeException e) {
            log.warn("Falha ao chamar o LLM para o aluno {}: {}", alunoCod, e.getMessage());
            throw ApiException.indisponivel("A análise com IA está indisponível agora. Tente de novo mais tarde.");
        }
        if (texto == null || texto.isBlank()) {
            throw ApiException.indisponivel("A IA não gerou a análise. Tente de novo.");
        }
        return new AnaliseBoletimResponse(texto.strip(), Instant.now());
    }

    /** Uma matéria pede atenção com média abaixo de 60 ou frequência abaixo de 75%. */
    static List<AlertaDesempenhoEvent> alertasDe(BoletimAlunoResponse boletim) {
        List<AlertaDesempenhoEvent> lista = new ArrayList<>();
        for (BoletimTurmaResponse t : boletim.turmas()) {
            motivo(t).ifPresent(motivo -> lista.add(new AlertaDesempenhoEvent(
                    boletim.alunoCod(), boletim.nome(), t.turma().cod(), t.turma().tipoCurso(),
                    t.media(), t.frequencia(), motivo)));
        }
        return lista;
    }

    static Optional<String> motivo(BoletimTurmaResponse t) {
        List<String> motivos = new ArrayList<>();
        if (t.media() != null && t.media() < MEDIA_ATENCAO) {
            motivos.add("média " + numero(t.media()));
        }
        if (t.frequencia() != null && t.frequencia() < FREQUENCIA_ATENCAO) {
            motivos.add("frequência " + Math.round(t.frequencia() * 100) + "%");
        }
        return motivos.isEmpty() ? Optional.empty() : Optional.of(String.join(" e ", motivos));
    }

    /**
     * Boletim em texto curto para o prompt: uma linha por matéria, só média e frequência, já com o
     * veredito. Menos dados = menos chance de o modelo trocar números. Só o primeiro nome; nada de CPF.
     */
    static String descrever(BoletimAlunoResponse boletim) {
        StringBuilder sb = new StringBuilder();
        sb.append("Aluno: ").append(boletim.nome().split(" ")[0]).append('\n');
        for (BoletimTurmaResponse t : boletim.turmas()) {
            sb.append("- ").append(t.turma().tipoCurso()).append(": ");
            if (t.media() == null) {
                sb.append("ainda sem notas");
            } else {
                sb.append("média ").append(numero(t.media()))
                        .append(t.media() < MEDIA_ATENCAO ? " (abaixo de 60)" : " (boa)");
            }
            sb.append("; ");
            if (t.frequencia() == null) {
                sb.append("frequência ainda não registrada");
            } else {
                sb.append("frequência ").append(Math.round(t.frequencia() * 100)).append('%')
                        .append(t.frequencia() < FREQUENCIA_ATENCAO ? " (abaixo de 75%)" : " (boa)");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String numero(double valor) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f", valor);
    }
}
