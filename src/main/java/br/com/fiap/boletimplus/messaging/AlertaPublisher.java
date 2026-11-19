package br.com.fiap.boletimplus.messaging;

import br.com.fiap.boletimplus.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Publica o alerta e segue: quem avisa os responsáveis é o consumidor, de forma assíncrona. */
@Component
public class AlertaPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlertaPublisher.class);

    private final RabbitTemplate rabbit;

    public AlertaPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void publicar(AlertaDesempenhoEvent evento) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.CHAVE_ALERTA, evento);
            log.info("Alerta publicado: aluno {} em {} ({})", evento.alunoCod(), evento.materia(), evento.motivo());
        } catch (AmqpException e) {
            log.warn("RabbitMQ indisponível; alerta do aluno {} não publicado: {}", evento.alunoCod(), e.getMessage());
        }
    }
}
