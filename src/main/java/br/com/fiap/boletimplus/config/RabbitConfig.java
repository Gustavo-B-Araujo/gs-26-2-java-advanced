package br.com.fiap.boletimplus.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "boletim.eventos";
    public static final String FILA_ALERTAS = "boletim.alertas";
    public static final String CHAVE_ALERTA = "alerta.desempenho";

    @Bean
    TopicExchange boletimEventos() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue filaAlertas() {
        return QueueBuilder.durable(FILA_ALERTAS).build();
    }

    @Bean
    Binding alertasNaFila(Queue filaAlertas, TopicExchange boletimEventos) {
        return BindingBuilder.bind(filaAlertas).to(boletimEventos).with(CHAVE_ALERTA);
    }

    @Bean
    MessageConverter mensagensEmJson() {
        return new JacksonJsonMessageConverter();
    }
}
